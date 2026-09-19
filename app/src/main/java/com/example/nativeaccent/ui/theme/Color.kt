package com.example.nativeaccent.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dark coaching-app palette. Everything is defined once here so the future
// scoring panel can pick up the same surfaces and accents.
val Ink = Color(0xFF0E0B14)          // app background
val InkElevated = Color(0xFF1A1523)  // cards, banners
val InkStroke = Color(0xFF2C2438)    // hairlines and icon-button outlines
val InkChip = Color(0xFF231C31)      // secondary buttons

val AccentPink = Color(0xFFFF4E8B)
val AccentOrange = Color(0xFFFF9D4D)
val AccentCoral = Color(0xFFFF6F61)

val TextPrimary = Color(0xFFF5F2FA)
val TextSecondary = Color(0xFFA79FB8)
val TextMuted = Color(0xFF6F677F)

/** Primary mic button fill: pink into orange, top-left to bottom-right. */
val MicGradient = Brush.linearGradient(listOf(AccentPink, AccentOrange))

// Score bands: green >= 80, yellow 60..79, red < 60.
val ScoreGood = Color(0xFF34D399)
val ScoreFair = Color(0xFFFBBF24)
val ScorePoor = Color(0xFFF87171)
