package com.example.myapplication

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * Gemeinsame Dart-Erkennungs-Pipeline für alle Plattformen (Android, Desktop, iOS).
 * Enthält Schwellenwerte, NMS (Non-Maximum Suppression) und zeitliche Stabilitätsfilterung.
 */
class DartDetectionPipeline(
    private val dartConfidenceThreshold: Float = 0.40f,
    private val tipConfidenceThreshold: Float = 0.40f,
    private val boardConfidenceThreshold: Float = 0.40f,
    private val markerConfidenceThreshold: Float = 0.40f,
    private val nmsThreshold: Float = 0.65f,
    private val centerDistanceThreshold: Float = 0.008f,
    private val minBoxSize: Float = 0.005f,
    private val maxDartDetections: Int = 20,
    private val maxDartboardDetections: Int = 20,
    private val historySize: Int = 5,
    private val minHistoryFramesRequired: Int = 2
) {
    private val detectionHistory = mutableListOf<List<ObjectDetection>>()
    private var lastKnownDartboardBox: Rect? = null

    fun reset() {
        detectionHistory.clear()
        lastKnownDartboardBox = null
    }

    /**
     * Parst Roh-Inferenz-Ausgabe im Format [x1, y1, x2, y2, score, classId]
     * Funktioniert sowohl mit Pixelkoordinaten (0..inputSize) als auch mit normalisierten Koordinaten (0..1).
     */
    fun parseRawOutput(rawOutput: FloatArray, inputSize: Int = 640): List<ObjectDetection> {
        if (rawOutput.isEmpty()) return emptyList()

        val detections = mutableListOf<ObjectDetection>()
        val numDetections = rawOutput.size / 6

        var isPixelCoordinates = false
        for (i in 0 until min(numDetections, 100)) {
            val idx = i * 6
            if (idx + 3 < rawOutput.size) {
                if (rawOutput[idx] > 2f || rawOutput[idx + 1] > 2f || rawOutput[idx + 2] > 2f || rawOutput[idx + 3] > 2f) {
                    isPixelCoordinates = true
                    break
                }
            }
        }

        val scale = if (isPixelCoordinates) inputSize.toFloat() else 1f

        for (detectionIdx in 0 until numDetections) {
            val baseIndex = detectionIdx * 6
            if (baseIndex + 5 >= rawOutput.size) continue

            val confidence = rawOutput[baseIndex + 4]
            val bestClassId = rawOutput[baseIndex + 5].toInt()

            val threshold = when (bestClassId) {
                3 -> markerConfidenceThreshold
                2 -> boardConfidenceThreshold
                1 -> tipConfidenceThreshold
                else -> dartConfidenceThreshold
            }

            if (confidence < threshold) continue

            val xMin = (rawOutput[baseIndex]     / scale).coerceIn(0f, 1f)
            val yMin = (rawOutput[baseIndex + 1] / scale).coerceIn(0f, 1f)
            val xMax = (rawOutput[baseIndex + 2] / scale).coerceIn(0f, 1f)
            val yMax = (rawOutput[baseIndex + 3] / scale).coerceIn(0f, 1f)

            if (xMax - xMin < minBoxSize || yMax - yMin < minBoxSize) continue

            detections.add(
                ObjectDetection(
                    boundingBox = Rect(xMin, yMin, xMax, yMax),
                    confidence = confidence,
                    classId = bestClassId,
                    className = getDartClassName(bestClassId)
                )
            )
        }
        return detections
    }

    /**
     * Führt Non-Maximum Suppression (NMS) pro Klasse durch.
     */
    fun applyNMS(detections: List<ObjectDetection>): List<ObjectDetection> {
        if (detections.size <= 1) return detections

        val groupedByClass = detections.groupBy { it.classId }
        val finalSelected = mutableListOf<ObjectDetection>()

        for ((classId, classDetections) in groupedByClass) {
            val maxKeep = if (classId == 2) maxDartboardDetections else maxDartDetections
            val sorted = classDetections.sortedByDescending { it.confidence }
            val selected = mutableListOf<ObjectDetection>()

            for (detection in sorted) {
                var shouldAdd = true
                for (sel in selected) {
                    val isDuplicate = if (classId == 1) {
                        calculateCenterDistance(detection.boundingBox, sel.boundingBox) < centerDistanceThreshold
                    } else {
                        calculateIoU(detection.boundingBox, sel.boundingBox) > nmsThreshold
                    }
                    if (isDuplicate) {
                        shouldAdd = false
                        break
                    }
                }
                if (shouldAdd) {
                    selected.add(detection)
                    if (selected.size >= maxKeep) break
                }
            }
            finalSelected.addAll(selected)
        }
        return finalSelected
    }

    /**
     * Zeitlicher Stabilitätsfilter: Nur Darts beibehalten, die über mehrere Frames konsistent sichtbar sind.
     */
    fun filterStableDetections(currentFrame: List<ObjectDetection>): List<ObjectDetection> {
        val boardDetections = currentFrame.filter { it.classId == 2 }
        if (boardDetections.isNotEmpty()) {
            var uLeft = boardDetections[0].boundingBox.left
            var uTop = boardDetections[0].boundingBox.top
            var uRight = boardDetections[0].boundingBox.right
            var uBottom = boardDetections[0].boundingBox.bottom
            for (det in boardDetections) {
                if (det.boundingBox.left < uLeft) uLeft = det.boundingBox.left
                if (det.boundingBox.top < uTop) uTop = det.boundingBox.top
                if (det.boundingBox.right > uRight) uRight = det.boundingBox.right
                if (det.boundingBox.bottom > uBottom) uBottom = det.boundingBox.bottom
            }
            lastKnownDartboardBox = Rect(uLeft, uTop, uRight, uBottom)
        }

        detectionHistory.add(currentFrame)
        if (detectionHistory.size > historySize) {
            detectionHistory.removeAt(0)
        }

        return currentFrame.filter { current ->
            if (current.classId == 2 || current.classId == 3) return@filter true

            lastKnownDartboardBox?.let { boardBox ->
                val marginX = boardBox.width * 0.15f
                val marginY = boardBox.height * 0.15f
                val expandedBoard = Rect(
                    boardBox.left - marginX,
                    boardBox.top - marginY,
                    boardBox.right + marginX,
                    boardBox.bottom + marginY
                )
                val centerX = (current.boundingBox.left + current.boundingBox.right) / 2f
                val centerY = (current.boundingBox.top + current.boundingBox.bottom) / 2f
                if (!expandedBoard.contains(Offset(centerX, centerY))) {
                    return@filter false
                }
            }

            var counts = 0
            for (historyFrame in detectionHistory) {
                val match = historyFrame.any { prev ->
                    if (prev.classId != current.classId) return@any false
                    if (current.classId == 1) {
                        calculateCenterDistance(current.boundingBox, prev.boundingBox) < 0.04f
                    } else {
                        calculateIoU(current.boundingBox, prev.boundingBox) > 0.3f
                    }
                }
                if (match) counts++
            }
            counts >= minHistoryFramesRequired
        }
    }

    private fun getDartClassName(classId: Int): String {
        val dartClasses = arrayOf("Dart", "Spitze", "Dartboard", "Marker")
        return if (classId in dartClasses.indices) dartClasses[classId] else "unknown_$classId"
    }

    private fun calculateCenterDistance(box1: Rect, box2: Rect): Float {
        val cx1 = (box1.left + box1.right) / 2f
        val cy1 = (box1.top + box1.bottom) / 2f
        val cx2 = (box2.left + box2.right) / 2f
        val cy2 = (box2.top + box2.bottom) / 2f
        val dx = cx1 - cx2
        val dy = cy1 - cy2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun calculateIoU(box1: Rect, box2: Rect): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)

        val intersectionArea = max(0f, intersectionRight - intersectionLeft) * max(0f, intersectionBottom - intersectionTop)
        val area1 = box1.width * box1.height
        val area2 = box2.width * box2.height
        val unionArea = area1 + area2 - intersectionArea

        return if (unionArea > 0f) intersectionArea / unionArea else 0f
    }
}
