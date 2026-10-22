package com.sdart.app.domain.game

import androidx.compose.ui.geometry.Offset
import com.sdart.app.CricketConfig
import com.sdart.app.CricketVariant
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.domain.game.engine.CricketGameEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CricketGameEngineTest {

    private fun createCricketGame(
        variant: CricketVariant = CricketVariant.STANDARD,
        playerNames: List<String> = listOf("Player 1", "Player 2")
    ): Game {
        val players = playerNames.map { name ->
            Player(
                name = name,
                currentScore = 0,
                scores = mutableListOf(),
                dartThrows = mutableListOf()
            )
        }
        return Game(
            players = players,
            mode = GameMode.CRICKET,
            currentPlayerIndex = 0,
            isFinished = false,
            throwsInRound = 0,
            doubleIn = false,
            doubleOut = false,
            targetScore = 0,
            totalLegs = 1,
            currentLeg = 1,
            cricketConfig = CricketConfig(variant = variant),
            startScore = 0
        )
    }

    private val noOpSound: (String, Int) -> Unit = { _, _ -> }

    @Test
    fun testTripleTwentyClosesSegment() {
        val game = createCricketGame()
        // Hit Triple 20 (score 60, mult 3)
        val nextGame = CricketGameEngine.processThrow(
            score = 60,
            position = Offset.Zero,
            multiplier = 3,
            currentGame = game,
            audioAnnounceEachDart = false,
            triggerSound = noOpSound
        )

        val player1 = nextGame.players[0]
        assertEquals(3, player1.cricketMarks[20])
        assertTrue(player1.isSegmentClosed(20))
        assertEquals(0, player1.currentScore) // Closed exactly, no extra scoring yet
    }

    @Test
    fun testExtraHitsScorePointsInStandardMode() {
        var game = createCricketGame(variant = CricketVariant.STANDARD)
        // First hit: Triple 20 (closes 20)
        game = CricketGameEngine.processThrow(
            score = 60,
            position = Offset.Zero,
            multiplier = 3,
            currentGame = game,
            audioAnnounceEachDart = false,
            triggerSound = noOpSound
        )
        // Second hit: Single 20 (already closed -> 1 * 20 = 20 points)
        game = CricketGameEngine.processThrow(
            score = 20,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            triggerSound = noOpSound
        )

        val player1 = game.players[0]
        assertEquals(4, player1.cricketMarks[20])
        assertEquals(20, player1.currentScore)
    }

    @Test
    fun testNonCricketSegmentAddsNoMarks() {
        val game = createCricketGame()
        // Hit Single 14 (not in 15..20 or 25)
        val nextGame = CricketGameEngine.processThrow(
            score = 14,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            triggerSound = noOpSound
        )

        val player1 = nextGame.players[0]
        assertEquals(0, player1.cricketMarks.getOrDefault(14, 0))
        assertEquals(0, player1.currentScore)
        assertEquals(1, nextGame.throwsInRound)
    }

    @Test
    fun testDoubleBullAddsTwoMarks() {
        val game = createCricketGame()
        // Hit Double Bull (score 50, mult 2)
        val nextGame = CricketGameEngine.processThrow(
            score = 50,
            position = Offset.Zero,
            multiplier = 2,
            currentGame = game,
            audioAnnounceEachDart = false,
            triggerSound = noOpSound
        )

        val player1 = nextGame.players[0]
        assertEquals(2, player1.cricketMarks[25])
        assertFalse(player1.isSegmentClosed(25))
    }
}
