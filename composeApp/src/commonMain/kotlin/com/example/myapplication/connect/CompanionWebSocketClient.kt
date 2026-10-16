package com.example.myapplication.connect

/**
 * Listener für WebSocket-Client-Ereignisse.
 */
interface WebSocketClientListener {
    fun onOpen()
    fun onClose(code: Int, reason: String?, remote: Boolean)
    fun onMessage(text: String)
    fun onBinary(bytes: ByteArray)
    fun onError(error: Throwable)
}

/**
 * Plattformunabhängige Schnittstelle für den WebSocket-Client eines Teilnehmers.
 */
interface CompanionWebSocketClient {
    val isConnected: Boolean

    /**
     * Verbindet sich asynchron mit der angegebenen WebSocket-URL (z. B. "ws://192.168.1.50:8085").
     */
    fun connect(url: String, listener: WebSocketClientListener)

    /** Sendet eine Textnachricht. */
    fun send(text: String): Boolean

    /** Sendet Binärdaten (z. B. JPEG-Frame). */
    fun sendBinary(bytes: ByteArray): Boolean

    /** Trennt die Verbindung. */
    fun disconnect()
}

/** Erzeugt den plattformspezifischen [CompanionWebSocketClient]. */
expect fun createWebSocketClient(): CompanionWebSocketClient

/**
 * Fake-Implementierung für Unit-Tests ohne Netzwerk-Sockets.
 */
class FakeWebSocketClient : CompanionWebSocketClient {
    override var isConnected: Boolean = false
        private set

    var connectedUrl: String? = null
        private set

    private var listener: WebSocketClientListener? = null
    val sentMessages = mutableListOf<String>()
    val sentBinary = mutableListOf<ByteArray>()

    override fun connect(url: String, listener: WebSocketClientListener) {
        this.connectedUrl = url
        this.listener = listener
        this.isConnected = true
        listener.onOpen()
    }

    override fun send(text: String): Boolean {
        if (!isConnected) return false
        sentMessages.add(text)
        return true
    }

    override fun sendBinary(bytes: ByteArray): Boolean {
        if (!isConnected) return false
        sentBinary.add(bytes)
        return true
    }

    override fun disconnect() {
        if (isConnected) {
            isConnected = false
            listener?.onClose(1000, "Normal closure", false)
            listener = null
        }
    }

    // Test-Hilfsmethoden
    fun simulateIncomingMessage(text: String) {
        listener?.onMessage(text)
    }

    fun simulateIncomingBinary(bytes: ByteArray) {
        listener?.onBinary(bytes)
    }

    fun simulateDisconnect(remote: Boolean = true) {
        isConnected = false
        listener?.onClose(1006, "Connection lost", remote)
    }

    fun simulateError(error: Throwable) {
        listener?.onError(error)
    }
}
