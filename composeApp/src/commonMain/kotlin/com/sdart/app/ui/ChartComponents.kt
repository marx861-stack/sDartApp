package com.sdart.app.ui

import com.sdart.app.GameHistoryRecord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.theme.PrimaryOrange
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun HighScoresBarChart(
    scores100: Int,
    scores140: Int,
    scores180: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val maxScore = max(5, max(scores100, max(scores140, scores180)))
    val data = listOf(
        Pair("100+", scores100),
        Pair("140+", scores140),
        Pair("180s", scores180)
    )
    val textColor = if (isDark) Color.White else Color.Black
    val trackColor = Color(0xFFFFFFFF).copy(alpha = 0.05f)
    
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
        data.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(30.dp)) {
                Text(
                    text = label,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.width(45.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(15.dp) // Gap between bars is handled by height difference
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Track (dimmed background)
                        drawRect(
                            color = trackColor,
                            size = size
                        )
                        // Bar Fill (Gradient)
                        val barWidth = (value.toFloat() / maxScore) * size.width
                        if (value > 0) {
                            val brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryOrange, Color(0xFFffaa33)),
                                startX = 0f,
                                endX = barWidth
                            )
                            drawRect(
                                brush = brush,
                                size = Size(barWidth, size.height)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = value.toString(),
                    color = textColor,
                    fontSize = 12.sp,
                    modifier = Modifier.width(30.dp)
                )
            }
        }
    }
}

