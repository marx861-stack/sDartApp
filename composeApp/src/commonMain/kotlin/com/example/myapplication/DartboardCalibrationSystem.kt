package com.example.myapplication

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

data class DartSegment(
    val name: String,
    val scoreVal: Int,
    val multiplier: String, // "s", "d", "t"
    val number: Int
)

enum class BoardDistanceStatus { OK, TOO_CLOSE, TOO_FAR, UNKNOWN }

/**
 * 3x3 Matrix für projektive 2D-Transformationen (Homographie).
 */
data class Matrix3x3(
    val m00: Double, val m01: Double, val m02: Double,
    val m10: Double, val m11: Double, val m12: Double,
    val m20: Double, val m21: Double, val m22: Double
) {
    fun transformPoint(x: Double, y: Double): Pair<Double, Double> {
        val w = m20 * x + m21 * y + m22
        if (abs(w) < 1e-10) return Pair(x, y)
        val xNew = (m00 * x + m01 * y + m02) / w
        val yNew = (m10 * x + m11 * y + m12) / w
        return Pair(xNew, yNew)
    }

    fun transformPointF(point: PointF): PointF {
        val (x, y) = transformPoint(point.x.toDouble(), point.y.toDouble())
        return PointF(x.toFloat(), y.toFloat())
    }

    fun invert(): Matrix3x3? {
        val det = m00 * (m11 * m22 - m12 * m21) -
                  m01 * (m10 * m22 - m12 * m20) +
                  m02 * (m10 * m21 - m11 * m20)

        if (abs(det) < 1e-12) return null
        val invDet = 1.0 / det

        return Matrix3x3(
            m00 = (m11 * m22 - m12 * m21) * invDet,
            m01 = (m02 * m21 - m01 * m22) * invDet,
            m02 = (m01 * m12 - m02 * m11) * invDet,

            m10 = (m12 * m20 - m10 * m22) * invDet,
            m11 = (m00 * m22 - m02 * m20) * invDet,
            m12 = (m02 * m10 - m00 * m12) * invDet,

            m20 = (m10 * m21 - m11 * m20) * invDet,
            m21 = (m01 * m20 - m00 * m21) * invDet,
            m22 = (m00 * m11 - m01 * m10) * invDet
        )
    }
}

/**
 * Plattformunabhängiges Kalibrierungssystem für das Dartboard (1:1 Port der Original-App).
 *
 * Dieses System:
 * 1. Führt automatische Kalibrierung aus Spider-Detektionen (Klasse 2) + 20er-Marker (Klasse 3) durch
 * 2. Fittet iterativ eine Ellipse mit Outlier-Filterung (5-Parameter Conic Fit + PCA Fallback)
 * 3. Bestimmt den Rotations-Offset über ein 0.1°-Raster an den 9°-Segmentgrenzen
 * 4. Matcht die 4 Control-Boundary-Points (D20, D6, D3, D11) direkt auf Detektionen (mit Ellipsen-Fallback)
 * 5. Berechnet die Perspektiv-Homographie und stellt Overlay-Pfade sowie O(1) Polar-Scoring bereit
 */
class DartboardCalibrationSystem {
    companion object {
        // Standard-Dartboard-Radien in mm (identisch zur Original-App)
        const val R_DOUBLE_BULL = 6.35f
        const val R_SINGLE_BULL = 15.9f
        const val R_TRIPLE_INNER = 97.0f
        const val R_TRIPLE_OUTER = 107.0f
        const val R_DOUBLE_INNER = 160.0f
        const val R_DOUBLE_OUTER = 170.0f
        const val R_BOARD_OUTER = 225.5f // Gesamtradius des Dartboards (451 mm Durchmesser)

        // Normierte Radien relativ zum Double-Outer-Radius (1.0 = 170 mm)
        const val NORM_DOUBLE_BULL = R_DOUBLE_BULL / R_DOUBLE_OUTER    // ~0.0374
        const val NORM_SINGLE_BULL = R_SINGLE_BULL / R_DOUBLE_OUTER    // ~0.0935
        const val NORM_TRIPLE_INNER = R_TRIPLE_INNER / R_DOUBLE_OUTER   // ~0.5706
        const val NORM_TRIPLE_OUTER = R_TRIPLE_OUTER / R_DOUBLE_OUTER  // ~0.6294
        const val NORM_DOUBLE_INNER = R_DOUBLE_INNER / R_DOUBLE_OUTER  // ~0.9412
        const val NORM_DOUBLE_OUTER = 1.0000f
        const val NORM_BOARD_OUTER = R_BOARD_OUTER / R_DOUBLE_OUTER    // ~1.3265

        val DARTBOARD_NUMBERS = listOf(
            20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5
        )

        // Control Points: D20 (oben), D6 (rechts), D3 (unten), D11 (links)
        val CONTROL_POINT_NUMBERS = listOf(20, 6, 3, 11)

        // Winkel für jedes Segment (0° = oben, negativ im Uhrzeigersinn, identisch zur Original-App)
        val DARTBOARD_ANGLES = mapOf(
            20 to 0.0f, 1 to -18.0f, 18 to -36.0f, 4 to -54.0f, 13 to -72.0f, 6 to -90.0f,
            10 to -108.0f, 15 to -126.0f, 2 to -144.0f, 17 to -162.0f, 3 to -180.0f, 19 to 162.0f,
            7 to 144.0f, 16 to 126.0f, 8 to 108.0f, 11 to 90.0f, 14 to 72.0f, 9 to 54.0f,
            12 to 36.0f, 5 to 18.0f
        )

        private const val MIN_DETECTIONS_FOR_CALIBRATION = 5
        private const val MIN_OUTER_RING_POINTS = 8
    }

