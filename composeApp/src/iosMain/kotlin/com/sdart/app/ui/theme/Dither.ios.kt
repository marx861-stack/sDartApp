package com.sdart.app.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

private val ditherEffect by lazy { RuntimeEffect.makeForShader(DITHERED_GRADIENT_SHADER) }

internal actual fun ditheredGradientShader(size: Size, start: Color, mid: Color, end: Color): Shader? =
    RuntimeShaderBuilder(ditherEffect).run {
        uniform("size", size.width, size.height)
        uniform("c0", start.red, start.green, start.blue, start.alpha)
        uniform("c1", mid.red, mid.green, mid.blue, mid.alpha)
        uniform("c2", end.red, end.green, end.blue, end.alpha)
        makeShader()
    }

internal actual fun Paint.enableDither() {
    asFrameworkPaint().isDither = true
}
