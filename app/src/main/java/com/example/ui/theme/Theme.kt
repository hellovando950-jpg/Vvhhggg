package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DerivPrimary,
    onPrimary = Color.White,
    secondary = ActionBlue,
    onSecondary = Color.White,
    tertiary = HighlightGold,
    background = DerivDarkBg,
    onBackground = White90,
    surface = DerivSurface,
    onSurface = White90,
    surfaceVariant = DerivSurfaceLight,
    onSurfaceVariant = White90,
    outline = NeutralGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
