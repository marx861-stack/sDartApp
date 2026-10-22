package com.sdart.app.domain.player

import com.sdart.app.*

/**
 * Zentrale Datei für alle Spieler-Statistik-Berechnungen
 * Alle Berechnungen sind hier zentral definiert und werden von anderen Klassen verwendet
 */
object PlayerStatistics {
    
    /**
     * Berechnet alle Runden-Scores (3 Würfe pro Runde) für einen Spieler
     * Diese private Hilfsfunktion wird von allen anderen Funktionen verwendet
     * Nur vollständige Runden (3 Würfe) werden gezählt - unvollständige Runden werden ignoriert
     */
    private fun calculateRoundScores(player: Player): List<Int> {
        val roundScores = mutableListOf<Int>()
        var currentRoundScore = 0
        var dartsInRound = 0
        
        for (score in player.scores) {
            // Jedes Feld (auch 0 für Miss) wird zur Runde hinzugefügt
            currentRoundScore += score
            dartsInRound++
            if (dartsInRound == 3) { // Runde vollständig
                roundScores.add(currentRoundScore)
                currentRoundScore = 0
                dartsInRound = 0
            }
        }
        // Unvollständige letzte Runde (1-2 Darts, z.B. beim Finish) ebenfalls mitzählen
        if (dartsInRound > 0) {
            roundScores.add(currentRoundScore)
        }
        
        return roundScores
    }
    
    /**
     * Berechnet die Durchschnitts-Punkte pro Runde (3 Würfe)
     */
    fun getAverageScorePerRound(player: Player): Int {
        if (player.scores.isEmpty()) return 0
        
        val roundScores = calculateRoundScores(player)
        
        return if (roundScores.isNotEmpty()) {
            val totalRounds = roundScores.size
            val totalPoints = roundScores.sum()
            totalPoints / totalRounds
        } else 0
    }
    
    /**
     * Berechnet die Durchschnittliche Anzahl an getroffenen Feldern pro Runde (3 Würfe)
     * Speziell für den Around the Clock Modus
     */
    fun getAverageHitsPerRound(player: Player, atcConfig: ATCGameState?): Float {
        if (player.dartThrows.isEmpty()) return 0f
        
        val variant = atcConfig?.variant ?: ATCVariant.STANDARD
        val reverse = atcConfig?.reverse == true
        val withBulls = atcConfig?.withBulls == true
        
        var hits = 0
        var currentTarget = if (reverse) {
            if (withBulls) 25 else 20
        } else 1
        var currentShanghaiHits = mutableSetOf<Int>()
        
        // Wir simulieren den Spielverlauf, um zu wissen, welcher Wurf ein Treffer war
        for (dart in player.dartThrows) {
            val score = dart.score
            val multiplier = dart.multiplier
            
            val isTargetHit = score == currentTarget
            val isBullHit = (score == 25 || score == 50) && currentTarget == 25
            
            if (isTargetHit || isBullHit) {
                var advanceBy = 0
                
                when (variant) {
                    ATCVariant.STANDARD -> {
                        hits++
                        advanceBy = 1
                    }
                    ATCVariant.DOUBLE -> {
                        if (multiplier == 2 || (score == 50 && currentTarget == 25)) {
                            hits++
                            advanceBy = 1
                        }
                    }
                    ATCVariant.TRIPLE -> {
                        if (multiplier == 3 || (currentTarget == 25 && (score == 25 || score == 50))) {
                            hits++
                            advanceBy = 1
                        }
                    }
                    ATCVariant.QUICKLY -> {
                        hits++ // Jeder korrekte Pfeil ist ein Treffer
                        advanceBy = if (currentTarget == 25) 1 else multiplier
                        if (score == 50 && currentTarget == 25) advanceBy = 1
                    }
                    ATCVariant.SHANGHAI -> {
                        val nextRequired = when {
                            currentShanghaiHits.isEmpty() -> 1
                            currentShanghaiHits.contains(1) && !currentShanghaiHits.contains(2) -> 2
                            currentShanghaiHits.contains(1) && currentShanghaiHits.contains(2) -> 3
                            else -> 1
                        }
                        
                        if (currentTarget == 25) {
                            val hitType = if (score == 25) 1 else 2 // 1=Single Bull, 2=Double Bull
                            if (hitType == nextRequired) {
                                hits++
                                currentShanghaiHits.add(hitType)
                            }
                        } else {
                            if (multiplier == nextRequired) {
                                hits++
                                currentShanghaiHits.add(multiplier)
                            }
                        }
                        
                        val neededHits = if (currentTarget == 25) setOf(1, 2) else setOf(1, 2, 3)
                        if (currentShanghaiHits.containsAll(neededHits)) {
                            advanceBy = 1
                            currentShanghaiHits.clear()
                        }
                    }
                }
                
                // Ziel weiterschalten für die Simulation
                if (advanceBy > 0) {
                    if (reverse) {
                        if (currentTarget == 25) {
                            currentTarget = 20
                        } else {
                            currentTarget -= advanceBy
                            if (currentTarget < 1) {
                                break
                            }
                        }
                    } else {
                        if (currentTarget == 25) {
                            break
                        } else {
                            currentTarget += advanceBy
                            if (currentTarget > 20) {
                                if (withBulls) currentTarget = 25 else break
                            }
                        }
                    }
                }
            }
        }
        
        val totalDarts = player.dartThrows.size
        return (hits.toFloat() / totalDarts) * 3
    }
    