    private data class Ellipse(
        val center: PointF,
        val width: Float,
        val height: Float,
        val angle: Float
    )

    // Matrix von Canvas -> Normierter Board-Raum [-1, 1]
    private var canvasToBoardMatrix: Matrix3x3? = null
    // Matrix von Normierter Board-Raum [-1, 1] -> Canvas (für Overlay-Rendering)
    private var boardToCanvasMatrix: Matrix3x3? = null

    private var isCalibratedState: Boolean = false
    private var detectedEllipseSize: Float? = null
    private var detectedRotationOffset: Float = 0f

    // Gecachte transformierte Polygone zur Vermeidung von Matrix-Rechnungen pro Draw-Frame
    private var cachedOverlayPolygons: List<List<PointF>>? = null
    private var cachedMarkers: Triple<List<PointF>, List<PointF>, List<PointF>>? = null

    // Für Live-Kalibrierung: Smoothing (0.8f wie in Original-App für schnelle Reaktion)
    private var lastControlPoints: Array<PointF>? = null
    private var controlPointsDst: Array<PointF>? = null
    private val smoothingFactor: Float = 0.8f

    // Vorberechnete ideale Segment-Polygone im normierten Board-Raum [-1, 1] für das Overlay
    private val idealSegmentPolygons: List<List<PointF>> by lazy {
        buildIdealOverlayPolygons()
    }

    // Ideale Control Points (Segmentmitten auf dem Double-Outer-Ring) im normierten Raum [-1, 1]
    private val idealControlPointsNorm: List<PointF> by lazy {
        CONTROL_POINT_NUMBERS.map { cpNum ->
            val angleDeg = DARTBOARD_ANGLES[cpNum] ?: 0f
            val standardAngleDeg = angleDeg + 90.0f
            val rad = toRadians(standardAngleDeg.toDouble())
            PointF(cos(rad).toFloat(), (-sin(rad)).toFloat())
        }
    }

    // Ideale Boundary Points (Übergangspunkte bei +9° auf dem Double-Outer-Ring) im normierten Raum [-1, 1]
    private val idealBoundaryPointsNorm: List<PointF> by lazy {
        CONTROL_POINT_NUMBERS.map { cpNum ->
            val angleDeg = DARTBOARD_ANGLES[cpNum] ?: 0f
            val boundaryAngleDeg = angleDeg + 9.0f
            val standardAngleDeg = boundaryAngleDeg + 90.0f
            val rad = toRadians(standardAngleDeg.toDouble())
            PointF(cos(rad).toFloat(), (-sin(rad)).toFloat())
        }
    }

    fun isCalibrated(): Boolean = isCalibratedState

    fun resetCalibration() {
        canvasToBoardMatrix = null
        boardToCanvasMatrix = null
        cachedOverlayPolygons = null
        cachedMarkers = null
        isCalibratedState = false
        detectedEllipseSize = null
        detectedRotationOffset = 0f
        lastControlPoints = null
        controlPointsDst = null
    }

    /**
     * Bewertet die Entfernung des Dartboards basierend auf der detektierten Ellipsengröße
     * (1:1 identisch zur Original-App inkl. R_BOARD_OUTER / R_DOUBLE_OUTER Skalierung
     * sowie optionalem digitalem Anzeige-Zoom).
     */
    fun getBoardDistanceStatus(canvasWidth: Int, canvasHeight: Int, displayZoom: Float = 1f): BoardDistanceStatus {
        if (!isCalibratedState) return BoardDistanceStatus.UNKNOWN
        val size = detectedEllipseSize ?: return BoardDistanceStatus.UNKNOWN
        val boardOuterSize = size * (R_BOARD_OUTER / R_DOUBLE_OUTER) * displayZoom.coerceAtLeast(1f)
        val minDimension = min(canvasWidth, canvasHeight)
        if (minDimension <= 0) return BoardDistanceStatus.UNKNOWN
        val ratio = boardOuterSize / minDimension.toFloat()
        return when {
            ratio < 0.60f -> BoardDistanceStatus.TOO_FAR
            ratio > 0.95f -> BoardDistanceStatus.TOO_CLOSE
            else -> BoardDistanceStatus.OK
        }
    }

