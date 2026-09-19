package com.example.nativeaccent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

            Text(
                text = state.message ?: if (state.isRecording) "Listening…" else "Tap the mic when you are ready",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.message != null) AccentPink else TextSecondary,
                textAlign = TextAlign.Center,
                // Reserved so the layout does not jump when the status changes.
                modifier = Modifier.heightIn(min = 40.dp),
            )
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
