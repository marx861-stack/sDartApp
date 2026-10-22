package com.sdart.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerRepositorySerializationTest {

    @Test
    fun testSerializationAndDeserializationRoundtrip() {
        val originalPlayers = listOf(
            SavedPlayer(
                name = "Max",
                totalGames = 12,
                gamesWon = 8,
                totalWonLegs = 24,
                totalRounds = 120,
                totalPoints = 7200,
                totalDartsThrown = 360,
                highestRoundScore = 180,
                bestAverageScorePerRound = 75,
                highScores180s = 3,
                highScores140s = 7,
                highScores100s = 15,
                bestCheckout = 120,
                bestMatchDarts = 15,
                totalBusts = 2,
                checkoutAttempts = 10,
                checkoutHits = 5,
                first9Points = 1800,
                first9Darts = 90,
                segmentHits = mapOf("T20" to 14, "Bull" to 5),
                doubleHits = mapOf(16 to 3, 20 to 2),
                savedDartPositions = listOf(SavedPosition(x = 100.5f, y = 200.5f)),
                gameHistory = listOf(
                    GameHistoryRecord(
                        timestamp = 1700000000000L,
                        averageScore = 65.4f,
                        dartsPerLeg = 18.2f
                    )
                )
            ),
            SavedPlayer(name = "Gast")
        )

        val json = serializeSavedPlayers(originalPlayers)
        assertTrue(json.contains("\"name\": \"Max\""))
        assertTrue(json.contains("\"name\": \"Gast\""))

        val deserialized = deserializeSavedPlayers(json)
        assertEquals(2, deserialized.size)
        assertEquals("Max", deserialized[0].name)
        assertEquals(12, deserialized[0].totalGames)
        assertEquals(3, deserialized[0].highScores180s)
        assertEquals(14, deserialized[0].segmentHits["T20"])
        assertEquals(1, deserialized[0].savedDartPositions.size)
        assertEquals(100.5f, deserialized[0].savedDartPositions[0].x)
        assertEquals(1, deserialized[0].gameHistory.size)
        assertEquals(65.4f, deserialized[0].gameHistory[0].averageScore)

        assertEquals("Gast", deserialized[1].name)
        assertEquals(0, deserialized[1].totalGames)
    }

    @Test
    fun testEmptyOrInvalidJsonHandling() {
        val emptyList = deserializeSavedPlayers("")
        assertTrue(emptyList.isEmpty())

        val invalidList = deserializeSavedPlayers("{ invalid json }")
        assertTrue(invalidList.isEmpty())
    }
}
