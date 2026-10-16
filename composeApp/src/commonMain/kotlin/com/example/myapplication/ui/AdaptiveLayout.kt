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
    EXPANDED   // > 840dp (Desktop, Tablets Landscape)
}

enum class WindowHeightSizeClass {
    COMPACT,   // < 480dp
    MEDIUM,    // 480dp - 900dp
    EXPANDED   // > 900dp
}

data class WindowSizeInfo(
    val widthClass: WindowWidthSizeClass,
    val heightClass: WindowHeightSizeClass,
    val isWidescreen: Boolean
) {
    val isExpanded: Boolean get() = widthClass == WindowWidthSizeClass.EXPANDED
    val isCompact: Boolean get() = widthClass == WindowWidthSizeClass.COMPACT
    val isMedium: Boolean get() = widthClass == WindowWidthSizeClass.MEDIUM
}

fun calculateWindowSizeInfo(maxWidth: Dp, maxHeight: Dp): WindowSizeInfo {
    val widthClass = when {
        maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
        maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
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
        isWidescreen = isWidescreen
    )
}

@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeInfo) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val sizeInfo = calculateWindowSizeInfo(maxWidth, maxHeight)
        content(sizeInfo)
    }
}
