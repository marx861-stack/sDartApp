package com.example.myapplication.connect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

actual fun createWebSocketClient(): CompanionWebSocketClient = JavaWebSocketClient()

/**
 * JVM/Desktop-Implementierung von [CompanionWebSocketClient] basierend auf org.java-websocket.
 */
class JavaWebSocketClient : CompanionWebSocketClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var internalClient: InternalClient? = null

    override val isConnected: Boolean
        get() = internalClient?.isOpen == true

    override fun connect(url: String, listener: WebSocketClientListener) {
        disconnect()

        scope.launch {
            try {
                val client = InternalClient(URI(url), listener)
                client.isTcpNoDelay = true
                client.connectionLostTimeout = 10
                internalClient = client
                client.connect()
            } catch (e: Exception) {
                listener.onError(e)
            }
        }
    }

    override fun send(text: String): Boolean {
        val client = internalClient ?: return false
        return if (client.isOpen) {
            try {
                client.send(text)
                true
            } catch (e: Exception) {
                false
            }
        } else false
    }

    override fun sendBinary(bytes: ByteArray): Boolean {
        val client = internalClient ?: return false
        return if (client.isOpen) {
            try {
                client.send(bytes)
                true
            } catch (e: Exception) {
                false
            }
        } else false
    }

    override fun disconnect() {
        try {
            internalClient?.close()
        } catch (_: Exception) {}
        internalClient = null
    }

    private class InternalClient(
        uri: URI,
        private val listener: WebSocketClientListener
    ) : WebSocketClient(uri) {

        override fun onOpen(handshakedata: ServerHandshake?) {
            listener.onOpen()
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            listener.onClose(code, reason, remote)
        }

        override fun onMessage(message: String) {
            listener.onMessage(message)
        }

        override fun onMessage(bytes: ByteBuffer) {
            val array = ByteArray(bytes.remaining())
            bytes.get(array)
            listener.onBinary(array)
        }

        override fun onError(ex: Exception) {
            listener.onError(ex)
        }
    }
}