    /**
     * Automatische Kalibrierung aus Dartboard-Detektionen (1:1 Port der Original-App).
     */
    fun autoCalibrateFromDetections(
        detections: List<ObjectDetection>,
        canvasWidth: Int,
        canvasHeight: Int,
        frameWidth: Float = if (canvasWidth < canvasHeight) 720f else 1280f,
        frameHeight: Float = if (canvasWidth < canvasHeight) 1280f else 720f
    ): Boolean {
        if (canvasWidth <= 0 || canvasHeight <= 0) return false

        val dartboardOnly = detections.filter { detection ->
            detection.classId == 2 || detection.className.lowercase().contains("board")
        }

        if (dartboardOnly.size < MIN_DETECTIONS_FOR_CALIBRATION) {
            return false
        }

        // 1. Trenne 20er-Marker (Klasse 3) von den Spider-Punkten (Klasse 2)
        val marker20 = detections.find { it.classId == 3 }

        // 2. Extrahiere Zentren der Spider-Detektionen in Canvas-Koordinaten
        val spiderCenters = dartboardOnly.map { detection ->
            val bbox = detection.boundingBox
            val normalizedCenterX = (bbox.left + bbox.right) / 2f
            val normalizedCenterY = (bbox.top + bbox.bottom) / 2f
            transformDetectionToCanvas(normalizedCenterX, normalizedCenterY, canvasWidth, canvasHeight, frameWidth, frameHeight)
        }

        // Schätze vorläufiges Dartboard-Zentrum aus Spider-Punkten
        val estimatedCenter = PointF(
            spiderCenters.map { it.x }.average().toFloat(),
            spiderCenters.map { it.y }.average().toFloat()
        )

        // Identifiziere äußerste Punkte (Double-Ring)
        val outerRingPoints = selectPointsOnOuterRing(spiderCenters, estimatedCenter)
        if (outerRingPoints.size < MIN_OUTER_RING_POINTS) {
            return false
        }

        // 3. Iteratives Ellipsen-Fitting mit Outlier-Filterung
        var ellipse = fitEllipseToPoints(outerRingPoints) ?: return false

        val filteredPoints = filterOutliersFromEllipse(
            outerRingPoints,
            ellipse,
            maxDistanceFactor = 0.10f
        )

        if (filteredPoints.size >= 5) {
            val refinedEllipse = fitEllipseToPoints(filteredPoints)
            if (refinedEllipse != null) {
                ellipse = refinedEllipse
            }
        }

        detectedEllipseSize = max(ellipse.width, ellipse.height)
        val boardCenter = PointF(ellipse.center.x, ellipse.center.y)

        // 4. Bestimme die optimale Rotation anhand der Spider-Trennlinien (9° versetzt zur Segmentmitte)
        val spiderAngles = spiderCenters.map { point ->
            val dx = point.x - boardCenter.x
            val dy = point.y - boardCenter.y
            val angleDeg = toDegrees(atan2(-dy.toDouble(), dx.toDouble())).toFloat()
            var dartboardAngle = angleDeg - 90.0f
            while (dartboardAngle > 180f) dartboardAngle -= 360f
            while (dartboardAngle < -180f) dartboardAngle += 360f
            dartboardAngle
        }

        var rotationAnchor: Float? = null
        if (marker20 != null) {
            val markerCenter = transformDetectionToCanvas(
                (marker20.boundingBox.left + marker20.boundingBox.right) / 2f,
                (marker20.boundingBox.top + marker20.boundingBox.bottom) / 2f,
                canvasWidth, canvasHeight, frameWidth, frameHeight
            )
            val dx = markerCenter.x - boardCenter.x
            val dy = markerCenter.y - boardCenter.y
            val angleDeg = toDegrees(atan2(-dy.toDouble(), dx.toDouble())).toFloat()
            var marker20Angle = angleDeg - 90.0f
            while (marker20Angle > 180f) marker20Angle -= 360f
            while (marker20Angle < -180f) marker20Angle += 360f
            rotationAnchor = marker20Angle
        }

        val searchMin: Float
        val searchMax: Float
        if (rotationAnchor != null) {
            searchMin = rotationAnchor - 5f
            searchMax = rotationAnchor + 5f
        } else if (isCalibratedState) {
            searchMin = detectedRotationOffset - 5f
            searchMax = detectedRotationOffset + 5f
        } else {
            searchMin = -9f
            searchMax = 9f
        }

        var bestTheta = 0f
        var minScore = Float.MAX_VALUE
        var theta = searchMin
        while (theta <= searchMax) {
            var score = 0f
            for (angle in spiderAngles) {
                val diff = (angle - theta - 9f) % 18f
                val normalizedDiff = (diff + 18f) % 18f
                val error = if (normalizedDiff > 9f) normalizedDiff - 18f else normalizedDiff
                score += error * error
            }
            if (score < minScore) {
                minScore = score
                bestTheta = theta
            }
            theta += 0.1f
        }

        while (bestTheta > 180f) bestTheta -= 360f
        while (bestTheta < -180f) bestTheta += 360f
        detectedRotationOffset = bestTheta

        // 5. Bestimme Control Points (bevorzuge echte Detektionen für Perspektive, Fallback auf Ellipse)
        var detectedControlPoints = findControlPointsFromDetections(
            spiderCenters,
            boardCenter,
            ellipse,
            detectedRotationOffset
        )

        if (detectedControlPoints == null) {
            detectedControlPoints = CONTROL_POINT_NUMBERS.map { cpNum ->
                val centerAngleDeg = DARTBOARD_ANGLES[cpNum] ?: 0f
                val standardAngleDeg = centerAngleDeg + 9.0f + detectedRotationOffset + 90.0f
                getPointOnEllipse(ellipse, standardAngleDeg)
            }.toTypedArray()
        }

        // Smoothing: Mische neue Control Points mit alten (smoothingFactor = 0.8f wie im Original)
        val smoothedControlPoints = if (lastControlPoints != null && isCalibratedState) {
            Array(4) { i ->
                PointF(
                    lastControlPoints!![i].x * (1f - smoothingFactor) + detectedControlPoints[i].x * smoothingFactor,
                    lastControlPoints!![i].y * (1f - smoothingFactor) + detectedControlPoints[i].y * smoothingFactor
                )
            }
        } else {
            detectedControlPoints
        }

        lastControlPoints = smoothedControlPoints
        controlPointsDst = smoothedControlPoints

        // Berechne Homographie zwischen idealen Boundary Points (bei +9° auf dem Einheitskreis)
        // und den detektierten Boundary Points auf dem Canvas
        val boardToCanvas = solveHomography4Points(idealBoundaryPointsNorm, smoothedControlPoints.toList())
        val canvasToBoard = boardToCanvas?.invert()

        if (boardToCanvas != null && canvasToBoard != null) {
            boardToCanvasMatrix = boardToCanvas
            canvasToBoardMatrix = canvasToBoard
            cachedOverlayPolygons = idealSegmentPolygons.map { polygon ->
                polygon.map { pt -> boardToCanvas.transformPointF(pt) }
            }
            val transformedIdealControl = idealControlPointsNorm.map { boardToCanvas.transformPointF(it) }
            val detectedBoundary = smoothedControlPoints.toList()
            val transformedIdealBoundary = idealBoundaryPointsNorm.map { boardToCanvas.transformPointF(it) }
            cachedMarkers = Triple(transformedIdealControl, detectedBoundary, transformedIdealBoundary)
            isCalibratedState = true
            return true
        }

        return false
    }