@Composable
fun WinLossDonutChart(
    gamesWon: Int,
    gamesLost: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val total = gamesWon + gamesLost
    val winRatio = if (total > 0) gamesWon.toFloat() / total else 0f

    Box(modifier = modifier.fillMaxWidth().height(100.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (total == 0) {
                val noDataText = textMeasurer.measure(
                    text = "Keine Spiele absolviert",
                    style = TextStyle(color = Color.Gray, fontSize = 16.sp)
                )
                drawText(noDataText, topLeft = Offset(w / 2 - noDataText.size.width / 2, h / 2 - noDataText.size.height / 2))
                return@Canvas
            }

            val padding = 10.dp.toPx()
            val barH = 50.dp.toPx()
            val barY = h / 2 - barH / 2
            val barW = w - (padding * 2)
            val splitX = padding + (barW * winRatio)
            val slant = 15.dp.toPx()

            // Win Side (Left) - Orange Gradient
            if (gamesWon > 0) {
                val winPath = Path().apply {
                    moveTo(padding, barY)
                    lineTo(splitX + if (gamesLost > 0) slant else 0f, barY)
                    lineTo(splitX - if (gamesLost > 0) slant else 0f, barY + barH)
                    lineTo(padding, barY + barH)
                    close()
                }
                drawPath(
                    path = winPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFcc5500), PrimaryOrange),
                        startX = padding,
                        endX = splitX
                    )
                )

                // Win % Text
                val winPctText = textMeasurer.measure(
                    text = "${(winRatio * 100).toInt()}%",
                    style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                drawText(winPctText, topLeft = Offset(padding + 15.dp.toPx(), barY + barH / 2 - winPctText.size.height / 2))

                // WIN label above
                val winLabel = textMeasurer.measure(
                    text = "WIN: $gamesWon",
                    style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                drawText(winLabel, topLeft = Offset(padding + 15.dp.toPx(), barY - winLabel.size.height - 4.dp.toPx()))
            }

            // Loss Side (Right) - Dark Gradient
            if (gamesLost > 0) {
                val lossPath = Path().apply {
                    moveTo(splitX + if (gamesWon > 0) slant else 0f, barY)
                    lineTo(w - padding, barY)
                    lineTo(w - padding, barY + barH)
                    lineTo(splitX - if (gamesWon > 0) slant else 0f, barY + barH)
                    close()
                }
                drawPath(
                    path = lossPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF444444), Color(0xFF222222)),
                        startX = splitX,
                        endX = w - padding
                    )
                )

                // Loss % Text
                val lossPctText = textMeasurer.measure(
                    text = "${((1 - winRatio) * 100).toInt()}%",
                    style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                drawText(lossPctText, topLeft = Offset(w - padding - 15.dp.toPx() - lossPctText.size.width, barY + barH / 2 - lossPctText.size.height / 2))

                // LOSS label above
                val lossLabel = textMeasurer.measure(
                    text = "LOSS: $gamesLost",
                    style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                drawText(lossLabel, topLeft = Offset(w - padding - 15.dp.toPx() - lossLabel.size.width, barY - lossLabel.size.height - 4.dp.toPx()))
            }

            // Clash Line between Win and Loss
            if (gamesWon > 0 && gamesLost > 0) {
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(splitX + slant, barY),
                    end = Offset(splitX - slant, barY + barH),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun CheckoutPyramidChart(
    doubleHits: Map<Int, Int>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val dartboardOrder = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
    
    val maxHits = max(1, doubleHits.values.maxOrNull() ?: 1)
    
    Box(modifier = modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.height * 0.18f // approx 55px
            val maxSpikeH = size.height * 0.25f // approx 75px
            
            // Decorative rings
            drawCircle(
                color = PrimaryOrange.copy(alpha = 0.1f),
                radius = baseRadius - 5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.02f),
                radius = baseRadius + maxSpikeH + 20.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Draw Spikes
            dartboardOrder.forEachIndexed { i, num ->
                val hitVal = doubleHits[num] ?: 0
                val angleDeg = i * (360f / 20f) - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                
                val spikeH = (hitVal.toFloat() / maxHits) * maxSpikeH
                
                val xBase1 = center.x + cos(angleRad - 0.08f) * baseRadius
                val yBase1 = center.y + sin(angleRad - 0.08f) * baseRadius
                val xBase2 = center.x + cos(angleRad + 0.08f) * baseRadius
                val yBase2 = center.y + sin(angleRad + 0.08f) * baseRadius
                val xTip = center.x + cos(angleRad) * (baseRadius + spikeH)
                val yTip = center.y + sin(angleRad) * (baseRadius + spikeH)
                
                val path = Path().apply {
                    moveTo(xBase1, yBase1)
                    lineTo(xTip, yTip)
                    lineTo(xBase2, yBase2)
                    close()
                }
                
                if (hitVal > 0) {
                    // Fill Gradient
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryOrange.copy(alpha = 0.9f), PrimaryOrange.copy(alpha = 0.4f)),
                            center = center,
                            radius = baseRadius + maxSpikeH
                        )
                    )
                } else {
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.05f)
                    )
                }
                
                // Text
                val textRadius = baseRadius + spikeH + 15.dp.toPx()
                val hasHits = hitVal > 0
                val textColor = if (hasHits) PrimaryOrange else Color.Gray
                val fontSize = if (hasHits) 14.sp else 10.sp
                
                val textLayoutResult = textMeasurer.measure(
                    text = num.toString(),
                    style = TextStyle(color = textColor, fontSize = fontSize, fontWeight = if (hasHits) FontWeight.Bold else FontWeight.Normal)
                )
                
                val tx = center.x + cos(angleRad) * textRadius - textLayoutResult.size.width / 2
                val ty = center.y + sin(angleRad) * textRadius - textLayoutResult.size.height / 2
                drawText(textLayoutResult, topLeft = Offset(tx, ty))
            }
            
            // Center Labels
            val favText = textMeasurer.measure(
                text = "FAVORITE",
                style = TextStyle(color = if (isDark) Color.White else Color.Black, fontSize = 10.sp)
            )
            val checkoutText = textMeasurer.measure(
                text = "CHECKOUT",
                style = TextStyle(color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black)
            )
            
            drawText(favText, topLeft = Offset(center.x - favText.size.width / 2, center.y - 12.dp.toPx()))
            drawText(checkoutText, topLeft = Offset(center.x - checkoutText.size.width / 2, center.y + 4.dp.toPx()))
        }
    }
}

