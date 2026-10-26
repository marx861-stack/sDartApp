package com.sdart.app.connect

import androidx.compose.ui.geometry.Offset
import com.sdart.app.DartThrow
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectProtocolTest {

    @Test
    fun helloSurvivesRoundTrip() {
        val hello = ConnectMessage.Hello(
            deviceId = "abc-123",
            deviceName = "Marcs Handy",
            platform = "android",
            role = DeviceRole.CAMERA,
            capabilities = setOf(DeviceCapability.CAMERA, DeviceCapability.ML_DETECTION)
        )
        assertEquals(hello, ConnectProtocol.decode(ConnectProtocol.encode(hello)))
    }

    @Test
    fun dartHitAndObjectsSurviveRoundTrip() {
        val messages = listOf(
            ConnectMessage.DartHit(20, 3, 1, 0.9f, 0.5f, 0.25f),
            ConnectMessage.ScoreInput(60, 3, 0.1f, -0.5f),
            ConnectMessage.ScoreInput(25, 1),
            ConnectMessage.BoardCleared,
            ConnectMessage.Undo,
            ConnectMessage.FinishTurn,
            ConnectMessage.Bye,
            ConnectMessage.Rejected(RejectReason.DECLINED_BY_USER)
        )
        messages.forEach { assertEquals(it, ConnectProtocol.decode(ConnectProtocol.encode(it))) }
    }

    @Test
    fun invalidTextReturnsNull() {
        assertNull(ConnectProtocol.decode("kein json"))
        assertNull(ConnectProtocol.decode("{\"type\":\"gibt_es_nicht\"}"))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val text = "{\"type\":\"ping\",\"timestampMs\":5,\"neu\":true}"
        assertEquals(ConnectMessage.Ping(5), ConnectProtocol.decode(text))
    }

    @Test
    fun rolesDependOnCapabilities() {
        val roles = availableRoles(setOf(DeviceCapability.CAMERA, DeviceCapability.TOUCH_INPUT))
        assertEquals(setOf(DeviceRole.CAMERA, DeviceRole.INPUT), roles)
        assertTrue(DeviceRole.HOST !in roles)
    }

    @Test
    fun gameSnapshotEncodesAndDecodesCorrectly() {
        val snapshot = GameSnapshot(
            isGameActive = true,
            gameMode = "501 Double Out",
            currentPlayerIndex = 0,
            currentPlayerName = "Marc",
            currentPlayerScore = 381,
            throwsInRound = 2,
            currentRoundDarts = listOf(
                DartSnapshot(score = 60, multiplier = 3, label = "T20 (60)", x = 0.5f, y = 0.2f),
                DartSnapshot(score = 60, multiplier = 3, label = "T20 (60)", x = 0.51f, y = 0.21f)
            ),
            currentRoundTotal = 120,
            checkoutSuggestion = null,
            isBust = false,
            isFinished = false,
            highlightedSegments = setOf(20),
            players = listOf(
                PlayerSnapshot(index = 0, name = "Marc", score = 381, isCurrent = true),
                PlayerSnapshot(index = 1, name = "Gegner", score = 501, isCurrent = false)
            )
        )

        val encoded = GameSnapshotProtocol.encode(snapshot)
        val decoded = GameSnapshotProtocol.decode(encoded)

        assertNotNull(decoded)
        assertEquals(snapshot, decoded)

        // Auch über GameStateUpdate
        val updateMsg = ConnectMessage.GameStateUpdate(encoded)
        val decodedMsg = ConnectProtocol.decode(ConnectProtocol.encode(updateMsg)) as? ConnectMessage.GameStateUpdate
        assertNotNull(decodedMsg)
        val decodedSnapshot = GameSnapshotProtocol.decode(decodedMsg.snapshotJson)
        assertEquals(snapshot, decodedSnapshot)
    }

    @Test
    fun toSnapshotConvertsGameProperly() {
        val player1 = Player(name = "Spieler 1", currentScore = 170)
        val player2 = Player(name = "Spieler 2", currentScore = 501)
        val game = Game(
            players = listOf(player1, player2),
            mode = GameMode.X501,
            currentPlayerIndex = 0,
            throwsInRound = 1,
            doubleOut = true
        )
        game.throwHistory.add(
            DartThrow(score = 60, position = Offset(0.1f, -0.2f), playerIndex = 0, multiplier = 3)
        )

        val snapshot = game.toSnapshot()
        assertTrue(snapshot.isGameActive)
        assertEquals("Spieler 1", snapshot.currentPlayerName)
        assertEquals(170, snapshot.currentPlayerScore)
        assertEquals(1, snapshot.throwsInRound)
        assertEquals(1, snapshot.currentRoundDarts.size)
        assertEquals("T20 (60)", snapshot.currentRoundDarts[0].label)
        assertEquals("T20 T20 Bull", snapshot.checkoutSuggestion)
        assertEquals(2, snapshot.players.size)
        assertTrue(snapshot.players[0].isCurrent)
        assertFalse(snapshot.players[1].isCurrent)
    }

    @Test
    fun nullGameProducesInactiveSnapshot() {
        val nullGame: Game? = null
        val snapshot = nullGame.toSnapshot()
        assertFalse(snapshot.isGameActive)
        assertEquals("", snapshot.currentPlayerName)
    }

    @Test
    fun formatDartThrowLabelHandlesAllCases() {
        assertEquals("Bull (50)", formatDartThrowLabel(50, 2))
        assertEquals("Bull (25)", formatDartThrowLabel(25, 1))
        assertEquals("Miss (0)", formatDartThrowLabel(0, 0))
        assertEquals("T20 (60)", formatDartThrowLabel(60, 3))
        assertEquals("D16 (32)", formatDartThrowLabel(32, 2))
        assertEquals("S20 (20)", formatDartThrowLabel(20, 1))
    }
}
