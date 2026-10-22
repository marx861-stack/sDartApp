package com.sdart.app.core.detection

import com.sdart.app.*

import androidx.compose.ui.geometry.Offset

data class PointF(val x: Float, val y: Float) {
    fun toOffset(): Offset = Offset(x, y)
}

data class DetectionCandidate(
    var pos: PointF,
    val firstSeen: Long,
    var lastSeen: Long,
    val detectionData: ObjectDetection
)

enum class BoardAction {
    NONE,
    RESET,
    ROUND_COMPLETE,
    AUTO_MISS
}

data class BoardStatusResult(
    val action: BoardAction,
    val count: Int = 0,
    val booked: Int = 0
)

/**
 * Plattformunabhängiges Auto-Detection Tracking-System für Darts.
 * Verwaltet Stabilität, Jitter-Toleranzen, Rundenwechsel und Erkennung von leergeräumten Boards.
 */
class AutoDetectionSystem(
    private val stabilityThresholdMs: Long = 100L,
    private val boardEmptyThresholdMs: Long = 1000L
) {
    private val jitterToleranceSq = 0.06f * 0.06f
    private val trackingToleranceSq = 0.05f * 0.05f
    
    private val bookedDarts = mutableListOf<PointF>()
    private val ignoredDarts = mutableListOf<PointF>()
    private var activeCandidates = mutableListOf<DetectionCandidate>()
    private var activeBodyCandidates = mutableListOf<DetectionCandidate>()
    private var noDartsDetectedTime: Long? = null
    
    var isWaitingForCleanup: Boolean = false
        private set
    private var lastPlayerIndex: Int = -1
    private var lastThrowsInRound: Int = 0
    private var lastThrowHistorySize: Int = 0
    private var lastLeg: Int = -1

    var isEnabled: Boolean = false
        private set

    fun enable() {
        isEnabled = true
    }
    
    fun disable() {
        isEnabled = false
    }
    
    fun resetTracking() {
        bookedDarts.clear()
        ignoredDarts.clear()
        activeCandidates.clear()
        activeBodyCandidates.clear()
        noDartsDetectedTime = null
        isWaitingForCleanup = false
        lastPlayerIndex = -1
        lastThrowsInRound = 0
        lastThrowHistorySize = 0
        lastLeg = -1
    }

    fun resetTrackingIfNeeded() {
        if (lastPlayerIndex != -1 || isWaitingForCleanup || bookedDarts.isNotEmpty()) {
            resetTracking()
        }
    }

    fun updateGameState(currentPlayerIndex: Int, throwsInRound: Int, throwHistorySize: Int, currentLeg: Int) {
        if (!isEnabled) return

        if (lastPlayerIndex != -1) {
            val playerChanged = currentPlayerIndex != lastPlayerIndex
            val legChanged = currentLeg != lastLeg
            val isUndo = !legChanged && throwHistorySize < lastThrowHistorySize
            val dartThrown = throwHistorySize > lastThrowHistorySize
            val roundEndedByThrow = dartThrown && throwsInRound == 0
            
            if (isUndo) {
                val diff = lastThrowHistorySize - throwHistorySize
                for (i in 0 until diff) {
                    if (bookedDarts.isNotEmpty()) {
                        val removed = bookedDarts.removeAt(bookedDarts.size - 1)
                        ignoredDarts.add(removed)
                    }
                }
                isWaitingForCleanup = false
            } else if (legChanged || playerChanged || roundEndedByThrow || (throwsInRound == 0 && lastThrowsInRound > 0)) {
                if (bookedDarts.isNotEmpty() || lastThrowsInRound > 0 || roundEndedByThrow) {
                    isWaitingForCleanup = true
                }
            }
        }
        lastPlayerIndex = currentPlayerIndex
        lastThrowsInRound = throwsInRound
        lastThrowHistorySize = throwHistorySize
        lastLeg = currentLeg
    }
    
    private fun calculateDistanceSq(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return dx * dx + dy * dy
    }
    
    fun findUnbookedDarts(
        dartTips: List<Pair<PointF, ObjectDetection>>,
        dartBodies: List<Pair<PointF, ObjectDetection>>
    ): List<Pair<PointF, ObjectDetection>> {
        val currentTime = currentTimeMillis()
        
        if (isWaitingForCleanup || lastThrowsInRound >= 3) {
            return emptyList()
        }
        
        if (dartTips.isEmpty()) {
            activeCandidates.clear()
        }
        if (dartBodies.isEmpty()) {
            activeBodyCandidates.clear()
        }
        if (dartTips.isEmpty()) {
            return emptyList()
        }
        
        // 1. Frame-to-Frame Tracking für Spitzen
        val newActiveCandidates = mutableListOf<DetectionCandidate>()
        val matchedTipIndices = mutableSetOf<Int>()
        
        for ((tipPos, det) in dartTips) {
            var matchIdx = -1
            var minDistSq = trackingToleranceSq
            
            for (i in activeCandidates.indices) {
                if (matchedTipIndices.contains(i)) continue
                
                val cand = activeCandidates[i]
                val dSq = calculateDistanceSq(tipPos, cand.pos)
                if (dSq < minDistSq) {
                    minDistSq = dSq
                    matchIdx = i
                }
            }
            
            if (matchIdx >= 0) {
                val match = activeCandidates[matchIdx]
                match.pos = tipPos
                match.lastSeen = currentTime
                newActiveCandidates.add(match)
                matchedTipIndices.add(matchIdx)
            } else {
                newActiveCandidates.add(
                    DetectionCandidate(
                        pos = tipPos,
                        firstSeen = currentTime,
                        lastSeen = currentTime,
                        detectionData = det
                    )
                )
            }
        }
        activeCandidates = newActiveCandidates
        
        // 2. Frame-to-Frame Tracking für Körper
        val newActiveBodyCandidates = mutableListOf<DetectionCandidate>()
        val matchedBodyIndices = mutableSetOf<Int>()
        
        for ((bodyPos, det) in dartBodies) {
            var matchIdx = -1
            var minDistSq = trackingToleranceSq
            
            for (i in activeBodyCandidates.indices) {
                if (matchedBodyIndices.contains(i)) continue
                
                val cand = activeBodyCandidates[i]
                val dSq = calculateDistanceSq(bodyPos, cand.pos)
                if (dSq < minDistSq) {
                    minDistSq = dSq
                    matchIdx = i
                }
            }
            
            if (matchIdx >= 0) {
                val match = activeBodyCandidates[matchIdx]
                match.pos = bodyPos
                match.lastSeen = currentTime
                newActiveBodyCandidates.add(match)
                matchedBodyIndices.add(matchIdx)
            } else {
                newActiveBodyCandidates.add(
                    DetectionCandidate(
                        pos = bodyPos,
                        firstSeen = currentTime,
                        lastSeen = currentTime,
                        detectionData = det
                    )
                )
            }
        }
        activeBodyCandidates = newActiveBodyCandidates
        
        // 3. Filter nach Stabilität
        val stableCandidates = activeCandidates.filter {
            (currentTime - it.firstSeen) >= stabilityThresholdMs
        }
        val stableBodies = activeBodyCandidates.filter {
            (currentTime - it.firstSeen) >= stabilityThresholdMs
        }
        
        if (stableCandidates.isEmpty()) return emptyList()
        
        // 4. 1-zu-1 Abgleich mit gebuchten Darts
        val unbookedResults = mutableListOf<Pair<PointF, ObjectDetection>>()
        val matchedBookedIndices = mutableSetOf<Int>()
        val sortedStable = stableCandidates.sortedByDescending { it.detectionData.confidence }
        var allowedJitterBypasses = maxOf(0, stableBodies.size - bookedDarts.size - ignoredDarts.size)
        
        for (cand in sortedStable) {
            var matchBookedIdx = -1
            var minBookedDistSq = jitterToleranceSq
            
            for (i in bookedDarts.indices) {
                val distSq = calculateDistanceSq(cand.pos, bookedDarts[i])
                if (distSq < minBookedDistSq) {
                    minBookedDistSq = distSq
                    matchBookedIdx = i
                }
            }
            
            if (matchBookedIdx >= 0) {
                if (matchedBookedIndices.contains(matchBookedIdx)) {
                    if (allowedJitterBypasses > 0) {
                        unbookedResults.add(Pair(cand.pos, cand.detectionData))
                        allowedJitterBypasses--
                    }
                } else {
                    bookedDarts[matchBookedIdx] = cand.pos
                    matchedBookedIndices.add(matchBookedIdx)
                }
            } else {
                var matchIgnoredIdx = -1
                var minIgnoredDistSq = jitterToleranceSq
                for (i in ignoredDarts.indices) {
                    val distSq = calculateDistanceSq(cand.pos, ignoredDarts[i])
                    if (distSq < minIgnoredDistSq) {
                        minIgnoredDistSq = distSq
                        matchIgnoredIdx = i
                    }
                }

                if (matchIgnoredIdx >= 0) {
                    ignoredDarts[matchIgnoredIdx] = cand.pos
                } else {
                    unbookedResults.add(Pair(cand.pos, cand.detectionData))
                }
            }
        }
        
        return unbookedResults
    }
    
    fun markDartAsBooked(pos: PointF) {
        bookedDarts.add(pos)
    }
    
    fun checkBoardEmptyStatus(
        currentDartCount: Int, 
        currentThrowCount: Int, 
        isBoardVisible: Boolean = true,
        resetOnly: Boolean = false
    ): BoardStatusResult? {
        val currentTime = currentTimeMillis()
        
        if (currentDartCount > 0 || !isBoardVisible) {
            noDartsDetectedTime = null
            return null
        }
        
        if (noDartsDetectedTime == null) {
            noDartsDetectedTime = currentTime
            return null
        }
        
        val emptyDuration = currentTime - noDartsDetectedTime!!
        
        if (emptyDuration >= boardEmptyThresholdMs) {
            if (resetOnly) return BoardStatusResult(BoardAction.RESET)
            
            val dartsBooked = bookedDarts.size
            if (dartsBooked == 0) return BoardStatusResult(BoardAction.RESET)
            
            if (currentThrowCount >= 3 || dartsBooked >= 3) {
                return BoardStatusResult(BoardAction.ROUND_COMPLETE, booked = dartsBooked)
            }
            
            if (isWaitingForCleanup) {
                return BoardStatusResult(BoardAction.ROUND_COMPLETE, booked = dartsBooked)
            }
            
            val dartsMissing = maxOf(0, 3 - currentThrowCount)
            if (dartsMissing > 0) {
                return BoardStatusResult(BoardAction.AUTO_MISS, count = dartsMissing)
            }
            
            return BoardStatusResult(BoardAction.ROUND_COMPLETE, booked = dartsBooked)
        }
        
        return null
    }
}
