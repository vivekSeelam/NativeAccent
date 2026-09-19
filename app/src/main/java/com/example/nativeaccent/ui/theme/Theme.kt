package com.example.nativeaccent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NativeAccentColors = darkColorScheme(
    primary = AccentPink,
    onPrimary = Color.White,
    secondary = AccentOrange,
    onSecondary = Color.Black,
    tertiary = AccentCoral,
    background = Ink,
    onBackground = TextPrimary,
    surface = InkElevated,
    onSurface = TextPrimary,
    surfaceVariant = InkChip,
    onSurfaceVariant = TextSecondary,
    outline = InkStroke,
    error = AccentCoral,
)

/** The app is dark-only by design — a coaching session should not flash white. */
@Composable
fun NativeAccentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NativeAccentColors,
        typography = NativeAccentTypography,
        content = content,
    )
}
