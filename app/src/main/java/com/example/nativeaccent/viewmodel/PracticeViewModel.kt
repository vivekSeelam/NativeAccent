package com.example.nativeaccent.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nativeaccent.audio.AudioPlayer
import com.example.nativeaccent.audio.AudioRecorder
import com.example.nativeaccent.audio.CoachVoice
import com.example.nativeaccent.audio.PlaybackSpeed
import com.example.nativeaccent.audio.RecordingResult
import com.example.nativeaccent.audio.RecordingStore
import com.example.nativeaccent.data.LocalPracticeRepository
import com.example.nativeaccent.data.PracticeItem
import com.example.nativeaccent.data.PracticeRepository
import com.example.nativeaccent.scoring.NoScoringYet
import com.example.nativeaccent.scoring.PronunciationScore
import com.example.nativeaccent.scoring.PronunciationScorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val score: PronunciationScore? = null,
    val message: String? = null,
) {
    val hasPrevious: Boolean get() = currentIndex > 0
}

/**
 * Single source of truth for the practice flow.
 *
 * Audio work is delegated to [AudioRecorder], [AudioPlayer] and [CoachVoice];
 * this class only sequences them and folds the outcome into [uiState]. Scoring
 * is asked for through [PronunciationScorer] and is a no-op in v0.
 */
class PracticeViewModel(
    private val repository: PracticeRepository,
    private val recorder: AudioRecorder,
    private val player: AudioPlayer,
    private val coach: CoachVoice,
    private val recordings: RecordingStore,
    private val scorer: PronunciationScorer = NoScoringYet,
) : ViewModel() {

    private val items: List<PracticeItem> = repository.items

    private val _uiState = MutableStateFlow(
        PracticeUiState(currentItem = items.first(), totalItems = items.size)
    )
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    val currentItem: PracticeItem get() = _uiState.value.currentItem
    val currentIndex: Int get() = _uiState.value.currentIndex
    val isRecording: Boolean get() = _uiState.value.isRecording
    val lastRecordingPath: String? get() = _uiState.value.lastRecordingPath

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
        _uiState.update {
            it.copy(
                currentIndex = index,
                currentItem = items[index],
                isRecording = false,
                lastRecordingPath = null,
                score = null,
                message = null,
            )
        }
    }

    fun nextItem() = selectItem((currentIndex + 1).coerceAtMost(items.lastIndex))

    fun previousItem() = selectItem((currentIndex - 1).coerceAtLeast(0))

    /** Screen 2's mic button: same item, blank slate. */
    fun startOver() {
        stopAllPlayback()
        recordings.deleteAttemptsFor(currentItem.id)
        _uiState.update { it.copy(lastRecordingPath = null, score = null, message = null) }
    }

    // ---- Recording ----------------------------------------------------------------

    /**
     * Toggles capture. Returns the finished recording's path when this call
     * stopped a recording successfully, so the caller can navigate; null otherwise.
     */
    fun toggleRecording(): String? {
        return if (isRecording) {
            stopRecording()
        } else {
            startRecording()
            null
        }
    }

    fun startRecording() {
        if (isRecording) return
        stopAllPlayback()
        // Replaces this item's previous take, so the cache holds one file per item.
        val file = recordings.newAttemptFile(currentItem.id)
        val started = recorder.start(file)
        _uiState.update {
            if (started) {
                it.copy(isRecording = true, lastRecordingPath = null, score = null, message = null)
            } else {
                it.copy(isRecording = false, message = "Couldn't reach the microphone")
            }
        }
    }

    fun stopRecording(): String? {
        if (!isRecording) return null
        return when (val result = recorder.stop()) {
            is RecordingResult.Success -> {
                val path = result.file.absolutePath
                _uiState.update {
                    it.copy(isRecording = false, lastRecordingPath = path, message = null)
                }
                requestScore(path)
                path
            }

            is RecordingResult.Failure -> {
                _uiState.update {
                    it.copy(isRecording = false, lastRecordingPath = null, message = result.reason)
                }
                null
            }
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
     * Re-seeds state when Screen 2 is opened from a navigation argument, which
     * keeps the screen correct after process death restores the back stack.
     */
    fun restoreAttempt(index: Int, recordingPath: String) {
        if (index in items.indices && index != currentIndex) {
            _uiState.update { it.copy(currentIndex = index, currentItem = items[index]) }
        }
        if (_uiState.value.lastRecordingPath != recordingPath) {
            _uiState.update { it.copy(lastRecordingPath = recordingPath) }
            requestScore(recordingPath)
        }
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(isRecording = false, message = "Microphone permission is needed to record")
        }
    }

    private fun requestScore(path: String) {
        val item = currentItem
        viewModelScope.launch {
            val score = scorer.score(item, path)
            _uiState.update { if (it.lastRecordingPath == path) it.copy(score = score) else it }
        }
    }

    override fun onCleared() {
        // TTS binds a service and MediaRecorder/MediaPlayer hold native handles;
        // this ViewModel outlives configuration changes and is the right owner.
        coach.setSpeakingListener(null)
        coach.shutdown()
        player.release()
        recorder.release()
        super.onCleared()
    }

    companion object {
        /** Builds the default v0 wiring from an application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PracticeViewModel(
                        repository = LocalPracticeRepository(),
                        recorder = AudioRecorder(appContext),
                        player = AudioPlayer(),
                        coach = CoachVoice(appContext),
                        recordings = RecordingStore(appContext.cacheDir),
                        scorer = NoScoringYet,
                    ) as T
                }
            }
        }
    }
}
