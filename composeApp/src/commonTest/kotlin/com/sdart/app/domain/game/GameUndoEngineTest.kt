package com.sdart.app.domain.game

import androidx.compose.ui.geometry.Offset
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.domain.game.engine.GameUndoEngine
import com.sdart.app.domain.game.engine.X01GameEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameUndoEngineTest {

    private fun createGame(startScore: Int = 501): Game {
        val players = listOf("Player 1", "Player 2").map { name ->
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
            doubleIn = false,
            doubleOut = true,
            targetScore = 0,
            totalLegs = 1,
            currentLeg = 1,
            startScore = startScore
        )
    }

    private val noOpSound: (String, Int) -> Unit = { _, _ -> }

    @Test
    fun testUndoEmptyHistoryReturnsNull() {
        val game = createGame()
        val undone = GameUndoEngine.undo(game)
        assertNull(undone)
    }

    @Test
    fun testUndoSingleThrowRestoresScore() {
        var game = createGame(startScore = 501)
        game = X01GameEngine.processThrow(
            score = 60,
            position = Offset.Zero,
            multiplier = 3,
            currentGame = game,
            audioAnnounceEachDart = false,
            audioAnnounceTurnTotal = false,
            triggerSound = noOpSound
        )!!

        assertEquals(441, game.players[0].currentScore)
        assertEquals(1, game.throwsInRound)

        val undone = GameUndoEngine.undo(game)
        assertNotNull(undone)
        assertEquals(501, undone.players[0].currentScore)
        assertEquals(0, undone.throwsInRound)
        assertEquals(0, undone.throwHistory.size)
    }

    @Test
    fun testUndoAcrossPlayerRotationRestoresPlayerIndex() {
        var game = createGame(startScore = 501)
        // Player 1 throws 3 darts
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

        // Switched to Player 2
        assertEquals(1, game.currentPlayerIndex)
        assertEquals(0, game.throwsInRound)

        // Undo 3rd throw of Player 1
        val undone = GameUndoEngine.undo(game)
        assertNotNull(undone)
        assertEquals(0, undone.currentPlayerIndex) // Restored back to Player 1
        assertEquals(2, undone.throwsInRound) // Now 2 throws in round
        assertEquals(461, undone.players[0].currentScore) // (501 - 40)
    }
}
