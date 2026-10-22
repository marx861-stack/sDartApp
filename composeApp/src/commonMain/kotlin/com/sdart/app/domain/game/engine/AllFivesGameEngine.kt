package com.sdart.app.domain.game.engine

import androidx.compose.ui.geometry.Offset
import com.sdart.app.*

object AllFivesGameEngine {

        fun processThrow(
        score: Int,
        position: androidx.compose.ui.geometry.Offset?,
        multiplier: Int,
        currentGame: Game,
        audioAnnounceEachDart: Boolean,
        audioAnnounceTurnTotal: Boolean,
        triggerSound: (String, Int) -> Unit
    ): Game {
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        val isMiss = score == 0
        
        // Play the score sound for single dart
        if (audioAnnounceEachDart) {
            val voiceKey = when {
                score == 50 -> "Bullseye"
                score == 25 -> "Twenty Five"
                multiplier == 3 -> "Triple ${score / multiplier}"
                multiplier == 2 -> "Double ${score / multiplier}"
                score == 0 -> "Miss"
                else -> score.toString()
            }
            triggerSound(voiceKey, 0)
        }
        
        // Berechne aktuelles Runden-Total (Summe der letzten WÃ¼rfe des aktuellen Spielers in dieser Runde)
        var roundTotal = 0
        var throwsInCurrentRound = 0
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == currentGame.currentPlayerIndex) {
                // ZÃ¤hle nur WÃ¼rfe der aktuellen Runde (bis zum letzten Spielerwechsel)
                if (throwsInCurrentRound < currentGame.throwsInRound) {
                    roundTotal += throwData.score
                    throwsInCurrentRound++
                } else {
                    break
                }
            } else {
                break
            }
        }
        
        // FÃ¼ge aktuellen Wurf zum Runden-Total hinzu (wenn kein Miss)
        val newRoundTotal = if (isMiss) roundTotal else roundTotal + score
        
        // Spezielle Regel: Wenn Runden-Total bereits durch 5 teilbar ist UND ein Miss geworfen wird
        // â†’ Runde wird sofort als ungÃ¼ltig markiert
        val roundWasDivisibleBy5 = roundTotal > 0 && roundTotal % 5 == 0
        val roundInvalidated = roundWasDivisibleBy5 && isMiss
        
        // DartThrow-Objekt erstellen (auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        
        // Deep copy der Spieler
        val playersCopy = currentGame.players.mapIndexed { idx, player ->
            if (idx == currentGame.currentPlayerIndex) {
                val newScores = player.scores.toMutableList().apply { add(score) }
                val newDartThrows = player.dartThrows.toMutableList().apply { 
                    dartThrow?.let { add(it) }
                }
                player.copy(
                    scores = newScores,
                    dartThrows = newDartThrows
                )
            } else {
                player.copy(
                    scores = player.scores.toMutableList(),
                    dartThrows = player.dartThrows.toMutableList()
                )
            }
        }.toMutableList()
        
        // Runden-Logik
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        var isFinished = false
        var currentLeg = currentGame.currentLeg
        
        // Wenn Runde ungÃ¼ltig gemacht wurde, setze Runden-Total auf 0
        val finalRoundTotal = if (roundInvalidated) 0 else newRoundTotal
        
        // Nach 3 WÃ¼rfen: Runde auswerten
        if (throwsInRound >= 3) {
            val currentPlayerUpdated = playersCopy[currentGame.currentPlayerIndex]
            
            // PrÃ¼fe ob Runde gÃ¼ltig ist:
            // 1. Runden-Total muss durch 5 teilbar sein
            // 2. Alle 3 Pfeile mÃ¼ssen getroffen werden (keine Misses)
            // Sammle die letzten 3 WÃ¼rfe des aktuellen Spielers (inkl. aktueller Wurf)
            val lastThreeThrows = mutableListOf<DartThrow>()
            var foundThrows = 0
            // FÃ¼ge zuerst aktuellen Wurf hinzu
            dartThrow?.let { lastThreeThrows.add(it) }
            // Dann die letzten 2 WÃ¼rfe aus der History
            for (i in currentGame.throwHistory.size - 1 downTo 0) {
                val throwData = currentGame.throwHistory[i]
                if (throwData.playerIndex == currentGame.currentPlayerIndex && foundThrows < 2) {
                    lastThreeThrows.add(0, throwData)
                    foundThrows++
                } else if (throwData.playerIndex != currentGame.currentPlayerIndex) {
                    break
                }
            }
            
            // PrÃ¼fe ob alle 3 WÃ¼rfe getroffen wurden (kein Miss)
            val hasNoMisses = lastThreeThrows.size == 3 && lastThreeThrows.all { it.score > 0 }
            val isDivisibleBy5 = finalRoundTotal > 0 && finalRoundTotal % 5 == 0
            val isValidRound = isDivisibleBy5 && hasNoMisses && !roundInvalidated
            
            if (isValidRound) {
                // Runde ist gÃ¼ltig: Punkte vergeben (jetzt direkt Zahlenwert summiert)
                val pointsEarned = finalRoundTotal
                val newScore = currentPlayerUpdated.currentScore + pointsEarned
                
                println("DEBUG All Fives: GÃ¼ltige Runde! Runden-Total: $finalRoundTotal, Punkte: $pointsEarned")
                
                // Aktualisiere Spieler-Score
                playersCopy[currentGame.currentPlayerIndex] = currentPlayerUpdated.copy(
                    currentScore = newScore,
                    totalRounds = currentPlayerUpdated.totalRounds + 1
                )
                
                var soundToPlay = "bing"
                
                // PrÃ¼fe ob Spieler Ziel-Punktzahl erreicht hat (Leg gewonnen)
                if (newScore >= currentGame.targetScore) {
                    // Leg gewonnen!
                    val updatedPlayer = playersCopy[currentGame.currentPlayerIndex]
                    val newWonLegs = updatedPlayer.wonLegs + 1
                    playersCopy[currentGame.currentPlayerIndex] = updatedPlayer.copy(
                        wonLegs = newWonLegs
                    )
                    
                    // PrÃ¼fe ob Spieler genug Legs gewonnen hat (Anzahl Legs Ã· 2 + 1)
                    val requiredLegs = if (currentGame.players.size == 1) currentGame.totalLegs else (currentGame.totalLegs / 2) + 1
                    if (newWonLegs >= requiredLegs) {
                        // Spiel gewonnen!
                        isFinished = true
                        soundToPlay = "bing ... Game Shot and the Win"
                    } else {
                        // Neues Leg starten
                        currentLeg++
                        soundToPlay = "bing ... Leg to ... Game on"
                        // Setze alle Punkte auf 0 zurÃ¼ck
                        for (i in playersCopy.indices) {
                            playersCopy[i] = playersCopy[i].copy(currentScore = 0)
                        }
                        
                        // Spieler-Reihenfolge mischen wenn aktiviert
                        if (currentGame.randomizeOrder) {
                            val shuffledPlayers = playersCopy.shuffled()
                            playersCopy.clear()
                            playersCopy.addAll(shuffledPlayers)
                            currentPlayerIndex = 0
                            println("DEBUG All Fives: Spieler-Reihenfolge fÃ¼r Leg $currentLeg gemischt")
                        } else {
                            currentPlayerIndex = 0
                        }
                        
                        println("DEBUG All Fives: Neues Leg $currentLeg gestartet")
                    }
                }
                triggerSound(soundToPlay, 0)
            } else {
                // Runde ist ungÃ¼ltig: Keine Punkte
                playersCopy[currentGame.currentPlayerIndex] = currentPlayerUpdated.copy(
                    totalRounds = currentPlayerUpdated.totalRounds + 1
                )
                triggerSound("buzzer", 0)
                println("DEBUG All Fives: UngÃ¼ltige Runde! Runden-Total: $finalRoundTotal (nicht durch 5 teilbar oder Miss)")
            }
            
            // Zum nÃ¤chsten Spieler wechseln
            if (!isFinished) {
                currentPlayerIndex = (currentPlayerIndex + 1) % playersCopy.size
                throwsInRound = 0
            }
        }
        
        // Neues Game-Objekt zuweisen
        val updatedThrowHistory = currentGame.throwHistory.toMutableList().apply {
            dartThrow?.let { add(it) }
        }
        
        return currentGame.copy(
            players = playersCopy,
            currentPlayerIndex = currentPlayerIndex,
            isFinished = isFinished,
            throwsInRound = throwsInRound,
            throwHistory = updatedThrowHistory,
            currentLeg = currentLeg
        )
    }
    
    // setMultiplier wurde entfernt - der Multiplikator wird direkt aus dem Dartboard berechnet
    
    // ATC-MODUS: Spezielle Logik fÃ¼r Around the Clock



    fun undoThrow(
        currentGame: Game,
        targetPlayer: Player,
        playerIndex: Int
    ) {
        if (currentGame.throwsInRound == 0) {
            val lastThreeThrows = mutableListOf<DartThrow>()
            for (i in currentGame.throwHistory.size - 1 downTo 0) {
                val throwData = currentGame.throwHistory[i]
                if (throwData.playerIndex == playerIndex && lastThreeThrows.size < 3) {
                    lastThreeThrows.add(0, throwData)
                } else if (throwData.playerIndex != playerIndex) {
                    break
                }
            }
            
            if (lastThreeThrows.size == 3) {
                val roundSum = lastThreeThrows.sumOf { it.score }
                val hasNoMisses = lastThreeThrows.all { it.score > 0 }
                val isDivisibleBy5 = roundSum > 0 && roundSum % 5 == 0
                val isValidRound = isDivisibleBy5 && hasNoMisses
                
                if (isValidRound) {
                    targetPlayer.currentScore -= roundSum
                }
            }
        }
    }

}
