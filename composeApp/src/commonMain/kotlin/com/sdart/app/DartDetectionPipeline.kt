package com.sdart.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * Parameter für das Top-Left Letterbox + Dynamic ROI Cropping eines Kamera-Frames.
 */
data class RoiCropParams(
    val rotatedWidth: Int,
    val rotatedHeight: Int,
    val baseRatio: Float,
    val frameSafeRoi: Rect,
    val roiScale: Float,
    val roiOffsetX: Float = 0f,
    val roiOffsetY: Float = 0f
)

/**
 * Gemeinsame Dart-Erkennungs-Pipeline für alle Plattformen (Android, Desktop, iOS).
 * 1:1 synchronisiert mit der Original-App (RTMDet Schwellenwerte, hybrides NMS,
 * zeitliche Stabilitätsfilterung über 3/5 Frames und Dynamic Auto-Tracking ROI mit 30% Padding).
 */
class DartDetectionPipeline(
    private val dartConfidenceThreshold: Float = 0.50f,
    private val tipConfidenceThreshold: Float = 0.50f,
    private val boardConfidenceThreshold: Float = 0.50f,
    private val markerConfidenceThreshold: Float = 0.50f,
    private val nmsThreshold: Float = 0.65f,
    private val centerDistanceThreshold: Float = 0.008f,
    private val minBoxSize: Float = 0.005f,
    private val maxDartDetections: Int = 20,
    private val maxDartboardDetections: Int = 20,
    private val historySize: Int = 5,
    private val minHistoryFramesRequired: Int = 3
) {
    private val detectionHistory = mutableListOf<List<ObjectDetection>>()
    private var lastKnownDartboardBox: Rect? = null

    // Dynamic Auto-Tracking ROI (identisch zur Original-App)
    var currentDynamicRoi: Rect? = null
        private set
    var normalizedDynamicRoi: Rect? = null
        private set
    private var framesWithoutBoard: Int = 0
    private val roiPadding: Float = 0.30f // 30% Padding um das erkannte Dartboard

    fun reset() {
        detectionHistory.clear()
        lastKnownDartboardBox = null
        currentDynamicRoi = null
        normalizedDynamicRoi = null
        framesWithoutBoard = 0
    }

    /**
     * Berechnet die Top-Left-Letterbox- und Dynamic-ROI-Skalierungsparameter für einen Kamera-Frame.
     */
    fun computeCropParams(rotatedWidth: Int, rotatedHeight: Int, inputSize: Int = 640): RoiCropParams {
        val baseRatio = min(inputSize.toFloat() / rotatedWidth, inputSize.toFloat() / rotatedHeight)
        val baseRoi = Rect(0f, 0f, inputSize.toFloat(), inputSize.toFloat())
        val roi = currentDynamicRoi ?: baseRoi

        val safeLeft = max(0f, roi.left)
        val safeTop = max(0f, roi.top)
        val safeRight = min(inputSize.toFloat(), roi.right)
        val safeBottom = min(inputSize.toFloat(), roi.bottom)

        val frameSafeRoi = if (safeRight - safeLeft < 10f || safeBottom - safeTop < 10f) {
            baseRoi
        } else {
            Rect(safeLeft, safeTop, safeRight, safeBottom)
        }

        val roiScaleX = inputSize.toFloat() / frameSafeRoi.width
        val roiScaleY = inputSize.toFloat() / frameSafeRoi.height
        val roiScale = min(roiScaleX, roiScaleY)

        return RoiCropParams(
            rotatedWidth = rotatedWidth,
            rotatedHeight = rotatedHeight,
            baseRatio = baseRatio,
            frameSafeRoi = frameSafeRoi,
            roiScale = roiScale,
            roiOffsetX = 0f,
            roiOffsetY = 0f
        )
    }

    /**
     * Mappt Detektionen aus dem gezoomten 640x640 Tensor zurück in den normalisierten [0..1]
     * Vollbild-Kameraraum und aktualisiert das Dynamic Auto-Tracking ROI (EMA mit alpha=0.15).
     */
    fun mapDetectionsAndUpdateDynamicRoi(
        stableDetections: List<ObjectDetection>,
        cropParams: RoiCropParams,
        inputSize: Int = 640
    ): List<ObjectDetection> {
        val rotatedWidth = cropParams.rotatedWidth
        val rotatedHeight = cropParams.rotatedHeight
        val baseRatio = cropParams.baseRatio
        val frameSafeRoi = cropParams.frameSafeRoi
        val roiScale = cropParams.roiScale
        val roiOffsetX = cropParams.roiOffsetX
        val roiOffsetY = cropParams.roiOffsetY

        val mappedDetections = stableDetections.map { det ->
            val box = det.boundingBox
            val leftZoomPx = box.left * inputSize
            val topZoomPx = box.top * inputSize
            val rightZoomPx = box.right * inputSize
            val bottomZoomPx = box.bottom * inputSize

            val leftOrigPx = (leftZoomPx - roiOffsetX) / roiScale + frameSafeRoi.left
            val topOrigPx = (topZoomPx - roiOffsetY) / roiScale + frameSafeRoi.top
            val rightOrigPx = (rightZoomPx - roiOffsetX) / roiScale + frameSafeRoi.left
            val bottomOrigPx = (bottomZoomPx - roiOffsetY) / roiScale + frameSafeRoi.top

            val finalLeft = ((leftOrigPx / baseRatio) / rotatedWidth).coerceIn(0f, 1f)
            val finalTop = ((topOrigPx / baseRatio) / rotatedHeight).coerceIn(0f, 1f)
            val finalRight = ((rightOrigPx / baseRatio) / rotatedWidth).coerceIn(0f, 1f)
            val finalBottom = ((bottomOrigPx / baseRatio) / rotatedHeight).coerceIn(0f, 1f)

            det.copy(boundingBox = Rect(finalLeft, finalTop, finalRight, finalBottom))
        }

        // Update Dynamic ROI basierend auf Dartboard-Detektionen (classId == 2)
        val boardDets = mappedDetections.filter { it.classId == 2 }
        val shouldUpdateRoi = (currentDynamicRoi == null && boardDets.size >= 20) ||
                              (currentDynamicRoi != null && boardDets.isNotEmpty())

        val scaledWidth = rotatedWidth * baseRatio
        val scaledHeight = rotatedHeight * baseRatio

        if (shouldUpdateRoi) {
            framesWithoutBoard = 0
            var uLeft = boardDets[0].boundingBox.left
            var uTop = boardDets[0].boundingBox.top
            var uRight = boardDets[0].boundingBox.right
            var uBottom = boardDets[0].boundingBox.bottom
            for (det in boardDets) {
                if (det.boundingBox.left < uLeft) uLeft = det.boundingBox.left
                if (det.boundingBox.top < uTop) uTop = det.boundingBox.top
                if (det.boundingBox.right > uRight) uRight = det.boundingBox.right
                if (det.boundingBox.bottom > uBottom) uBottom = det.boundingBox.bottom
            }

            val cx = (uLeft + uRight) / 2f
            val cy = (uTop + uBottom) / 2f
            val w = (uRight - uLeft) * scaledWidth
            val h = (uBottom - uTop) * scaledHeight

            val paddedW = min(w * (1f + roiPadding), scaledWidth)
            val paddedH = min(h * (1f + roiPadding), scaledHeight)

            val targetLeft = cx * scaledWidth - paddedW / 2f
            val targetTop = cy * scaledHeight - paddedH / 2f
            val targetRight = cx * scaledWidth + paddedW / 2f
            val targetBottom = cy * scaledHeight + paddedH / 2f

            if (currentDynamicRoi == null) {
                currentDynamicRoi = Rect(targetLeft, targetTop, targetRight, targetBottom)
            } else {
                val alpha = 0.15f
                val old = currentDynamicRoi!!
                currentDynamicRoi = Rect(
                    old.left * (1f - alpha) + targetLeft * alpha,
                    old.top * (1f - alpha) + targetTop * alpha,
                    old.right * (1f - alpha) + targetRight * alpha,
                    old.bottom * (1f - alpha) + targetBottom * alpha
                )
            }
        } else {
            framesWithoutBoard++
            if (framesWithoutBoard > 5 && currentDynamicRoi != null) {
                val alpha = 0.1f
                val old = currentDynamicRoi!!
                val updated = Rect(
                    old.left * (1f - alpha),
                    old.top * (1f - alpha),
                    old.right * (1f - alpha) + scaledWidth * alpha,
                    old.bottom * (1f - alpha) + scaledHeight * alpha
                )
                currentDynamicRoi = if (updated.width > scaledWidth * 0.95f && updated.height > scaledHeight * 0.95f) {
                    null
                } else {
                    updated
                }
            }
        }

        normalizedDynamicRoi = currentDynamicRoi?.let { roi ->
            if (scaledWidth > 0f && scaledHeight > 0f) {
                Rect(
                    roi.left / scaledWidth,
                    roi.top / scaledHeight,
                    roi.right / scaledWidth,
                    roi.bottom / scaledHeight
                )
            } else {
                null
            }
        }

        return mappedDetections
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
     * Führt hybride Non-Maximum Suppression (NMS) pro Klasse durch (1:1 Original-App):
     * - Spitze (classId 1): Center-Distance-NMS (< 0.008f)
     * - Alle anderen Klassen: IoU-NMS (> 0.65f)
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
     * Zeitlicher und räumlicher Stabilitätsfilter (1:1 Original-App):
     * - Dartboard (classId == 2) ist persistent und immer erlaubt.
     * - Alle anderen Klassen müssen innerhalb der um 15% erweiterten Board-Union-Box liegen
     *   und in mindestens 3 von 5 Frames mit IoU > 0.5 bestätigt werden.
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
            if (current.classId == 2) return@filter true

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
                    prev.classId == current.classId &&
                    calculateIoU(prev.boundingBox, current.boundingBox) > 0.5f
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

        if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) {
            return 0f
        }

        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val area1 = box1.width * box1.height
        val area2 = box2.width * box2.height
        val unionArea = area1 + area2 - intersectionArea

        return if (unionArea > 0f) intersectionArea / unionArea else 0f
    }
}
