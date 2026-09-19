package com.example.nativeaccent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nativeaccent.R
import com.example.nativeaccent.assessment.PronunciationResult
import com.example.nativeaccent.assessment.ScoreBand
import com.example.nativeaccent.assessment.alignToReference
import com.example.nativeaccent.assessment.unscoredWords
import com.example.nativeaccent.audio.PlaybackSpeed
import com.example.nativeaccent.coaching.CoachTip
import com.example.nativeaccent.ui.components.BottomControlRow
import com.example.nativeaccent.ui.components.CircleIconButton
import com.example.nativeaccent.ui.components.CoachBanner
import com.example.nativeaccent.ui.components.MicButton
import com.example.nativeaccent.ui.components.PhonemeChips
import com.example.nativeaccent.ui.components.PracticeTopBar
import com.example.nativeaccent.ui.components.ScoreRing
import com.example.nativeaccent.ui.components.ScoredSentence
import com.example.nativeaccent.ui.components.SubScoreRow
import com.example.nativeaccent.ui.theme.AccentOrange
import com.example.nativeaccent.ui.theme.AccentPink
import com.example.nativeaccent.ui.theme.Ink
import com.example.nativeaccent.ui.theme.InkChip
import com.example.nativeaccent.ui.theme.InkElevated
import com.example.nativeaccent.ui.theme.InkStroke
import com.example.nativeaccent.ui.theme.TextMuted
import com.example.nativeaccent.ui.theme.TextPrimary
import com.example.nativeaccent.ui.theme.TextSecondary
import com.example.nativeaccent.viewmodel.AnalysisStatus
import com.example.nativeaccent.viewmodel.PracticeUiState

/**
 * Screen 2 — the score for the take, word by word and sound by sound, plus
 * the v0 Coach / You playback for comparing by ear.
 *
 * Works without a score too (scoring unavailable, or the learner chose to
 * compare by ear): the sentence renders uncoloured and "Get score" is offered.
 */
@Composable
fun AnalysisScreen(
    state: PracticeUiState,
    onClose: () -> Unit,
    onWordClick: (Int) -> Unit,
    onCoachTip: () -> Unit,
    onGetScore: () -> Unit,
    onPlayCoach: () -> Unit,
    onPlayAttempt: () -> Unit,
    onToggleSpeed: () -> Unit,
    onReRecord: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = state.lastResult

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding(),
    ) {
        PracticeTopBar(onClose = onClose, showFlag = true)

        CoachBanner(
            message = bannerFor(result),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (result != null) {
                ScoreRing(score = result.overallScore)
                Spacer(Modifier.height(16.dp))
                SubScoreRow(result)
                Spacer(Modifier.height(24.dp))
            }

            val words = remember(state.currentItem.text, result) {
                if (result != null) alignToReference(state.currentItem.text, result)
                else unscoredWords(state.currentItem.text)
            }
            ScoredSentence(
                words = words,
                result = result,
                focusedIndex = state.focusedWordIndex,
                onWordClick = onWordClick,
            )

            Spacer(Modifier.height(20.dp))

            if (result != null) {
                FocusedWordBreakdown(state, result)
                Spacer(Modifier.height(16.dp))
                CoachTipSection(tip = state.coachTip, loading = state.isTipLoading, onCoachTip = onCoachTip)
            } else {
                NoScoreStatus(state.analysis, canScore = state.lastRecordingPath != null, onGetScore = onGetScore)
            }
        }

        // Kept from v0 and pinned, so comparing by ear never needs a scroll.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            PlaybackButton(
                label = stringResource(R.string.btn_coach),
                contentDescription = stringResource(R.string.cd_coach),
                active = state.isCoachSpeaking,
                enabled = true,
                onClick = onPlayCoach,
            )
            PlaybackButton(
                label = stringResource(R.string.btn_you),
                contentDescription = stringResource(R.string.cd_you),
                active = state.isPlayingAttempt,
                enabled = state.lastRecordingPath != null,
                onClick = onPlayAttempt,
            )
            SpeedToggle(speed = state.playbackSpeed, onClick = onToggleSpeed)
        }

        BottomControlRow(
            modifier = Modifier.padding(bottom = 28.dp),
            start = { Spacer(Modifier.size(52.dp)) },
            center = { MicButton(isRecording = false, onClick = onReRecord) },
            end = {
                CircleIconButton(
                    iconRes = R.drawable.ic_arrow_forward,
                    contentDescription = stringResource(R.string.cd_next),
                    onClick = onNext,
                )
            },
        )
    }
}

