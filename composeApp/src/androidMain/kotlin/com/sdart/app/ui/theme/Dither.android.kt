package com.sdart.app.ui.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader

internal actual fun ditheredGradientShader(size: Size, start: Color, mid: Color, end: Color): Shader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return RuntimeShader(DITHERED_GRADIENT_SHADER).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("c0", start.red, start.green, start.blue, start.alpha)
        setFloatUniform("c1", mid.red, mid.green, mid.blue, mid.alpha)
        setFloatUniform("c2", end.red, end.green, end.blue, end.alpha)
    }
}

internal actual fun Paint.enableDither() {
    asFrameworkPaint().isDither = true
}
