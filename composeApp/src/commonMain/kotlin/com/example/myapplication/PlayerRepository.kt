package com.example.myapplication

/**
 * Repository-Interface für die Verwaltung gespeicherter Spieler und Statistiken.
 */
interface PlayerRepository {
    suspend fun loadAllPlayers(): List<SavedPlayer>
    suspend fun savePlayer(player: SavedPlayer): Boolean
    suspend fun deletePlayer(name: String): Boolean
    suspend fun findPlayer(name: String): SavedPlayer?
    suspend fun updatePlayerStatistics(playerName: String, gameStats: PlayerStats): Boolean
}

/**
 * Erzeugt die plattformspezifische persistente Implementierung von PlayerRepository.
 */
expect fun createPersistentPlayerRepository(): PlayerRepository

/**
 * Basis-Implementierung der Update-Statistik-Logik (unabhängig vom Speicher-Backend).
 */
abstract class BasePlayerRepository : PlayerRepository {
    override suspend fun updatePlayerStatistics(playerName: String, gameStats: PlayerStats): Boolean {
        val existingPlayer = findPlayer(playerName) ?: return false

        val mergedSegmentHits = existingPlayer.segmentHits.toMutableMap()
        gameStats.segmentHits.forEach { (key, value) ->
            mergedSegmentHits[key] = mergedSegmentHits.getOrDefault(key, 0) + value
        }

        val mergedDoubleHits = existingPlayer.doubleHits.toMutableMap()
        gameStats.doubleHits.forEach { (key, value) ->
            mergedDoubleHits[key] = mergedDoubleHits.getOrDefault(key, 0) + value
        }

        val mergedDartPositions = existingPlayer.savedDartPositions + gameStats.dartPositions

        val newRecord = GameHistoryRecord(
            timestamp = currentTimeMillis(),
            averageScore = gameStats.lastGameAverage,
            dartsPerLeg = gameStats.lastGameDartsPerLeg
        )
        val mergedHistory = (existingPlayer.gameHistory + newRecord).takeLast(20)

        val updatedPlayer = SavedPlayer(
            name = existingPlayer.name,
            totalGames = existingPlayer.totalGames + 1,
            gamesWon = existingPlayer.gamesWon + if (gameStats.gameWon) 1 else 0,
            totalWonLegs = existingPlayer.totalWonLegs + gameStats.wonLegs,
            totalRounds = existingPlayer.totalRounds + gameStats.totalRounds,
            totalPoints = existingPlayer.totalPoints + gameStats.totalPointsFromRounds,
            totalDartsThrown = existingPlayer.totalDartsThrown + gameStats.totalDartsThrown,
            highestRoundScore = maxOf(existingPlayer.highestRoundScore, gameStats.highestRoundScore),
            bestAverageScorePerRound = maxOf(existingPlayer.bestAverageScorePerRound, gameStats.averageScorePerRound),
            highScores180s = existingPlayer.highScores180s + gameStats.highScores180s,
            highScores140s = existingPlayer.highScores140s + gameStats.highScores140s,
            highScores100s = existingPlayer.highScores100s + gameStats.highScores100s,
            bestCheckout = maxOf(existingPlayer.bestCheckout, gameStats.bestCheckout),
            bestMatchDarts = if (existingPlayer.bestMatchDarts == 0 || (gameStats.bestMatchDarts > 0 && gameStats.bestMatchDarts < existingPlayer.bestMatchDarts)) {
                gameStats.bestMatchDarts
            } else {
                existingPlayer.bestMatchDarts
            },
            totalBusts = existingPlayer.totalBusts + gameStats.totalBusts,
            checkoutAttempts = existingPlayer.checkoutAttempts + gameStats.checkoutAttempts,
            checkoutHits = existingPlayer.checkoutHits + gameStats.checkoutHits,
            first9Points = existingPlayer.first9Points + gameStats.first9Points,
            first9Darts = existingPlayer.first9Darts + gameStats.first9Darts,
            segmentHits = mergedSegmentHits,
            doubleHits = mergedDoubleHits,
            savedDartPositions = mergedDartPositions,
            gameHistory = mergedHistory
        )

        return savePlayer(updatedPlayer)
    }
}

/**
 * Schnelle In-Memory-Implementierung für Tests und Vorschau.
 */
class InMemoryPlayerRepository(
    initialPlayers: List<SavedPlayer> = emptyList()
) : BasePlayerRepository() {
    private val players = initialPlayers.toMutableList()

    override suspend fun loadAllPlayers(): List<SavedPlayer> = players.toList()

    override suspend fun savePlayer(player: SavedPlayer): Boolean {
        val index = players.indexOfFirst { it.name == player.name }
        if (index >= 0) {
            players[index] = player
        } else {
            players.add(player)
        }
        return true
    }

    override suspend fun deletePlayer(name: String): Boolean {
        return players.removeAll { it.name == name }
    }

    override suspend fun findPlayer(name: String): SavedPlayer? {
        return players.find { it.name == name }
    }
}
