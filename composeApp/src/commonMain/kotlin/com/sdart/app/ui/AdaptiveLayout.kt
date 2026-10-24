package com.sdart.app.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Window Size Classes nach Google Material 3 Adaptive Richtlinien.
 */
enum class WindowWidthSizeClass {
    COMPACT,   // < 600dp (Smartphones Portrait)
    MEDIUM,    // 600dp - 840dp (Foldables, Tablets Portrait)
    EXPANDED,  // 840dp - 1600dp (Desktop, Tablets Landscape)
    ULTRA      // > 1600dp (4K, UltraWide & große Monitore)
}

enum class WindowHeightSizeClass {
    COMPACT,   // < 480dp
    MEDIUM,    // 480dp - 900dp
    EXPANDED   // > 900dp
}

data class WindowSizeInfo(
    val widthClass: WindowWidthSizeClass,
    val heightClass: WindowHeightSizeClass,
    val isWidescreen: Boolean,
    val maxWidth: Dp,
    val maxHeight: Dp
) {
    val isUltra: Boolean get() = widthClass == WindowWidthSizeClass.ULTRA
    val isExpanded: Boolean get() = widthClass == WindowWidthSizeClass.EXPANDED || isUltra
    val isCompact: Boolean get() = widthClass == WindowWidthSizeClass.COMPACT
    val isMedium: Boolean get() = widthClass == WindowWidthSizeClass.MEDIUM

    /**
     * Maximalbreite für zentrierten Inhalt auf großen Displays (z.B. Startbildschirm, Dialoge).
     */
    val maxContentWidth: Dp
        get() = when {
            isUltra -> 1400.dp
            isWidescreen -> 1000.dp
            else -> Dp.Unspecified
        }
}

fun calculateWindowSizeInfo(maxWidth: Dp, maxHeight: Dp): WindowSizeInfo {
    val widthClass = when {
        maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
        maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
        maxWidth < 1600.dp -> WindowWidthSizeClass.EXPANDED
        else -> WindowWidthSizeClass.ULTRA
    }
    val heightClass = when {
        maxHeight < 480.dp -> WindowHeightSizeClass.COMPACT
        maxHeight < 900.dp -> WindowHeightSizeClass.MEDIUM
        else -> WindowHeightSizeClass.EXPANDED
    }
    val isWidescreen = maxWidth >= 840.dp || (maxWidth > maxHeight && maxWidth >= 600.dp)
    return WindowSizeInfo(
        widthClass = widthClass,
        heightClass = heightClass,
        isWidescreen = isWidescreen,
        maxWidth = maxWidth,
        maxHeight = maxHeight
    )
}

/**
 * true auf schmalen Fenstern (Handy, Tablet hochkant): Seiten stapeln ihre Karten dann untereinander
 * statt sie nebeneinander zu legen. Wird in App.kt aus der Fensterbreite gesetzt.
 */
val LocalCompactLayout = staticCompositionLocalOf { false }

/**
 * Aktiver UI-Skalierungsfaktor aus [ProvideAdaptiveScale] (1.0 = keine Skalierung).
 */
val LocalUiScale = staticCompositionLocalOf { 1.0f }

private val ReferenceWidth = 1500.dp
private val ReferenceHeight = 860.dp

/**
 * Skaliert die gesamte UI auf sehr großen Fenstern einheitlich hoch, damit Text und UI-Elemente
 * auf 4K nicht winzig wirken. Einmal um die ganze App legen, damit alle Screens und Dialoge
 * gleich groß dargestellt werden.
 */
@Composable
fun ProvideAdaptiveScale(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val sizeInfo = calculateWindowSizeInfo(maxWidth, maxHeight)
        // Große Fenster proportional hochskalieren, sodass effektiv etwa 1500 × 860 dp Platz bleiben;
        // so füllen die Layouts den Bildschirm statt als kleine Elemente in viel Leerraum zu stehen
        val scaleFactor = if (sizeInfo.isUltra) {
            minOf(maxWidth / ReferenceWidth, maxHeight / ReferenceHeight).coerceIn(1.2f, 2.2f)
        } else {
            1.0f
        }
        val currentDensity = LocalDensity.current
        val scaledDensity = Density(
            density = currentDensity.density * scaleFactor,
            // sp skaliert bereits über density mit; fontScale bleibt unverändert
            fontScale = currentDensity.fontScale
        )
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalUiScale provides scaleFactor,
            content = content
        )
    }
}

@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeInfo) -> Unit
) {
    val scale = LocalUiScale.current
    BoxWithConstraints(modifier = modifier) {
        // Größenklassen aus der unskalierten Fenstergröße, Maße in skalierten dp
        val sizeInfo = calculateWindowSizeInfo(maxWidth * scale, maxHeight * scale)
            .copy(maxWidth = maxWidth, maxHeight = maxHeight)
        content(sizeInfo)
    }
}
