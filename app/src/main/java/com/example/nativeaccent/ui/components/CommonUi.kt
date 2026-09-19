package com.example.nativeaccent.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.nativeaccent.R
import com.example.nativeaccent.ui.theme.AccentPink
import com.example.nativeaccent.ui.theme.InkChip
import com.example.nativeaccent.ui.theme.InkElevated
import com.example.nativeaccent.ui.theme.InkStroke
import com.example.nativeaccent.ui.theme.MicGradient
import com.example.nativeaccent.ui.theme.TextPrimary
import com.example.nativeaccent.ui.theme.TextSecondary

/**
 * The top bar both screens share. Screen 2 adds the flag icon by passing
 * [showFlag]; nothing here has behaviour yet beyond close.
 */
@Composable
fun PracticeTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showFlag: Boolean = false,
    onBookmark: () -> Unit = {},
    onSettings: () -> Unit = {},
    onFlag: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(R.drawable.ic_close, stringResource(R.string.cd_close), onClose)
        Spacer(Modifier.weight(1f))
        if (showFlag) {
            BarIcon(R.drawable.ic_flag, stringResource(R.string.cd_flag), onFlag)
        }
        BarIcon(R.drawable.ic_bookmark, stringResource(R.string.cd_bookmark), onBookmark)
        BarIcon(R.drawable.ic_settings, stringResource(R.string.cd_settings), onSettings)
    }
}

@Composable
private fun BarIcon(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** The coach's speech bubble at the top of both screens. */
@Composable
fun CoachBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 8.dp),
        color = InkElevated,
        border = BorderStroke(1.dp, InkStroke),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MicGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
        }
    }
}

/** A dark circular icon button — used for back / camera / re-record affordances. */
@Composable
fun CircleIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 52.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(InkChip)
            .border(1.dp, InkStroke, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

/**
 * The primary control: a large gradient circle that swaps between mic and stop.
 * While recording it breathes, so the state is obvious without reading a label.
 */
@Composable
fun MicButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
) {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "mic-pulse-scale",
    )

    Box(
        modifier = modifier.size(size + 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(size + 24.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(AccentPink.copy(alpha = 0.18f)),
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .scale(if (isRecording) pulse else 1f)
                .clip(CircleShape)
                .background(MicGradient),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(size)) {
                Icon(
                    painter = painterResource(
                        if (isRecording) R.drawable.ic_stop else R.drawable.ic_mic
                    ),
                    contentDescription = stringResource(
                        if (isRecording) R.string.cd_stop else R.string.cd_record
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.40f),
                )
            }
        }
    }
}

/** Row of three slots with the mic centred, so both screens line up identically. */
@Composable
fun BottomControlRow(
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit,
    center: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { start() }
        center()
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { end() }
    }
}
