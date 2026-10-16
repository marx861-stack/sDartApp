package com.example.myapplication

import kotlin.math.*

data class Point(val x: Int = 0, val y: Int = 0)
typealias DartPoint = Point

data class DartboardSegment(
    val ring: Int, // 1-5 (Bull, Single, Triple, Double, Triple)
    val segment: Int, // 1-20 (Segmente des Dartboards)
    val points: Int
)

class ScoreCalculator {
    
    companion object {
        // Standard Dartboard-Dimensionen (in Pixeln)
        private const val BOARD_RADIUS = 1000.0
        private const val BULL_RADIUS = 50.0
        private const val TRIPLE_RING_INNER = 400.0
        private const val TRIPLE_RING_OUTER = 450.0
        private const val DOUBLE_RING_INNER = 800.0
        private const val DOUBLE_RING_OUTER = 850.0
    }
    
    fun calculateScore(dartPosition: DartPoint, boardCenter: Point): Int {
        val distance = calculateDistance(dartPosition, boardCenter)
        val angle = calculateAngle(dartPosition, boardCenter)
        
        return when {
            distance <= BULL_RADIUS -> 50 // Bull
            distance <= TRIPLE_RING_INNER -> getSingleScore(angle)
            distance in TRIPLE_RING_INNER..TRIPLE_RING_OUTER -> getTripleScore(angle)
            distance in DOUBLE_RING_INNER..DOUBLE_RING_OUTER -> getDoubleScore(angle)
            distance <= BOARD_RADIUS -> getSingleScore(angle)
            else -> 0 // Außerhalb des Boards
        }
    }
    
    private fun calculateDistance(dart: DartPoint, center: Point): Double {
        val dx = dart.x.toDouble() - center.x.toDouble()
        val dy = dart.y.toDouble() - center.y.toDouble()
        return sqrt(dx * dx + dy * dy)
    }
    
    private fun calculateAngle(dart: DartPoint, center: Point): Double {
        val dx = dart.x.toDouble() - center.x.toDouble()
        val dy = dart.y.toDouble() - center.y.toDouble()
        var angle = atan2(dy, dx) * 180 / PI
        
        // Normalisiere auf 0-360 Grad
        if (angle < 0) angle += 360
        
        return angle
    }
    
    private fun getSingleScore(angle: Double): Int {
        return getSegmentScore(angle)
    }
    
    private fun getTripleScore(angle: Double): Int {
        return getSegmentScore(angle) * 3
    }
    
    private fun getDoubleScore(angle: Double): Int {
        return getSegmentScore(angle) * 2
    }
    
    private fun getSegmentScore(angle: Double): Int {
        // Dartboard-Segmente (20 Segmente, je 18 Grad)
        val segment = ((angle + 9) / 18).toInt() % 20
        
        return when (segment) {
            0 -> 20  // 12 Uhr Position
            1 -> 1
            2 -> 18
            3 -> 4
            4 -> 13
            5 -> 6
            6 -> 10
            7 -> 15
            8 -> 2
            9 -> 17
            10 -> 3
            11 -> 19
            12 -> 7
            13 -> 16
            14 -> 8
            15 -> 11
            16 -> 14
            17 -> 9
            18 -> 12
            19 -> 5
            else -> 0
        }
    }
    
    // Kalibrierung: Board-Center finden
    fun calibrateBoardCenter(imageWidth: Int, imageHeight: Int): Point {
        return Point(imageWidth / 2, imageHeight / 2)
    }
    
    // ML-optimierte Punkteberechnung
    fun calculateScoreML(dartPosition: DartPoint, boardCenter: Point, confidence: Double): Int {
        val baseScore = calculateScore(dartPosition, boardCenter)
        
        return when {
            confidence > 0.8 -> baseScore
            confidence > 0.6 -> (baseScore * 0.9).toInt()
            confidence > 0.4 -> (baseScore * 0.8).toInt()
            else -> 0
        }
    }
}
