package com.sdart.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import org.jetbrains.compose.resources.imageResource
import sdartapp.composeapp.generated.resources.Res
import sdartapp.composeapp.generated.resources.dither_noise

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

val DarkBgGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
    colors = listOf(DarkBgStart, DarkBgMid, DarkBgEnd)
)

/**
 * Zeichnet den edlen vertikalen Dark-Hintergrundverlauf inklusive stochastischem
 * Hardware-Dithering (Noise Overlay im BlendMode.Overlay).
 * Eliminiert 8-Bit-Color-Banding (Streifenbildung) vollständig auf Desktop, Android und iOS.
 */
@Composable
fun Modifier.ditheredBackground(): Modifier {
    val ditherImage = imageResource(Res.drawable.dither_noise)
    val ditherBrush = remember(ditherImage) {
        ShaderBrush(
            ImageShader(
                image = ditherImage,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }
    return this.drawBehind {
        drawRect(brush = DarkBgGradient)
        drawRect(
            brush = ditherBrush,
            alpha = 0.04f,
            blendMode = BlendMode.Overlay
        )
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