    /**
     * Berechnet die höchsten Punkte in einer Runde (3 Würfe)
     */
    fun getHighestRoundScore(player: Player): Int {
        if (player.scores.isEmpty()) return 0
        
        val roundScores = calculateRoundScores(player)
        
        return if (roundScores.isNotEmpty()) roundScores.maxOrNull() ?: 0 else 0
    }
    
    /**
     * Berechnet die Anzahl der echten Runden (3 Würfe pro Runde)
     */
    fun getTotalRounds(player: Player): Int {
        if (player.scores.isEmpty()) return 0
        
        val roundScores = calculateRoundScores(player)
        return roundScores.size
    }
    
    /**
     * Berechnet die Gesamtpunkte aller Runden
     */
    fun getTotalPointsFromRounds(player: Player): Int {
        if (player.scores.isEmpty()) return 0
        
        val roundScores = calculateRoundScores(player)
        return roundScores.sum()
    }
    
    /**
     * Berechnet die Anzahl der gewonnenen Legs
     * Für All Fives und Cricket Modus wird player.wonLegs verwendet.
     * Für andere Modi (X01) hat ein Spieler ein Leg gewonnen, wenn sein Score genau 0 ist.
     * Für Around the Clock hat er das Leg/Spiel gewonnen, wenn sein Target 0 ist.
     */
    fun getWonLegs(player: Player, gameMode: GameMode? = null): Int {
        if (gameMode == null) {
            if (player.wonLegs > 0) return player.wonLegs
            return if (player.currentScore == 0 && player.scores.isNotEmpty()) 1 else 0
        }
        return when (gameMode) {
            GameMode.ALL_FIVES, GameMode.CRICKET -> player.wonLegs
            GameMode.AROUND_THE_CLOCK -> if (player.atcCurrentTarget == 0) 1 else 0
            else -> if (player.currentScore == 0 && player.scores.isNotEmpty()) 1 else 0
        }
    }
    
    /**
     * Berechnet die geschätzten höchsten Punkte in einem einzelnen Wurf
     * Basierend auf der höchsten Runden-Performance
     */
    fun getEstimatedHighestSingleThrow(player: Player): Int {
        if (player.scores.isEmpty()) return 0
        
        val highestRoundScore = getHighestRoundScore(player)
        
        return when {
            highestRoundScore >= 180 -> 60 // Triple 20
            highestRoundScore >= 140 -> 50 // Bullseye
            highestRoundScore >= 100 -> 40 // Double 20
            highestRoundScore >= 60 -> 20  // Single 20
            else -> highestRoundScore / 3
        }
    }
    
    /**
     * Berechnet die Gesamtanzahl der erzielten Marken (Marks)
     */
    fun getCricketTotalMarks(player: Player): Int {
        var totalMarks = 0
        player.dartThrows.forEach { dart ->
            val baseScore = if (dart.multiplier > 0) dart.score / dart.multiplier else dart.score
            if (baseScore in 15..20 || baseScore == 25) {
                totalMarks += when {
                    baseScore == 25 && dart.score == 50 -> 2
                    baseScore == 25 && dart.score == 25 -> 1
                    else -> dart.multiplier
                }
            }
        }
        return totalMarks
    }

