package com.sdart.app.domain.game.engine

import androidx.compose.ui.geometry.Offset
import com.sdart.app.*

object CricketGameEngine {

        fun processThrow(
        score: Int,
        position: androidx.compose.ui.geometry.Offset?,
        multiplier: Int,
        currentGame: Game,
        audioAnnounceEachDart: Boolean,
        triggerSound: (String, Int) -> Unit
    ): Game {
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        
        // Bestimme welches Segment getroffen wurde
        val baseScore = if (multiplier > 0) score / multiplier else score
        val cricketSegment: Int? = when {
            baseScore == 25 -> 25 // Bull (25 = Single Bull, 50 = Double Bull)
            baseScore in 15..20 -> baseScore // GÃ¼ltige Cricket-Segmente
            else -> null // Nicht gÃ¼ltig fÃ¼r Cricket
        }
        
        // DartThrow-Objekt erstellen (IMMER fÃ¼r alle Segmente, auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        println("DEBUG Cricket: DartThrow erstellt - Score: $score, Multiplier: $multiplier, Position: ${position ?: "Unspecified"}, PlayerIndex: ${currentGame.currentPlayerIndex}")
        
        if (position == null) {
            println("DEBUG Cricket: WARNUNG - Keine Position fÃ¼r DartThrow! Der Wurf wird nicht auf dem Dartboard angezeigt.")
        }
        
        // Deep copy der Spieler mit aktualisierten Statistiken
        val playersCopy = currentGame.players.toMutableList()
        val player = playersCopy[currentGame.currentPlayerIndex]
        
        // Basis-Update: Wurf und Score immer zur Statistik hinzufÃ¼gen
        var updatedPlayer = player.copy(
            scores = player.scores.toMutableList().apply { add(score) },
            dartThrows = player.dartThrows.toMutableList().apply { 
                dartThrow?.let { add(it) }
            }
        )
        playersCopy[currentGame.currentPlayerIndex] = updatedPlayer
        
        var isFinished = false
        var currentLeg = currentGame.currentLeg
        var legWon = false
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        var winners = emptyList<Player>()
        var pointsScoredThisDart = 0
        var announceAction: String? = null

        // Wenn Segment gÃ¼ltig fÃ¼r Cricket, Marken und Punkte berechnen
        if (cricketSegment != null) {
            // Marken hinzufÃ¼gen (Single=1, Double=2, Triple=3)
            // WICHTIG: FÃ¼r Bull: 25 = Single Bull (1 Mark), 50 = Double Bull (2 Marks)
            val marksToAdd = when {
                cricketSegment == 25 && score == 50 -> 2 // Double Bull = 2 Marks
                cricketSegment == 25 && score == 25 -> 1 // Single Bull = 1 Mark
                multiplier == 3 -> 3 // Triple = 3 Marken
                multiplier == 2 -> 2 // Double = 2 Marken
                else -> 1 // Single = 1 Marke
            }
            
            val currentMarks = updatedPlayer.cricketMarks.getOrDefault(cricketSegment, 0)
            val newMarks = currentMarks + marksToAdd // KEINE Begrenzung auf 3 - Marks Ã¼ber 3 hinaus zÃ¤hlen als Scoring-Marks
            
            // Update Marks fÃ¼r aktuellen Spieler
            val updatedMarks = updatedPlayer.cricketMarks.toMutableMap()
            updatedMarks[cricketSegment] = newMarks
            updatedPlayer = updatedPlayer.copy(cricketMarks = updatedMarks)
            playersCopy[currentGame.currentPlayerIndex] = updatedPlayer

            // Punkteberechnung
            val wasAlreadyClosed = currentMarks >= 3
            val isNowClosed = newMarks >= 3
            
            if (isNowClosed && !wasAlreadyClosed) {
                val allPlayersHaveItClosedNow = playersCopy.all { it.isSegmentClosed(cricketSegment) }
                announceAction = if (allPlayersHaveItClosedNow) "closed" else "open"
            }
            
            if (isNowClosed) {
                 val variant = currentGame.cricketConfig?.variant ?: CricketVariant.STANDARD
                 
                 // Berechne Scoring-Marks (Marks Ã¼ber 3 hinaus)
                 val scoringMarks = if (wasAlreadyClosed) {
                     marksToAdd
                 } else {
                     newMarks - 3
                 }
                 
                 if (scoringMarks > 0) {
                     val pointsPerMark = if (cricketSegment == 25) 25 else cricketSegment
                     val pointsTotal = scoringMarks * pointsPerMark
                     
                     if (variant == CricketVariant.STANDARD) {
                         // STANDARD: Punkte fÃ¼r mich, wenn andere noch offen haben
                          val hasOpponentWithOpenSegment = playersCopy.any { opponent ->
                            opponent.name != updatedPlayer.name && !opponent.isSegmentClosed(cricketSegment)
                        }
                        
                        if (hasOpponentWithOpenSegment) {
                            updatedPlayer = updatedPlayer.copy(currentScore = updatedPlayer.currentScore + pointsTotal)
                            playersCopy[currentGame.currentPlayerIndex] = updatedPlayer
                            pointsScoredThisDart = pointsTotal
                            println("DEBUG Cricket: Punkte fÃ¼r ${updatedPlayer.name}: +$pointsTotal")
                        }
                     } else {
                         // CUT THROAT: Punkte fÃ¼r Gegner, die das Segment noch offen haben
                         var pointsAwarded = false
                         playersCopy.forEachIndexed { paramsIndex, opponent ->
                             if (paramsIndex != currentGame.currentPlayerIndex && !opponent.isSegmentClosed(cricketSegment)) {
                                 // Gegner hat Segment noch offen -> bekommt Punkte (Strafe)
                                 val newOpponentScore = opponent.currentScore + pointsTotal
                                 playersCopy[paramsIndex] = opponent.copy(currentScore = newOpponentScore)
                                 pointsAwarded = true
                                 println("DEBUG Cricket Cut Throat: Punkte fÃ¼r Gegner ${opponent.name}: +$pointsTotal")
                             }
                         }
                         if (pointsAwarded) {
                             pointsScoredThisDart = pointsTotal
                         }
                     }
                 }
            }
        } else {
            println("DEBUG: Cricket - Segment $score ist nicht gÃ¼ltig fÃ¼r Cricket-Punkte (nur 15-20 und Bull)")
        }
        
        // Spielende prÃ¼fen: Hat der aktuelle Spieler alle Segmente geschlossen?
        val currentPlayerUpdated = playersCopy[currentGame.currentPlayerIndex]
        val currentPlayerFinished = currentPlayerUpdated.hasAllSegmentsClosed()
        
        // PrÃ¼fe ob alle Spieler fertig sind (alle Segmente geschlossen)
        val allPlayersFinished = playersCopy.all { it.hasAllSegmentsClosed() }
        
        if (allPlayersFinished) {
            // Alle Spieler haben alle Segmente geschlossen
            
             val variant = currentGame.cricketConfig?.variant ?: CricketVariant.STANDARD
             
            // Gewinnerbestimmung je nach Variante
            winners = if (variant == CricketVariant.CUT_THROAT) {
                // Cut Throat: Gewinner ist wer die NIEDRIGSTE Punktzahl hat
                val minScore = playersCopy.minOfOrNull { it.currentScore } ?: 0
                playersCopy.filter { it.currentScore == minScore }
            } else {
                // Standard: Gewinner ist wer die HÃ–CHSTE Punktzahl hat
                val maxScore = playersCopy.maxOfOrNull { it.currentScore } ?: 0
                playersCopy.filter { it.currentScore == maxScore }
            }
            
            if (winners.isNotEmpty()) {
                // Leg gewonnen! (kann mehrere Gewinner bei Unentschieden geben)
                legWon = true
                
                // ErhÃ¶he wonLegs fÃ¼r alle Gewinner
                winners.forEach { winner ->
                    val winnerIndex = playersCopy.indexOfFirst { it.name == winner.name }
                    if (winnerIndex != -1) {
                        val winnerPlayer = playersCopy[winnerIndex]
                        val newWonLegs = winnerPlayer.wonLegs + 1
                        playersCopy[winnerIndex] = winnerPlayer.copy(wonLegs = newWonLegs)
                        println("DEBUG Cricket: Spieler ${winnerPlayer.name} hat Leg ${currentGame.currentLeg} gewonnen (Legs: $newWonLegs/${currentGame.totalLegs})")
                    }
                }
                
                // PrÃ¼fe ob ein Spieler genug Legs gewonnen hat (totalLegs / 2 + 1)
                val requiredLegs = if (currentGame.players.size == 1) currentGame.totalLegs else (currentGame.totalLegs / 2) + 1
                val hasGameWinner = playersCopy.any { it.wonLegs >= requiredLegs }
                
                if (hasGameWinner) {
                    // Spiel gewonnen!
                    isFinished = true
                    val gameWinner = playersCopy.first { it.wonLegs >= requiredLegs }
                    println("DEBUG Cricket: Spiel beendet! Gewinner: ${gameWinner.name} mit ${gameWinner.wonLegs} Legs")
                } else {
                    // Neues Leg starten
                    currentLeg++
                    println("DEBUG Cricket: Neues Leg $currentLeg gestartet")
                    
                    // Setze Marks und Punkte zurÃ¼ck, aber behalte wonLegs
                    playersCopy.forEachIndexed { idx, p ->
                        val resetMarks = mutableMapOf<Int, Int>(
                            15 to 0, 16 to 0, 17 to 0, 18 to 0, 19 to 0, 20 to 0, 25 to 0
                        )
                        playersCopy[idx] = p.copy(
                            currentScore = 0,
                            cricketMarks = resetMarks
                        )
                    }
                    
                    // Gewinner des vorherigen Legs startet das nÃ¤chste Leg
                    val winnerIndex = playersCopy.indexOfFirst { it.name == winners[0].name }
                    if (winnerIndex != -1) {
                        currentPlayerIndex = winnerIndex
                    }
                    throwsInRound = 0
                }
            }
        } else if (currentPlayerFinished) {
            // Aktueller Spieler hat alle Segmente geschlossen, aber andere noch nicht
            // (Logik zur Information, keine Ã„nderung am Status nÃ¶tig)
        }
        
        // 1. Single Dart Sound
        if (audioAnnounceEachDart && !legWon) {
            val voiceKey = when {
                score == 50 -> "Bullseye"
                score == 25 -> "Twenty Five"
                multiplier == 3 -> "Triple ${score / multiplier}"
                multiplier == 2 -> "Double ${score / multiplier}"
                score == 0 -> "Miss"
                else -> score.toString()
            }
            
            var announcement = voiceKey
            
            if (announceAction != null) {
                announcement += " ... $announceAction"
            }
            
            if (pointsScoredThisDart > 0) {
                announcement += " ... Score"
            }
            
            triggerSound(announcement, 0)
        }

        // 2. Leg / Match Win
        if (legWon) {
            if (isFinished) {
                triggerSound("Game Shot and the Win", 0)
            } else {
                triggerSound("Game Shot", 0)
            }
        }

        if (!legWon) { // Nur fortfahren wenn kein neues Leg gestartet wurde
            if (currentPlayerFinished && !allPlayersFinished) {
                // Aktueller Spieler ist fertig, aber es gibt noch andere
                println("DEBUG Cricket: Spieler ${currentPlayerIndex + 1} (${playersCopy[currentPlayerIndex].name}) hat alle Segmente geschlossen, aber andere Spieler spielen noch weiter")
                val nextActivePlayerIndex = findNextActivePlayerCricket(currentPlayerIndex, playersCopy)
                if (nextActivePlayerIndex != -1) {
                    currentPlayerIndex = nextActivePlayerIndex
                    throwsInRound = 0
                    println("DEBUG Cricket: NÃ¤chster aktiver Spieler: ${playersCopy[nextActivePlayerIndex].name} (Index: $nextActivePlayerIndex)")
                }
            } else if (!isFinished && throwsInRound >= 3) {
                // Normale Runden-Logik
                val nextActivePlayerIndex = findNextActivePlayerCricket(currentPlayerIndex, playersCopy)
                if (nextActivePlayerIndex != -1) {
                    currentPlayerIndex = nextActivePlayerIndex
                    throwsInRound = 0
                }
            }
        }
        
        // Neues Game-Objekt zuweisen
        val updatedThrowHistory = if (legWon && !isFinished) {
            // Neues Leg: Throw-History zurÃ¼cksetzen
            mutableListOf<DartThrow>()
        } else {
            currentGame.throwHistory.toMutableList().apply {
                dartThrow?.let { 
                    add(it)
                    println("DEBUG Cricket: DartThrow zur throwHistory hinzugefÃ¼gt. Neue GrÃ¶ÃŸe: ${size}")
                }
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
        
        if (isFinished) {
            println("DEBUG: Cricket - Spiel beendet! Alle Spieler haben alle Segmente geschlossen!")
        } else if (legWon) {
            println("DEBUG: Cricket - Leg $currentLeg gestartet (vorheriges Leg wurde gewonnen)")
        } else if (currentPlayerUpdated.hasAllSegmentsClosed()) {
            println("DEBUG: Cricket - Spieler ${currentPlayerUpdated.name} hat alle Segmente geschlossen, andere Spieler spielen weiter")
        }
    }
    



    fun undoThrow(
        currentGame: Game,
        lastThrow: DartThrow,
        targetPlayer: Player,
        playersCopy: MutableList<Player>,
        playerIndex: Int
    ) {
        val baseScore = if (lastThrow.multiplier > 0) lastThrow.score / lastThrow.multiplier else lastThrow.score
        val segment = when {
            baseScore == 25 -> 25
            baseScore in 15..20 -> baseScore
            else -> null
        }
        
        if (segment != null) {
            val marksToRemove = when {
                segment == 25 && lastThrow.score == 50 -> 2
                segment == 25 && lastThrow.score == 25 -> 1
                else -> lastThrow.multiplier
            }
            
            val currentMarks = targetPlayer.cricketMarks.getOrDefault(segment, 0)
            val oldMarks = maxOf(0, currentMarks - marksToRemove)
            
            val scoringMarksCalculated = maxOf(0, currentMarks - maxOf(3, oldMarks))
            
            if (scoringMarksCalculated > 0) {
                val pointsPerMark = if (segment == 25) 25 else segment
                val pointsToUndo = scoringMarksCalculated * pointsPerMark
                val variant = currentGame.cricketConfig?.variant ?: CricketVariant.STANDARD
                
                if (variant == CricketVariant.STANDARD) {
                    targetPlayer.currentScore -= pointsToUndo
                } else {
                    playersCopy.forEachIndexed { idx, player ->
                        if (idx != playerIndex && !player.isSegmentClosed(segment)) {
                            player.currentScore -= pointsToUndo
                        }
                    }
                }
            }
            targetPlayer.cricketMarks[segment] = oldMarks
        }
    }

    fun findNextActivePlayerCricket(startIndex: Int, players: List<Player>): Int {
        for (offset in 1..players.size) {
            val idx = (startIndex + offset) % players.size
            if (!players[idx].hasAllSegmentsClosed()) {
                return idx
            }
        }
        return -1
    }

}
