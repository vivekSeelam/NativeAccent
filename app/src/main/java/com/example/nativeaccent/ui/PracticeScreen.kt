package com.example.nativeaccent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nativeaccent.R
import com.example.nativeaccent.ui.components.BottomControlRow
import com.example.nativeaccent.ui.components.CircleIconButton
import com.example.nativeaccent.ui.components.CoachBanner
import com.example.nativeaccent.ui.components.MicButton
import com.example.nativeaccent.ui.components.PracticeTopBar
import com.example.nativeaccent.ui.components.rememberMicPermission
import com.example.nativeaccent.ui.theme.AccentPink
import com.example.nativeaccent.ui.theme.Ink
import com.example.nativeaccent.ui.theme.TextMuted
import com.example.nativeaccent.ui.theme.TextPrimary
import com.example.nativeaccent.ui.theme.TextSecondary
import com.example.nativeaccent.viewmodel.AnalysisStatus
import com.example.nativeaccent.viewmodel.PracticeUiState

/**
 * Screen 1 — read the prompt, optionally hear the coach, then record an attempt.
 *
 * Stateless with respect to audio: every action is a callback the caller wires
 * to the ViewModel, which keeps this file previewable and testable.
 */
@Composable
fun PracticeScreen(
    state: PracticeUiState,
    onClose: () -> Unit,
    onPlayCoach: () -> Unit,
    onToggleRecording: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPrevious: () -> Unit,
    onCamera: () -> Unit,
    onRetryAnalysis: () -> Unit,
    onCompareWithoutScore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val micPermission = rememberMicPermission(
        onGranted = onToggleRecording,
        onDenied = onPermissionDenied,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding(),
    ) {
        PracticeTopBar(onClose = onClose)

        Spacer(Modifier.height(8.dp))

        CoachBanner(
            message = stringResource(R.string.coach_practice_prompt),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${state.currentIndex + 1} / ${state.totalItems}",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = state.currentItem.text,
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            state.currentItem.hint?.let { hint ->
                Spacer(Modifier.height(14.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(28.dp))

            // Preview: hear it before attempting it. The ring lights up while
            // the coach is actually speaking.
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.isCoachSpeaking) AccentPink.copy(alpha = 0.18f) else Color.Transparent
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CircleIconButton(
                    iconRes = R.drawable.ic_volume_up,
                    contentDescription = stringResource(R.string.cd_preview),
                    onClick = onPlayCoach,
                    enabled = !state.isRecording,
                    size = 56.dp,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Min height reserved so the layout does not jump as the status changes.
            Box(Modifier.heightIn(min = 88.dp), contentAlignment = Alignment.TopCenter) {
                PracticeStatus(
                    state = state,
                    onRetry = onRetryAnalysis,
                    onCompareWithoutScore = onCompareWithoutScore,
                )
            }
        }

        BottomControlRow(
            modifier = Modifier.padding(bottom = 36.dp),
            start = {
                CircleIconButton(
                    iconRes = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.cd_previous),
                    onClick = onPrevious,
                    enabled = state.hasPrevious,
                )
            },
            center = {
                MicButton(
                    isRecording = state.isRecording,
                    onClick = { micPermission.request() },
                    enabled = !state.isAnalyzing,
                )
            },
            end = {
                CircleIconButton(
                    iconRes = R.drawable.ic_camera,
                    contentDescription = stringResource(R.string.cd_camera),
                    onClick = onCamera,
                )
            },
        )
    }
}

/** Recording / analyzing / retake / failure line under the prompt. */
@Composable
private fun PracticeStatus(
    state: PracticeUiState,
    onRetry: () -> Unit,
    onCompareWithoutScore: () -> Unit,
) {
    val analysis = state.analysis
    when {
        state.isRecording -> StatusText("Listening…", TextSecondary)

        analysis == AnalysisStatus.Analyzing -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentPink)
            Spacer(Modifier.width(10.dp))
            StatusText("Analyzing…", TextSecondary)
        }

        analysis is AnalysisStatus.NeedsRetake -> StatusText(analysis.message, AccentPink)

        analysis is AnalysisStatus.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StatusText(analysis.message, AccentPink)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (analysis.canRetry) {
                    OutlinedButton(
                        onClick = onRetry,
                        border = BorderStroke(1.dp, AccentPink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink),
                    ) { Text("Retry") }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onCompareWithoutScore) {
                    Text("Compare without score", color = TextSecondary)
                }
            }
        }

        state.message != null -> StatusText(state.message, AccentPink)

        else -> StatusText("Tap the mic and read the sentence", TextSecondary)
    }
}

@Composable
private fun StatusText(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
    )
}

