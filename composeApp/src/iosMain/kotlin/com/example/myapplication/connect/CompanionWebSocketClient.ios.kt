package com.example.myapplication.connect

actual fun createWebSocketClient(): CompanionWebSocketClient = IosWebSocketClientStub()

/**
 * iOS-Stub für CompanionWebSocketClient (wird in Schritt 8 via NSURLSessionWebSocketTask angebunden).
 */
class IosWebSocketClientStub : CompanionWebSocketClient {
    override val isConnected: Boolean = false
    override fun connect(url: String, listener: WebSocketClientListener) {}
    override fun send(text: String): Boolean = false
    override fun sendBinary(bytes: ByteArray): Boolean = false
    override fun disconnect() {}
}
