package com.sdart.app

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable

data class DartThrow(
    val score: Int,
    val position: Offset,
    val playerIndex: Int,
    val multiplier: Int = 1,
    val timestamp: Long = currentTimeMillis(),
    val isBust: Boolean = false
)

data class Player(
    val name: String,
    var currentScore: Int,
    val scores: MutableList<Int> = mutableListOf(),
    val dartThrows: MutableList<DartThrow> = mutableListOf(),
    var hasDoubleIn: Boolean = false,
    // Cricket-Modus: Segment-Marken (15, 16, 17, 18, 19, 20, Bull)
    val cricketMarks: MutableMap<Int, Int> = mutableMapOf(
        15 to 0, 16 to 0, 17 to 0, 18 to 0, 19 to 0, 20 to 0, 25 to 0 // 25 = Bull
    ),
    // All Fives Modus: Runden und Legs
    var totalRounds: Int = 0,
    var wonLegs: Int = 0,
    // ATC Modus
    var atcCurrentTarget: Int = 1,
    var atcShanghaiHits: Set<Int> = emptySet()
) {
    init {
        require(name.isNotBlank()) { "Player name cannot be blank" }
        require(currentScore >= 0) { "Current score cannot be negative" }
        
        if (scores.isEmpty()) {
            scores.clear()
        }
    }
    
    // Cricket-Hilfsfunktionen
    fun isSegmentClosed(segment: Int): Boolean {
        return cricketMarks.getOrDefault(segment, 0) >= 3
    }
    
    fun hasAllSegmentsClosed(): Boolean {
        return cricketMarks.all { (_, marks) -> marks >= 3 }
    }
}

enum class GameMode(val startScore: Int) {
    X301(301), X501(501), X701(701), CUSTOM(0), CRICKET(0), ALL_FIVES(0), AROUND_THE_CLOCK(0)
}

/**
 * Hilfsklasse zum Speichern von Offset-Koordinaten ohne Gson-Probleme
 */
@Serializable
data class SavedPosition(
    val x: Float,
    val y: Float
)

/**
 * Speichert den Verlauf eines einzelnen beendeten Spiels.
 */
@Serializable
data class GameHistoryRecord(
    val timestamp: Long,
    val averageScore: Float,
    val dartsPerLeg: Float
)

/**
 * Gespeicherter Spieler mit kumulativen Statistiken
 */
@Serializable
data class SavedPlayer(
    val name: String,
    val totalGames: Int = 0,
    val gamesWon: Int = 0,
    val totalWonLegs: Int = 0,
    val totalRounds: Int = 0,
    val totalPoints: Int = 0,
    val totalDartsThrown: Int = 0,
    val highestRoundScore: Int = 0,
    val bestAverageScorePerRound: Int = 0,
    // Erweiterte Statistiken
    val highScores180s: Int = 0,
    val highScores140s: Int = 0,
    val highScores100s: Int = 0,
    val bestCheckout: Int = 0,
    val bestMatchDarts: Int = 0,
    val totalBusts: Int = 0,
    val checkoutAttempts: Int = 0,
    val checkoutHits: Int = 0,
    val first9Points: Int = 0,
    val first9Darts: Int = 0,
    // Maps
    val segmentHits: Map<String, Int> = emptyMap(),
    val doubleHits: Map<Int, Int> = emptyMap(),
    // Koordinaten
    val savedDartPositions: List<SavedPosition> = emptyList(),
    // Verlauf
    val gameHistory: List<GameHistoryRecord> = emptyList()
)

enum class ATCVariant {
    STANDARD,
    DOUBLE,
    TRIPLE,
    QUICKLY,
    SHANGHAI
}

data class ATCGameState(
    val variant: ATCVariant = ATCVariant.STANDARD,
    val reverse: Boolean = false,
    val withBulls: Boolean = false
)

enum class CricketVariant {
    STANDARD,
    CUT_THROAT
}

data class CricketConfig(
    val variant: CricketVariant = CricketVariant.STANDARD
)

data class Game(
    val players: List<Player>,
    val mode: GameMode,
    var currentPlayerIndex: Int = 0,
    var isFinished: Boolean = false,
    var throwsInRound: Int = 0,
    val throwHistory: MutableList<DartThrow> = mutableListOf(),
    val doubleIn: Boolean = false,
    val doubleOut: Boolean = true,
    // All Fives spezifische Felder
    val targetScore: Int = 0,
    val totalLegs: Int = 1,
    val currentLeg: Int = 1,

    val randomizeOrder: Boolean = false,
    // ATC Modus Konfiguration
    val atcConfig: ATCGameState? = null,
    // Cricket Modus Konfiguration
    val cricketConfig: CricketConfig? = null,
    val startScore: Int = 0
) {
    init {
        try {
            println("=== INITIALIZING GAME ===")
            
            require(players.isNotEmpty()) { "Game must have at least one player" }
            require(throwsInRound in 0..3) { "Throws in round must be between 0 and 3" }
            
            currentPlayerIndex = currentPlayerIndex.coerceIn(0, players.size - 1)
            
            players.forEachIndexed { index, player ->
                require(player.name.isNotBlank()) { "Player $index name cannot be blank" }
                require(player.currentScore >= 0) { "Player $index score cannot be negative" }
                println("Player $index: ${player.name} with score ${player.currentScore}")
            }
            
            println("Game initialized successfully:")
            println("- Players: ${players.size}")
            println("- Mode: $mode")
            println("- Current player index: $currentPlayerIndex")
            println("- Throws in round: $throwsInRound")
            
        } catch (e: Exception) {
            println("=== CRITICAL ERROR INITIALIZING GAME ===")
            println("Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