@Composable
fun HistoryLineChart(
    history: List<GameHistoryRecord>,
    isDark: Boolean,
    title: String,
    valueSelector: (GameHistoryRecord) -> Float,
    modifier: Modifier = Modifier
) {
    if (history.size < 2) {
        Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Mindestens 2 Spiele für Verlauf",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    
    Column(modifier = modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                val data = history.map { valueSelector(it) }
                val minVal = 0f
                var maxVal = data.maxOrNull() ?: 20f
                maxVal = (kotlin.math.ceil(maxVal.toDouble() / 10.0) * 10).toFloat()
                if (maxVal < 60f) maxVal = 60f
                
                val chartW = size.width - 40.dp.toPx()
                val chartH = size.height
                val startX = 40.dp.toPx()
                
                val xScale = chartW / (data.size - 1)
                val yScale = chartH / (maxVal - minVal)
                
                val getX = { i: Int -> startX + (i * xScale) }
                val getY = { v: Float -> chartH - ((v - minVal) * yScale) }
                
                // Draw Grid
                val gridColor = if (isDark) Color(0xFF444444) else Color(0xFFDDDDDD)
                val labelColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF888888)
                val steps = 5
                
                for (i in 0..steps) {
                    val v = minVal + (maxVal - minVal) * (i.toFloat() / steps)
                    val y = getY(v)
                    
                    drawLine(
                        color = gridColor,
                        start = Offset(startX, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    
                    val textLayout = textMeasurer.measure(
                        text = v.toInt().toString(),
                        style = TextStyle(color = labelColor, fontSize = 10.sp)
                    )
                    drawText(
                        textLayout,
                        topLeft = Offset(startX - textLayout.size.width - 8.dp.toPx(), y - textLayout.size.height / 2f)
                    )
                }
                
                // Draw Gradient Fill
                val path = Path().apply {
                    moveTo(getX(0), getY(data[0]))
                    for (i in 1 until data.size) {
                        lineTo(getX(i), getY(data[i]))
                    }
                    lineTo(getX(data.size - 1), chartH)
                    lineTo(getX(0), chartH)
                    close()
                }
                
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryOrange.copy(alpha = 0.5f), PrimaryOrange.copy(alpha = 0.0f)),
                        startY = 0f,
                        endY = chartH
                    )
                )
                
                // Draw Line
                val linePath = Path().apply {
                    moveTo(getX(0), getY(data[0]))
                    for (i in 1 until data.size) {
                        lineTo(getX(i), getY(data[i]))
                    }
                }
                
                drawPath(
                    path = linePath,
                    color = PrimaryOrange,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
                
                // Draw Dots
                data.forEachIndexed { i, v ->
                    val cx = getX(i)
                    val cy = getY(v)
                    drawCircle(color = if (isDark) Color.Black else Color.White, radius = 4.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = PrimaryOrange, radius = 4.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}

/**
 * Hit Distribution Chart - Zeigt die Treffer-Verteilung pro Segment (1-20 + Bull)
 * Exakte Nachbildung von renderHitDistribution aus der Windows-App
 */
@Composable
fun HitDistributionChart(
    segmentHits: Map<String, Int>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Aggregate by number (1-20, 25=Bull)
    val counts = IntArray(26) // index 0 unused, 1-20, 25=Bull
    segmentHits.forEach { (key, count) ->
        val parts = key.split("_")
        val num = parts[0].toIntOrNull() ?: return@forEach
        if (num in 1..20) counts[num] += count
        else if (num == 25) counts[25] += count
    }

    val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 25)
    var maxVal = numbers.maxOfOrNull { counts[it] } ?: 0
    maxVal = ((maxVal + 4) / 5) * 5 // Round up to nearest 5
    if (maxVal < 10) maxVal = 10

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingLeft = 40.dp.toPx()
                val paddingRight = 20.dp.toPx()
                val paddingTop = 10.dp.toPx()
                val paddingBottom = 30.dp.toPx()
                val chartW = size.width - paddingLeft - paddingRight
                val chartH = size.height - paddingTop - paddingBottom

                val barW = chartW / numbers.size
                val gridColor = if (isDark) Color(0xFF444444) else Color(0xFFDDDDDD)
                val labelColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF888888)

                // Grid Lines
                val steps = 5
                for (i in 0..steps) {
                    val v = (maxVal * i) / steps
                    val y = paddingTop + chartH - ((v.toFloat() / maxVal) * chartH)

                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1f
                    )

                    val label = textMeasurer.measure(
                        text = v.toString(),
                        style = TextStyle(color = labelColor, fontSize = 10.sp)
                    )
                    drawText(label, topLeft = Offset(paddingLeft - label.size.width - 5.dp.toPx(), y - label.size.height / 2))
                }

                // Bars
                numbers.forEachIndexed { i, num ->
                    val v = counts[num]
                    if (v == 0) return@forEachIndexed

                    val x = paddingLeft + (i * barW)
                    val barHeight = (v.toFloat() / maxVal) * chartH
                    val y = paddingTop + chartH - barHeight
                    val gap = 2.dp.toPx()

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryOrange, PrimaryOrange.copy(alpha = 0.4f)),
                            startY = y,
                            endY = y + barHeight
                        ),
                        topLeft = Offset(x + gap, y),
                        size = Size(barW - gap * 2, barHeight)
                    )
                }

                // X Axis Labels
                numbers.forEachIndexed { i, num ->
                    // Jede zweite Zahl ausblenden für bessere Lesbarkeit (Bull aber immer anzeigen)
                    if (i % 2 != 0 && num != 25) return@forEachIndexed
                    
                    val x = paddingLeft + (i * barW) + (barW / 2)
                    val labelText = if (num == 25) "B" else num.toString()
                    val label = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(color = labelColor, fontSize = 10.sp)
                    )
                    drawText(label, topLeft = Offset(x - label.size.width / 2, size.height - paddingBottom + 8.dp.toPx()))
                }
            }
        }
    }
}
