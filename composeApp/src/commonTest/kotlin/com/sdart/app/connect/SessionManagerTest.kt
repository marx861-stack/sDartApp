package com.sdart.app.connect

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SessionManagerTest {

    private lateinit var fakeServer: FakeHostServer
    private lateinit var fakeDiscovery: FakeServiceDiscovery
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setup() {
        fakeServer = FakeHostServer()
        fakeDiscovery = FakeServiceDiscovery()
        sessionManager = SessionManager(fakeServer, fakeDiscovery, persistent = false)
        sessionManager.clearAllTrustedDevices()
    }

    private fun sampleHello(
        deviceId: String = "dev-1",
        name: String = "Phone 1",
        role: DeviceRole = DeviceRole.CAMERA
    ) = ConnectMessage.Hello(
        deviceId = deviceId,
        deviceName = name,
        platform = "android",
        role = role,
        capabilities = setOf(DeviceCapability.CAMERA, DeviceCapability.ML_DETECTION)
    )

    @Test
    fun startHostActivatesServerAndDiscovery() {
        val port = sessionManager.startHost("TestHost", 8085, "desktop")
        assertEquals(8085, port)
        assertTrue(fakeServer.isRunning)
        assertNotNull(fakeDiscovery.advertisement)
        assertEquals("TestHost", fakeDiscovery.advertisement?.name)

        val state = sessionManager.hostState.value as HostState.Active
        assertEquals("TestHost", state.hostName)
        assertEquals(8085, state.port)
    }

    @Test
    fun untrustedDeviceTriggersPendingAuthorizationAndApprovingSavesToTrustList() {
        sessionManager.startHost("TestHost", 8085)

        fakeServer.simulateConnect("conn-1", "192.168.1.50")
        val hello = sampleHello("dev-phone-1", "Phone 1", DeviceRole.CAMERA)
        fakeServer.simulateMessage("conn-1", ConnectProtocol.encode(hello))

        // Nicht sofort akzeptiert, sondern pendingAuthorization
        val state = sessionManager.hostState.value as HostState.Active
        assertTrue(state.participants.isEmpty())

        val pending = sessionManager.pendingAuthorization.value
        assertNotNull(pending)
        assertEquals("dev-phone-1", pending.hello.deviceId)
        assertEquals("192.168.1.50", pending.remoteAddress)

        // Bestätigen mit "Merken"
        sessionManager.approvePendingAuthorization(remember = true)
        assertNull(sessionManager.pendingAuthorization.value)
        assertTrue(sessionManager.isDeviceTrusted("dev-phone-1"))
        assertEquals(1, sessionManager.trustedDevices.value.size)
        assertEquals("Phone 1", sessionManager.trustedDevices.value.first().name)

        // Jetzt Teilnehmer aktiv
        val updatedState = sessionManager.hostState.value as HostState.Active
        assertEquals(1, updatedState.participants.size)
        assertEquals("dev-phone-1", updatedState.participants.first().deviceId)

        // Widerrufen
        sessionManager.revokeTrust("dev-phone-1")
        assertFalse(sessionManager.isDeviceTrusted("dev-phone-1"))
        assertTrue(sessionManager.trustedDevices.value.isEmpty())
    }

    @Test
    fun alreadyTrustedDeviceConnectsImmediatelyWithoutDialog() {
        sessionManager.trustDevice("known-phone", "Known Phone", "android")
        sessionManager.startHost("TestHost", 8085)

        fakeServer.simulateConnect("conn-2")
        fakeServer.simulateMessage("conn-2", ConnectProtocol.encode(sampleHello("known-phone", "Known Phone")))

        // Sofort akzeptiert
        assertNull(sessionManager.pendingAuthorization.value)
        val state = sessionManager.hostState.value as HostState.Active
        assertEquals(1, state.participants.size)
        assertEquals("known-phone", state.participants.first().deviceId)
    }

    @Test
    fun rejectPendingAuthorizationDisconnectsClient() {
        sessionManager.startHost("TestHost", 8085)

        fakeServer.simulateConnect("conn-x")
        fakeServer.simulateMessage("conn-x", ConnectProtocol.encode(sampleHello("unknown-x")))

        assertNotNull(sessionManager.pendingAuthorization.value)

        sessionManager.rejectPendingAuthorization()
        assertNull(sessionManager.pendingAuthorization.value)

        val state = sessionManager.hostState.value as HostState.Active
        assertTrue(state.participants.isEmpty())
        assertTrue(fakeServer.disconnectedClients.contains("conn-x"))
    }

    @Test
    fun protocolVersionMismatchIsRejectedDirectly() {
        sessionManager.startHost("TestHost", 8085)

        fakeServer.simulateConnect("conn-old")
        val oldHello = sampleHello("old-dev").copy(protocolVersion = 999)
        fakeServer.simulateMessage("conn-old", ConnectProtocol.encode(oldHello))

        assertNull(sessionManager.pendingAuthorization.value)
        val state = sessionManager.hostState.value as HostState.Active
        assertTrue(state.participants.isEmpty())

        val rejectedMsg = fakeServer.sentMessages.any { (conn, msg) ->
            conn == "conn-old" && msg.contains("PROTOCOL_MISMATCH")
        }
        assertTrue(rejectedMsg)
    }

    @Test
    fun disconnectParticipantRemovesClientAndSendsBye() {
        sessionManager.trustDevice("phone-3")
        sessionManager.startHost("TestHost", 8085)
        fakeServer.simulateConnect("conn-3")
        fakeServer.simulateMessage("conn-3", ConnectProtocol.encode(sampleHello("phone-3")))

        val state = sessionManager.hostState.value as HostState.Active
        assertEquals(1, state.participants.size)

        sessionManager.disconnectParticipant("conn-3")
        val updatedState = sessionManager.hostState.value as HostState.Active
        assertTrue(updatedState.participants.isEmpty())
        assertTrue(fakeServer.disconnectedClients.contains("conn-3"))
    }

    @Test
    fun dartHitAndUndoAreRoutedToCallbacks() {
        sessionManager.trustDevice("cam-1")
        sessionManager.startHost("TestHost", 8085)
        fakeServer.simulateConnect("conn-4")
        fakeServer.simulateMessage("conn-4", ConnectProtocol.encode(sampleHello("cam-1")))

        var receivedHit: ConnectMessage.DartHit? = null
        var undoCalled = false
        sessionManager.onDartHit = { receivedHit = it }
        sessionManager.onUndo = { undoCalled = true }

        fakeServer.simulateMessage(
            "conn-4",
            ConnectProtocol.encode(ConnectMessage.DartHit(20, 3, 1, 0.95f, 0.5f, 0.3f))
        )
        fakeServer.simulateMessage("conn-4", ConnectProtocol.encode(ConnectMessage.Undo))

        assertNotNull(receivedHit)
        assertEquals(20, receivedHit?.segment)
        assertEquals(3, receivedHit?.multiplier)
        assertTrue(undoCalled)
    }

    @Test
    fun undoFromInputRoleTriggersOnUndoCallback() {
        sessionManager.trustDevice("input-undo-1")
        sessionManager.startHost("TestHost", 8085)
        fakeServer.simulateConnect("conn-input-undo")
        fakeServer.simulateMessage(
            "conn-input-undo",
            ConnectProtocol.encode(sampleHello("input-undo-1", "Input Pad", DeviceRole.INPUT))
        )

        var undoTriggered = false
        sessionManager.onUndo = { undoTriggered = true }

        fakeServer.simulateMessage(
            "conn-input-undo",
            ConnectProtocol.encode(ConnectMessage.Undo)
        )

        assertTrue(undoTriggered)
    }

    @Test
    fun scoreInputAndFinishTurnAreRoutedToCallbacks() {
        sessionManager.trustDevice("input-1")
        sessionManager.startHost("TestHost", 8085)
        fakeServer.simulateConnect("conn-input")
        fakeServer.simulateMessage(
            "conn-input",
            ConnectProtocol.encode(sampleHello("input-1", "Input Pad", DeviceRole.INPUT))
        )

        var receivedScoreInput: ConnectMessage.ScoreInput? = null
        var finishTurnCalled = false
        sessionManager.onScoreInput = { receivedScoreInput = it }
        sessionManager.onFinishTurn = { finishTurnCalled = true }

        fakeServer.simulateMessage(
            "conn-input",
            ConnectProtocol.encode(ConnectMessage.ScoreInput(60, 3, 0.2f, 0.1f))
        )
        fakeServer.simulateMessage(
            "conn-input",
            ConnectProtocol.encode(ConnectMessage.FinishTurn)
        )

        assertNotNull(receivedScoreInput)
        assertEquals(60, receivedScoreInput?.segment)
        assertEquals(3, receivedScoreInput?.multiplier)
        assertTrue(finishTurnCalled)
    }

    @Test
    fun broadcastGameStateSendsToInputAndDisplayParticipantsAndCaches() {
        sessionManager.trustDevice("disp-1")
        sessionManager.startHost("TestHost", 8085)
        fakeServer.simulateConnect("conn-disp")
        fakeServer.simulateMessage(
            "conn-disp",
            ConnectProtocol.encode(sampleHello("disp-1", "Display", DeviceRole.DISPLAY))
        )

        val snapshotJson = "{\"isGameActive\":true,\"gameMode\":\"501\"}"
        sessionManager.broadcastGameState(snapshotJson)

        val stateMsg = fakeServer.sentMessages.any { (conn, msg) ->
            conn == "conn-disp" && msg.contains("game_state") && msg.contains("501")
        }
        assertTrue(stateMsg)

        // Neuer Teilnehmer mit Rolle INPUT verbindet sich und erhält sofort den gecachten Snapshot
        sessionManager.trustDevice("input-new")
        fakeServer.simulateConnect("conn-input-new")
        fakeServer.simulateMessage(
            "conn-input-new",
            ConnectProtocol.encode(sampleHello("input-new", "Input New", DeviceRole.INPUT))
        )

        val cachedStateMsgSent = fakeServer.sentMessages.any { (conn, msg) ->
            conn == "conn-input-new" && msg.contains("game_state") && msg.contains("501")
        }
        assertTrue(cachedStateMsgSent)
    }

    @Test
    fun stopHostSendsByeAndClearsState() {
        sessionManager.trustDevice("dev-a")
        sessionManager.startHost("TestHost", 8085)
        fakeServer.simulateConnect("conn-a")
        fakeServer.simulateMessage("conn-a", ConnectProtocol.encode(sampleHello("dev-a")))

        sessionManager.stopHost()
        assertTrue(sessionManager.hostState.value is HostState.Inactive)
        assertNull(fakeDiscovery.advertisement)

        val byeSent = fakeServer.sentMessages.any { (conn, msg) ->
            conn == "conn-a" && msg.contains("\"type\":\"bye\"")
        }
        assertTrue(byeSent)
    }
}
