package com.sdart.app.domain.game.engine

import androidx.compose.ui.geometry.Offset
import com.sdart.app.*

object ATCGameEngine {

        fun processThrow(
        score: Int,
        position: androidx.compose.ui.geometry.Offset?,
        multiplier: Int,
        currentGame: Game,
        audioAnnounceEachDart: Boolean,
        triggerSound: (String, Int) -> Unit
    ): Game {
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        val atcConfig = currentGame.atcConfig ?: ATCGameState() // Fallback zu Standard
        
        // PrÃ¼fe ob Treffer relevant ist
        val baseScore = if (multiplier > 0) score / multiplier else score
        val currentTarget = currentPlayer.atcCurrentTarget
        val isTargetHit = baseScore == currentTarget
        
        // Spezieller Check fÃ¼r Bull (25 oder 50)
        val isBullHit = (score == 25 || score == 50) && currentTarget == 25
        
        // Spezieller Check fÃ¼r Shanghai und Quickly
        var advanceBy = 0
        var shanghaiHit: Int? = null
        
        if (isTargetHit || isBullHit) {
            when (atcConfig.variant) {
                ATCVariant.STANDARD -> {
                     // Standard: Beliebiges Segment der Zahl (S, D, T) zÃ¤hlt
                     advanceBy = 1
                }
                ATCVariant.DOUBLE -> {
                     // Nur Doubles zÃ¤hlen (oder Double Bull fÃ¼r Target 25)
                     if (multiplier == 2 || (score == 50 && currentTarget == 25)) {
                         advanceBy = 1
                     }
                }
                ATCVariant.TRIPLE -> {
                     // Nur Triples zÃ¤hlen (Bull hat keine Triples, daher erlauben wir beliebige Bull-Treffer fÃ¼r Target 25)
                     if (multiplier == 3 || (currentTarget == 25 && (score == 25 || score == 50))) {
                         advanceBy = 1
                     }
                }
                ATCVariant.QUICKLY -> {
                    // S=1, D=2, T=3 SprÃ¼nge
                    if (currentTarget == 25) {
                        // Bei Bull zÃ¤hlt Bull simple als +1 (oder Spielende)
                        advanceBy = 1
                    } else {
                        advanceBy = multiplier
                         // Bei Double Bull (50) als Ziel 25 -> Spielende
                        if (score == 50 && currentTarget == 25) advanceBy = 1
                    }
                }
                ATCVariant.SHANGHAI -> {
                    // Muss S, D und T in Reihenfolge (1 -> 2 -> 3) treffen
                    val currentHits = currentPlayer.atcShanghaiHits
                    val nextRequired = when {
                        currentHits.isEmpty() -> 1
                        currentHits.contains(1) && !currentHits.contains(2) -> 2
                        currentHits.contains(1) && currentHits.contains(2) -> 3
                        else -> 1
                    }
                    
                    if (currentTarget == 25) {
                        val hitType = if (score == 25) 1 else 2 // 1=Single Bull, 2=Double Bull
                        if (hitType == nextRequired) {
                            shanghaiHit = hitType
                        }
                    } else {
                        if (multiplier == nextRequired) {
                            shanghaiHit = multiplier
                        }
                    }
                }
            }
        }
        
        // Handling Logic
        var newTarget = currentTarget
        var newShanghaiHits = currentPlayer.atcShanghaiHits
        
        if (atcConfig.variant == ATCVariant.SHANGHAI) {
            shanghaiHit?.let { hit ->
                // FÃ¼ge Hit hinzu
                val updatedHits = newShanghaiHits.toMutableSet().apply { add(hit) }
                newShanghaiHits = updatedHits
                
                // PrÃ¼fe ob weiter
                val neededHits = if (currentTarget == 25) setOf(1, 2) else setOf(1, 2, 3) 
                if (updatedHits.containsAll(neededHits)) {
                    advanceBy = 1
                    newShanghaiHits = emptySet()
                }
            }
        }
        
        // Wenn weitergeschaltet werden soll
        var isFinished = false
        if (advanceBy > 0) {
            if (atcConfig.reverse) {
                if (currentTarget == 25) {
                    newTarget = 20
                } else {
                    newTarget = currentTarget - advanceBy
                    if (newTarget < 1) {
                        isFinished = true
                    }
                }
            } else {
                if (currentTarget == 25) {
                    isFinished = true
                } else {
                    newTarget = currentTarget + advanceBy
                    if (newTarget > 20) {
                        if (atcConfig.withBulls) {
                            newTarget = 25
                        } else {
                            isFinished = true
                        }
                    }
                }
            }
        }
        
        val wasHit = advanceBy > 0 || (atcConfig.variant == ATCVariant.SHANGHAI && shanghaiHit != null)
        triggerSound(if (wasHit) "bing" else "buzzer", 0)

        // DartThrow erstellen (auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        
        // Spieler updaten
        val playersCopy = currentGame.players.mapIndexed { idx, player ->
            if (idx == currentGame.currentPlayerIndex) {
                player.copy(
                    currentScore = 0, // Score ist im ATC irrelevant fÃ¼r Gewinn, nutzen wir fÃ¼r Anzeige?
                    atcCurrentTarget = if (isFinished) 0 else newTarget, // 0 = Finished
                    atcShanghaiHits = newShanghaiHits,
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
        }.toMutableList()
        
        // PrÃ¼fen ob finished
        if (isFinished) {
             println("DEBUG ATC: Player finished!")
        }
        
        // Runden-Logik (Standard 3 WÃ¼rfe)
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        var gameFinished = isFinished // Wenn aktueller Spieler fertig ist, ist das Spiel vorbei? 
        // Standard: Wer zuerst fertig ist gewinnt Leg.
        // Aber Runde zu Ende spielen?
        // User Docs: "Ein Leg oder das Spiel endet erst, wenn der aktuelle Spieler seine 3 Darts geworfen hat."
        
        if (isFinished) {
            // Markiere Spieler als fertig (Target 0 oder Ã¤hnliches Flag wÃ¤re gut, hier nutzen wir isFinished im Game status spÃ¤ter)
            // PrÃ¼fe am Ende der Runde wer gewonnen hat.
            // ABER: "Wer zuerst das Ziel erreicht, gewinnt das Leg."
            // UND: "Ein Leg oder das Spiel endet erst, wenn der aktuelle Spieler seine 3 Darts geworfen hat."
            // Also: Spiel lÃ¤uft weiter bis throwsInRound == 3.
            gameFinished = false 
        }

        // Spielerwechsel nach 3 WÃ¼rfen
        if (throwsInRound >= 3) {
            // Check win condition
            // Hat jemand target erreicht (Target = 0 oder Target > max)?
            // Im ATC: target 0 oder so?
            // Wir schauen auf isFinished Flag vom Schritt oben.
            // Wir mÃ¼ssen speichern dass er fertig ist. `atcCurrentTarget` setzen wir auf 0/Finished?
            
            // Check if ANY player is finished
            val anyFinished = playersCopy.any { 
                   // Logic for finished player: 
                   // Reverse: target < 1 (or 0)
                   // Forward: target > 20 (or > 25)
                   // We set it to 0 if finished above.
                   // Wait, initialization is 1. 0 is a good "finished" marker? 
                   // Let's assume 0 means "Done".
                   it.atcCurrentTarget == 0
            }
            
            if (anyFinished) {
                gameFinished = true
                val winner = playersCopy.first { it.atcCurrentTarget == 0 }
                triggerSound("Game Shot and the Win", 0)
            } else {
                 currentPlayerIndex = (currentPlayerIndex + 1) % playersCopy.size
                 throwsInRound = 0
            }
        }
        
        // Update Game
        val updatedThrowHistory = currentGame.throwHistory.toMutableList().apply {
            dartThrow?.let { add(it) }
        }
        
        return Game(
            players = playersCopy,
            mode = currentGame.mode,
            currentPlayerIndex = currentPlayerIndex,
            isFinished = gameFinished,
            throwsInRound = throwsInRound,
            throwHistory = updatedThrowHistory,
            doubleIn = currentGame.doubleIn,
            doubleOut = currentGame.doubleOut,
            targetScore = currentGame.targetScore,
            totalLegs = currentGame.totalLegs,
            currentLeg = currentGame.currentLeg,
            randomizeOrder = currentGame.randomizeOrder,
            atcConfig = currentGame.atcConfig,
            startScore = currentGame.startScore
        )
    }

    // ML-optimierte Punkteeingabe mit Konfidenz


}
