package com.sdart.app.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.currentTimeMillis
import com.sdart.app.domain.STANDARD_DARTBOARD_SEGMENTS
import com.sdart.app.domain.calculateDartScore
import com.sdart.app.toRadians
import com.sdart.app.ui.theme.dartboardBeige
import com.sdart.app.ui.theme.dartboardBlack
import com.sdart.app.ui.theme.dartboardGreen
import com.sdart.app.ui.theme.dartboardRed
import kotlin.math.*

@Composable
fun InteractiveDartboard(
    thrownDarts: List<Offset>,
    onDartThrown: (score: Int, multiplier: Int, position: Offset) -> Unit,
    onMagnifierUpdate: (Offset?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    // Hervorzuhebende Zahlen (25 = Bull); ist die Menge nicht leer, werden alle anderen Felder abgedunkelt
    highlightedSegments: Set<Int> = emptySet()
) {
    val textMeasurer = rememberTextMeasurer()
    val scores = STANDARD_DARTBOARD_SEGMENTS

    var magnifierCenter by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val startTime = currentTimeMillis()
                val startPosition = down.position
                
                magnifierCenter = startPosition
                isDragging = true
                var hasMoved = false
                var currentPos = startPosition
                val pointerId = down.id

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == pointerId }
                        if (change == null || !change.pressed) break
                        
                        currentPos = change.position
                        magnifierCenter = currentPos
                        
                        if ((currentPos - startPosition).getDistance() > 5f) {
                            hasMoved = true
                        }
                        change.consume()
                    }
                    
                    val duration = currentTimeMillis() - startTime
                    val isQuickTap = duration < 200L && !hasMoved
                    
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (min(size.width, size.height) / 2f) / 1.25f
                    
                    val finalPos = if (isQuickTap) startPosition else currentPos
                    
                    val distance = sqrt(
                        (finalPos.x - center.x).pow(2) + (finalPos.y - center.y).pow(2)
                    )
                    
                    if (distance <= radius * 1.25f) {
                        val (score, multiplier) = calculateDartScore(finalPos, center, radius, scores)
                        
                        onDartThrown(
                            score, 
                            multiplier, 
                            Offset((finalPos.x - center.x) / radius, (finalPos.y - center.y) / radius)
                        )
                    }
                } finally {
                    isDragging = false
                    magnifierCenter = null
                }
            }
        }
    ) {
        drawBoardContent(scores, textMeasurer, highlightedSegments)

        val center = this.center
        val radius = (min(size.width, size.height) / 2f) / 1.25f
        thrownDarts.forEach { normalizedPosition ->
            val dartPosition = Offset(
                center.x + normalizedPosition.x * radius,
                center.y + normalizedPosition.y * radius
            )
            drawCircle(color = Color.Yellow, radius = 8f, center = dartPosition)
            drawCircle(color = Color.Black, radius = 8f, center = dartPosition, style = Stroke(width = 3f))
        }

        if (isDragging && magnifierCenter != null) {
            onMagnifierUpdate(magnifierCenter, true)
        } else {
            onMagnifierUpdate(null, false)
        }
    }
}

private fun DrawScope.drawBoardContent(
    scores: List<Int>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    highlightedSegments: Set<Int> = emptySet()
) {
    val center = this.center
    val radius = (min(size.width, size.height) / 2f) / 1.25f

    val bullseyeRadius = radius * 0.037f
    val outerBullRadius = radius * 0.093f
    val tripleRingInnerRadius = radius * 0.571f
    val tripleRingOuterRadius = radius * 0.629f
    val doubleRingInnerRadius = radius * 0.941f
    val doubleRingOuterRadius = radius * 1.000f
    val outerRingRadius = radius * 1.25f

    drawCircle(color = dartboardBlack, radius = outerRingRadius)
    drawCircle(color = dartboardBlack, radius = radius)
    
    val segmentAngle = 18f
    for (i in scores.indices) {
        val startAngle = (segmentAngle * (i - 0.5f)) - 90f
        val segmentColor = if (i % 2 != 0) dartboardBeige else dartboardBlack
        drawArc(
            color = segmentColor,
            startAngle = startAngle,
            sweepAngle = segmentAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        val ringColor = if (i % 2 != 0) dartboardGreen else dartboardRed
        
        val triplePathRadius = (tripleRingInnerRadius + tripleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - triplePathRadius, center.y - triplePathRadius),
            size = Size(triplePathRadius * 2, triplePathRadius * 2),
            style = Stroke(width = tripleRingOuterRadius - tripleRingInnerRadius)
        )
        
        val doublePathRadius = (doubleRingInnerRadius + doubleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - doublePathRadius, center.y - doublePathRadius),
            size = Size(doublePathRadius * 2, doublePathRadius * 2),
            style = Stroke(width = doubleRingOuterRadius - doubleRingInnerRadius)
        )
    }
    drawCircle(color = dartboardGreen, radius = outerBullRadius, center = center)
    drawCircle(color = dartboardRed, radius = bullseyeRadius, center = center)

    val highlighting = highlightedSegments.isNotEmpty()
    if (highlighting) {
        drawSegmentHighlights(scores, highlightedSegments, center, radius, outerBullRadius)
    }

    val numberRadius = radius * 1.125f
    scores.forEachIndexed { index, score ->
        val angle = (segmentAngle * index) - 90f
        val angleRad = toRadians(angle.toDouble())
        val text = score.toString()
        val numberColor = when {
            !highlighting -> Color.White
            score in highlightedSegments -> HighlightColor
            else -> Color.White.copy(alpha = 0.3f)
        }
        val textLayoutResult = textMeasurer.measure(
            text,
            style = TextStyle(
                color = numberColor,
                fontSize = (radius * 0.18f).toSp(),
                fontWeight = if (highlighting && score in highlightedSegments) FontWeight.Bold else FontWeight.Normal
            )
        )
        val textWidth = textLayoutResult.size.width
        val textHeight = textLayoutResult.size.height

        val x = (center.x + numberRadius * cos(angleRad)).toFloat() - textWidth / 2
        val y = (center.y + numberRadius * sin(angleRad)).toFloat() - textHeight / 2
        drawText(textLayoutResult = textLayoutResult, topLeft = Offset(x, y))
    }
}