    private fun transformDetectionToCanvas(
        normalizedX: Float,
        normalizedY: Float,
        canvasWidth: Int,
        canvasHeight: Int,
        cameraWidth: Float,
        cameraHeight: Float
    ): PointF {
        val scaleX = canvasWidth.toFloat() / cameraWidth
        val scaleY = canvasHeight.toFloat() / cameraHeight
        val scale = max(scaleX, scaleY)

        val scaledWidth = cameraWidth * scale
        val scaledHeight = cameraHeight * scale

        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f

        val inputX = normalizedX.coerceIn(0f, 1f) * cameraWidth
        val inputY = normalizedY.coerceIn(0f, 1f) * cameraHeight

        return PointF(inputX * scale + offsetX, inputY * scale + offsetY)
    }

    private fun selectPointsOnOuterRing(
        points: List<PointF>,
        center: PointF
    ): List<PointF> {
        val distances = points.map { point ->
            val dx = point.x - center.x
            val dy = point.y - center.y
            sqrt(dx * dx + dy * dy).toDouble()
        }

        val avgDistance = distances.average()
        val maxDistance = distances.maxOrNull() ?: 0.0
        val minDistance = distances.minOrNull() ?: 0.0

        val diff = maxDistance - minDistance
        val similarityThreshold = avgDistance * 0.2
        if (diff < similarityThreshold) {
            return points
        }

        val outerRingThreshold = avgDistance * 0.85
        val outerPoints = points.filterIndexed { index, _ ->
            distances[index] >= outerRingThreshold
        }

        return if (outerPoints.size < MIN_OUTER_RING_POINTS) {
            val sorted = points.zip(distances).sortedByDescending { it.second }
            sorted.take(max(MIN_OUTER_RING_POINTS, points.size / 2)).map { it.first }
        } else {
            outerPoints
        }
    }

