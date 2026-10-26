package com.sdart.app.connect

import androidx.compose.ui.geometry.Offset
import com.sdart.app.DartThrow
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.domain.CHECKOUT_PATHS
import com.sdart.app.domain.game.engine.GameRoundHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Snapshot eines einzelnen Darts in der aktuellen Runde.
 */
@Serializable
data class DartSnapshot(
    val score: Int,
    val multiplier: Int,
    val label: String,
    val x: Float? = null,
    val y: Float? = null,
    val isBust: Boolean = false
)

/**
 * Snapshot eines Spielers für die Übersicht auf Zweitgeräten.
 */
@Serializable
data class PlayerSnapshot(
    val index: Int,
    val name: String,
    val score: Int,
    val legsWon: Int = 0,
    val isCurrent: Boolean = false
)

/**
 * Vollständiger Snapshot des laufenden Spielstands für Companion-Geräte (Rolle INPUT und DISPLAY).
 */
@Serializable
data class GameSnapshot(
    val isGameActive: Boolean = false,
    val gameMode: String = "",
    val currentPlayerIndex: Int = 0,
    val currentPlayerName: String = "",
    val currentPlayerScore: Int = 0,
    val throwsInRound: Int = 0,
    val currentRoundDarts: List<DartSnapshot> = emptyList(),
    val currentRoundTotal: Int = 0,
    val checkoutSuggestion: String? = null,
    val isBust: Boolean = false,
    val isFinished: Boolean = false,
    val winnerName: String? = null,
    val highlightedSegments: Set<Int> = emptySet(),
    val currentLeg: Int = 1,
    val totalLegs: Int = 1,
    val players: List<PlayerSnapshot> = emptyList()
)

object GameSnapshotProtocol {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(snapshot: GameSnapshot): String = json.encodeToString(GameSnapshot.serializer(), snapshot)

    fun decode(text: String): GameSnapshot? = runCatching {
        json.decodeFromString(GameSnapshot.serializer(), text)
    }.getOrNull()
}

/**
 * Hilfsfunktion zur Erstellung eines formatierten Text-Labels für einen Dartwurf.
 */
fun formatDartThrowLabel(score: Int, multiplier: Int): String {
    val actualMultiplier = if (score == 50) 2 else multiplier
    return when {
        score == 50 -> "Bull (50)"
        score == 25 -> "Bull (25)"
        score == 0 -> "Miss (0)"
        actualMultiplier == 3 -> {
            val base = if (score % 3 == 0) score / 3 else score
            "T$base ($score)"
        }
        actualMultiplier == 2 -> {
            val base = if (score % 2 == 0) score / 2 else score
            "D$base ($score)"
        }
        else -> "S$score ($score)"
    }
}

/**
 * Konvertiert ein [Game]-Objekt in einen serialisierbaren [GameSnapshot].
 */
fun Game?.toSnapshot(): GameSnapshot {
    if (this == null) {
        return GameSnapshot(isGameActive = false)
    }

    val currentPlayer = players.getOrNull(currentPlayerIndex)
    val playerName = currentPlayer?.name ?: ""
    val playerScore = currentPlayer?.currentScore ?: 0

    // Modus-Bezeichnung
    val modeText = when (mode) {
        GameMode.X301 -> if (doubleOut) "301 Double Out" else "301"
        GameMode.X501 -> if (doubleOut) "501 Double Out" else "501"
        GameMode.X701 -> if (doubleOut) "701 Double Out" else "701"
        GameMode.CRICKET -> "Cricket"
        GameMode.ALL_FIVES -> "All Fives"
        GameMode.AROUND_THE_CLOCK -> "Around the Clock"
        GameMode.CUSTOM -> "Custom"
    }

    // Aktuelle Würfe der laufenden Aufnahme ermitteln
    val currentTurnThrows = mutableListOf<DartThrow>()
    if (throwsInRound in 1..3 && throwHistory.isNotEmpty()) {
        var count = 0
        for (i in throwHistory.size - 1 downTo 0) {
            val t = throwHistory[i]
            if (t.playerIndex == currentPlayerIndex && count < throwsInRound) {
                currentTurnThrows.add(0, t)
                count++
            } else {
                break
            }
        }
    }

    val currentRoundDarts = currentTurnThrows.map { t ->
        val pos = t.position
        val hasValidPos = pos != Offset.Unspecified && (pos.x != 0f || pos.y != 0f)
        DartSnapshot(
            score = t.score,
            multiplier = t.multiplier,
            label = formatDartThrowLabel(t.score, t.multiplier),
            x = if (hasValidPos) pos.x else null,
            y = if (hasValidPos) pos.y else null,
            isBust = t.isBust
        )
    }

    val roundTotal = GameRoundHelper.getCurrentRoundSum(this)
    val hasBust = currentTurnThrows.any { it.isBust }

    // Checkout-Empfehlung (bei X01 Spielen)
    val checkout = if (mode in setOf(GameMode.X301, GameMode.X501, GameMode.X701, GameMode.CUSTOM) && !isFinished) {
        when {
            playerScore in CHECKOUT_PATHS.keys -> CHECKOUT_PATHS[playerScore]?.firstOrNull()
            playerScore == 50 -> "Bull"
            playerScore in 2..40 && playerScore % 2 == 0 -> "D${playerScore / 2}"
            else -> null
        }
    } else null

    // Hervorzuhebende Segmente
    val highlighted = when (mode) {
        GameMode.CRICKET -> currentPlayer?.cricketMarks?.filterValues { it < 3 }?.keys?.toSet() ?: emptySet()
        GameMode.AROUND_THE_CLOCK -> currentPlayer?.atcCurrentTarget?.let { setOf(it) } ?: emptySet()
        GameMode.X301, GameMode.X501, GameMode.X701 -> {
            when {
                playerScore == 50 -> setOf(25)
                playerScore in 2..40 && playerScore % 2 == 0 -> setOf(playerScore / 2)
                else -> emptySet()
            }
        }
        else -> emptySet()
    }

    val playerSnapshots = players.mapIndexed { idx, p ->
        PlayerSnapshot(
            index = idx,
            name = p.name,
            score = p.currentScore,
            legsWon = p.wonLegs,
            isCurrent = idx == currentPlayerIndex
        )
    }

    val winner = if (isFinished) {
        currentPlayer?.name
    } else null

    return GameSnapshot(
        isGameActive = true,
        gameMode = modeText,
        currentPlayerIndex = currentPlayerIndex,
        currentPlayerName = playerName,
        currentPlayerScore = playerScore,
        throwsInRound = throwsInRound,
        currentRoundDarts = currentRoundDarts,
        currentRoundTotal = roundTotal,
        checkoutSuggestion = checkout,
        isBust = hasBust,
        isFinished = isFinished,
        winnerName = winner,
        highlightedSegments = highlighted,
        currentLeg = currentLeg,
        totalLegs = totalLegs,
        players = playerSnapshots
    )
}
