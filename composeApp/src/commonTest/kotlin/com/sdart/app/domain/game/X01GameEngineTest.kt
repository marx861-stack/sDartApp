package com.sdart.app.domain.game

import androidx.compose.ui.geometry.Offset
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.domain.game.engine.X01GameEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class X01GameEngineTest {

    private fun createGame(
        startScore: Int = 501,
        doubleIn: Boolean = false,
        doubleOut: Boolean = true,
        playerNames: List<String> = listOf("Player 1", "Player 2")
    ): Game {
        val players = playerNames.map { name ->
            Player(
                name = name,
                currentScore = startScore,
                scores = mutableListOf(),
                dartThrows = mutableListOf()
            )
        }
        return Game(
            players = players,
            mode = GameMode.X501,
            currentPlayerIndex = 0,
            isFinished = false,
            throwsInRound = 0,
            doubleIn = doubleIn,
            doubleOut = doubleOut,
            targetScore = 0,
            totalLegs = 1,
            currentLeg = 1,
            startScore = startScore
        )
    }

    private val noOpSound: (String, Int) -> Unit = { _, _ -> }

    @Test
    fun testSingleThrowReducesScore() {
        val game = createGame(startScore = 501)
        val nextGame = X01GameEngine.processThrow(
            score = 60,
            position = Offset.Zero,
            multiplier = 3,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        assertEquals(441, nextGame.players[0].currentScore)
        assertEquals(1, nextGame.throwsInRound)
        assertEquals(0, nextGame.currentPlayerIndex)
        assertEquals(1, nextGame.throwHistory.size)
        assertEquals(60, nextGame.throwHistory.first().score)
    }

    @Test
    fun testPlayerRotationAfterThreeThrows() {
        var game = createGame(startScore = 501)
        // 3 Throws for Player 1
        repeat(3) {
            game = X01GameEngine.processThrow(
                score = 20,
                position = Offset.Zero,
                multiplier = 1,
                currentGame = game,
                audioAnnounceEachDart = false,
                audioAnnounceTurnTotal = false,
                triggerSound = noOpSound
            )!!
        }

        assertEquals(441, game.players[0].currentScore)
        assertEquals(1, game.currentPlayerIndex) // Switched to Player 2
        assertEquals(0, game.throwsInRound)
        assertEquals(3, game.throwHistory.size)
    }

    @Test
    fun testDoubleInRuleIgnoredIfSingle() {
        val game = createGame(startScore = 501, doubleIn = true)
        // Try single 20
        val nextGame = X01GameEngine.processThrow(
            score = 20,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        assertEquals(501, nextGame.players[0].currentScore)
        assertFalse(nextGame.players[0].hasDoubleIn)
        assertEquals(1, nextGame.throwsInRound)
    }

    @Test
    fun testDoubleInRuleActivatedWithDouble() {
        val game = createGame(startScore = 501, doubleIn = true)
        // Throw Double 20 (score 40)
        val nextGame = X01GameEngine.processThrow(
            score = 40,
            position = Offset.Zero,
            multiplier = 2,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        assertEquals(461, nextGame.players[0].currentScore)
        assertTrue(nextGame.players[0].hasDoubleIn)
    }

    @Test
    fun testBustWhenScoreGoesBelowZero() {
        var game = createGame(startScore = 40)
        // Throw 60 (exceeds 40)
        game = X01GameEngine.processThrow(
            score = 60,
            position = Offset.Zero,
            multiplier = 3,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        // Score must revert to round start (40) and player must switch immediately
        assertEquals(40, game.players[0].currentScore)
        assertEquals(1, game.currentPlayerIndex)
        assertEquals(0, game.throwsInRound)
        assertTrue(game.throwHistory.last().isBust)
    }

    @Test
    fun testBustWhenDoubleOutLeavesScoreOfOne() {
        var game = createGame(startScore = 32, doubleOut = true)
        // Throw Single 31 -> leaves 1 (impossible to finish with double)
        game = X01GameEngine.processThrow(
            score = 31,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        assertEquals(32, game.players[0].currentScore)
        assertEquals(1, game.currentPlayerIndex)
        assertTrue(game.throwHistory.last().isBust)
    }

    @Test
    fun testValidDoubleOutCheckoutFinishesGame() {
        var game = createGame(startScore = 40, doubleOut = true)
        // Throw Double 20 (score 40, mult 2)
        game = X01GameEngine.processThrow(
            score = 40,
            position = Offset.Zero,
            multiplier = 2,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        assertEquals(0, game.players[0].currentScore)
        assertEquals(1, game.players[0].wonLegs)
        assertTrue(game.isFinished)
    }

    @Test
    fun testInvalidSingleOutWhenDoubleOutRequiredBusts() {
        var game = createGame(startScore = 20, doubleOut = true)
        // Throw Single 20 (mult 1) -> reaches 0, but is not a double
        game = X01GameEngine.processThrow(
            score = 20,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        // Must bust and remain at 20
        assertEquals(20, game.players[0].currentScore)
        assertFalse(game.isFinished)
        assertEquals(1, game.currentPlayerIndex)
    }
}
