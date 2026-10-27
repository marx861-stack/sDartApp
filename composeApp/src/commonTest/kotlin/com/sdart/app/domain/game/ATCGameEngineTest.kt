package com.sdart.app.domain.game

import androidx.compose.ui.geometry.Offset
import com.sdart.app.ATCGameState
import com.sdart.app.ATCVariant
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.domain.game.engine.ATCGameEngine
import com.sdart.app.domain.game.engine.GameUndoEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ATCGameEngineTest {

    private fun createAtcGame(
        config: ATCGameState,
        startTarget: Int,
        playerNames: List<String> = listOf("Player 1")
    ): Game {
        val players = playerNames.map { name ->
            Player(
                name = name,
                currentScore = 0,
                scores = mutableListOf(),
                dartThrows = mutableListOf(),
                atcCurrentTarget = startTarget
            )
        }
        return Game(
            players = players,
            mode = GameMode.AROUND_THE_CLOCK,
            currentPlayerIndex = 0,
            isFinished = false,
            throwsInRound = 0,
            doubleIn = false,
            doubleOut = false,
            totalLegs = 1,
            currentLeg = 1,
            atcConfig = config,
            startScore = 0
        )
    }

    private val noOpSound: (String, Int) -> Unit = { _, _ -> }

    private fun throwDart(game: Game, score: Int, multiplier: Int = 1): Game =
        ATCGameEngine.processThrow(
            score = score,
            position = Offset.Zero,
            multiplier = multiplier,
            currentGame = game,
            audioAnnounceEachDart = false,
            triggerSound = noOpSound
        )

    @Test
    fun testForwardWithoutBullsFinishesAfterTwenty() {
        val game = throwDart(createAtcGame(ATCGameState(ATCVariant.STANDARD), startTarget = 20), 20)
        assertEquals(0, game.players[0].atcCurrentTarget)
    }

    @Test
    fun testForwardWithBullsRequiresBullAfterTwenty() {
        var game = createAtcGame(ATCGameState(ATCVariant.STANDARD, withBulls = true), startTarget = 20)
        game = throwDart(game, 20)
        assertEquals(25, game.players[0].atcCurrentTarget)

        game = throwDart(game, 25)
        assertEquals(0, game.players[0].atcCurrentTarget)
    }

    @Test
    fun testReverseCountsDownAndFinishesAfterOne() {
        var game = createAtcGame(ATCGameState(ATCVariant.STANDARD, reverse = true), startTarget = 20)
        game = throwDart(game, 20)
        assertEquals(19, game.players[0].atcCurrentTarget)

        game = createAtcGame(ATCGameState(ATCVariant.STANDARD, reverse = true), startTarget = 1)
        game = throwDart(game, 1)
        assertEquals(0, game.players[0].atcCurrentTarget)
    }

    @Test
    fun testReverseWithBullsStartsWithBullThenTwenty() {
        var game = createAtcGame(ATCGameState(ATCVariant.STANDARD, reverse = true, withBulls = true), startTarget = 25)
        game = throwDart(game, 20)
        assertEquals(25, game.players[0].atcCurrentTarget, "Wrong number must not advance")

        game = throwDart(game, 50, multiplier = 2)
        assertEquals(20, game.players[0].atcCurrentTarget)
    }

    @Test
    fun testReverseQuicklyJumpsByMultiplier() {
        var game = createAtcGame(ATCGameState(ATCVariant.QUICKLY, reverse = true), startTarget = 20)
        game = throwDart(game, 60, multiplier = 3)
        assertEquals(17, game.players[0].atcCurrentTarget)

        game = createAtcGame(ATCGameState(ATCVariant.QUICKLY, reverse = true), startTarget = 2)
        game = throwDart(game, 6, multiplier = 3)
        assertEquals(0, game.players[0].atcCurrentTarget)
    }

    @Test
    fun testGameEndsImmediatelyWhenLastTargetIsHit() {
        val game = throwDart(createAtcGame(ATCGameState(ATCVariant.STANDARD), startTarget = 20), 20)
        assertTrue(game.isFinished)
        assertEquals(1, game.throwsInRound)
    }

    @Test
    fun testUndoRevertsTarget() {
        val config = ATCGameState(ATCVariant.STANDARD)
        var game = createAtcGame(config, startTarget = ATCGameEngine.startTarget(config))
        game = throwDart(game, 1)
        game = throwDart(game, 2)
        assertEquals(3, game.players[0].atcCurrentTarget)

        game = GameUndoEngine.undo(game)!!
        assertEquals(2, game.players[0].atcCurrentTarget)
        assertEquals(1, game.throwsInRound)
    }

    @Test
    fun testUndoOfMissKeepsTarget() {
        val config = ATCGameState(ATCVariant.STANDARD)
        var game = createAtcGame(config, startTarget = ATCGameEngine.startTarget(config))
        game = throwDart(game, 1)
        game = throwDart(game, 0, multiplier = 0)

        game = GameUndoEngine.undo(game)!!
        assertEquals(2, game.players[0].atcCurrentTarget)
    }

    @Test
    fun testUndoOfWinningDartReopensGameInReverseWithBulls() {
        val config = ATCGameState(ATCVariant.STANDARD, reverse = true, withBulls = true)
        var game = createAtcGame(config, startTarget = ATCGameEngine.startTarget(config))
        assertEquals(25, game.players[0].atcCurrentTarget)

        game = throwDart(game, 25)
        assertEquals(20, game.players[0].atcCurrentTarget)

        game = GameUndoEngine.undo(game)!!
        assertEquals(25, game.players[0].atcCurrentTarget)
        assertFalse(game.isFinished)
    }

    @Test
    fun testUndoRestoresShanghaiHits() {
        val config = ATCGameState(ATCVariant.SHANGHAI)
        var game = createAtcGame(config, startTarget = ATCGameEngine.startTarget(config))
        game = throwDart(game, 1)
        game = throwDart(game, 2, multiplier = 2)
        game = throwDart(game, 3, multiplier = 3)
        assertEquals(2, game.players[0].atcCurrentTarget)

        game = GameUndoEngine.undo(game)!!
        assertEquals(1, game.players[0].atcCurrentTarget)
        assertEquals(setOf(1, 2), game.players[0].atcShanghaiHits)
    }

    @Test
    fun testUndoOnlyReplaysThrowsOfSamePlayer() {
        val config = ATCGameState(ATCVariant.STANDARD)
        var game = createAtcGame(config, startTarget = 1, playerNames = listOf("A", "B"))
        repeat(3) { i -> game = throwDart(game, i + 1) } // A: 1, 2, 3 -> Ziel 4
        game = throwDart(game, 1) // B trifft die 1 -> Ziel 2

        game = GameUndoEngine.undo(game)!!
        assertEquals(1, game.players[1].atcCurrentTarget)
        assertEquals(4, game.players[0].atcCurrentTarget)
        assertEquals(1, game.currentPlayerIndex)
    }
}