    /**
     * Berechnet die Durchschnittlichen Marken pro Runde (Marks Per Round)
     * Speziell für den Cricket Modus
     */
    fun getCricketMPR(player: Player): Float {
        if (player.dartThrows.isEmpty()) return 0f
        
        var totalMarks = 0
        player.dartThrows.forEach { dart ->
            val baseScore = if (dart.multiplier > 0) dart.score / dart.multiplier else dart.score
            if (baseScore in 15..20 || baseScore == 25) {
                val marks = when {
                    baseScore == 25 && dart.score == 50 -> 2
                    baseScore == 25 && dart.score == 25 -> 1
                    else -> dart.multiplier
                }
                totalMarks += marks
            }
        }
        
        // Wir rechnen mit echten Runden (Teile durch 3)
        val totalDarts = player.dartThrows.size
        val totalRounds = totalDarts / 3.0f
        
        return if (totalRounds > 0) totalMarks / totalRounds else 0f
    }
    
    /**
     * Berechnet die Anzahl der geschlossenen Segmente in Cricket
     */
    fun getCricketClosedCount(player: Player): Int {
        return player.cricketMarks.count { it.value >= 3 }
    }

    /**
     * Berechnet alle Statistiken auf einmal für bessere Performance
     * Verwendet die gleiche Runden-Berechnung für alle Werte
     * @param game Optional: Der Spielkontext, um alle Metriken (Gewonnen, Checkout) zu berechnen
     */
    fun getAllStatistics(player: Player, game: Game? = null): PlayerStats {
        val gameMode = game?.mode
        
        if (player.scores.isEmpty() && player.dartThrows.isEmpty()) {
            return PlayerStats(
                averageScorePerRound = 0,
                highestRoundScore = 0,
                totalRounds = 0,
                totalPointsFromRounds = 0,
                wonLegs = 0,
                estimatedHighestSingleThrow = 0,
                gameWon = false,
                totalDartsThrown = 0,
                highScores180s = 0,
                highScores140s = 0,
                highScores100s = 0,
                bestCheckout = 0,
                bestMatchDarts = 0,
                totalBusts = 0,
                checkoutAttempts = 0,
                checkoutHits = 0,
                first9Points = 0,
                first9Darts = 0,
                segmentHits = emptyMap(),
                doubleHits = emptyMap(),
                dartPositions = emptyList(),
                lastGameAverage = 0f,
                lastGameDartsPerLeg = 0f
            )
        }
        
        val roundScores = calculateRoundScores(player)
        
        // Erweiterte Berechnungen
        var highScores180 = 0
        var highScores140 = 0
        var highScores100 = 0
        var checkoutAttempts = 0
        var checkoutHits = 0
        var totalBusts = 0
        var first9Points = 0
        var first9Darts = 0
        var bestCheckout = 0
        var bestMatchDarts = 0
        val segmentHits = mutableMapOf<String, Int>()
        val doubleHits = mutableMapOf<Int, Int>()
        
        // High Scores aus Runden
        for (score in roundScores) {
            when {
                score == 180 -> highScores180++
                score >= 140 -> highScores140++
                score >= 100 -> highScores100++
            }
        }
        
        // First 9 Berechnen (Aus den ersten 3 Runden = 9 Darts)
        val first9Throws = player.dartThrows.take(9)
        first9Darts = first9Throws.size
        first9Points = first9Throws.filter { !it.isBust }.sumOf { it.score }
        
        // Würfe analysieren für Maps, Busts und Doubles
        player.dartThrows.forEach { dart ->
            // Position speichern
            if (dart.isBust) totalBusts++
            
            // Segment Hits generieren (z.B. "20_t", "25_d", "Miss_s")
            val baseScore = if (dart.multiplier > 0) dart.score / dart.multiplier else dart.score
            val suffix = when (dart.multiplier) {
                1 -> "s"
                2 -> "d"
                3 -> "t"
                else -> "s"
            }
            
            val segId = when {
                dart.score == 0 -> "Miss_s"
                baseScore == 25 || dart.score == 50 || dart.score == 25 -> "25_$suffix" // Bull = 25
                else -> "${baseScore}_$suffix"
            }
            segmentHits[segId] = segmentHits.getOrDefault(segId, 0) + 1
            
            // Double Hits
            if (dart.multiplier == 2 && dart.score > 0) {
                val dbScore = if (dart.score == 50) 25 else dart.score / 2
                doubleHits[dbScore] = doubleHits.getOrDefault(dbScore, 0) + 1
            }
        }
        
        // Checkout-Analyse (Näherung aus Leg-Siegen und Double-Treffern)
        val isGameWon = getWonLegs(player, gameMode) >= (((game?.totalLegs ?: 1)) / 2) + 1
        val wonLegsCount = getWonLegs(player, gameMode)
        
        // Wenn Spieler das Leg mit Double Out gewinnen muss
        if (game?.doubleOut == true) {
            checkoutHits = wonLegsCount
            if (isGameWon) checkoutHits = maxOf(checkoutHits, 1)
        }
        
        // Checkout Attempts grob schätzen: Jeder Wurf auf Double, wenn man unter 41 Punkte hat
        // Für eine exakte Berechnung bräuchte man den restlichen Punktestand bei jedem Wurf,
        // hier verwenden wir eine Näherung anhand der geworfenen Doubles, wenn wir wissen wir checken out
        
        // Best Match Darts & Best Checkout (könnten wir aus den Legs ermitteln, 
        // aktuell fehlen uns die Leg-Details im Player-Objekt.
        // Als Proxy verwenden wir die Gesamtzahl der Darts wenn das Spiel in einem Leg endete
        if (isGameWon && ((game?.totalLegs ?: 1) == 1 || wonLegsCount > 0)) {
            bestMatchDarts = player.dartThrows.size
            
            // Letzter Wurf war Checkout
            if (player.dartThrows.isNotEmpty()) {
                val lastThrows = player.dartThrows.takeLast(3) // Checkout Score der letzten Runde
                bestCheckout = lastThrows.sumOf { if (it.isBust) 0 else it.score }
            }
        }
        
        val dartPositions = player.dartThrows.map { SavedPosition(it.position.x, it.position.y) }
        
        return PlayerStats(
            averageScorePerRound = if (roundScores.isNotEmpty()) {
                val totalRounds = roundScores.size
                val totalPointsFromRounds = roundScores.sum()
                totalPointsFromRounds / totalRounds
            } else 0,
            highestRoundScore = if (roundScores.isNotEmpty()) roundScores.maxOrNull() ?: 0 else 0,
            totalRounds = roundScores.size,
            totalPointsFromRounds = roundScores.sum(),
            wonLegs = wonLegsCount,
            estimatedHighestSingleThrow = getEstimatedHighestSingleThrow(player),
            gameWon = isGameWon,
            totalDartsThrown = player.dartThrows.size,
            highScores180s = highScores180,
            highScores140s = highScores140,
            highScores100s = highScores100,
            bestCheckout = bestCheckout,
            bestMatchDarts = bestMatchDarts,
            totalBusts = totalBusts,
            checkoutAttempts = checkoutAttempts, // Näherung, noch nicht 100% präzise
            checkoutHits = checkoutHits,
            first9Points = first9Points,
            first9Darts = first9Darts,
            segmentHits = segmentHits,
            doubleHits = doubleHits,
            dartPositions = dartPositions,
            lastGameAverage = if (roundScores.isNotEmpty()) roundScores.sum().toFloat() / roundScores.size else 0f,
            lastGameDartsPerLeg = if (wonLegsCount > 0) player.dartThrows.size.toFloat() / wonLegsCount else player.dartThrows.size.toFloat()
        )
    }
}

/**
 * Datenklasse für alle Spieler-Statistiken
 * Wird verwendet um alle Werte auf einmal zu berechnen
 */
data class PlayerStats(
    val averageScorePerRound: Int,
    val highestRoundScore: Int,
    val totalRounds: Int,
    val totalPointsFromRounds: Int,
    val wonLegs: Int,
    val estimatedHighestSingleThrow: Int,
    // Erweiterte Felder:
    val gameWon: Boolean,
    val totalDartsThrown: Int,
    val highScores180s: Int,
    val highScores140s: Int,
    val highScores100s: Int,
    val bestCheckout: Int,
    val bestMatchDarts: Int, // Geringste Anzahl Darts für ein Leg
    val totalBusts: Int,
    val checkoutAttempts: Int,
    val checkoutHits: Int,
    val first9Points: Int,
    val first9Darts: Int,
    val segmentHits: Map<String, Int>,
    val doubleHits: Map<Int, Int>,
    val dartPositions: List<SavedPosition>,
    val lastGameAverage: Float,
    val lastGameDartsPerLeg: Float
)
