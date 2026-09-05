package com.fbr.ntn.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Runtime theme switch. Colors below are snapshot-state backed, so every screen
 *  recomposes automatically when [dark] flips — no call-site changes needed. */
object ThemeMode {
    var dark by mutableStateOf(false)
}

// iOS-soft light theme: warm porcelain paper, near-black ink, vivid #0700FF accent.
private val LightInk = Color(0xFF111318)
private val LightInkMuted = Color(0xFF6B7280)
private val LightPaper = Color(0xFFF3F0E9)
private val LightCard = Color(0xFFFFFFFF)
private val LightLine = Color(0xFFE7E2D8)
private val LightTrack = Color(0xFFE9E4D8)
private val LightTile = Color(0xFFEDEAE2)
private val LightAccentSoft = Color(0xFFE1E2FF)
private val LightErrorTint = Color(0xFFFFE9E6)

// iOS-soft dark theme: true-black paper, iOS dark cards, same #0700FF brand.
private val DarkInk = Color(0xFFF5F5F7)
private val DarkInkMuted = Color(0xFF9AA0AE)
private val DarkPaper = Color(0xFF0B0B0F)
private val DarkCard = Color(0xFF1C1C1E)
private val DarkLine = Color(0xFF2E2E33)
private val DarkTrack = Color(0xFF232328)
private val DarkTile = Color(0xFF26262B)
private val DarkAccentSoft = Color(0xFF23234D)
private val DarkErrorTint = Color(0xFF3A1D1A)

val Ink: Color get() = if (ThemeMode.dark) DarkInk else LightInk
val InkMuted: Color get() = if (ThemeMode.dark) DarkInkMuted else LightInkMuted
val Paper: Color get() = if (ThemeMode.dark) DarkPaper else LightPaper
val CardWhite: Color get() = if (ThemeMode.dark) DarkCard else LightCard
val Line: Color get() = if (ThemeMode.dark) DarkLine else LightLine
val Track: Color get() = if (ThemeMode.dark) DarkTrack else LightTrack
val Tile: Color get() = if (ThemeMode.dark) DarkTile else LightTile
val Accent: Color = Color(0xFF0700FF)
val AccentDeep: Color get() = if (ThemeMode.dark) Color(0xFF9AA5FF) else Color(0xFF0500C2)
val AccentSoft: Color get() = if (ThemeMode.dark) DarkAccentSoft else LightAccentSoft
val ErrorRed: Color = Color(0xFFE4453A)
val ErrorTint: Color get() = if (ThemeMode.dark) DarkErrorTint else LightErrorTint
val ErrorInk: Color get() = if (ThemeMode.dark) Color(0xFFFF8A80) else Color(0xFFC93A2E)
