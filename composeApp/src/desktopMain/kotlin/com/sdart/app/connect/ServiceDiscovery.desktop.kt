package com.sdart.app.connect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

private const val JMDNS_SERVICE_TYPE = "$CONNECT_SERVICE_TYPE.local."

actual fun createServiceDiscovery(): ServiceDiscovery = DesktopServiceDiscovery()

/**
 * Desktop-Implementierung mit JmDNS. Ankündigung und Suche nutzen getrennte JmDNS-Instanzen,
 * die nur existieren, solange sie aktiv gestartet sind (nichts läuft im Leerlauf).
 * Das Erzeugen von JmDNS blockiert und läuft deshalb auf Dispatchers.IO.
 */
class DesktopServiceDiscovery : ServiceDiscovery {
    private val registry = HostRegistry()
    override val hosts: StateFlow<List<DiscoveredHost>> = registry.hosts

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private var advertiser: JmDNS? = null
    private var browser: JmDNS? = null
    private var browserListener: ServiceListener? = null

    /** Erhöht bei jedem Start/Stop, damit verspätet fertige Starts verworfen werden. */
    private var advertiseGeneration = 0
    private var browseGeneration = 0

    override fun startAdvertising(advertisement: HostAdvertisement) {
        val generation = synchronized(lock) { ++advertiseGeneration }
        scope.launch {
            try {
                val jmdns = JmDNS.create(findLocalAddress(), advertisement.name)
                val txt = mapOf(
                    "hostId" to advertisement.hostId,
                    "platform" to advertisement.platform,
                    "protocol" to advertisement.protocolVersion.toString()
                )
                val info = ServiceInfo.create(
                    JMDNS_SERVICE_TYPE, advertisement.name, advertisement.port, 0, 0, txt
                )
                jmdns.registerService(info)
                val stillWanted = synchronized(lock) {
                    if (generation == advertiseGeneration) {
                        advertiser = jmdns
                        true
                    } else {
                        false
                    }
                }
                if (!stillWanted) closeQuietly(jmdns)
            } catch (e: Exception) {
                println("mDNS advertising failed: ${e.message}")
            }
        }
    }

    override fun stopAdvertising() {
        val toClose = synchronized(lock) {
            advertiseGeneration++
            advertiser.also { advertiser = null }
        }
        if (toClose != null) scope.launch { closeQuietly(toClose) }
    }

    override fun startBrowsing() {
        val generation = synchronized(lock) { ++browseGeneration }
        scope.launch {
            try {
                val jmdns = JmDNS.create(findLocalAddress())
                val listener = createListener(jmdns)
                val stillWanted = synchronized(lock) {
                    if (generation == browseGeneration) {
                        browser = jmdns
                        browserListener = listener
                        true
                    } else {
                        false
                    }
                }
                if (stillWanted) {
                    jmdns.addServiceListener(JMDNS_SERVICE_TYPE, listener)
                } else {
                    closeQuietly(jmdns)
                }
            } catch (e: Exception) {
                println("mDNS browsing failed: ${e.message}")
            }
        }
    }

    override fun stopBrowsing() {
        val toClose = synchronized(lock) {
            browseGeneration++
            browserListener = null
            browser.also { browser = null }
        }
        registry.clear()
        if (toClose != null) scope.launch { closeQuietly(toClose) }
    }

    private fun createListener(jmdns: JmDNS): ServiceListener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            // Details (Adresse, Port, TXT) werden asynchron nachgeladen -> serviceResolved
            jmdns.requestServiceInfo(event.type, event.name, true)
        }

        override fun serviceRemoved(event: ServiceEvent) {
            registry.remove(event.name)
        }

        override fun serviceResolved(event: ServiceEvent) {
            val info = event.info
            val address = info.inet4Addresses.firstOrNull()?.hostAddress ?: return
            registry.addOrUpdate(
                DiscoveredHost(
                    serviceName = info.name,
                    hostId = info.getPropertyString("hostId") ?: info.name,
                    name = info.name,
                    address = address,
                    port = info.port,
                    platform = info.getPropertyString("platform") ?: "unknown",
                    protocolVersion = info.getPropertyString("protocol")?.toIntOrNull() ?: 0
                )
            )
        }
    }

    private fun closeQuietly(jmdns: JmDNS) {
        try {
            jmdns.unregisterAllServices()
            jmdns.close()
        } catch (e: Exception) {
            println("mDNS close failed: ${e.message}")
        }
    }

    /**
     * Wählt die IPv4-Adresse des aktiven LAN/WLAN-Adapters (bevorzugt Site-Local, z. B. 192.168.x.x).
     * `InetAddress.getLocalHost()` liefert unter Windows mit VPN/virtuellen Adaptern oft die falsche Adresse.
     */
    private fun findLocalAddress(): InetAddress {
        val candidates = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && it.supportsMulticast() }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
        return candidates.firstOrNull { it.isSiteLocalAddress }
            ?: candidates.firstOrNull()
            ?: InetAddress.getLocalHost()
    }
}
