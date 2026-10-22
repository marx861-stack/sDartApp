package com.sdart.app.domain.game.engine

import androidx.compose.ui.geometry.Offset
import com.sdart.app.*

object X01GameEngine {

    fun processThrow(
        score: Int,
        position: Offset?,
        multiplier: Int,
        currentGame: Game,
        audioAnnounceEachDart: Boolean,
        audioAnnounceTurnTotal: Boolean,
        triggerSound: (String, Int) -> Unit
    ): Game? {
        // Validierung: Score muss im gültigen Bereich sein
        if (score !in 0..180) {
            println("DEBUG: Ungültiger Score $score (muss zwischen 0 und 180 sein)")
            return currentGame
        }
        
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        val currentScore = currentPlayer.currentScore
        
        // Double-In Validierung: Wenn Double-In aktiv ist und Spieler noch nicht Double-In hat
        val isDouble = multiplier == 2 || score == 50 // Double Ring oder Double Bull
        val needsDoubleIn = currentGame.doubleIn && !currentPlayer.hasDoubleIn
        
        if (needsDoubleIn) {
            // Nur Doubles werden akzeptiert, bis Double-In erreicht ist
            if (!isDouble) {
                println("DEBUG: Double-In Regel: Wurf wird ignoriert (kein Double). Score bleibt: $currentScore")
                
                // Wurf wird ignoriert, aber Runde lÃ¤uft weiter
                var throwsInRound = currentGame.throwsInRound + 1
                var currentPlayerIndex = currentGame.currentPlayerIndex
                var isFinished = false
                
                if (throwsInRound >= 3) {
                    // Finde den nÃ¤chsten nicht-fertigen Spieler
                    val nextActivePlayerIndex = findNextActivePlayer(currentPlayerIndex, currentGame.players)
                    if (nextActivePlayerIndex != -1) {
                        currentPlayerIndex = nextActivePlayerIndex
                        throwsInRound = 0
                    } else {
                        // Alle Spieler sind fertig
                        isFinished = areAllPlayersFinished(currentGame.players)
                    }
                }
                
                // Neues Game-Objekt mit aktualisiertem Spielerwechsel (Score bleibt unverÃ¤ndert)
                return Game(
                    players = currentGame.players,
                    mode = currentGame.mode,
                    currentPlayerIndex = currentPlayerIndex,
                    isFinished = isFinished,
                    throwsInRound = throwsInRound,
                    throwHistory = currentGame.throwHistory,
                    doubleIn = currentGame.doubleIn,
                    doubleOut = currentGame.doubleOut,
                    targetScore = currentGame.targetScore,
                    totalLegs = currentGame.totalLegs,
                    currentLeg = currentGame.currentLeg,
                    randomizeOrder = currentGame.randomizeOrder,
                    startScore = currentGame.startScore
                )
            } else {
                // Erster Double-Wurf: Double-In erreicht!
                println("DEBUG: Double-In erreicht! Spieler ${currentGame.currentPlayerIndex + 1} hat mit Double gestartet.")
            }
        }
        
        val newScore = currentScore - score
        
        // BUST: Wenn Score negativ wird oder bei Double-Out 1 Ã¼brig bleibt (da kein Double-Finish mÃ¶glich ist)
        val isBust = newScore < 0 || (newScore == 1 && currentGame.doubleOut)
        
        // Double-Out Validierung: Wenn Score auf 0 geht, muss der letzte Wurf ein Double sein (auÃŸer Double Bull = 50)
        val isValidFinish = if (newScore == 0) {
            if (currentGame.doubleOut) {
                // Double-Out aktiv: Muss mit Double enden (Multiplikator 2 oder Double Bull 50)
                val isDoubleOut = multiplier == 2 || score == 50
                if (!isDoubleOut) {
                    println("DEBUG: Double-Out Regel: Spiel kann nicht mit Single beendet werden")
                }
                isDoubleOut
            } else {
                // Double-Out nicht aktiv: Jeder Wurf kann beenden
                true
            }
        } else {
            true
        }
        
        // Wenn Bust oder ungÃ¼ltiges Finish, wird der Wurf nicht akzeptiert
        // Score wird zurÃ¼ckgesetzt, aber WÃ¼rfe bleiben fÃ¼r Statistik erhalten
        if (isBust || !isValidFinish) {
            println("DEBUG: Bust oder ungÃ¼ltiges Finish - Score wird zurÃ¼ckgesetzt, WÃ¼rfe bleiben fÃ¼r Statistik erhalten")
            triggerSound("buzzer ... Bust", 0)
            
            val bustingPlayerIndex = currentGame.currentPlayerIndex
            val bustingPlayer = currentGame.players[bustingPlayerIndex]
            
            // Erstelle DartThrow-Objekt fÃ¼r den Bust-Wurf (wird fÃ¼r Statistik benÃ¶tigt, auch bei position == null)
            val dartThrow = DartThrow(
                score = score,
                position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
                playerIndex = bustingPlayerIndex,
                multiplier = multiplier,
                isBust = true
            )
            
            // Berechne die Summe aller WÃ¼rfe der aktuellen Runde (OHNE dem aktuellen Bust-Wurf)
            // Der Bust-Wurf wird NICHT zur roundScoreSum gezÃ¤hlt, da er nicht akzeptiert wird
            var roundScoreSum = 0 // Starte bei 0, da Bust-Wurf nicht zÃ¤hlt
            val throwsInCurrentRound = currentGame.throwsInRound
            
            // Gehe rÃ¼ckwÃ¤rts durch die Throw-History und berechne die Summe der aktuellen Runde
            // (nur die gÃ¼ltigen WÃ¼rfe vor dem Bust-Wurf)
            var throwsFound = 0
            for (i in currentGame.throwHistory.size - 1 downTo 0) {
                val throwData = currentGame.throwHistory[i]
                
                if (throwData.playerIndex != bustingPlayerIndex) {
                    // Anderer Spieler gefunden - Runde ist abgeschlossen, stoppe sofort
                    break
                }
                
                // Gleicher Spieler: PrÃ¼fe ob dies noch zur aktuellen Runde gehÃ¶rt
                if (throwsFound < throwsInCurrentRound) {
                    // Dies ist ein Wurf aus der aktuellen Runde (vor dem Bust-Wurf)
                    roundScoreSum += throwData.score
                    throwsFound++
                } else {
                    // Wir haben bereits alle WÃ¼rfe der aktuellen Runde gefunden
                    // Alle weiteren WÃ¼rfe gehÃ¶ren zu frÃ¼heren Runden - stoppe
                    break
                }
            }
            
            // Berechne den Score vom Anfang der Runde
            // bustingPlayer.currentScore ist der Score NACH allen gÃ¼ltigen WÃ¼rfen der Runde (vor dem Bust)
            // roundScoreSum ist die Summe aller gÃ¼ltigen WÃ¼rfe der Runde
            // Also: scoreAtRoundStart = aktueller Score + alle gÃ¼ltigen WÃ¼rfe der Runde
            val scoreAtRoundStart = bustingPlayer.currentScore + roundScoreSum
            
            // PrÃ¼fe ob Double-In in dieser Runde erreicht wurde (muss zurÃ¼ckgesetzt werden)
            // Double-In sollte nur zurÃ¼ckgesetzt werden, wenn es in dieser Runde ERREICHT wurde,
            // nicht wenn der Spieler bereits Double-In hatte und dann bustet
            val hadDoubleInBeforeRound = bustingPlayer.hasDoubleIn
            
            // PrÃ¼fe ob in dieser Runde ein Double geworfen wurde (das Double-In erreicht haben kÃ¶nnte)
            var hasDoubleInCurrentRound = false
            throwsFound = 0
            for (i in currentGame.throwHistory.size - 1 downTo 0) {
                val throwData = currentGame.throwHistory[i]
                
                if (throwData.playerIndex != bustingPlayerIndex) {
                    break
                }
                
                if (throwsFound < throwsInCurrentRound) {
                    if (throwData.multiplier == 2 || throwData.score == 50) {
                        hasDoubleInCurrentRound = true
                        break // Ein Double gefunden reicht
                    }
                    throwsFound++
                } else {
                    break
                }
            }
            
            // PrÃ¼fe auch den aktuellen Bust-Wurf (falls er ein Double ist)
            if (!hasDoubleInCurrentRound && (multiplier == 2 || score == 50)) {
                hasDoubleInCurrentRound = true
            }
            
            // Double-In wurde in dieser Runde erreicht, wenn:
            // 1. Double-In ist aktiv
            // 2. Spieler hatte KEIN Double-In vor der Runde
            // 3. Ein Double wurde in dieser Runde geworfen
            // Nur dann sollte Double-In zurÃ¼ckgesetzt werden (da es in dieser Runde erreicht wurde)
            val doubleInAchievedThisRound = currentGame.doubleIn && 
                !hadDoubleInBeforeRound && 
                hasDoubleInCurrentRound
            
            // WICHTIG: WÃ¼rfe werden NICHT entfernt - sie bleiben fÃ¼r die Statistik erhalten
            // Der Bust-Wurf wird zu den Listen hinzugefÃ¼gt, aber der Score wird zurÃ¼ckgesetzt
            val updatedThrowHistory = currentGame.throwHistory.toMutableList().apply {
                dartThrow?.let { add(it) }
            }
            
            val playersCopy = currentGame.players.mapIndexed { idx, player ->
                if (idx == bustingPlayerIndex) {
                    // Wenn Double-In in dieser Runde erreicht wurde, zurÃ¼cksetzen
                    val updatedHasDoubleIn = if (doubleInAchievedThisRound) {
                        false
                    } else {
                        hadDoubleInBeforeRound
                    }
                    
                    player.copy(
                        currentScore = scoreAtRoundStart,
                        hasDoubleIn = updatedHasDoubleIn,
                        scores = player.scores.toMutableList().apply { add(score) }, // Bust-Wurf fÃ¼r Statistik hinzufÃ¼gen
                        dartThrows = player.dartThrows.toMutableList().apply { 
                            dartThrow?.let { add(it) } // Bust-Wurf fÃ¼r Statistik hinzufÃ¼gen
                        }
                    )
                } else {
                    player.copy()
                }
            }
            
            // Runde sofort beenden: Spieler wechseln (unabhÃ¤ngig von der Anzahl der WÃ¼rfe)
            // Finde den nÃ¤chsten nicht-fertigen Spieler
            val nextPlayerIndex = findNextActivePlayer(bustingPlayerIndex, playersCopy)
            val throwsInRound = 0 // Runde ist beendet, nÃ¤chster Spieler startet mit 0 WÃ¼rfen
            
            // PrÃ¼fe ob alle Spieler fertig sind
            val isFinished = if (nextPlayerIndex == -1) {
                areAllPlayersFinished(playersCopy)
            } else {
                false
            }
            
            println("DEBUG: Score zurÃ¼ckgesetzt von $currentScore auf $scoreAtRoundStart (Anfang der Runde)")
            println("DEBUG: ${throwsInCurrentRound + 1} WÃ¼rfe der Runde (inkl. Bust-Wurf) bleiben fÃ¼r Statistik erhalten")
            
            // Neues Game-Objekt mit aktualisiertem Spielerwechsel und zurÃ¼ckgesetztem Score
            // throwHistory enthÃ¤lt jetzt auch den Bust-Wurf
            // Wenn kein aktiver Spieler gefunden wurde, verwende den nÃ¤chsten Index (fÃ¼r Fallback)
            val finalPlayerIndex = if (nextPlayerIndex == -1) {
                (bustingPlayerIndex + 1) % playersCopy.size
            } else {
                nextPlayerIndex
            }
            
            return Game(
                players = playersCopy,
                mode = currentGame.mode,
                currentPlayerIndex = finalPlayerIndex,
                isFinished = isFinished,
                throwsInRound = throwsInRound,
                throwHistory = updatedThrowHistory, // EnthÃ¤lt jetzt auch den Bust-Wurf
                doubleIn = currentGame.doubleIn,
                doubleOut = currentGame.doubleOut,
                targetScore = currentGame.targetScore,
                totalLegs = currentGame.totalLegs,
                currentLeg = currentGame.currentLeg,
                randomizeOrder = currentGame.randomizeOrder,
                startScore = currentGame.startScore
            )
        }
        
        // GÃ¼ltiger Wurf: Score wird akzeptiert
        println("DEBUG: GÃ¼ltiger Wurf - Score: $score, Multiplikator: $multiplier, Neuer Score: $newScore")
        
        // Play the score sound for single dart
        if (audioAnnounceEachDart && newScore != 0) {
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
        
        // DartThrow-Objekt erstellen (auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        
        // Deep copy der Spieler mit aktualisiertem Score
        // Wenn Double-In erreicht wurde, setze hasDoubleIn = true
        val justAchievedDoubleIn = needsDoubleIn && isDouble
        var playersCopy = currentGame.players.mapIndexed { idx, player ->
            if (idx == currentGame.currentPlayerIndex) {
                player.copy(
                    currentScore = newScore,
                    hasDoubleIn = player.hasDoubleIn || justAchievedDoubleIn, // Double-In Status aktualisieren
                    scores = player.scores.toMutableList().apply { add(score) },
                    dartThrows = player.dartThrows.toMutableList().apply { 
                        dartThrow?.let { add(it) }
                    }
                )
            } else {
                player.copy(
                    scores = player.scores.toMutableList(),
                    dartThrows = player.dartThrows.toMutableList()
                )
            }
        }
        
        // Runden-Logik
        var isFinished = false
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        
        var currentLeg = currentGame.currentLeg
        var legWon = false
        if (newScore == 0) {
            // Aktueller Spieler hat 0 erreicht
            println("DEBUG: Spieler ${currentPlayerIndex + 1} (${playersCopy[currentPlayerIndex].name}) hat 0 erreicht!")
            
            // Gewonnenes Leg verbuchen
            val winningPlayer = playersCopy[currentPlayerIndex]
            val newWonLegs = winningPlayer.wonLegs + 1
            playersCopy = playersCopy.mapIndexed { idx, player ->
                if (idx == currentPlayerIndex) {
                    player.copy(wonLegs = newWonLegs)
                } else {
                    player.copy()
                }
            }

            val requiredLegs = if (currentGame.players.size == 1) currentGame.totalLegs else (currentGame.totalLegs / 2) + 1
            
            // Hat dieser Leg-Gewinn das Spiel entschieden?
            // Oder sind wir bereits in der VerlÃ¤ngerung ("Weiterspielen" nach Spielende)?
            val alreadyHasWinner = currentGame.players.any { it.wonLegs >= requiredLegs }
            val nowHasWinner = playersCopy.any { it.wonLegs >= requiredLegs }
            
            if (nowHasWinner) {
                if (alreadyHasWinner) {
                    // Im Weiterspielen-Modus: PrÃ¼fen ob alle fertig sind
                    if (areAllPlayersFinished(playersCopy)) {
                        isFinished = true
                        println("DEBUG: Alle Spieler sind fertig - Spiel beendet!")
                    } else {
                        // NÃ¤chsten aktiven Spieler suchen
                        val nextActivePlayerIndex = findNextActivePlayer(currentPlayerIndex, playersCopy)
                        if (nextActivePlayerIndex != -1) {
                            currentPlayerIndex = nextActivePlayerIndex
                            throwsInRound = 0
                        } else {
                            isFinished = true
                        }
                    }
                } else {
                    // Das Spiel wurde gerade eben erst entschieden!
                    isFinished = true
                    triggerSound("Game Shot and the Win", 0)
                    println("DEBUG: Spiel beendet! Gewinner: ${winningPlayer.name} mit ${newWonLegs} Legs")
                }
            } else {
                // Normaler Leg-Gewinn (noch kein Spielgewinner)
                legWon = true
                triggerSound("Game Shot", 0)
                
                currentLeg++
                println("DEBUG: Leg gewonnen! Neues Leg $currentLeg gestartet.")
                
                // Spieler-Scores zurÃ¼cksetzen
                playersCopy = playersCopy.map { player ->
                    player.copy(
                        currentScore = currentGame.startScore,
                        hasDoubleIn = false
                    )
                }
                
                // Rotation fÃ¼r das nÃ¤chste Leg
                currentPlayerIndex = (currentGame.currentLeg) % playersCopy.size
                throwsInRound = 0
            }
        } else {
            // Play round total announcement if enabled
            if (throwsInRound >= 3 && audioAnnounceTurnTotal) {
                var roundScoreSum = score
                var foundThrows = 0
                for (i in currentGame.throwHistory.size - 1 downTo 0) {
                    val throwData = currentGame.throwHistory[i]
                    if (throwData.playerIndex == currentGame.currentPlayerIndex && foundThrows < 2) {
                        roundScoreSum += throwData.score
                        foundThrows++
                    } else if (throwData.playerIndex != currentGame.currentPlayerIndex) {
                        break
                    }
                }
                if (roundScoreSum == 180) {
                    triggerSound("... 180", 2)
                } else if (roundScoreSum == 0) {
                    triggerSound("... No score", 0)
                } else {
                    triggerSound("... $roundScoreSum", 0)
                }
            }

            // Normale Runden-Logik
            if (throwsInRound >= 3) {
                // Finde den nÃ¤chsten nicht-fertigen Spieler
                val nextActivePlayerIndex = findNextActivePlayer(currentPlayerIndex, playersCopy)
                if (nextActivePlayerIndex != -1) {
                    currentPlayerIndex = nextActivePlayerIndex
                    throwsInRound = 0
                } else {
                    // Alle Spieler sind fertig - Spiel beendet
                    isFinished = true
                    println("DEBUG: Alle Spieler sind fertig - Spiel beendet!")
                }
            }
        }
        
        // Neues Game-Objekt zuweisen mit aktualisierter Throw-History
        val updatedThrowHistory = if (legWon && !isFinished) {
            // Neues Leg: Throw-History zurÃ¼cksetzen
            mutableListOf<DartThrow>()
        } else {
            currentGame.throwHistory.toMutableList().apply {
                dartThrow?.let { add(it) }
            }
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


    fun findNextActivePlayer(startIndex: Int, players: List<Player>): Int {
        for (offset in 1..players.size) {
            val idx = (startIndex + offset) % players.size
            if (players[idx].currentScore > 0) {
                return idx
            }
        }
        return -1
    }

    fun areAllPlayersFinished(players: List<Player>): Boolean {
        return players.all { it.currentScore == 0 }
    }

}