private fun bannerFor(result: PronunciationResult?): String = when {
    result == null -> "Now compare with the coach."
    else -> when (ScoreBand.of(result.overallScore)) {
        ScoreBand.GOOD -> "Great job — that sounded natural."
        ScoreBand.FAIR -> "Nice — a few sounds to polish."
        ScoreBand.POOR -> "Good start — let's work on a few sounds."
    }
}

/** The focused word's phoneme chips, or why there are none. */
@Composable
private fun FocusedWordBreakdown(state: PracticeUiState, result: PronunciationResult) {
    val word = state.focusedWordIndex?.let(result.words::getOrNull) ?: return

    Text(
        text = "Sounds in “${word.word}”",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
    )
    Text(
        text = "Tap any word above to see its sounds",
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
    )
    Spacer(Modifier.height(12.dp))

    when {
        word.wasSkipped -> Text(
            text = "This word wasn't heard — it may have been skipped.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        word.phonemes.isEmpty() -> Text(
            text = "No sound-level detail for this word.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        else -> PhonemeChips(word.phonemes)
    }
}

@Composable
private fun CoachTipSection(tip: CoachTip?, loading: Boolean, onCoachTip: () -> Unit) {
    if (tip == null) {
        OutlinedButton(
            onClick = onCoachTip,
            enabled = !loading,
            border = BorderStroke(1.dp, AccentPink),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentPink)
                Spacer(Modifier.width(8.dp))
            }
            Text("Coach tip", style = MaterialTheme.typography.labelLarge)
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = InkElevated,
        border = BorderStroke(1.dp, AccentPink.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(text = tip.headline, style = MaterialTheme.typography.titleMedium, color = AccentPink)
            Spacer(Modifier.height(6.dp))
            Text(text = tip.body, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

/** Shown in place of the breakdown when there is no score for this take. */
@Composable
private fun NoScoreStatus(analysis: AnalysisStatus, canScore: Boolean, onGetScore: () -> Unit) {
    when (analysis) {
        AnalysisStatus.Analyzing -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentPink)
            Spacer(Modifier.width(10.dp))
            Text("Analyzing…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        is AnalysisStatus.Failed -> StatusWithAction(
            message = analysis.message,
            action = if (analysis.canRetry && canScore) "Try again" else null,
            onAction = onGetScore,
        )

        is AnalysisStatus.NeedsRetake -> StatusWithAction(analysis.message, action = null, onAction = {})

        AnalysisStatus.Idle -> StatusWithAction(
            message = "No score for this take yet.",
            action = if (canScore) "Get score" else null,
            onAction = onGetScore,
        )
    }
}

@Composable
private fun StatusWithAction(message: String, action: String?, onAction: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, AccentPink),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink),
            ) { Text(action) }
        }
    }
}

/** "Coach" / "You" — a pill with a play glyph that fills in while it is playing. */
@Composable
private fun PlaybackButton(
    label: String,
    contentDescription: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = when {
        !enabled -> TextMuted
        active -> Color.White
        else -> TextPrimary
    }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (active) Color.Transparent else InkChip,
        border = BorderStroke(1.dp, if (active) AccentPink else InkStroke),
        modifier = Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (active) Modifier.background(Brush.linearGradient(listOf(AccentPink, AccentOrange)))
                else Modifier
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

/** Cycles 1x -> 0.5x for both voices at once. */
@Composable
private fun SpeedToggle(speed: PlaybackSpeed, onClick: () -> Unit) {
    val highlighted = speed != PlaybackSpeed.NORMAL
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (highlighted) AccentPink.copy(alpha = 0.18f) else InkChip)
            .border(1.dp, if (highlighted) AccentPink else InkStroke, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = speed.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) AccentPink else TextSecondary,
        )
    }
}
