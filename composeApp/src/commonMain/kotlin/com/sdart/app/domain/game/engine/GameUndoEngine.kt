package com.sdart.app.domain.game.engine

import com.sdart.app.*

object GameUndoEngine {

        fun undo(currentGame: Game): Game? {
        
        
        // PrÃ¼fe ob Ã¼berhaupt WÃ¼rfe vorhanden sind
        if (currentGame.throwHistory.isEmpty()) {
            println("DEBUG: Keine WÃ¼rfe zum RÃ¼ckgÃ¤ngig machen verfÃ¼gbar")
            return null
        }
        
        val currentPlayerIndex = currentGame.currentPlayerIndex
        val lastThrowInHistory = currentGame.throwHistory.lastOrNull()
        

        
        // Normales Undo: Entferne den letzten Wurf aus der Throw-History (LIFO-Prinzip)
        // Erstelle eine Kopie der Throw-History fÃ¼r die Manipulation
        val updatedThrowHistory = currentGame.throwHistory.toMutableList()
        val lastThrow = updatedThrowHistory.removeAt(updatedThrowHistory.size - 1)
        val playerIndex = lastThrow.playerIndex
        
        println("DEBUG: Undo - Entferne letzten Wurf von Spieler ${playerIndex + 1}: Score ${lastThrow.score}")
        
        // Erstelle eine Kopie des aktuellen Spiels mit korrekter Deep Copy
        val playersCopy = currentGame.players.map { player ->
            player.copy(
                cricketMarks = player.cricketMarks.toMutableMap(),
                scores = player.scores.toMutableList(),
                dartThrows = player.dartThrows.toMutableList()
            )
        }.toMutableList()
        
        // Entferne den Wurf vom entsprechenden Spieler
        val targetPlayer = playersCopy[playerIndex]
        if (targetPlayer.scores.isNotEmpty()) {
            val removedScore = targetPlayer.scores.removeAt(targetPlayer.scores.size - 1)
            
            // Spielmodus-spezifisches Undo der Punkte und Stati
            when (currentGame.mode) {
                GameMode.CRICKET -> {
                    // Cricket-Undo: Marks und Scoring-Punkte zurÃ¼cksetzen
                    val baseScore = if (lastThrow.multiplier > 0) lastThrow.score / lastThrow.multiplier else lastThrow.score
                    val segment = when {
                        baseScore == 25 -> 25
                        baseScore in 15..20 -> baseScore
                        else -> null
                    }
                    
                    if (segment != null) {
                        // Marks berechnen die entfernt werden mÃ¼ssen
                        val marksToRemove = when {
                            segment == 25 && lastThrow.score == 50 -> 2
                            segment == 25 && lastThrow.score == 25 -> 1
                            else -> lastThrow.multiplier
                        }
                        
                        val currentMarks = targetPlayer.cricketMarks.getOrDefault(segment, 0)
                        val oldMarks = maxOf(0, currentMarks - marksToRemove)
                        
                        // Scoring-Punkte berechnen die zurÃ¼ckgenommen werden mÃ¼ssen
                        // Scoring-Marks sind Marks, die den Schwellenwert von 3 bereits Ã¼berschritten hatten
                        val scoringMarksCalculated = maxOf(0, currentMarks - maxOf(3, oldMarks))
                        
                        if (scoringMarksCalculated > 0) {
                            val pointsPerMark = if (segment == 25) 25 else segment
                            val pointsToUndo = scoringMarksCalculated * pointsPerMark
                            val variant = currentGame.cricketConfig?.variant ?: CricketVariant.STANDARD
                            
                            if (variant == CricketVariant.STANDARD) {
                                // Standard: Punkte vom eigenen Score abziehen
                                targetPlayer.currentScore -= pointsToUndo
                                println("DEBUG Cricket Undo: Score reduziert um $pointsToUndo fÃ¼r ${targetPlayer.name}")
                            } else {
                                // Cut-Throat: Punkte von allen Gegnern abziehen, die das Segment offen haben
                                playersCopy.forEachIndexed { idx, player ->
                                    if (idx != playerIndex && !player.isSegmentClosed(segment)) {
                                        player.currentScore -= pointsToUndo
                                        println("DEBUG Cricket Cut Throat Undo: Score reduziert um $pointsToUndo fÃ¼r Gegner ${player.name}")
                                    }
                                }
                            }
                        }
                        
                        // Marks in der Map aktualisieren
                        targetPlayer.cricketMarks[segment] = oldMarks
                        println("DEBUG Cricket Undo: Marks fÃ¼r Segment $segment reduziert von $currentMarks auf $oldMarks")
                    }
                }
                GameMode.ALL_FIVES -> {
                    // Wenn wir das Ende einer Runde undo-en (throwsInRound == 0),
                    // mÃ¼ssen wir prÃ¼fen ob in dieser abgeschlossenen Runde Punkte vergeben wurden.
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
                                println("DEBUG All Fives Undo: Runden-Punkte $roundSum von ${targetPlayer.name} abgezogen")
                            }
                        }
                    }
                }
                GameMode.AROUND_THE_CLOCK -> {
                    // ATC: Ziel und Shanghai-Treffer aus den verbleibenden Würfen des Spielers neu berechnen
                    val config = currentGame.atcConfig ?: ATCGameState()
                    var progress = ATCProgress(ATCGameEngine.startTarget(config))
                    updatedThrowHistory.filter { it.playerIndex == playerIndex }.forEach { throwData ->
                        progress = ATCGameEngine.advance(progress, throwData.score, throwData.multiplier, config).first
                    }
                    targetPlayer.atcCurrentTarget = progress.target
                    targetPlayer.atcShanghaiHits = progress.shanghaiHits
                }
                else -> {
                    if (lastThrow.isBust) {
                        // Bust-Undo: Der Wurf war ein Bust. Der Score wurde am Ende der Runde (beim Bust-Wurf)
                        // auf den Score zu Rundenbeginn zurÃ¼ckgesetzt. Um auf den Score vor dem Bust-Wurf zurÃ¼ckzukehren,
                        // mÃ¼ssen wir die Summe der anderen (gÃ¼ltigen) WÃ¼rfe der Runde abziehen.
                        val newThrowsInRound = GameRoundHelper.calculateThrowsInRound(playerIndex, updatedThrowHistory)
                        val otherThrowsSum = targetPlayer.scores.takeLast(newThrowsInRound).sum()
                        targetPlayer.currentScore -= otherThrowsSum
                        println("DEBUG X01 Undo (Bust): Score um $otherThrowsSum auf ${targetPlayer.currentScore} reduziert")
                    } else {
                        // X01: Punkte zum Score hinzufÃ¼gen (da sie beim Wurf abgezogen wurden)
                        targetPlayer.currentScore += removedScore
                        println("DEBUG X01 Undo: Score $removedScore zu Spieler ${playerIndex + 1} hinzugefÃ¼gt, neuer Score: ${targetPlayer.currentScore}")
                    }
                }
            }
        }
        
        // Entferne die entsprechende Dart-Position
        val removedThrow: DartThrow? = if (targetPlayer.dartThrows.isNotEmpty()) {
            targetPlayer.dartThrows.removeAt(targetPlayer.dartThrows.size - 1)
            println("DEBUG: Dart-Position von Spieler ${playerIndex + 1} entfernt")
            // Speichere den entfernten Wurf fÃ¼r Double-In PrÃ¼fung
            lastThrow
        } else {
            null
        }
        
        // Double-In Status prÃ¼fen und ggf. zurÃ¼cksetzen
        // Wenn der entfernte Wurf ein Double war (der Double-In erreicht hat) und keine weiteren Doubles in der History sind
        if (currentGame.doubleIn && removedThrow != null) {
            val wasDoubleInWurf = removedThrow.multiplier == 2 || removedThrow.score == 50
            if (wasDoubleInWurf) {
                // PrÃ¼fe ob noch andere Double-WÃ¼rfe in der verbleibenden History sind
                val hasOtherDoubles = updatedThrowHistory.any { throwData ->
                    throwData.playerIndex == playerIndex && (throwData.multiplier == 2 || throwData.score == 50)
                }
                if (!hasOtherDoubles) {
                    // Keine weiteren Doubles: Double-In Status zurÃ¼cksetzen
                    targetPlayer.hasDoubleIn = false
                    println("DEBUG: Double-In Status zurÃ¼ckgesetzt fÃ¼r Spieler ${playerIndex + 1}")
                }
            }
        }
        
        // Berechne den neuen Spieler und die Wurf-Anzahl
        val newCurrentPlayerIndex = playerIndex
        val newThrowsInRound = GameRoundHelper.calculateThrowsInRound(newCurrentPlayerIndex, updatedThrowHistory)
        
        // Neues Game-Objekt erstellen mit aktualisierter Throw-History
        return currentGame.copy(
            players = playersCopy,
            currentPlayerIndex = newCurrentPlayerIndex,
            isFinished = false,
            throwsInRound = newThrowsInRound,
            throwHistory = updatedThrowHistory
        )
        
        println("DEBUG: Undo abgeschlossen - Spieler ${newCurrentPlayerIndex + 1} ist jetzt dran mit $newThrowsInRound WÃ¼rfen")
        println("DEBUG: Throw-History hat jetzt ${updatedThrowHistory.size} EintrÃ¤ge (vorher: ${currentGame.throwHistory.size})")
    }
    
    // Hilfsfunktion zur Berechnung der WÃ¼rfe in der aktuellen Runde

}