    /**
     * Fittet eine Ellipse an die Punkte (Algebraic 5-Parameter Least-Squares Conic Fit
     * analog zu OpenCV fitEllipse, mit PCA-Fallback bei degenerierten Punktmengen).
     */
    private fun fitEllipseToPoints(points: List<PointF>): Ellipse? {
        if (points.size < 5) return null

        val meanX = points.map { it.x.toDouble() }.average()
        val meanY = points.map { it.y.toDouble() }.average()
        val scale = points.map { hypot(it.x.toDouble() - meanX, it.y.toDouble() - meanY) }.average().coerceAtLeast(1.0)

        // 1. Versuche exakten 5-Parameter Conic Fit: A*u^2 + B*u*v + C*v^2 + D*u + E*v = 1
        val normalMat = Array(5) { DoubleArray(5) }
        val rhs = DoubleArray(5)

        for (p in points) {
            val u = (p.x.toDouble() - meanX) / scale
            val v = (p.y.toDouble() - meanY) / scale
            val phi = doubleArrayOf(u * u, u * v, v * v, u, v)
            for (r in 0 until 5) {
                rhs[r] += phi[r]
                for (c in 0 until 5) {
                    normalMat[r][c] += phi[r] * phi[c]
                }
            }
        }

        val params = solveLinearSystem(normalMat, rhs, 5)
        if (params != null) {
            val A = params[0]
            val B = params[1]
            val C = params[2]
            val D = params[3]
            val E = params[4]
            val disc = 4.0 * A * C - B * B
            if (disc > 1e-9) {
                val u0 = (B * E - 2.0 * C * D) / disc
                val v0 = (B * D - 2.0 * A * E) / disc
                val f0 = 1.0 + A * u0 * u0 + B * u0 * v0 + C * v0 * v0
                if (f0 > 1e-9) {
                    val a11 = A / f0
                    val a12 = B / (2.0 * f0)
                    val a22 = C / f0
                    val delta = sqrt((a11 - a22) * (a11 - a22) + 4.0 * a12 * a12)
                    val lambda1 = (a11 + a22 - delta) / 2.0
                    val lambda2 = (a11 + a22 + delta) / 2.0
                    if (lambda1 > 1e-6 && lambda2 > 1e-6) {
                        val semiA = scale / sqrt(lambda1)
                        val semiB = scale / sqrt(lambda2)
                        val ratio = max(semiA, semiB) / min(semiA, semiB).coerceAtLeast(1e-6)
                        if (ratio <= 3.0) {
                            val angleRad = 0.5 * atan2(-2.0 * a12, a22 - a11)
                            var angleDeg = toDegrees(angleRad).toFloat()
                            if (angleDeg < 0f) angleDeg += 180f
                            val cx = (meanX + u0 * scale).toFloat()
                            val cy = (meanY + v0 * scale).toFloat()
                            return Ellipse(
                                center = PointF(cx, cy),
                                width = (2.0 * semiA).toFloat(),
                                height = (2.0 * semiB).toFloat(),
                                angle = angleDeg
                            )
                        }
                    }
                }
            }
        }

        // 2. Fallback: PCA Kovarianz-Ellipse
        var covXX = 0.0
        var covYY = 0.0
        var covXY = 0.0
        for (p in points) {
            val dx = p.x.toDouble() - meanX
            val dy = p.y.toDouble() - meanY
            covXX += dx * dx
            covYY += dy * dy
            covXY += dx * dy
        }
        val n = points.size.toDouble()
        covXX /= n
        covYY /= n
        covXY /= n

        val delta = sqrt(max(0.0, (covXX - covYY) * (covXX - covYY) + 4.0 * covXY * covXY))
        val lambda1 = (covXX + covYY + delta) / 2.0
        val lambda2 = (covXX + covYY - delta) / 2.0
        val radiusA = sqrt(max(1.0, 2.0 * lambda1))
        val radiusB = sqrt(max(1.0, 2.0 * lambda2))
        val angleRad = 0.5 * atan2(2.0 * covXY, covXX - covYY)
        var angleDeg = toDegrees(angleRad).toFloat()
        if (angleDeg < 0f) angleDeg += 180f

        return Ellipse(
            center = PointF(meanX.toFloat(), meanY.toFloat()),
            width = (2.0 * radiusA).toFloat(),
            height = (2.0 * radiusB).toFloat(),
            angle = angleDeg
        )
    }

