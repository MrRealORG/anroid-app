package com.fbr.ntn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Accent,
    onPrimaryContainer = Ink,
    secondary = InkMuted,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Track,
    onSurfaceVariant = InkMuted,
    outline = Line,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun FbrNtnTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
