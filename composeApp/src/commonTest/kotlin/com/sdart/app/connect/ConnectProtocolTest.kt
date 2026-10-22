package com.sdart.app.connect

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