    /**
     * Filtert Outlier, die zu weit von der gefitteten Ellipse entfernt sind (1:1 aus Original-App).
     */
    private fun filterOutliersFromEllipse(
        points: List<PointF>,
        ellipse: Ellipse,
        maxDistanceFactor: Float = 0.15f
    ): List<PointF> {
        if (points.isEmpty()) return points

        val centerX = ellipse.center.x
        val centerY = ellipse.center.y
        val width = ellipse.width
        val height = ellipse.height
        val maxAxis = max(width, height)
        val maxDistanceDouble = (maxAxis * maxDistanceFactor).toDouble()

        val angleRad = toRadians(ellipse.angle.toDouble())
        val cosA = cos(-angleRad)
        val sinA = sin(-angleRad)

        val a = width.toDouble() / 2.0
        val b = height.toDouble() / 2.0
        if (a <= 0.0 || b <= 0.0) return points

        val filteredPoints = mutableListOf<PointF>()
        for (point in points) {
            val dx = (point.x - centerX).toDouble()
            val dy = (point.y - centerY).toDouble()

            val rx = dx * cosA - dy * sinA
            val ry = dx * sinA + dy * cosA

            val nx = rx / a
            val ny = ry / b

            val distFromEdge = abs(1.0 - (nx * nx + ny * ny))
            val pixelDist = distFromEdge * maxAxis.toDouble()

            if (pixelDist <= maxDistanceDouble) {
                filteredPoints.add(point)
            }
        }

        if (filteredPoints.size < points.size * 0.5) {
            return points
        }
        return filteredPoints
    }

    /**
     * Berechnet einen Punkt auf der Ellipse für einen gegebenen Polarwinkel in Standard-Grad (1:1 aus Original-App).
     */
    private fun getPointOnEllipse(ellipse: Ellipse, standardAngleDeg: Float): PointF {
        val a = ellipse.width.toDouble() / 2.0
        val b = ellipse.height.toDouble() / 2.0
        val theta = toRadians(ellipse.angle.toDouble())

        val phiCanvas = -toRadians(standardAngleDeg.toDouble())
        val phiRel = phiCanvas - theta

        val t = atan2(a * sin(phiRel), b * cos(phiRel))

        val xCanonical = a * cos(t)
        val yCanonical = b * sin(t)

        val x = ellipse.center.x.toDouble() + xCanonical * cos(theta) - yCanonical * sin(theta)
        val y = ellipse.center.y.toDouble() + xCanonical * sin(theta) + yCanonical * cos(theta)

        return PointF(x.toFloat(), y.toFloat())
    }

    /**
     * Matcht die 4 Control-Boundary-Points (D20, D6, D3, D11 bei +9° Übergang) direkt auf die
     * erkannten Spider-Punkte (1:1 aus Original-App).
     */
    private fun findControlPointsFromDetections(
        detectionCenters: List<PointF>,
        boardCenter: PointF,
        ellipse: Ellipse,
        rotationOffset: Float
    ): Array<PointF>? {
        if (detectionCenters.size < 4) return null

        val centerX = if (ellipse.center.x.isNaN()) boardCenter.x else ellipse.center.x
        val centerY = if (ellipse.center.y.isNaN()) boardCenter.y else ellipse.center.y

        val avgEllipseRadius = (ellipse.width + ellipse.height) / 4f
        val minAllowedRadius = avgEllipseRadius * 0.85f
        val maxAllowedRadius = avgEllipseRadius * 1.15f

        val pointsWithAngle = detectionCenters.map { point ->
            val dx = point.x - centerX
            val dy = point.y - centerY
            val distance = sqrt(dx * dx + dy * dy)
            val angleDeg = toDegrees(atan2(-dy.toDouble(), dx.toDouble()))
            var dartboardAngle = angleDeg - 90.0
            while (dartboardAngle > 180) dartboardAngle -= 360
            while (dartboardAngle < -180) dartboardAngle += 360
            Triple(point, distance, dartboardAngle.toFloat())
        }

        val boundaryPoints = mutableListOf<PointF>()

        for (cpNum in CONTROL_POINT_NUMBERS) {
            val centerAngleDeg = DARTBOARD_ANGLES[cpNum] ?: 0f
            var targetAngle = centerAngleDeg + 9.0f + rotationOffset
            while (targetAngle > 180f) targetAngle -= 360f
            while (targetAngle < -180f) targetAngle += 360f

            var bestPoint: PointF? = null
            var minAngleDiff = Float.MAX_VALUE

            for ((point, distance, dartboardAngle) in pointsWithAngle) {
                if (distance < minAllowedRadius || distance > maxAllowedRadius) continue

                var angleDiff = abs(dartboardAngle - targetAngle)
                if (angleDiff > 180f) angleDiff = 360f - angleDiff

                if (angleDiff <= 8.0f && angleDiff < minAngleDiff) {
                    minAngleDiff = angleDiff
                    bestPoint = point
                }
            }

            if (bestPoint == null) return null
            boundaryPoints.add(bestPoint)
        }

        return boundaryPoints.toTypedArray()
    }

