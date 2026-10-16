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
 * Plattformunabhängiges Kalibrierungssystem für das Dartboard.
 * Berechnet die projektive Entzerrung der Kamera auf die reale Dartscheibe
 * über eine reine Kotlin-Homographie ohne native OpenCV-Abhängigkeit.
 */
class DartboardCalibrationSystem {
    companion object {
        // Standard-Radien normiert auf den Double-Outer-Radius (1.0 = 170 mm)
        const val NORM_DOUBLE_BULL = 6.35f / 170.0f    // ~0.0374
        const val NORM_SINGLE_BULL = 15.9f / 170.0f    // ~0.0935
        const val NORM_TRIPLE_INNER = 97.0f / 170.0f   // ~0.5706
        const val NORM_TRIPLE_OUTER = 107.0f / 170.0f  // ~0.6294
        const val NORM_DOUBLE_INNER = 160.0f / 170.0f  // ~0.9412
        const val NORM_DOUBLE_OUTER = 1.0000f

        val DARTBOARD_NUMBERS = listOf(
            20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5
        )
    }

    private var perspectiveMatrix: Matrix3x3? = null
    private var inverseMatrix: Matrix3x3? = null
    private var isCalibratedState: Boolean = false
    private var detectedBoardRadiusPx: Float = 0f
    private var lastControlPoints: List<PointF>? = null
    private val smoothingFactor: Float = 0.3f

    fun isCalibrated(): Boolean = isCalibratedState

    fun resetCalibration() {
        perspectiveMatrix = null
        inverseMatrix = null
        isCalibratedState = false
        detectedBoardRadiusPx = 0f
        lastControlPoints = null
    }

    fun getBoardDistanceStatus(width: Int, height: Int): BoardDistanceStatus {
        if (!isCalibratedState || detectedBoardRadiusPx <= 0f) return BoardDistanceStatus.UNKNOWN
        val minDim = min(width, height).toFloat()
        val ratio = (detectedBoardRadiusPx * 2f) / minDim
        return when {
            ratio < 0.55f -> BoardDistanceStatus.TOO_FAR
            ratio > 0.95f -> BoardDistanceStatus.TOO_CLOSE
            else -> BoardDistanceStatus.OK
        }
    }

