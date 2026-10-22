package com.sdart.app.connect

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual fun createServiceDiscovery(): ServiceDiscovery = IosServiceDiscovery()

/**
 * iOS-Platzhalter: Bonjour (NWBrowser/NWListener) wird in einem späteren Schritt angebunden
 * (siehe docs/companion_connection_concept.md, Schritt 8). Bis dahin findet iOS keine Hosts.
 */
class IosServiceDiscovery : ServiceDiscovery {
    override val hosts: StateFlow<List<DiscoveredHost>> = MutableStateFlow(emptyList())
    override fun startAdvertising(advertisement: HostAdvertisement) {}
    override fun stopAdvertising() {}
    override fun startBrowsing() {}
    override fun stopBrowsing() {}
}
