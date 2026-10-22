package com.sdart.app.connect

/**
 * Listener für eingehende Server-Ereignisse (Netzwerk-Ebene).
 */
interface HostServerListener {
    fun onClientConnected(connectionId: String, remoteAddress: String)
    fun onClientDisconnected(connectionId: String)
    fun onMessageReceived(connectionId: String, text: String)
    fun onBinaryReceived(connectionId: String, bytes: ByteArray)
    fun onError(connectionId: String?, error: Throwable)
}

/**
 * Plattformunabhängige Schnittstelle für den WebSocket-Server des Hosts.
 */
interface HostServer {
    val port: Int
    val isRunning: Boolean

    /**
     * Startet den WebSocket-Server auf dem angegebenen Port (0 = automatischer freier Port).
     * @return der tatsächlich gebundene Port.
     */
    fun start(port: Int = 0): Int

    /** Stoppt den Server und schließt alle aktiven Verbindungen. */
    fun stop()

    /** Sendet eine Textnachricht an eine spezifische Verbindung. */
    fun send(connectionId: String, text: String): Boolean

    /** Sendet Binärdaten (z. B. Frame) an eine Verbindung. */
    fun sendBinary(connectionId: String, bytes: ByteArray): Boolean

    /** Trennt eine spezifische Verbindung. */
    fun disconnect(connectionId: String)

    fun setListener(listener: HostServerListener?)
}

/** Erzeugt die plattformspezifische Implementierung des [HostServer]. */
expect fun createHostServer(): HostServer

/**
 * Fake-Implementierung für Unit-Tests ohne Sockets.
 */
class FakeHostServer : HostServer {
    override var port: Int = 0
        private set
    override var isRunning: Boolean = false
        private set

    private var listener: HostServerListener? = null
    val sentMessages = mutableListOf<Pair<String, String>>()
    val sentBinary = mutableListOf<Pair<String, ByteArray>>()
    val disconnectedClients = mutableListOf<String>()

    override fun start(port: Int): Int {
        this.port = if (port == 0) 8085 else port
        isRunning = true
        return this.port
    }

    override fun stop() {
        isRunning = false
        port = 0
    }

    override fun send(connectionId: String, text: String): Boolean {
        if (!isRunning) return false
        sentMessages.add(connectionId to text)
        return true
    }

    override fun sendBinary(connectionId: String, bytes: ByteArray): Boolean {
        if (!isRunning) return false
        sentBinary.add(connectionId to bytes)
        return true
    }

    override fun disconnect(connectionId: String) {
        disconnectedClients.add(connectionId)
        listener?.onClientDisconnected(connectionId)
    }

    override fun setListener(listener: HostServerListener?) {
        this.listener = listener
    }

    // Test-Hilfsmethoden
    fun simulateConnect(connectionId: String, remoteAddress: String = "127.0.0.1") {
        listener?.onClientConnected(connectionId, remoteAddress)
    }

    fun simulateMessage(connectionId: String, text: String) {
        listener?.onMessageReceived(connectionId, text)
    }

    fun simulateBinary(connectionId: String, bytes: ByteArray) {
        listener?.onBinaryReceived(connectionId, bytes)
    }

    fun simulateDisconnect(connectionId: String) {
        listener?.onClientDisconnected(connectionId)
    }
}
