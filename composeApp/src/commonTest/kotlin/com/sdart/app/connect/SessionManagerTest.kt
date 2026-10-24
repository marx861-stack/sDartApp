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
        assertEquals("conn-1", pending.connectionId)
        assertEquals("dev-phone-1", pending.hello.deviceId)

        // Nutzer genehmigt & merkt
        sessionManager.approvePendingAuthorization(remember = true)
        assertNull(sessionManager.pendingAuthorization.value)
        assertTrue(sessionManager.isDeviceTrusted("dev-phone-1"))
        assertEquals(1, sessionManager.trustedDevices.value.size)
        assertEquals("Phone 1", sessionManager.trustedDevices.value.first().name)

        // Nun als Teilnehmer registriert
        val updatedState = sessionManager.hostState.value as HostState.Active
        assertEquals(1, updatedState.participants.size)

        // Trust-Entfernung testen
        sessionManager.revokeTrust("dev-phone-1")
        assertFalse(sessionManager.isDeviceTrusted("dev-phone-1"))
        assertTrue(sessionManager.trustedDevices.value.isEmpty())
    }

    @Test
    fun trustedDeviceConnectsImmediatelyWithoutAuthorization() {
        sessionManager.trustDevice("known-phone", "Known Phone", "android")
        sessionManager.startHost("TestHost", 8085)

        fakeServer.simulateConnect("conn-known")
        fakeServer.simulateMessage("conn-known", ConnectProtocol.encode(sampleHello("known-phone")))

        // Direkt als Teilnehmer registriert - kein pending
        assertNull(sessionManager.pendingAuthorization.value)
        val state = sessionManager.hostState.value as HostState.Active
        assertEquals(1, state.participants.size)
        assertEquals("known-phone", state.participants.first().deviceId)
    }

    @Test
    fun rejectingPendingAuthSendsRejectedAndDisconnects() {
        sessionManager.startHost("TestHost", 8085)

        fakeServer.simulateConnect("conn-reject")
        fakeServer.simulateMessage("conn-reject", ConnectProtocol.encode(sampleHello("reject-phone")))
        assertNotNull(sessionManager.pendingAuthorization.value)

        sessionManager.rejectPendingAuthorization()
        assertNull(sessionManager.pendingAuthorization.value)

        val state = sessionManager.hostState.value as HostState.Active
        assertTrue(state.participants.isEmpty())
        assertTrue(fakeServer.disconnectedClients.contains("conn-reject"))
    }

    @Test
    fun protocolMismatchRejectsConnection() {
        sessionManager.startHost("TestHost", 8085)

        fakeServer.simulateConnect("conn-2")
        val outdatedHello = sampleHello("outdated-dev").copy(protocolVersion = 999)
        fakeServer.simulateMessage("conn-2", ConnectProtocol.encode(outdatedHello))

        val state = sessionManager.hostState.value as HostState.Active
        assertTrue(state.participants.isEmpty())
        assertTrue(fakeServer.disconnectedClients.contains("conn-2"))

        val rejectedSent = fakeServer.sentMessages.any { (conn, msg) ->
            conn == "conn-2" && msg.contains("\"type\":\"rejected\"")
        }
        assertTrue(rejectedSent)
    }

    @Test
    fun disconnectParticipantRemovesDeviceAndSendsBye() {
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
