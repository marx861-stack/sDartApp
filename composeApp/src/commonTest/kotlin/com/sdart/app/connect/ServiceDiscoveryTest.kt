package com.sdart.app.connect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServiceDiscoveryTest {

    private fun host(serviceName: String, port: Int = 8080) = DiscoveredHost(
        serviceName = serviceName,
        hostId = "id-$serviceName",
        name = serviceName,
        address = "192.168.0.10",
        port = port,
        platform = "desktop",
        protocolVersion = CONNECT_PROTOCOL_VERSION
    )

    @Test
    fun registryReplacesHostWithSameServiceName() {
        val registry = HostRegistry()
        registry.addOrUpdate(host("A", port = 1))
        registry.addOrUpdate(host("A", port = 2))
        registry.addOrUpdate(host("B"))
        assertEquals(2, registry.hosts.value.size)
        assertEquals(2, registry.hosts.value.first { it.serviceName == "A" }.port)
    }

    @Test
    fun registryRemoveAndClear() {
        val registry = HostRegistry()
        registry.addOrUpdate(host("A"))
        registry.addOrUpdate(host("B"))
        registry.remove("A")
        assertEquals(listOf("B"), registry.hosts.value.map { it.serviceName })
        registry.clear()
        assertTrue(registry.hosts.value.isEmpty())
    }

    @Test
    fun fakeIsInvisibleUntilAdvertising() {
        val discovery = FakeServiceDiscovery()
        assertNull(discovery.advertisement)
        discovery.startAdvertising(HostAdvertisement("h1", "Desktop", 8080, "desktop"))
        assertNotNull(discovery.advertisement)
        discovery.stopAdvertising()
        assertNull(discovery.advertisement)
    }

    @Test
    fun fakeFindsHostsOnlyWhileBrowsing() {
        val discovery = FakeServiceDiscovery()
        discovery.simulateHostFound(host("A"))
        assertTrue(discovery.hosts.value.isEmpty())

        discovery.startBrowsing()
        discovery.simulateHostFound(host("A"))
        assertEquals(1, discovery.hosts.value.size)

        discovery.stopBrowsing()
        assertTrue(discovery.hosts.value.isEmpty())
    }
}
