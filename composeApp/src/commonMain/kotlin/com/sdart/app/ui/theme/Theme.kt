package com.sdart.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

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

/**
 * Zeichnet den diagonalen Dark-Hintergrundverlauf mit Dithering.
 * Der Verlauf überspannt nur ~16 Helligkeitsstufen; ohne Dithering vor der
 * 8-Bit-Quantisierung entstehen sichtbare Streifen (Color-Banding).
 */
fun Modifier.ditheredBackground(): Modifier = drawWithCache {
    val paint = Paint()
    val shader = ditheredGradientShader(size, DarkBgStart, DarkBgMid, DarkBgEnd)
    if (shader != null) {
        paint.shader = shader
    } else {
        paint.enableDither()
        DarkBgGradient.applyTo(size, paint, alpha = 1f)
    }
    onDrawBehind {
        drawIntoCanvas { it.drawRect(Rect(Offset.Zero, size), paint) }
    }
}

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