    /**
     * Kalibriert aus YOLO/RTMDet Detektionen.
     * Unterstützt sowohl mehrere Spider-/Trennlinien-Punkte (Klasse 2) + 20er-Marker (Klasse 3)
     * als auch eine einzelne zusammenhängende Dartboard-Bounding-Box.
     */
    fun autoCalibrateFromDetections(
        detections: List<ObjectDetection>,
        canvasWidth: Int,
        canvasHeight: Int,
        frameWidth: Float = if (canvasWidth < canvasHeight) 720f else 1280f,
        frameHeight: Float = if (canvasWidth < canvasHeight) 1280f else 720f
    ): Boolean {
        if (canvasWidth <= 0 || canvasHeight <= 0) return false

        val cameraWidth = frameWidth.toDouble()
        val cameraHeight = frameHeight.toDouble()
        val scale = max(canvasWidth / cameraWidth, canvasHeight / cameraHeight)
        val offsetX = (canvasWidth - cameraWidth * scale) / 2.0
        val offsetY = (canvasHeight - cameraHeight * scale) / 2.0

        val spiderDetections = detections.filter {
            it.classId == 2 || it.className.lowercase().contains("board")
        }
        val marker20 = detections.firstOrNull {
            it.classId == 3 || it.className.lowercase().contains("marker") || it.className.contains("20")
        }

        val rawControlPoints: List<PointF>? = if (spiderDetections.size >= 5) {
            // Multi-Spider Detektion (Klasse 2 sind die 20 Schnittpunkte am Double/Spider)
            val spiderCenters = spiderDetections.map { det ->
                val cx = (det.boundingBox.left + det.boundingBox.right) / 2.0
                val cy = (det.boundingBox.top + det.boundingBox.bottom) / 2.0
                PointF((cx * cameraWidth * scale + offsetX).toFloat(), (cy * cameraHeight * scale + offsetY).toFloat())
            }

            val estCenterX = spiderCenters.map { it.x.toDouble() }.average()
            val estCenterY = spiderCenters.map { it.y.toDouble() }.average()

            val distances = spiderCenters.map { hypot(it.x.toDouble() - estCenterX, it.y.toDouble() - estCenterY) }
            val avgDist = distances.average()

            // Äußerste Punkte filtern (Double-Ring)
            val outerPoints = spiderCenters.filterIndexed { idx, _ -> distances[idx] >= avgDist * 0.82 }
            val finalPoints = if (outerPoints.size >= 5) outerPoints else spiderCenters.sortedByDescending { hypot(it.x.toDouble() - estCenterX, it.y.toDouble() - estCenterY) }.take(maxOf(5, spiderCenters.size / 2))

            // Kovarianz-Matrix für Achsen & Ausrichtung
            var covXX = 0.0
            var covYY = 0.0
            var covXY = 0.0
            for (p in finalPoints) {
                val dx = p.x.toDouble() - estCenterX
                val dy = p.y.toDouble() - estCenterY
                covXX += dx * dx
                covYY += dy * dy
                covXY += dx * dy
            }
            val n = finalPoints.size.toDouble()
            covXX /= n
            covYY /= n
            covXY /= n

            val delta = sqrt(max(0.0, (covXX - covYY) * (covXX - covYY) + 4.0 * covXY * covXY))
            val lambda1 = (covXX + covYY + delta) / 2.0
            val lambda2 = (covXX + covYY - delta) / 2.0
            val radiusA = sqrt(max(1.0, 2.0 * lambda1))
            val radiusB = sqrt(max(1.0, 2.0 * lambda2))
            val radiusAvg = (radiusA + radiusB) / 2.0
            detectedBoardRadiusPx = radiusAvg.toFloat()

            // Rotationsbestimmung
            var rotOffsetDeg = 0.0
            if (marker20 != null) {
                val mcx = (marker20.boundingBox.left + marker20.boundingBox.right) / 2.0
                val mcy = (marker20.boundingBox.top + marker20.boundingBox.bottom) / 2.0
                val mx = mcx * cameraWidth * scale + offsetX
                val my = mcy * cameraHeight * scale + offsetY
                val angleDeg = toDegrees(atan2(-(my - estCenterY), mx - estCenterX))
                rotOffsetDeg = angleDeg - 90.0 // 90° ist nominal 12 Uhr
                while (rotOffsetDeg > 180.0) rotOffsetDeg -= 360.0
                while (rotOffsetDeg < -180.0) rotOffsetDeg += 360.0
            } else {
                // Suche minimale Winkelabweichung zum 18° Spider-Gitter
                var minErr = Double.MAX_VALUE
                var bestRot = 0.0
                var rotTest = -9.0
                while (rotTest <= 9.0) {
                    var err = 0.0
                    for (p in finalPoints) {
                        val angle = toDegrees(atan2(-(p.y.toDouble() - estCenterY), p.x.toDouble() - estCenterX))
                        val dartAngle = angle - 90.0 - rotTest
                        val diff = abs(((dartAngle - 9.0) % 18.0 + 18.0) % 18.0 - 9.0)
                        err += diff * diff
                    }
                    if (err < minErr) {
                        minErr = err
                        bestRot = rotTest
                    }
                    rotTest += 0.2
                }
                rotOffsetDeg = bestRot
            }

            // 4 Kontrollpunkte berechnen (D20 oben, D6 rechts, D3 unten, D11 links)
            // Polar: D20 = 90°, D6 = 0°, D3 = -90°, D11 = 180°
            val anglesDeg = listOf(
                90.0 + rotOffsetDeg,
                0.0 + rotOffsetDeg,
                -90.0 + rotOffsetDeg,
                180.0 + rotOffsetDeg
            )

            anglesDeg.map { aDeg ->
                val rad = toRadians(aDeg)
                PointF(
                    (estCenterX + radiusAvg * cos(rad)).toFloat(),
                    (estCenterY - radiusAvg * sin(rad)).toFloat()
                )
            }
        } else {
            // Einzelne Bounding-Box Fallback
            val singleBoard = spiderDetections.firstOrNull() ?: return false
            val box = singleBoard.boundingBox
            val left = box.left * cameraWidth * scale + offsetX
            val right = box.right * cameraWidth * scale + offsetX
            val top = box.top * cameraHeight * scale + offsetY
            val bottom = box.bottom * cameraHeight * scale + offsetY

            val centerX = (left + right) / 2.0
            val centerY = (top + bottom) / 2.0
            val radiusX = (right - left) / 2.0
            val radiusY = (bottom - top) / 2.0
            val radius = (radiusX + radiusY) / 2.0
            detectedBoardRadiusPx = radius.toFloat()

            listOf(
                PointF(centerX.toFloat(), (centerY - radiusY).toFloat()), // D20 Oben
                PointF((centerX + radiusX).toFloat(), centerY.toFloat()), // D6 Rechts
                PointF(centerX.toFloat(), (centerY + radiusY).toFloat()), // D3 Unten
                PointF((centerX - radiusX).toFloat(), centerY.toFloat())  // D11 Links
            )
        }

        if (rawControlPoints == null || rawControlPoints.size != 4) return false

        // Temporäres Glätten mit vorherigen Kontrollpunkten zur Reduktion von Jitter
        val smoothedPoints = if (lastControlPoints != null && isCalibratedState) {
            val last = lastControlPoints!!
            List(4) { i ->
                PointF(
                    last[i].x * (1f - smoothingFactor) + rawControlPoints[i].x * smoothingFactor,
                    last[i].y * (1f - smoothingFactor) + rawControlPoints[i].y * smoothingFactor
                )
            }
        } else {
            rawControlPoints
        }
        lastControlPoints = smoothedPoints

        // Ideale Punkte im normierten Koordinatenraum [-1, 1]:
        // (0, -1) = Oben (D20), (1, 0) = Rechts (D6), (0, 1) = Unten (D3), (-1, 0) = Links (D11)
        val dstPoints = listOf(
            PointF(0f, -1f),
            PointF(1f, 0f),
            PointF(0f, 1f),
            PointF(-1f, 0f)
        )

        val homography = solveHomography4Points(smoothedPoints, dstPoints)
        if (homography != null) {
            perspectiveMatrix = homography
            inverseMatrix = homography.invert()
            isCalibratedState = true
            return true
        }

        return false
    }

