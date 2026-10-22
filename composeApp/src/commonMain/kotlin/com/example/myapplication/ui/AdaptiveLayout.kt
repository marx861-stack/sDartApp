package com.example.myapplication.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeInfo) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val sizeInfo = calculateWindowSizeInfo(maxWidth, maxHeight)
        
        // Bei sehr großen Auflösungen (z.B. 4K-Monitoren ab 1600dp Fensterbreite oder 1200dp Höhe)
        // skalieren wir die Density sanft hoch (z.B. 1.25x - 1.5x), damit Text und UI-Elemente
        // auf 4K nicht winzig wirken, sondern perfekt lesbar bleiben.
        val currentDensity = androidx.compose.ui.platform.LocalDensity.current
        val scaleFactor = when {
            sizeInfo.isUltra && maxHeight >= 1200.dp -> 1.35f
            sizeInfo.isUltra -> 1.2f
            else -> 1.0f
        }

        if (scaleFactor != 1.0f) {
            val scaledDensity = androidx.compose.ui.unit.Density(
                density = currentDensity.density * scaleFactor,
                // sp skaliert bereits über density mit; fontScale bleibt unverändert
                fontScale = currentDensity.fontScale
            )
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides scaledDensity
            ) {
                content(sizeInfo)
            }
        } else {
            content(sizeInfo)
        }
    }
}
