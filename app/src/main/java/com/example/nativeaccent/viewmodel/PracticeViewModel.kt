package com.example.nativeaccent.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativeaccent.BuildConfig
import com.example.nativeaccent.assessment.AssessmentOutcome
import com.example.nativeaccent.assessment.EmbeddedKeyCredentialsProvider
import com.example.nativeaccent.assessment.PronunciationAssessmentService
import com.example.nativeaccent.assessment.PronunciationResult
import com.example.nativeaccent.assessment.azure.AzurePronunciationAssessmentService
import com.example.nativeaccent.audio.AudioPlayer
import com.example.nativeaccent.audio.CoachVoice
import com.example.nativeaccent.audio.PlaybackSpeed
import com.example.nativeaccent.audio.RecordingResult
import com.example.nativeaccent.audio.RecordingStore
import com.example.nativeaccent.audio.WavRecorder
import com.example.nativeaccent.coaching.CoachTip
import com.example.nativeaccent.coaching.CoachTipProvider
import com.example.nativeaccent.coaching.StaticCoachTipProvider
import com.example.nativeaccent.data.LocalPracticeRepository
import com.example.nativeaccent.data.PracticeItem
import com.example.nativeaccent.data.PracticeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Where scoring of the latest take stands. */
sealed interface AnalysisStatus {
    data object Idle : AnalysisStatus
    data object Analyzing : AnalysisStatus

    /** The take itself was unusable (too short, silent, no speech found). */
    data class NeedsRetake(val message: String) : AnalysisStatus

    /** The take was fine but scoring failed. [canRetry]: sending it again may work. */
    data class Failed(val message: String, val canRetry: Boolean) : AnalysisStatus
}

/** Everything both screens render, in one place. */
data class PracticeUiState(
    val currentIndex: Int = 0,
    val currentItem: PracticeItem,
    val totalItems: Int,
    val isRecording: Boolean = false,
    val lastRecordingPath: String? = null,
    val playbackSpeed: PlaybackSpeed = PlaybackSpeed.NORMAL,
    val isCoachSpeaking: Boolean = false,
    val isPlayingAttempt: Boolean = false,
    val analysis: AnalysisStatus = AnalysisStatus.Idle,
    /** Score for [lastRecordingPath]; null until scoring succeeds. */
    val lastResult: PronunciationResult? = null,
    /** Index into [PronunciationResult.words] for the word whose sounds are shown. */
    val focusedWordIndex: Int? = null,
    val coachTip: CoachTip? = null,
    val isTipLoading: Boolean = false,
    val message: String? = null,
) {
    val hasPrevious: Boolean get() = currentIndex > 0
    val isAnalyzing: Boolean get() = analysis == AnalysisStatus.Analyzing
}

/** One-shot instructions for the navigation layer. */
sealed interface PracticeEvent {
    data class OpenAnalysis(val itemIndex: Int, val recordingPath: String) : PracticeEvent
}

/**
 * Single source of truth for the practice flow.
 *
 * Audio work is delegated to [WavRecorder], [AudioPlayer] and [CoachVoice],
 * scoring to [PronunciationAssessmentService], and advice to [CoachTipProvider];
 * this class only sequences them and folds the outcomes into [uiState].
 */