private val HighlightColor = Color(0xFFFF8800)
private const val OutlineWidth = 5f


/**
 * Dunkelt alle Felder ab, die nicht in [highlighted] stehen, und umrandet die offenen Segmente
 * (und das Bull, falls 25 enthalten ist) orange.
 */
private fun DrawScope.drawSegmentHighlights(
    scores: List<Int>,
    highlighted: Set<Int>,
    center: Offset,
    radius: Float,
    outerBullRadius: Float
) {
    val segmentAngle = 18f
    val boardTopLeft = Offset(center.x - radius, center.y - radius)
    val boardSize = Size(radius * 2, radius * 2)
    val dim = Color.Black.copy(alpha = 0.6f)

    // Nicht hervorgehobene Segmente abdunkeln (deckt auch das Bull ab, das unten bei Bedarf neu gezeichnet wird)
    scores.forEachIndexed { i, score ->
        if (score !in highlighted) {
            drawArc(
                color = dim,
                startAngle = (segmentAngle * (i - 0.5f)) - 90f,
                sweepAngle = segmentAngle,
                useCenter = true,
                topLeft = boardTopLeft,
                size = boardSize
            )
        }
    }

    if (25 in highlighted) {
        drawCircle(color = dartboardGreen, radius = outerBullRadius, center = center)
        drawCircle(color = dartboardRed, radius = outerBullRadius * (0.037f / 0.093f), center = center)
        drawCircle(color = HighlightColor, radius = outerBullRadius, center = center, style = Stroke(width = OutlineWidth))
    } else {
        drawCircle(color = dim, radius = outerBullRadius, center = center)
    }

    // Orangefarbene Umrandung der offenen Segmente vom Bull bis zum Double-Ring
    val innerTopLeft = Offset(center.x - outerBullRadius, center.y - outerBullRadius)
    val innerSize = Size(outerBullRadius * 2, outerBullRadius * 2)
    scores.forEachIndexed { i, score ->
        if (score in highlighted) {
            val start = (segmentAngle * (i - 0.5f)) - 90f
            val outline = Path().apply {
                arcTo(Rect(boardTopLeft, boardSize), start, segmentAngle, forceMoveTo = true)
                arcTo(Rect(innerTopLeft, innerSize), start + segmentAngle, -segmentAngle, forceMoveTo = false)
                close()
            }
            drawPath(outline, color = HighlightColor, style = Stroke(width = OutlineWidth))
        }
    }
}

private fun DrawScope.drawBoardContentAtPosition(
    scores: List<Int>, 
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    center: Offset,
    radius: Float
) {
    val bullseyeRadius = radius * 0.037f
    val outerBullRadius = radius * 0.093f
    val tripleRingInnerRadius = radius * 0.571f
    val tripleRingOuterRadius = radius * 0.629f
    val doubleRingInnerRadius = radius * 0.941f
    val doubleRingOuterRadius = radius * 1.000f
    val outerRingRadius = radius * 1.25f

    drawCircle(color = dartboardBlack, radius = outerRingRadius, center = center)
    drawCircle(color = dartboardBlack, radius = radius, center = center)
    
    val segmentAngle = 18f
    for (i in scores.indices) {
        val startAngle = (segmentAngle * (i - 0.5f)) - 90f
        val segmentColor = if (i % 2 != 0) dartboardBeige else dartboardBlack
        drawArc(
            color = segmentColor, 
            startAngle = startAngle, 
            sweepAngle = segmentAngle, 
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        val ringColor = if (i % 2 != 0) dartboardGreen else dartboardRed
        
        val triplePathRadius = (tripleRingInnerRadius + tripleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - triplePathRadius, center.y - triplePathRadius),
            size = Size(triplePathRadius * 2, triplePathRadius * 2),
            style = Stroke(width = tripleRingOuterRadius - tripleRingInnerRadius)
        )
        
        val doublePathRadius = (doubleRingInnerRadius + doubleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - doublePathRadius, center.y - doublePathRadius),
            size = Size(doublePathRadius * 2, doublePathRadius * 2),
            style = Stroke(width = doubleRingOuterRadius - doubleRingInnerRadius)
        )
    }
    drawCircle(color = dartboardGreen, radius = outerBullRadius, center = center)
    drawCircle(color = dartboardRed, radius = bullseyeRadius, center = center)

    val numberRadius = radius * 1.125f
    scores.forEachIndexed { index, score ->
        val angle = (segmentAngle * index) - 90f
        val angleRad = toRadians(angle.toDouble())
        val text = score.toString()
        val textLayoutResult = textMeasurer.measure(text, style = TextStyle(color = Color.White, fontSize = (radius * 0.18f).toSp()))
        val textWidth = textLayoutResult.size.width
        val textHeight = textLayoutResult.size.height

        val x = (center.x + numberRadius * cos(angleRad)).toFloat() - textWidth / 2
        val y = (center.y + numberRadius * sin(angleRad)).toFloat() - textHeight / 2
        drawText(textLayoutResult = textLayoutResult, topLeft = Offset(x, y))
    }
}

