package com.example.myapplication.connect

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Daten, mit denen sich ein Host im Netz ankündigt (mDNS TXT-Record).
 *
 * @property hostId Stabile ID des Host-Geräts (UUID), damit Teilnehmer ihn wiedererkennen.
 * @property name Anzeigename, z. B. "Desktop Marc".
 * @property port TCP-Port des WebSocket-Servers.
 * @property platform "desktop", "android" oder "ios".
 */
data class HostAdvertisement(
    val hostId: String,
    val name: String,
    val port: Int,
    val platform: String,
    val protocolVersion: Int = CONNECT_PROTOCOL_VERSION
)

/**
 * Ein im Netz gefundener Host.
 *
 * @property serviceName Eindeutiger mDNS-Dienstname (Schlüssel zum Entfernen, wenn der Host verschwindet).
 */
data class DiscoveredHost(
    val serviceName: String,
    val hostId: String,
    val name: String,
    val address: String,
    val port: Int,
    val platform: String,
    val protocolVersion: Int
)

/**
 * Plattformunabhängige Schnittstelle zur Geräteerkennung.
 *
 * Nur der Host ruft [startAdvertising] auf. Teilnehmer rufen ausschließlich [startBrowsing] auf
 * und sind selbst nie sichtbar. Alle Netzwerkoperationen laufen außerhalb des UI-Threads.
 */
interface ServiceDiscovery {
    /** Aktuell gefundene Hosts. Ist leer, solange nicht gesucht wird. */
    val hosts: StateFlow<List<DiscoveredHost>>

    /** Beginnt, diesen Host im Netz anzukündigen. */
    fun startAdvertising(advertisement: HostAdvertisement)

    /** Beendet die Ankündigung vollständig. */
    fun stopAdvertising()

    /** Beginnt die Suche nach Hosts. */
    fun startBrowsing()

    /** Beendet die Suche und leert [hosts]. */
    fun stopBrowsing()
}

/** Erzeugt die plattformspezifische [ServiceDiscovery]. */
expect fun createServiceDiscovery(): ServiceDiscovery

/**
 * Verwaltet die Liste gefundener Hosts. Wird von allen Plattform-Implementierungen und
 * dem Fake gemeinsam genutzt, damit Hinzufügen/Entfernen/Duplikate überall gleich funktioniert.
 */
class HostRegistry {
    private val _hosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val hosts: StateFlow<List<DiscoveredHost>> = _hosts

    /** Fügt den Host hinzu oder ersetzt einen vorhandenen mit gleichem [DiscoveredHost.serviceName]. */
    fun addOrUpdate(host: DiscoveredHost) {
        _hosts.update { current -> current.filterNot { it.serviceName == host.serviceName } + host }
    }

    fun remove(serviceName: String) {
        _hosts.update { current -> current.filterNot { it.serviceName == serviceName } }
    }

    fun clear() {
        _hosts.value = emptyList()
    }
}

/**
 * Netzwerkfreie Implementierung für Tests und Vorschau. Mit [simulateHostFound] lassen sich
 * gefundene Hosts nachstellen; [advertisement] zeigt, ob und wie sich der Host ankündigt.
 */
class FakeServiceDiscovery : ServiceDiscovery {
    private val registry = HostRegistry()
    override val hosts: StateFlow<List<DiscoveredHost>> = registry.hosts

    var advertisement: HostAdvertisement? = null
        private set
    var isBrowsing: Boolean = false
        private set

    override fun startAdvertising(advertisement: HostAdvertisement) {
        this.advertisement = advertisement
    }

    override fun stopAdvertising() {
        advertisement = null
    }

    override fun startBrowsing() {
        isBrowsing = true
    }

    override fun stopBrowsing() {
        isBrowsing = false
        registry.clear()
    }

    /** Stellt nach, dass ein Host gefunden wurde (nur wenn gerade gesucht wird). */
    fun simulateHostFound(host: DiscoveredHost) {
        if (isBrowsing) registry.addOrUpdate(host)
    }

    fun simulateHostLost(serviceName: String) = registry.remove(serviceName)
}
