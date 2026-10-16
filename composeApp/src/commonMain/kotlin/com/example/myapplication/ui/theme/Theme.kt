package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    secondary = PrimaryOrange,
    onSecondary = Color.White,
    tertiary = GlassWhite,
    onTertiary = DarkTextPrimary,
    surface = GlassWhite,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF2d3748),
    onSurfaceVariant = DarkTextSecondary,
    background = DarkBgStart,
    onBackground = DarkTextPrimary,
    error = Color(0xFFcf6679),
    onError = Color.Black,
    outline = GlassBorder,
    outlineVariant = DarkTextSecondary
)

val DarkBgGradient = androidx.compose.ui.graphics.Brush.linearGradient(
    colors = listOf(DarkBgStart, DarkBgMid, DarkBgEnd)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
