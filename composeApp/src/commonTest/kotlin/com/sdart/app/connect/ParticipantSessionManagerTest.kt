package com.sdart.app.connect

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantSessionManagerTest {

    private lateinit var fakeClient: FakeWebSocketClient
    private lateinit var fakeDiscovery: FakeServiceDiscovery
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var manager: ParticipantSessionManager

    @BeforeTest
    fun setup() {
        fakeClient = FakeWebSocketClient()
        fakeDiscovery = FakeServiceDiscovery()
        manager = ParticipantSessionManager(fakeClient, fakeDiscovery, testScope, persistent = false)
    }

    private fun sampleHost(name: String = "Desktop-Marc", port: Int = 8085) = DiscoveredHost(
        serviceName = name,
        hostId = "host-1",
        name = name,
        address = "192.168.1.100",
        port = port,
        platform = "desktop",
        protocolVersion = CONNECT_PROTOCOL_VERSION
    )

    @Test
    fun selectingRoleStartsSearching() {
        manager.selectRole(DeviceRole.CAMERA)
        assertTrue(manager.state.value is ParticipantState.Searching)
        assertTrue(fakeDiscovery.isBrowsing)
    }

    @Test
    fun singleDiscoveredHostTriggersAutoConnectAndHello() = testScope.runTest {
        manager.selectRole(DeviceRole.CAMERA, deviceName = "Pixel 8", platform = "android")
        runCurrent()

        // Discovery findet genau 1 Host
        fakeDiscovery.simulateHostFound(sampleHost())
        runCurrent()

        // Sollte automatisch in Connecting sein
        val connectingState = manager.state.value as ParticipantState.Connecting
        assertEquals("Desktop-Marc", connectingState.host.name)
        assertEquals("ws://192.168.1.100:8085", fakeClient.connectedUrl)

        // Hello Nachricht wurde automatisch gesendet
        assertTrue(fakeClient.sentMessages.isNotEmpty())
        val helloMsg = ConnectProtocol.decode(fakeClient.sentMessages.first()) as ConnectMessage.Hello
        assertEquals(DeviceRole.CAMERA, helloMsg.role)
        assertEquals("Pixel 8", helloMsg.deviceName)
    }

    @Test
    fun receivingWelcomeTransitionsToConnected() = testScope.runTest {
        manager.selectRole(DeviceRole.CAMERA)
        runCurrent()
        fakeDiscovery.simulateHostFound(sampleHost())
        runCurrent()

        // Host antwortet mit Welcome
        fakeClient.simulateIncomingMessage(
            ConnectProtocol.encode(ConnectMessage.Welcome(sessionId = "sess-123", hostName = "Desktop-Marc"))
        )

        val connectedState = manager.state.value as ParticipantState.Connected
        assertEquals("Desktop-Marc", connectedState.hostName)
        assertEquals("sess-123", connectedState.sessionId)
        assertEquals(DeviceRole.CAMERA, connectedState.role)
    }

    @Test
    fun sendingDartHitWhenConnectedSendsProtocolMessage() = testScope.runTest {
        manager.selectRole(DeviceRole.CAMERA)
        runCurrent()
        fakeDiscovery.simulateHostFound(sampleHost())
        runCurrent()
        fakeClient.simulateIncomingMessage(
            ConnectProtocol.encode(ConnectMessage.Welcome("sess-1", "Host"))
        )

        val sent = manager.sendDartHit(20, 3, 1, 0.98f, 0.5f, 0.2f)
        assertTrue(sent)

        val lastMessage = ConnectProtocol.decode(fakeClient.sentMessages.last()) as ConnectMessage.DartHit
        assertEquals(20, lastMessage.segment)
        assertEquals(3, lastMessage.multiplier)
        assertEquals(1, lastMessage.dartIndex)
    }

    @Test
    fun receivingRejectedTransitionsToRejected() = testScope.runTest {
        manager.selectRole(DeviceRole.CAMERA)
        runCurrent()
        fakeDiscovery.simulateHostFound(sampleHost())
        runCurrent()

        fakeClient.simulateIncomingMessage(
            ConnectProtocol.encode(ConnectMessage.Rejected(RejectReason.DECLINED_BY_USER))
        )

        val rejectedState = manager.state.value as ParticipantState.Rejected
        assertEquals(RejectReason.DECLINED_BY_USER, rejectedState.reason)
    }

    @Test
    fun disconnectResetsToIdle() {
        manager.selectRole(DeviceRole.CAMERA)
        manager.disconnect()

        assertTrue(manager.state.value is ParticipantState.Idle)
        assertTrue(!fakeDiscovery.isBrowsing)
    }
}
