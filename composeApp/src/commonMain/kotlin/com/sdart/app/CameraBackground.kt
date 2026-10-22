package com.sdart.app

import com.sdart.app.core.detection.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.SessionManager
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.ditheredBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

/**
 * Geteiltes Multiplattform-Kamera-Overlay & Auto-Scoring-Pipeline in commonMain.
 * Verwaltet:
 * 1. Plattform-unabhängiges Rendering des Overlays (Visier-Brackets, Abstands-Warnungen, Kalibrierungsgitter, Bounding-Boxes, ROI, Debug-HUD).
 * 2. Homographie-Kalibrierung aus Detections.
 * 3. Zeitliche Stabilität, Drift-Tracking und Punkteverbuchung (Auto-Scoring).
 * 4. Erkennung des leergeräumten Dartboards (Turn-Ende & Auto-Misses).
 */
@Composable
fun CameraBackground(
    modifier: Modifier = Modifier,
    vm: DartViewModel,
    showCamera: Boolean = true,
    showOverlay: Boolean = true,
    dartboardBounds: Rect? = null,
    sessionManager: SessionManager? = null,
    participantManager: ParticipantSessionManager? = null,
    autoDetectionSystem: AutoDetectionSystem = remember { AutoDetectionSystem() },
    calibrationSystem: DartboardCalibrationSystem = remember { DartboardCalibrationSystem() }
) {
    var currentDetections by remember { mutableStateOf<List<ObjectDetection>>(emptyList()) }
    var frameWidth by remember { mutableStateOf(1280) }
    var frameHeight by remember { mutableStateOf(720) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var telemetry by remember { mutableStateOf(CameraTelemetry()) }

    LaunchedEffect(autoDetectionSystem) {
        autoDetectionSystem.enable()
    }

    // Auto-Scoring & Board-Empty Pipeline (Coroutines auf dem Main-Dispatcher)
    LaunchedEffect(currentDetections, vm.game) {
        val currentGame = vm.game ?: return@LaunchedEffect

        // 1. Auto-Kalibrierung aus Board-/Spider-Detektionen
        if (currentDetections.isNotEmpty() && canvasSize.width > 0f && canvasSize.height > 0f) {
            val boardDetections = currentDetections.filter {
                it.classId == 2 || it.classId == 3 ||
                it.className.lowercase().contains("board") ||
                it.className.lowercase().contains("marker")
            }
            if (boardDetections.isNotEmpty()) {
                calibrationSystem.autoCalibrateFromDetections(
                    detections = boardDetections,
                    canvasWidth = canvasSize.width.toInt(),
                    canvasHeight = canvasSize.height.toInt(),
                    frameWidth = frameWidth.toFloat(),
                    frameHeight = frameHeight.toFloat()
                )
            }
        }

        // 2. Spielstatus im AutoDetectionSystem aktualisieren (Rundenwechsel, Bust, Undo)
        if (currentGame.currentLeg == 1 && currentGame.throwHistory.isEmpty() && currentGame.throwsInRound == 0) {
            autoDetectionSystem.resetTrackingIfNeeded()
        } else {
            autoDetectionSystem.updateGameState(
                currentPlayerIndex = currentGame.currentPlayerIndex,
                throwsInRound = currentGame.throwsInRound,
                throwHistorySize = currentGame.throwHistory.size,
                currentLeg = currentGame.currentLeg
            )
        }

        if (!calibrationSystem.isCalibrated() || canvasSize == Size.Zero || !autoDetectionSystem.isEnabled) {
            return@LaunchedEffect
        }

        // 3. Transformation Canvas-Skalierung (FILL_CENTER)
        val camW = frameWidth.toFloat()
        val camH = frameHeight.toFloat()
        val scale = max(canvasSize.width / camW, canvasSize.height / camH)
        val offsetX = (canvasSize.width - camW * scale) / 2f
        val offsetY = (canvasSize.height - camH * scale) / 2f

        // 4. Spitzen (class 1) und Körper (class 0) extrahieren & in normalisierte Board-Koordinaten umrechnen
        val dartTips = currentDetections.filter { it.classId == 1 }.mapNotNull { det ->
            val cx = (det.boundingBox.left + det.boundingBox.right) / 2f
            val cy = (det.boundingBox.top + det.boundingBox.bottom) / 2f
            val canvasX = cx * camW * scale + offsetX
            val canvasY = cy * camH * scale + offsetY
            val normPos = calibrationSystem.getNormalizedBoardPosition(canvasX.toDouble(), canvasY.toDouble())
            if (normPos != null) PointF(normPos.x, normPos.y) to det else null
        }

        val dartBodies = currentDetections.filter { it.classId == 0 }.mapNotNull { det ->
            val cx = (det.boundingBox.left + det.boundingBox.right) / 2f
            val cy = (det.boundingBox.top + det.boundingBox.bottom) / 2f
            val canvasX = cx * camW * scale + offsetX
            val canvasY = cy * camH * scale + offsetY
            val normPos = calibrationSystem.getNormalizedBoardPosition(canvasX.toDouble(), canvasY.toDouble())
            if (normPos != null) PointF(normPos.x, normPos.y) to det else null
        }

        // 5. Ungebuchte Darts ermitteln
        val unbooked = autoDetectionSystem.findUnbookedDarts(dartTips, dartBodies)
        var dartsProcessedThisFrame = 0

        for ((pos, det) in unbooked) {
            val currentThrows = currentGame.throwsInRound
            if (currentThrows + dartsProcessedThisFrame >= 3) {
                break
            }

            val normalizedPosition = Offset(pos.x, pos.y)
            val segment = calibrationSystem.getScoreFromNormalizedPosition(normalizedPosition)

            if (segment != null) {
                val intMultiplier = when (segment.multiplier.lowercase()) {
                    "d" -> 2
                    "t" -> 3
                    else -> 1
                }
                vm.enterScoreML(
                    score = segment.scoreVal,
                    confidence = det.confidence.toDouble(),
                    position = normalizedPosition,
                    multiplier = intMultiplier
                )
                autoDetectionSystem.markDartAsBooked(pos)
                dartsProcessedThisFrame++
            } else {
                // Dart außerhalb des Radius -> als MISS verbuchen
                vm.enterScoreML(
                    score = 0,
                    confidence = det.confidence.toDouble(),
                    position = normalizedPosition,
                    multiplier = 0
                )
                autoDetectionSystem.markDartAsBooked(pos)
                dartsProcessedThisFrame++
            }
        }

        // 6. Board Empty Erkennung
        val currentDartCount = currentDetections.count { it.classId == 0 || it.classId == 1 }
        val isBoardVisible = currentDetections.any {
            it.classId == 2 || it.classId == 3 || it.className.lowercase().contains("board")
        }
        val status = autoDetectionSystem.checkBoardEmptyStatus(
            currentDartCount = currentDartCount,
            currentThrowCount = vm.game?.throwsInRound ?: 0,
            isBoardVisible = isBoardVisible
        )

        if (status != null) {
            when (status.action) {
                BoardAction.RESET -> autoDetectionSystem.resetTracking()
                BoardAction.AUTO_MISS -> {
                    repeat(status.count) {
                        vm.enterScore(0)
                    }
                    autoDetectionSystem.resetTracking()
                }
                BoardAction.ROUND_COMPLETE -> {
                    autoDetectionSystem.resetTracking()
                }
                else -> {}
            }
        } else if (currentDartCount == 0 && autoDetectionSystem.isEnabled) {
            // Polling-Schleife für leeres Board, damit das Timeout zuverlässig anspringt
            while (isActive) {
                delay(100)
                val delayedStatus = autoDetectionSystem.checkBoardEmptyStatus(
                    currentDartCount = 0,
                    currentThrowCount = vm.game?.throwsInRound ?: 0,
                    isBoardVisible = isBoardVisible
                )
                if (delayedStatus != null) {
                    when (delayedStatus.action) {
                        BoardAction.RESET -> autoDetectionSystem.resetTracking()
                        BoardAction.AUTO_MISS -> {
                            repeat(delayedStatus.count) {
                                vm.enterScore(0)
                            }
                            autoDetectionSystem.resetTracking()
                        }
                        BoardAction.ROUND_COMPLETE -> {
                            autoDetectionSystem.resetTracking()
                        }
                        else -> {}
                    }
                    break
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orange_flash")
    val orangeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .ditheredBackground()
    ) {
        // 1. Nativer Video-Feed (Kamera / Stream)
        CameraVideoFeed(
            modifier = Modifier.fillMaxSize(),
            vm = vm,
            showCamera = showCamera,
            onDetectionsUpdated = { currentDetections = it },
            onFrameSizeChanged = { w, h ->
                frameWidth = w
                frameHeight = h
            },
            onTelemetryUpdated = { telemetry = it },
            sessionManager = sessionManager,
            participantManager = participantManager
        )

        // 2. Einheitliches UI-Overlay & Visier
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .onGloballyPositioned {
                    canvasSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
                }
        ) {
            val centerX: Float
            val centerY: Float
            val targetRadius: Float

            if (dartboardBounds != null) {
                centerX = dartboardBounds.left + dartboardBounds.width / 2f
                centerY = dartboardBounds.top + dartboardBounds.height / 2f
                targetRadius = (min(dartboardBounds.width, dartboardBounds.height) / 2f) * 0.93f
            } else {
                val minCanvasDimension = min(size.width, size.height)
                targetRadius = (minCanvasDimension * 0.775f) / 2f
                centerX = size.width / 2f
                centerY = size.height / 2f
            }

            val camW = frameWidth.toFloat()
            val camH = frameHeight.toFloat()
            val canvasScale = max(size.width / camW, size.height / camH)
            val offX = (size.width - camW * canvasScale) / 2f
            val offY = (size.height - camH * canvasScale) / 2f

            // Rein digitaler Anzeige-Zoom der Plattform (Desktop): Overlays um die Canvas-Mitte mitskalieren,
            // damit sie deckungsgleich zum skalierten Videobild bleiben. Visier-Brackets bleiben fix.
            val displayZoom = telemetry.displayZoom
            val zoomPivotX = size.width / 2f
            val zoomPivotY = size.height / 2f
            fun zx(x: Float) = zoomPivotX + (x - zoomPivotX) * displayZoom
            fun zy(y: Float) = zoomPivotY + (y - zoomPivotY) * displayZoom

            val isCalibrated = calibrationSystem.isCalibrated()
            val hasActiveBoardDetection = currentDetections.any {
                it.classId == 2 || it.classId == 3 || it.className.lowercase().contains("board")
            }
            val isCalibratedAndVisible = isCalibrated && hasActiveBoardDetection

            if (showOverlay) {
                // Kalibriertes Dartboard-Segmentgitter + Marker zeichnen (wie in der Original-App)
                if (isCalibratedAndVisible && vm.showDartboardOverlay) {
                    val polygons = calibrationSystem.getTransformedOverlayPolygons()
                    polygons.forEachIndexed { index, polyPoints ->
                        if (polyPoints.size < 2) return@forEachIndexed
                        val path = Path().apply {
                            moveTo(zx(polyPoints[0].x), zy(polyPoints[0].y))
                            for (i in 1 until polyPoints.size) {
                                lineTo(zx(polyPoints[i].x), zy(polyPoints[i].y))
                            }
                            close()
                        }
                        when (index) {
                            0 -> drawPath(path, Color.Red, style = Stroke(width = 2.dp.toPx()))   // Double Bull
                            1 -> drawPath(path, Color.Green, style = Stroke(width = 2.dp.toPx())) // Single Bull
                            else -> drawPath(path, Color.Green.copy(alpha = 0.6f), style = Stroke(width = 1.dp.toPx()))
                        }
                    }

                    calibrationSystem.getOverlayCalibrationMarkers()?.let { (idealControl, detectedBoundary, idealBoundary) ->
                        for (pt in idealControl) {
                            drawCircle(color = Color.Green, radius = 6.dp.toPx(), center = Offset(zx(pt.x), zy(pt.y)))
                        }
                        for (pt in detectedBoundary) {
                            drawCircle(color = Color.Yellow, radius = 8.dp.toPx(), center = Offset(zx(pt.x), zy(pt.y)))
                        }
                        for (pt in idealBoundary) {
                            drawCircle(color = Color.Cyan, radius = 5.dp.toPx(), center = Offset(zx(pt.x), zy(pt.y)))
                        }
                    }
                }

                val distanceStatus = calibrationSystem.getBoardDistanceStatus(
                    canvasWidth = size.width.toInt(),
                    canvasHeight = size.height.toInt(),
                    displayZoom = displayZoom
                )
                val isDistanceOk = distanceStatus == BoardDistanceStatus.OK

                val bracketColor = when {
                    isCalibratedAndVisible && isDistanceOk -> Color.Green
                    isCalibratedAndVisible && !isDistanceOk -> null
                    else -> Color.White
                }

                if (bracketColor != null) {
                    val legLength = targetRadius * 0.25f
                    val cornerRadius = 16.dp.toPx()
                    val bracketStrokeWidth = 6.dp.toPx()

                    // Top-Left
                    val pathTL = Path().apply {
                        moveTo(centerX - targetRadius + legLength, centerY - targetRadius)
                        lineTo(centerX - targetRadius + cornerRadius, centerY - targetRadius)
                        quadraticTo(
                            centerX - targetRadius, centerY - targetRadius,
                            centerX - targetRadius, centerY - targetRadius + cornerRadius
                        )
                        lineTo(centerX - targetRadius, centerY - targetRadius + legLength)
                    }
                    drawPath(pathTL, bracketColor, style = Stroke(bracketStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Top-Right
                    val pathTR = Path().apply {
                        moveTo(centerX + targetRadius - legLength, centerY - targetRadius)
                        lineTo(centerX + targetRadius - cornerRadius, centerY - targetRadius)
                        quadraticTo(
                            centerX + targetRadius, centerY - targetRadius,
                            centerX + targetRadius, centerY - targetRadius + cornerRadius
                        )
                        lineTo(centerX + targetRadius, centerY - targetRadius + legLength)
                    }
                    drawPath(pathTR, bracketColor, style = Stroke(bracketStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Bottom-Left
                    val pathBL = Path().apply {
                        moveTo(centerX - targetRadius, centerY + targetRadius - legLength)
                        lineTo(centerX - targetRadius, centerY + targetRadius - cornerRadius)
                        quadraticTo(
                            centerX - targetRadius, centerY + targetRadius,
                            centerX - targetRadius + cornerRadius, centerY + targetRadius
                        )
                        lineTo(centerX - targetRadius + legLength, centerY + targetRadius)
                    }
                    drawPath(pathBL, bracketColor, style = Stroke(bracketStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Bottom-Right
                    val pathBR = Path().apply {
                        moveTo(centerX + targetRadius, centerY + targetRadius - legLength)
                        lineTo(centerX + targetRadius, centerY + targetRadius - cornerRadius)
                        quadraticTo(
                            centerX + targetRadius, centerY + targetRadius,
                            centerX + targetRadius - cornerRadius, centerY + targetRadius
                        )
                        lineTo(centerX + targetRadius - legLength, centerY + targetRadius)
                    }
                    drawPath(pathBR, bracketColor, style = Stroke(bracketStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                if (isCalibratedAndVisible && !isDistanceOk) {
                    drawCircle(
                        color = Color(0xFFFFA500).copy(alpha = orangeAlpha),
                        radius = targetRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                        )
                    )

                    val warningText = if (distanceStatus == BoardDistanceStatus.TOO_FAR) "ZU WEIT WEG" else "ZU NAH"
                    val textLayoutResult = textMeasurer.measure(
                        text = warningText,
                        style = TextStyle(
                            color = Color(0xFFFFA500).copy(alpha = orangeAlpha),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )

                    val startX = (size.width - textLayoutResult.size.width) / 2f
                    val startY = (size.height - textLayoutResult.size.height) / 2f
                    drawText(textMeasurer, warningText, Offset(startX, startY))
                }
            }

            // Erkannte Objekte (Bounding Boxes, Spitzen & Auto-Tracking ROI) zeichnen
            if (vm.showCameraDetectionOverlay) {
                telemetry.dynamicRoi?.let { roi ->
                    val rx1 = zx(roi.left * camW * canvasScale + offX)
                    val ry1 = zy(roi.top * camH * canvasScale + offY)
                    val rx2 = zx(roi.right * camW * canvasScale + offX)
                    val ry2 = zy(roi.bottom * camH * canvasScale + offY)
                    val roiColor = Color(0xFFFF9800)

                    drawRect(
                        color = roiColor,
                        topLeft = Offset(rx1, ry1),
                        size = Size(max(0f, rx2 - rx1), max(0f, ry2 - ry1)),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "Auto-Tracking ROI",
                        topLeft = Offset(rx1 + 8.dp.toPx(), max(0f, ry1 - 18.dp.toPx())),
                        style = TextStyle(color = roiColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }

                for (det in currentDetections) {
                    val bx1 = zx(det.boundingBox.left * camW * canvasScale + offX)
                    val by1 = zy(det.boundingBox.top * camH * canvasScale + offY)
                    val bx2 = zx(det.boundingBox.right * camW * canvasScale + offX)
                    val by2 = zy(det.boundingBox.bottom * camH * canvasScale + offY)

                    val boxColor = when (det.classId) {
                        0 -> Color.Green      // Dart Körper
                        1 -> Color.Cyan       // Dart Spitze
                        2 -> Color.Red        // Dartboard / Spider
                        3 -> Color.Yellow     // Marker 20
                        else -> Color.White
                    }

                    if (det.classId == 1) {
                        val tipX = (bx1 + bx2) / 2.0f
                        val tipY = (by1 + by2) / 2.0f
                        drawCircle(color = boxColor, radius = 6.dp.toPx(), center = Offset(tipX, tipY))
                    } else {
                        drawRect(
                            color = boxColor,
                            topLeft = Offset(bx1, by1),
                            size = Size(max(0f, bx2 - bx1), max(0f, by2 - by1)),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    val confPercent = (det.confidence * 100).toInt()
                    val label = "${det.className} $confPercent%"
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(bx1, max(0f, by1 - 16.dp.toPx())),
                        style = TextStyle(color = boxColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 3. Einheitliches Debug HUD (Inferenzzeit, FPS, Hardware/Quelle) in commonMain
        if (showCamera && vm.showCameraDebugHud) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .zIndex(15f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Inferenz",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${telemetry.inferenceTimeMs} ms",
                        color = when {
                            telemetry.inferenceTimeMs in 1..49 -> Color.Green
                            telemetry.inferenceTimeMs in 50..100 -> Color.Yellow
                            telemetry.inferenceTimeMs > 100 -> Color.Red
                            else -> Color.Gray
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .width(40.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    Text(
                        text = "FPS",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${telemetry.fps}",
                        color = if (telemetry.fps > 15) Color.Green else if (telemetry.fps > 5) Color.Yellow else Color.Red,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (telemetry.acceleratorLabel.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .width(40.dp)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Text(
                            text = "Hardware",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = telemetry.acceleratorLabel,
                            color = if (telemetry.gpuActive) Color.Cyan else Color(0xFFFF9800),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Warnbanner falls GPU-Initialisierung fehlgeschlagen ist (wie in der Original-App)
        if (showCamera && telemetry.gpuInitFailed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xFFD32F2F).copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .zIndex(20f)
            ) {
                Text(
                    text = "WARNUNG: Vulkan GPU konnte nicht aktiviert werden (CPU Fallback)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