    /**
     * Konvertiert Canvas-Pixelkoordinaten in normierte Board-Koordinaten [-1.0, 1.0].
     */
    fun getNormalizedBoardPosition(canvasX: Double, canvasY: Double): PointF? {
        val mat = canvasToBoardMatrix ?: return null
        val (u, v) = mat.transformPoint(canvasX, canvasY)
        return PointF(u.toFloat(), v.toFloat())
    }

    /**
     * Ermittelt Punktwert (inkl. Multiplikator!), Multiplikator-Kürzel und Segmentname
     * aus einer normierten Board-Position [-1.0, 1.0] (1:1 identisch zur Original-App).
     */
    fun getScoreFromNormalizedPosition(normPos: Offset): DartSegment? {
        val u = normPos.x.toDouble()
        val v = normPos.y.toDouble()
        val distance = sqrt(u * u + v * v)

        // Exakt wie Original-App (dist > srDoubleOuter -> null): Außerhalb von 1.0 ist ein Miss!
        if (distance > NORM_DOUBLE_OUTER) {
            return null
        }

        if (distance <= NORM_DOUBLE_BULL) {
            return DartSegment(name = "DBull", scoreVal = 50, multiplier = "d", number = 25)
        }
        if (distance <= NORM_SINGLE_BULL) {
            return DartSegment(name = "Bull", scoreVal = 25, multiplier = "s", number = 25)
        }

        // Winkel berechnen (0 Grad = oben / 12 Uhr, im Uhrzeigersinn)
        var angleDeg = toDegrees(atan2(u, -v))
        if (angleDeg < 0) angleDeg += 360.0

        val segmentIndex = (((angleDeg + 9.0) / 18.0).toInt()) % 20
        val baseScore = DARTBOARD_NUMBERS[segmentIndex]

        return when {
            distance <= NORM_TRIPLE_INNER -> DartSegment(
                name = "S${baseScore}_inner",
                scoreVal = baseScore,
                multiplier = "s",
                number = baseScore
            )
            distance <= NORM_TRIPLE_OUTER -> DartSegment(
                name = "T$baseScore",
                scoreVal = baseScore * 3,
                multiplier = "t",
                number = baseScore
            )
            distance <= NORM_DOUBLE_INNER -> DartSegment(
                name = "S${baseScore}_outer",
                scoreVal = baseScore,
                multiplier = "s",
                number = baseScore
            )
            else -> DartSegment(
                name = "D$baseScore",
                scoreVal = baseScore * 2,
                multiplier = "d",
                number = baseScore
            )
        }
    }

    /**
     * Liefert die in Canvas-Koordinaten transformierten Segment-Polygone für das Dartboard-Overlay.
     */
    fun getTransformedOverlayPolygons(): List<List<PointF>> {
        if (!isCalibratedState) return emptyList()
        return cachedOverlayPolygons ?: emptyList()
    }

    /**
     * Liefert die transformierten idealen Control Points (grün), erkannten Boundary Points (rot)
     * und transformierten idealen Boundary Points (cyan) für das Debug-/Kalibrierungs-Overlay.
     */
    fun getOverlayCalibrationMarkers(): Triple<List<PointF>, List<PointF>, List<PointF>>? {
        if (!isCalibratedState) return null
        return cachedMarkers
    }

