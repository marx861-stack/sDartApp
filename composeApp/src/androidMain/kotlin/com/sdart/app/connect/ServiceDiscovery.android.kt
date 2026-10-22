package com.sdart.app.connect

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.sdart.app.requireAppContext
import kotlinx.coroutines.flow.StateFlow

actual fun createServiceDiscovery(): ServiceDiscovery = AndroidServiceDiscovery(requireAppContext())

/**
 * Android-Implementierung mit NsdManager (mDNS/DNS-SD des Systems).
 * Ankündigung und Suche laufen nur, solange sie gestartet sind.
 * Auflösungen werden nacheinander abgearbeitet, da Android nur eine gleichzeitig erlaubt.
 */
class AndroidServiceDiscovery(context: Context) : ServiceDiscovery {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val registry = HostRegistry()
    override val hosts: StateFlow<List<DiscoveredHost>> = registry.hosts

    private val lock = Any()
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    override fun startAdvertising(advertisement: HostAdvertisement) {
        synchronized(lock) {
            if (registrationListener != null) return
            val info = NsdServiceInfo().apply {
                serviceName = advertisement.name
                serviceType = CONNECT_SERVICE_TYPE
                port = advertisement.port
                setAttribute("hostId", advertisement.hostId)
                setAttribute("platform", advertisement.platform)
                setAttribute("protocol", advertisement.protocolVersion.toString())
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {}
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    println("NSD registration failed: $errorCode")
                    synchronized(lock) { registrationListener = null }
                }
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    println("NSD unregistration failed: $errorCode")
                }
            }
            registrationListener = listener
            nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    override fun stopAdvertising() {
        synchronized(lock) {
            val listener = registrationListener ?: return
            registrationListener = null
            try {
                nsdManager.unregisterService(listener)
            } catch (e: IllegalArgumentException) {
                println("NSD unregister: ${e.message}")
            }
        }
    }

    override fun startBrowsing() {
        synchronized(lock) {
            if (discoveryListener != null) return
            acquireMulticastLock()
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onServiceFound(info: NsdServiceInfo) = enqueueResolve(info)
                override fun onServiceLost(info: NsdServiceInfo) = registry.remove(info.serviceName)
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    println("NSD discovery start failed: $errorCode")
                    synchronized(lock) { discoveryListener = null }
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    println("NSD discovery stop failed: $errorCode")
                }
            }
            discoveryListener = listener
            nsdManager.discoverServices(CONNECT_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    override fun stopBrowsing() {
        synchronized(lock) {
            val listener = discoveryListener
            discoveryListener = null
            resolveQueue.clear()
            if (listener != null) {
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: IllegalArgumentException) {
                    println("NSD stop discovery: ${e.message}")
                }
            }
            releaseMulticastLock()
        }
        registry.clear()
    }

    private fun enqueueResolve(info: NsdServiceInfo) {
        synchronized(lock) {
            resolveQueue.addLast(info)
            resolveNextLocked()
        }
    }

    /** Muss unter [lock] aufgerufen werden. */
    @Suppress("DEPRECATION") // resolveService ist ab API 34 deprecated, aber ab minSdk 24 der einzige Weg
    private fun resolveNextLocked() {
        if (resolving) return
        val next = resolveQueue.removeFirstOrNull() ?: return
        resolving = true
        nsdManager.resolveService(next, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                println("NSD resolve failed: $errorCode")
                finishResolve()
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val address = info.host?.hostAddress
                if (address != null) {
                    registry.addOrUpdate(
                        DiscoveredHost(
                            serviceName = info.serviceName,
                            hostId = info.attribute("hostId") ?: info.serviceName,
                            name = info.serviceName,
                            address = address,
                            port = info.port,
                            platform = info.attribute("platform") ?: "unknown",
                            protocolVersion = info.attribute("protocol")?.toIntOrNull() ?: 0
                        )
                    )
                }
                finishResolve()
            }
        })
    }

    private fun finishResolve() {
        synchronized(lock) {
            resolving = false
            resolveNextLocked()
        }
    }

    private fun NsdServiceInfo.attribute(key: String): String? =
        attributes[key]?.toString(Charsets.UTF_8)

    /** Manche Geräte filtern Multicast-Pakete im Energiesparmodus; das Lock hält sie während der Suche offen. */
    private fun acquireMulticastLock() {
        val lock = wifiManager.createMulticastLock("sdart-discovery")
        lock.setReferenceCounted(false)
        lock.acquire()
        multicastLock = lock
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
    }
}
