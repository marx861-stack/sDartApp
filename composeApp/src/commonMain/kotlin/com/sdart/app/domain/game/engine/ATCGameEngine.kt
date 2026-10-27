package com.sdart.app.domain.game.engine

import androidx.compose.ui.geometry.Offset
import com.sdart.app.*

/**
 * Fortschritt eines Spielers im ATC: aktuelles Ziel (0 = fertig) und Shanghai-Treffer am aktuellen Ziel.
 */
data class ATCProgress(
    val target: Int,
    val shanghaiHits: Set<Int> = emptySet()
)

object ATCGameEngine {

    /** Erstes Ziel je nach Richtung: vorwärts die 1, rückwärts die 20 bzw. das Bull. */
    fun startTarget(config: ATCGameState): Int =
        if (config.reverse) {
            if (config.withBulls) 25 else 20
        } else 1

    /**
     * Wendet einen Wurf auf den Fortschritt an. Reine Funktion, damit das Undo die Würfe
     * eines Spielers von vorn nachspielen kann.
     *
     * @return neuer Fortschritt und ob der Wurf gezählt hat (für den Treffer-Sound)
     */
    fun advance(progress: ATCProgress, score: Int, multiplier: Int, config: ATCGameState): Pair<ATCProgress, Boolean> {
        val currentTarget = progress.target
        // Fertige Spieler kommen nicht weiter
        if (currentTarget == 0) return progress to false

        // Prüfe ob Treffer relevant ist
        val baseScore = if (multiplier > 0) score / multiplier else score
        val isTargetHit = baseScore == currentTarget

        // Spezieller Check für Bull (25 oder 50)
        val isBullHit = (score == 25 || score == 50) && currentTarget == 25

        // Spezieller Check für Shanghai und Quickly
        var advanceBy = 0
        var shanghaiHit: Int? = null

        if (isTargetHit || isBullHit) {
            when (config.variant) {
                ATCVariant.STANDARD -> {
                    // Standard: Beliebiges Segment der Zahl (S, D, T) zählt
                    advanceBy = 1
                }
                ATCVariant.DOUBLE -> {
                    // Nur Doubles zählen (oder Double Bull für Target 25)
                    if (multiplier == 2 || (score == 50 && currentTarget == 25)) {
                        advanceBy = 1
                    }
                }
                ATCVariant.TRIPLE -> {
                    // Nur Triples zählen (Bull hat keine Triples, daher erlauben wir beliebige Bull-Treffer für Target 25)
                    if (multiplier == 3 || (currentTarget == 25 && (score == 25 || score == 50))) {
                        advanceBy = 1
                    }
                }
                ATCVariant.QUICKLY -> {
                    // S=1, D=2, T=3 Sprünge; Bull zählt immer als +1
                    advanceBy = if (currentTarget == 25) 1 else multiplier
                }
                ATCVariant.SHANGHAI -> {
                    // Muss S, D und T in Reihenfolge (1 -> 2 -> 3) treffen
                    val currentHits = progress.shanghaiHits
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

        var newTarget = currentTarget
        var newShanghaiHits = progress.shanghaiHits

        if (config.variant == ATCVariant.SHANGHAI) {
            shanghaiHit?.let { hit ->
                val updatedHits = newShanghaiHits + hit
                newShanghaiHits = updatedHits

                // Weiter, sobald alle nötigen Treffer gesammelt sind
                val neededHits = if (currentTarget == 25) setOf(1, 2) else setOf(1, 2, 3)
                if (updatedHits.containsAll(neededHits)) {
                    advanceBy = 1
                    newShanghaiHits = emptySet()
                }
            }
        }

        // Wenn weitergeschaltet werden soll
        if (advanceBy > 0) {
            if (config.reverse) {
                newTarget = if (currentTarget == 25) 20 else currentTarget - advanceBy
                if (newTarget < 1) newTarget = 0
            } else if (currentTarget == 25) {
                newTarget = 0
            } else {
                newTarget = currentTarget + advanceBy
                if (newTarget > 20) {
                    newTarget = if (config.withBulls) 25 else 0
                }
            }
        }

        val wasHit = advanceBy > 0 || shanghaiHit != null
        return ATCProgress(newTarget, newShanghaiHits) to wasHit
    }

    fun processThrow(
        score: Int,
        position: Offset?,
        multiplier: Int,
        currentGame: Game,
        audioAnnounceEachDart: Boolean,
        triggerSound: (String, Int) -> Unit
    ): Game {
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        val atcConfig = currentGame.atcConfig ?: ATCGameState() // Fallback zu Standard

        val (progress, wasHit) = advance(
            ATCProgress(currentPlayer.atcCurrentTarget, currentPlayer.atcShanghaiHits),
            score,
            multiplier,
            atcConfig
        )
        triggerSound(if (wasHit) "bing" else "buzzer", 0)

        // DartThrow erstellen (auch bei position == null für vollständige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )

        // Spieler updaten
        val playersCopy = currentGame.players.mapIndexed { idx, player ->
            if (idx == currentGame.currentPlayerIndex) {
                player.copy(
                    currentScore = 0, // Score ist im ATC irrelevant
                    atcCurrentTarget = progress.target, // 0 = fertig
                    atcShanghaiHits = progress.shanghaiHits,
                    scores = player.scores.toMutableList().apply { add(score) },
                    dartThrows = player.dartThrows.toMutableList().apply { add(dartThrow) }
                )
            } else {
                player.copy(
                    scores = player.scores.toMutableList(),
                    dartThrows = player.dartThrows.toMutableList()
                )
            }
        }.toMutableList()

        // Wer zuerst das letzte Ziel trifft, gewinnt sofort; sonst Spielerwechsel nach 3 Würfen
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        val gameFinished = progress.target == 0

        if (gameFinished) {
            triggerSound("Game Shot and the Win", 0)
        } else if (throwsInRound >= 3) {
            currentPlayerIndex = (currentPlayerIndex + 1) % playersCopy.size
            throwsInRound = 0
        }

        return Game(
            players = playersCopy,
            mode = currentGame.mode,
            currentPlayerIndex = currentPlayerIndex,
            isFinished = gameFinished,
            throwsInRound = throwsInRound,
            throwHistory = currentGame.throwHistory.toMutableList().apply { add(dartThrow) },
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
}