    private fun buildIdealOverlayPolygons(): List<List<PointF>> {
        val polygons = mutableListOf<List<PointF>>()

        // Double Bull & Single Bull Kreise
        polygons.add(generateCirclePointsNorm(NORM_DOUBLE_BULL, 32))
        polygons.add(generateCirclePointsNorm(NORM_SINGLE_BULL, 32))

        // 20 Sektoren (Triple, Double, Single Outer, Single Inner)
        val segmentAngleWidthRad = toRadians(18.0)
        for (number in DARTBOARD_NUMBERS) {
            val angleDeg = DARTBOARD_ANGLES[number] ?: 0f
            val centerAngleStandard = angleDeg + 90.0f
            val centerAngleRad = toRadians(centerAngleStandard.toDouble())
            val angle1 = centerAngleRad - segmentAngleWidthRad / 2.0
            val angle2 = centerAngleRad + segmentAngleWidthRad / 2.0

            polygons.add(generateSectorPointsNorm(NORM_TRIPLE_INNER, NORM_TRIPLE_OUTER, angle1, angle2))
            polygons.add(generateSectorPointsNorm(NORM_DOUBLE_INNER, NORM_DOUBLE_OUTER, angle1, angle2))
            polygons.add(generateSectorPointsNorm(NORM_TRIPLE_OUTER, NORM_DOUBLE_INNER, angle1, angle2))
            polygons.add(generateSectorPointsNorm(NORM_SINGLE_BULL, NORM_TRIPLE_INNER, angle1, angle2))
        }
        return polygons
    }

    private fun generateCirclePointsNorm(radius: Float, numPoints: Int): List<PointF> {
        return (0 until numPoints).map { i ->
            val angle = 2.0 * PI * i / numPoints
            PointF((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
        }
    }

    private fun generateSectorPointsNorm(
        innerRadius: Float,
        outerRadius: Float,
        startAngle: Double,
        endAngle: Double,
        numPointsPerArc: Int = 8
    ): List<PointF> {
        val points = mutableListOf<PointF>()
        var normalizedEndAngle = endAngle
        if (normalizedEndAngle < startAngle) {
            normalizedEndAngle += 2.0 * PI
        }
        for (i in 0..numPointsPerArc) {
            val angle = startAngle + (normalizedEndAngle - startAngle) * i / numPointsPerArc
            points.add(PointF((outerRadius * cos(angle)).toFloat(), (-outerRadius * sin(angle)).toFloat()))
        }
        for (i in numPointsPerArc downTo 0) {
            val angle = startAngle + (normalizedEndAngle - startAngle) * i / numPointsPerArc
            points.add(PointF((innerRadius * cos(angle)).toFloat(), (-innerRadius * sin(angle)).toFloat()))
        }
        return points
    }

    /**
     * Löst das lineare Gleichungssystem für 4 Punkt-Korrespondenzen (Direct Linear Transformation / DLT).
     */
    private fun solveHomography4Points(src: List<PointF>, dst: List<PointF>): Matrix3x3? {
        if (src.size < 4 || dst.size < 4) return null

        val a = Array(8) { DoubleArray(8) }
        val b = DoubleArray(8)

        for (i in 0 until 4) {
            val xi = src[i].x.toDouble()
            val yi = src[i].y.toDouble()
            val ui = dst[i].x.toDouble()
            val vi = dst[i].y.toDouble()

            a[2 * i][0] = xi
            a[2 * i][1] = yi
            a[2 * i][2] = 1.0
            a[2 * i][3] = 0.0
            a[2 * i][4] = 0.0
            a[2 * i][5] = 0.0
            a[2 * i][6] = -xi * ui
            a[2 * i][7] = -yi * ui
            b[2 * i] = ui

            a[2 * i + 1][0] = 0.0
            a[2 * i + 1][1] = 0.0
            a[2 * i + 1][2] = 0.0
            a[2 * i + 1][3] = xi
            a[2 * i + 1][4] = yi
            a[2 * i + 1][5] = 1.0
            a[2 * i + 1][6] = -xi * vi
            a[2 * i + 1][7] = -yi * vi
            b[2 * i + 1] = vi
        }

        val h = solveLinearSystem(a, b, 8) ?: return null

        return Matrix3x3(
            m00 = h[0], m01 = h[1], m02 = h[2],
            m10 = h[3], m11 = h[4], m12 = h[5],
            m20 = h[6], m21 = h[7], m22 = 1.0
        )
    }

    private fun solveLinearSystem(a: Array<DoubleArray>, b: DoubleArray, n: Int): DoubleArray? {
        val augmented = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }

        for (col in 0 until n) {
            var maxRow = col
            var maxVal = abs(augmented[col][col])
            for (row in col + 1 until n) {
                val absVal = abs(augmented[row][col])
                if (absVal > maxVal) {
                    maxVal = absVal
                    maxRow = row
                }
            }

            if (maxVal < 1e-12) return null

            val temp = augmented[col]
            augmented[col] = augmented[maxRow]
            augmented[maxRow] = temp

            val pivot = augmented[col][col]
            for (j in col..n) {
                augmented[col][j] /= pivot
            }

            for (row in 0 until n) {
                if (row != col) {
                    val factor = augmented[row][col]
                    for (j in col..n) {
                        augmented[row][j] -= factor * augmented[col][j]
                    }
                }
            }
        }

        return DoubleArray(n) { i -> augmented[i][n] }
    }
}
