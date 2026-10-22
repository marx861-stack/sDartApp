package com.sdart.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.sdart.app.ui.theme.PrimaryOrange
import kotlin.math.*

// Lokale Farbdefinitionen für das Statistik-Dartboard
private val dbBeige = Color(0xFFF5DEB3)
private val dbBlack = Color(0xFF1B1B1B)
private val dbRed = Color(0xFFD32F2F)
private val dbGreen = Color(0xFF388E3C)

/**
 * Komponente zur Anzeige einer Hit-Heatmap auf einer Dartscheibe
 */
@Composable
fun HeatmapDartboard(
    hits: List<Offset>,
    modifier: Modifier = Modifier,
    showNumbers: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    val scores = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        // Platz für Zahlen am Rand lassen (1.25f entspricht dem vollen Außenradius inkl. Zahlenring)
        val radius = (min(size.width, size.height) / 2f) / 1.25f
        
        // 1. Statisches Dartboard zeichnen
        drawStaticBoard(center, radius, scores, textMeasurer, showNumbers)
        
        // 2. Hits als Heatmap-Punkte zeichnen
        hits.forEach { normalizedHit ->
            // Normalisierte Positionen (-1 bis 1) in Canvas-Koordinaten umrechnen
            val hitPos = Offset(
                center.x + normalizedHit.x * radius,
                center.y + normalizedHit.y * radius
            )
            
            // Heatmap-Effekt mit RadialGradient
            val gradientRadius = radius * 0.15f
            val heatmapBrush = Brush.radialGradient(
                colors = listOf(
                    Color.Red.copy(alpha = 0.65f),
                    Color(0xFFFF9800).copy(alpha = 0.4f), // Orange
                    Color.Yellow.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = hitPos,
                radius = gradientRadius
            )
            drawCircle(
                brush = heatmapBrush,
                radius = gradientRadius,
                center = hitPos
            )
            // Kleiner sichtbarer Punkt für exakten Trefferort
            drawCircle(
                color = Color.Yellow,
                radius = 2.5f,
                center = hitPos
            )
        }
    }
}

private fun DrawScope.drawStaticBoard(
    center: Offset,
    radius: Float,
    scores: List<Int>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    showNumbers: Boolean = true
) {
    val bullseyeRadius = radius * 0.037f
    val outerBullRadius = radius * 0.093f
    val tripleRingInnerRadius = radius * 0.571f
    val tripleRingOuterRadius = radius * 0.629f
    val doubleRingInnerRadius = radius * 0.941f
    val doubleRingOuterRadius = radius * 1.000f
    val outerRingRadius = radius * 1.25f

    // Schwarzer Außenring für Zahlen
    drawCircle(color = Color.Black, radius = outerRingRadius, center = center)
    
    // Hauptbereiche der Segmente
    val segmentAngle = 18f
    for (i in scores.indices) {
        val startAngle = (segmentAngle * (i - 0.5f)) - 90f
        val segmentColor = if (i % 2 != 0) dbBeige else dbBlack
        drawArc(
            color = segmentColor,
            startAngle = startAngle,
            sweepAngle = segmentAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        val ringColor = if (i % 2 != 0) dbGreen else dbRed
        
        // Triple Ring (Score x 3)
        val triplePathRadius = (tripleRingInnerRadius + tripleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - triplePathRadius, center.y - triplePathRadius),
            size = Size(triplePathRadius * 2, triplePathRadius * 2),
            style = Stroke(width = tripleRingOuterRadius - tripleRingInnerRadius)
        )
        
        // Double Ring (Score x 2)
        val doublePathRadius = (doubleRingInnerRadius + doubleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - doublePathRadius, center.y - doublePathRadius),
            size = Size(doublePathRadius * 2, doublePathRadius * 2),
            style = Stroke(width = doubleRingOuterRadius - doubleRingInnerRadius)
        )
    }
    
    // Bull's Eye
    drawCircle(color = dbGreen, radius = outerBullRadius, center = center)
    drawCircle(color = dbRed, radius = bullseyeRadius, center = center)

    // Zahlen am Rand zeichnen
    if (showNumbers && radius > 35f) {
        val numberRadius = radius * 1.125f // Exakte Mitte zwischen Dartboard (1.0) und Außenrand (1.25)
        scores.forEachIndexed { index, score ->
            val angle = (segmentAngle * index) - 90f
            val angleRad = Math.toRadians(angle.toDouble())
            val textLayoutResult = textMeasurer.measure(
                text = score.toString(),
                style = TextStyle(color = Color.White, fontSize = (radius * 0.18f).toSp())
            )
            
            val x = (center.x + numberRadius * cos(angleRad)).toFloat() - textLayoutResult.size.width / 2
            val y = (center.y + numberRadius * sin(angleRad)).toFloat() - textLayoutResult.size.height / 2
            drawText(textLayoutResult, topLeft = Offset(x, y))
        }
    }
}
