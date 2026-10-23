package com.sdart.app.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader

/**
 * Diagonaler Drei-Farben-Verlauf (oben links → unten rechts, wie Brush.linearGradient) mit TPDF-Dithering (triangulär verteiltes Rauschen, ±1 LSB),
 * berechnet im selben Pass wie der Verlauf – also vor der 8-Bit-Quantisierung.
 * Gültiges SkSL (Skia) und AGSL (Android 13+).
 */
internal const val DITHERED_GRADIENT_SHADER = """
uniform float2 size;
uniform half4 c0;
uniform half4 c1;
uniform half4 c2;

float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

half4 main(float2 xy) {
    float t = clamp(dot(xy, size) / dot(size, size), 0.0, 1.0);
    half4 c = t < 0.5 ? mix(c0, c1, half(t * 2.0)) : mix(c1, c2, half(t * 2.0 - 1.0));
    float n = hash12(xy) + hash12(xy + float2(57.13, 91.71)) - 1.0;
    c.rgb += half3(n / 255.0);
    return c;
}
"""

/**
 * Liefert den gedithert gerechneten Verlauf als Shader, oder `null`, wenn die Plattform
 * keine Runtime-Shader unterstützt (Android < 13).
 */
internal expect fun ditheredGradientShader(size: Size, start: Color, mid: Color, end: Color): Shader?

/**
 * Fallback: aktiviert das native Dithering des Paints (schwächer, je nach GPU-Backend nur teilweise wirksam).
 */
internal expect fun Paint.enableDither()
