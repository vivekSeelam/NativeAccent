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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.nativeaccent.audio.PlaybackSpeed
import com.example.nativeaccent.scoring.PronunciationScore
import com.example.nativeaccent.ui.components.BottomControlRow
import com.example.nativeaccent.ui.components.CircleIconButton
import com.example.nativeaccent.ui.components.CoachBanner
import com.example.nativeaccent.ui.components.MicButton
import com.example.nativeaccent.ui.components.PracticeTopBar
import com.example.nativeaccent.ui.theme.AccentOrange
import com.example.nativeaccent.ui.theme.AccentPink
import com.example.nativeaccent.ui.theme.Ink
import com.example.nativeaccent.ui.theme.InkChip
import com.example.nativeaccent.ui.theme.InkElevated
import com.example.nativeaccent.ui.theme.InkStroke
import com.example.nativeaccent.ui.theme.TextMuted
import com.example.nativeaccent.ui.theme.TextPrimary
import com.example.nativeaccent.ui.theme.TextSecondary
import com.example.nativeaccent.viewmodel.PracticeUiState

/**
 * Screen 2 — hear the coach and yourself back to back.
 *
 * The score panel is rendered only when [PracticeUiState.score] is non-null, so
 * the v1 scoring module lights it up without this file changing.
 */
@Composable
fun CompareScreen(
    state: PracticeUiState,
    onClose: () -> Unit,
    onPlayCoach: () -> Unit,
    onPlayAttempt: () -> Unit,
    onToggleSpeed: () -> Unit,
    onReRecord: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding(),
    ) {
        PracticeTopBar(onClose = onClose, showFlag = true)

        Spacer(Modifier.height(8.dp))

        CoachBanner(
            message = stringResource(R.string.coach_compare_prompt),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.currentItem.text,
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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

            Spacer(Modifier.height(20.dp))

            // Reserved for the scoring module; empty in v0.
            ScorePanel(score = state.score)

            state.message?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentPink,
                    textAlign = TextAlign.Center,
                )
            }
        }

        BottomControlRow(
            modifier = Modifier.padding(bottom = 36.dp),
            start = { Spacer(Modifier.size(52.dp)) },
            center = {
                MicButton(isRecording = false, onClick = onReRecord)
            },
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
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (active) {
                    Modifier.background(Brush.linearGradient(listOf(AccentPink, AccentOrange)))
                } else {
                    Modifier
                }
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

/** Cycles 1x -> 0.5x for both voices at once. */
@Composable
private fun SpeedToggle(speed: PlaybackSpeed, onClick: () -> Unit) {
    val highlighted = speed != PlaybackSpeed.NORMAL
    Box(
        modifier = Modifier
            .size(56.dp)
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

/**
 * Placeholder slot for phoneme-level feedback.
 *
 * Draws nothing until a [PronunciationScore] arrives, which keeps v0 honest:
 * no fake percentage, no empty chrome.
 */
@Composable
private fun ScorePanel(score: PronunciationScore?, modifier: Modifier = Modifier) {
    if (score == null) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = InkElevated,
        border = BorderStroke(1.dp, InkStroke),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "${score.overallPercent}%",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            if (score.phonemes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    score.phonemes.forEach { phoneme ->
                        Text(
                            text = phoneme.symbol,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (phoneme.accuracy >= 0.75f) TextSecondary else AccentPink,
                        )
                    }
                }
            }
        }
    }
}