    /**
     * Konvertiert Canvas-Pixelkoordinaten in normierte Board-Koordinaten [-1.0, 1.0].
     */
    fun getNormalizedBoardPosition(canvasX: Double, canvasY: Double): PointF? {
        val mat = perspectiveMatrix ?: return null
        val (u, v) = mat.transformPoint(canvasX, canvasY)
        return PointF(u.toFloat(), v.toFloat())
    }

    /**
     * Ermittelt Punktwert, Multiplikator und Segmentname aus normierter Position [-1.0, 1.0].
     */
    fun getScoreFromNormalizedPosition(normPos: Offset): DartSegment? {
        val u = normPos.x.toDouble()
        val v = normPos.y.toDouble()
        val distance = sqrt(u * u + v * v)

        if (distance > 1.05) {
            return null // Außerhalb des Boards (Miss)
        }

        if (distance <= NORM_DOUBLE_BULL) {
            return DartSegment(name = "DBull", scoreVal = 25, multiplier = "d", number = 25)
        }
        if (distance <= NORM_SINGLE_BULL) {
            return DartSegment(name = "Bull", scoreVal = 25, multiplier = "s", number = 25)
        }

        // Winkel berechnen (0 Grad = oben / 12 Uhr, im Uhrzeigersinn)
        var angleDeg = toDegrees(atan2(u, -v))
        if (angleDeg < 0) angleDeg += 360.0

        val segmentIndex = (((angleDeg + 9.0) / 18.0).toInt()) % 20
        val baseScore = DARTBOARD_NUMBERS[segmentIndex]

        val multiplier = when {
            distance in NORM_TRIPLE_INNER..NORM_TRIPLE_OUTER -> "t"
            distance in NORM_DOUBLE_INNER..NORM_DOUBLE_OUTER -> "d"
            else -> "s"
        }

        val name = when (multiplier) {
            "t" -> "T$baseScore"
            "d" -> "D$baseScore"
            else -> "$baseScore"
        }

        return DartSegment(name = name, scoreVal = baseScore, multiplier = multiplier, number = baseScore)
    }

    /**
     * Löst das lineare Gleichungssystem für 4 Punkt-Korrespondenzen (Direct Linear Transformation / DLT).
     * Berechnet die 3x3 Homographie-Matrix ohne externe Bibliotheken.
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

            // Zeile 2*i: xi*h00 + yi*h01 + h02 - xi*ui*h20 - yi*ui*h21 = ui
            a[2 * i][0] = xi
            a[2 * i][1] = yi
            a[2 * i][2] = 1.0
            a[2 * i][3] = 0.0
            a[2 * i][4] = 0.0
            a[2 * i][5] = 0.0
            a[2 * i][6] = -xi * ui
            a[2 * i][7] = -yi * ui
            b[2 * i] = ui

            // Zeile 2*i + 1: xi*h10 + yi*h11 + h12 - xi*vi*h20 - yi*vi*h21 = vi
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

        // Gaußsches Eliminationsverfahren mit partieller Pivotisierung
        val h = solveLinearSystem8x8(a, b) ?: return null

        return Matrix3x3(
            m00 = h[0], m01 = h[1], m02 = h[2],
            m10 = h[3], m11 = h[4], m12 = h[5],
            m20 = h[6], m21 = h[7], m22 = 1.0
        )
    }

    private fun solveLinearSystem8x8(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = 8
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

            if (maxVal < 1e-12) return null // Singuläre Matrix

            // Vertausche Zeilen
            val temp = augmented[col]
            augmented[col] = augmented[maxRow]
            augmented[maxRow] = temp

            // Normalisiere Pivot-Zeile
            val pivot = augmented[col][col]
            for (j in col..n) {
                augmented[col][j] /= pivot
            }

            // Eliminiere andere Zeilen
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
