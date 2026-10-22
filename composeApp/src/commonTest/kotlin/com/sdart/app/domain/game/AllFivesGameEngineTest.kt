package com.sdart.app.domain.game

import androidx.compose.ui.geometry.Offset
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.domain.game.engine.AllFivesGameEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class AllFivesGameEngineTest {

    private fun createAllFivesGame(
        targetPoints: Int = 50,
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
            mode = GameMode.ALL_FIVES,
            currentPlayerIndex = 0,
            isFinished = false,
            throwsInRound = 0,
            doubleIn = false,
            doubleOut = false,
            targetScore = targetPoints,
            totalLegs = 1,
            currentLeg = 1,
            startScore = 0
        )
    }

    private val noOpSound: (String, Int) -> Unit = { _, _ -> }

    @Test
    fun testValidRoundDivisibleByFiveAwardsPoints() {
        var game = createAllFivesGame()
        // Throw 5, 5, 5 -> sum = 15, divisible by 5 -> awards 15 / 5 = 3 points
        repeat(3) {
            game = AllFivesGameEngine.processThrow(
                score = 5,
                position = Offset.Zero,
                multiplier = 1,
                currentGame = game,
                audioAnnounceEachDart = false,
                audioAnnounceTurnTotal = false,
                triggerSound = noOpSound
            )
        }

        val player1 = game.players[0]
        assertEquals(15, player1.currentScore)
        assertEquals(1, game.currentPlayerIndex) // Switched to Player 2
        assertEquals(0, game.throwsInRound)
    }

    @Test
    fun testRoundWithMissAwardsZeroPoints() {
        var game = createAllFivesGame()
        // Throw 10, 0 (Miss), 10 -> sum = 20, but contains Miss
        game = AllFivesGameEngine.processThrow(
            score = 10,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )
        game = AllFivesGameEngine.processThrow(
            score = 0,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )
        game = AllFivesGameEngine.processThrow(
            score = 10,
            position = Offset.Zero,
            multiplier = 1,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )

        val player1 = game.players[0]
        assertEquals(0, player1.currentScore) // Miss invalidates round
        assertEquals(1, game.currentPlayerIndex)
    }

    @Test
    fun testRoundNotDivisibleByFiveAwardsZeroPoints() {
        var game = createAllFivesGame()
        // Throw 7, 7, 7 -> sum = 21, not divisible by 5
        repeat(3) {
            game = AllFivesGameEngine.processThrow(
                score = 7,
                position = Offset.Zero,
                multiplier = 1,
                currentGame = game,
                audioAnnounceEachDart = false,
                audioAnnounceTurnTotal = false,
                triggerSound = noOpSound
            )
        }

        val player1 = game.players[0]
        assertEquals(0, player1.currentScore)
        assertEquals(1, game.currentPlayerIndex)
    }
}