@Composable
fun MagnifierOverlay(
    localMagnifierCenter: Offset,
    dartboardBounds: Rect,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val scores = STANDARD_DARTBOARD_SEGMENTS
    
    BoxWithConstraints(modifier = modifier) {
        val magnifierCenter = localMagnifierCenter + dartboardBounds.topLeft
        val density = LocalDensity.current
        val width = with(density) { maxWidth.toPx() }
        val transitionZone = width / 2f 
        val magnifierRadius = with(density) { 80.dp.toPx() }
        val horizontalOffset = with(density) { 70.dp.toPx() }
        val verticalOffset = with(density) { 80.dp.toPx() }
        
        val centerX = width / 2f
        val relativeX = (magnifierCenter.x - centerX).coerceIn(-transitionZone, transitionZone)
        val horizontalOffsetFactor = -relativeX / transitionZone
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dartboardCenter = Offset(
                x = dartboardBounds.left + dartboardBounds.width / 2f,
                y = dartboardBounds.top + dartboardBounds.height / 2f
            )
            val radius = (min(dartboardBounds.width, dartboardBounds.height) / 2f) / 1.25f
            val zoomFactor = 2.5f
            
            val targetX = magnifierCenter.x + (horizontalOffsetFactor * horizontalOffset)
            val targetY = magnifierCenter.y - verticalOffset
            val loupeCenter = Offset(x = targetX, y = targetY)
            
            val loupeRect = Rect(
                left = loupeCenter.x - magnifierRadius,
                top = loupeCenter.y - magnifierRadius,
                right = loupeCenter.x + magnifierRadius,
                bottom = loupeCenter.y + magnifierRadius
            )
            
            clipPath(Path().apply { addOval(loupeRect) }) {
                withTransform({
                    translate(loupeCenter.x - magnifierCenter.x, loupeCenter.y - magnifierCenter.y)
                    scale(zoomFactor, zoomFactor, pivot = magnifierCenter)
                }) {
                    drawBoardContentAtPosition(scores, textMeasurer, dartboardCenter, radius)
                }
            }
            
            val crossSize = 20f
            val crossColor = Color.Yellow
            val crossThickness = 3f
            
            drawLine(
                color = crossColor,
                start = Offset(loupeCenter.x - crossSize, loupeCenter.y),
                end = Offset(loupeCenter.x + crossSize, loupeCenter.y),
                strokeWidth = crossThickness
            )
            
            drawLine(
                color = crossColor,
                start = Offset(loupeCenter.x, loupeCenter.y - crossSize),
                end = Offset(loupeCenter.x, loupeCenter.y + crossSize),
                strokeWidth = crossThickness
            )
            
            val (selectedScore, selectedMultiplier) = calculateDartScore(magnifierCenter, dartboardCenter, radius, scores)
            val displayText = when {
                selectedScore == 0 && selectedMultiplier == 0 -> "Miss"
                selectedScore == 25 && selectedMultiplier == 2 -> "DBull"
                selectedScore == 25 && selectedMultiplier == 1 -> "Bull"
                selectedMultiplier == 3 -> "T$selectedScore"
                selectedMultiplier == 2 -> "D$selectedScore"
                selectedMultiplier == 1 -> "$selectedScore"
                else -> "Miss"
            }
            
            val textLayoutResult = textMeasurer.measure(
                displayText,
                style = TextStyle(color = Color.White, fontSize = 16.sp)
            )
            
            drawRect(
                color = Color.Black.copy(alpha = 0.8f),
                topLeft = Offset(loupeCenter.x - textLayoutResult.size.width / 2 - 8f, loupeCenter.y - magnifierRadius - 40f),
                size = Size(textLayoutResult.size.width + 16f, textLayoutResult.size.height + 16f)
            )
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(loupeCenter.x - textLayoutResult.size.width / 2, loupeCenter.y - magnifierRadius - 32f)
            )
        }
    }
}