class PracticeViewModel(
    private val repository: PracticeRepository,
    private val recorder: WavRecorder,
    private val player: AudioPlayer,
    private val coach: CoachVoice,
    private val recordings: RecordingStore,
    private val assessment: PronunciationAssessmentService,
    private val tips: CoachTipProvider,
) : ViewModel() {

    private val items: List<PracticeItem> = repository.items

    private val _uiState = MutableStateFlow(
        PracticeUiState(currentItem = items.first(), totalItems = items.size)
    )
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private val _events = Channel<PracticeEvent>(Channel.BUFFERED)
    val events: Flow<PracticeEvent> = _events.receiveAsFlow()

    val currentItem: PracticeItem get() = _uiState.value.currentItem
    val currentIndex: Int get() = _uiState.value.currentIndex
    val isRecording: Boolean get() = _uiState.value.isRecording
    val lastRecordingPath: String? get() = _uiState.value.lastRecordingPath
    val lastResult: PronunciationResult? get() = _uiState.value.lastResult

    private var analysisJob: Job? = null
    private var tipJob: Job? = null

    init {
        coach.setSpeakingListener { speaking ->
            _uiState.update { it.copy(isCoachSpeaking = speaking) }
        }
    }

    // ---- Navigation between items -------------------------------------------------

    fun selectItem(index: Int) {
        if (index !in items.indices || index == currentIndex) return
        stopAllPlayback()
        recorder.cancel()
        cancelPendingWork()
        _uiState.update {
            it.copy(
                currentIndex = index,
                currentItem = items[index],
                isRecording = false,
                lastRecordingPath = null,
                message = null,
            ).clearedAnalysis()
        }
    }

    fun nextItem() = selectItem((currentIndex + 1).coerceAtMost(items.lastIndex))

    fun previousItem() = selectItem((currentIndex - 1).coerceAtLeast(0))

    /** Screen 2's mic button: same item, blank slate. */
    fun startOver() {
        stopAllPlayback()
        cancelPendingWork()
        recordings.deleteAttemptsFor(currentItem.id)
        _uiState.update { it.copy(lastRecordingPath = null, message = null).clearedAnalysis() }
    }

    // ---- Recording ----------------------------------------------------------------

    fun toggleRecording() {
        if (isRecording) stopRecording() else startRecording()
    }

    fun startRecording() {
        if (isRecording || _uiState.value.isAnalyzing) return
        stopAllPlayback()
        cancelPendingWork()
        // Replaces this item's previous take, so the cache holds one file per item.
        val file = recordings.newAttemptFile(currentItem.id)
        val started = recorder.start(file)
        _uiState.update {
            if (started) {
                it.copy(isRecording = true, lastRecordingPath = null, message = null).clearedAnalysis()
            } else {
                it.copy(isRecording = false, message = "Couldn't reach the microphone")
            }
        }
    }

    /** Stops the take and, if it is usable, sends it for scoring. */
    fun stopRecording() {
        if (!isRecording) return
        when (val result = recorder.stop()) {
            is RecordingResult.Success -> {
                val retake = when {
                    result.durationMs < MIN_TAKE_MS ->
                        "That was a bit short. Tap the mic, say the whole sentence, then tap again."
                    result.peakAmplitude < SILENCE_PEAK ->
                        "We couldn't hear anything. Check the mic isn't covered and try again."
                    else -> null
                }
                if (retake != null) {
                    result.file.delete()
                    _uiState.update {
                        it.copy(isRecording = false, lastRecordingPath = null, analysis = AnalysisStatus.NeedsRetake(retake))
                    }
                } else {
                    val path = result.file.absolutePath
                    _uiState.update { it.copy(isRecording = false, lastRecordingPath = path, message = null) }
                    analyze(path, openAnalysisWhenScored = true)
                }
            }

            is RecordingResult.Failure -> _uiState.update {
                it.copy(isRecording = false, lastRecordingPath = null, analysis = AnalysisStatus.NeedsRetake(result.reason))
            }
        }
    }

    // ---- Scoring ------------------------------------------------------------------

    /**
     * Screen 1's "Retry" after a failure (lands on Screen 2 when scored), and
     * Screen 2's "Get score" (already there, so [openAnalysisWhenScored] = false).
     */
    fun retryAnalysis(openAnalysisWhenScored: Boolean = true) {
        val path = lastRecordingPath ?: return
        analyze(path, openAnalysisWhenScored)
    }

    /** Screen 1's escape hatch when scoring is unavailable: compare by ear, no score. */
    fun compareWithoutScore() {
        val path = lastRecordingPath ?: return
        cancelPendingWork()
        _uiState.update { it.copy(analysis = AnalysisStatus.Idle) }
        _events.trySend(PracticeEvent.OpenAnalysis(currentIndex, path))
    }

    private fun analyze(path: String, openAnalysisWhenScored: Boolean) {
        analysisJob?.cancel()
        val item = currentItem
        val index = currentIndex
        _uiState.update { it.copy(analysis = AnalysisStatus.Analyzing).clearedResult() }

        analysisJob = viewModelScope.launch {
            val outcome = assessment.assess(path, item.text)
            // Drop results for a take the learner has already moved on from.
            if (currentIndex != index || lastRecordingPath != path) return@launch

            when (outcome) {
                is AssessmentOutcome.Scored -> {
                    _uiState.update {
                        it.copy(
                            analysis = AnalysisStatus.Idle,
                            lastResult = outcome.result,
                            focusedWordIndex = outcome.result.weakestWordIndex,
                            coachTip = null,
                        )
                    }
                    if (openAnalysisWhenScored) _events.send(PracticeEvent.OpenAnalysis(index, path))
                }

                is AssessmentOutcome.NoSpeech -> {
                    recordings.deleteAttemptsFor(item.id)
                    _uiState.update {
                        it.copy(lastRecordingPath = null, analysis = AnalysisStatus.NeedsRetake(outcome.message))
                    }
                }

                is AssessmentOutcome.Failed -> _uiState.update {
                    it.copy(analysis = AnalysisStatus.Failed(outcome.error.userMessage, outcome.error.retryable))
                }
            }
        }
    }

    fun focusWord(index: Int) {
        val result = lastResult ?: return
        if (index !in result.words.indices || index == _uiState.value.focusedWordIndex) return
        tipJob?.cancel()
        _uiState.update { it.copy(focusedWordIndex = index, coachTip = null, isTipLoading = false) }
    }

    fun requestCoachTip() {
        val result = lastResult ?: return
        val state = _uiState.value
        val word = state.focusedWordIndex?.let(result.words::getOrNull)
        tipJob?.cancel()
        _uiState.update { it.copy(isTipLoading = true) }
        tipJob = viewModelScope.launch {
            val tip = tips.tipFor(state.currentItem.text, word, result)
            _uiState.update { it.copy(coachTip = tip, isTipLoading = false) }
        }
    }

    // ---- Playback -----------------------------------------------------------------

    /** The speaker icon on Screen 1 and the "Coach" button on Screen 2. */
    fun playCoach() {
        if (isRecording) return
        player.stop()
        _uiState.update { it.copy(isPlayingAttempt = false) }
        coach.speak(currentItem.text, _uiState.value.playbackSpeed)
    }

    /** The "You" button on Screen 2. */
    fun playAttempt(path: String? = lastRecordingPath) {
        val source = path ?: return
        if (isRecording) return
        coach.stop()
        _uiState.update { it.copy(isPlayingAttempt = true) }
        player.play(source, _uiState.value.playbackSpeed) {
            _uiState.update { it.copy(isPlayingAttempt = false) }
        }
    }

    fun toggleSpeed() {
        val next = _uiState.value.playbackSpeed.next()
        stopAllPlayback()
        _uiState.update { it.copy(playbackSpeed = next) }
    }

    fun stopAllPlayback() {
        coach.stop()
        player.stop()
        _uiState.update { it.copy(isPlayingAttempt = false, isCoachSpeaking = false) }
    }

    // ---- Misc ---------------------------------------------------------------------

    /**
     * Re-seeds item and recording when Screen 2 is opened from its navigation
     * arguments (e.g. after process death). The score is not re-requested
     * automatically — Screen 2 offers "Get score" instead, so a restore never
     * silently spends an Azure call.
     */
    fun restoreAttempt(index: Int, recordingPath: String) {
        if (index in items.indices && index != currentIndex) {
            _uiState.update { it.copy(currentIndex = index, currentItem = items[index]).clearedAnalysis() }
        }
        if (lastRecordingPath != recordingPath && File(recordingPath).isFile) {
            _uiState.update { it.copy(lastRecordingPath = recordingPath).clearedAnalysis() }
        }
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(isRecording = false, message = "Microphone permission is needed to record")
        }
    }

    private fun cancelPendingWork() {
        analysisJob?.cancel()
        analysisJob = null
        tipJob?.cancel()
        tipJob = null
    }

    private fun PracticeUiState.clearedResult() =
        copy(lastResult = null, focusedWordIndex = null, coachTip = null, isTipLoading = false)

    private fun PracticeUiState.clearedAnalysis() = clearedResult().copy(analysis = AnalysisStatus.Idle)

    override fun onCleared() {
        // TTS binds a service and AudioRecord/MediaPlayer hold native handles;
        // this ViewModel outlives configuration changes and is the right owner.
        coach.setSpeakingListener(null)
        coach.shutdown()
        player.release()
        recorder.release()
        super.onCleared()
    }

    companion object {
        /** Shorter than this can't hold a sentence; don't spend a network call on it. */
        private const val MIN_TAKE_MS = 600L

        /** Peak sample below this (of 32768) is room noise, not speech. */
        private const val SILENCE_PEAK = 500

        /** Builds the production wiring from an application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val credentials = EmbeddedKeyCredentialsProvider(
                        key = BuildConfig.AZURE_SPEECH_KEY,
                        region = BuildConfig.AZURE_SPEECH_REGION,
                    )
                    return PracticeViewModel(
                        repository = LocalPracticeRepository(),
                        recorder = WavRecorder(appContext),
                        player = AudioPlayer(),
                        coach = CoachVoice(appContext),
                        recordings = RecordingStore(appContext.cacheDir),
                        assessment = AzurePronunciationAssessmentService(credentials),
                        tips = StaticCoachTipProvider(),
                    ) as T
                }
            }
        }
    }
}
