package com.sdart.app.connect

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

actual fun createHostServer(): HostServer = AndroidJavaWebSocketHostServer()

/**
 * Android-Implementierung von [HostServer] basierend auf org.java-websocket.
 */
class AndroidJavaWebSocketHostServer : HostServer {
    private var internalServer: InternalServer? = null
    private var listener: HostServerListener? = null

    override var port: Int = 0
        private set

    override val isRunning: Boolean
        get() = internalServer != null

    override fun start(port: Int): Int {
        stop()

        val targetPort = if (port == 0) {
            ServerSocket(0).use { it.localPort }
        } else {
            port
        }

        this.port = targetPort
        val server = InternalServer(targetPort)
        server.isReuseAddr = true
        server.isTcpNoDelay = true
        server.start()
        internalServer = server
        return targetPort
    }

    override fun stop() {
        try {
            internalServer?.stop(1000)
        } catch (_: Exception) {
        } finally {
            internalServer = null
            port = 0
        }
    }

    override fun send(connectionId: String, text: String): Boolean {
        val server = internalServer ?: return false
        val conn = server.connectionsMap[connectionId] ?: return false
        return try {
            conn.send(text)
            true
        } catch (e: Exception) {
            listener?.onError(connectionId, e)
            false
        }
    }

    override fun sendBinary(connectionId: String, bytes: ByteArray): Boolean {
        val server = internalServer ?: return false
        val conn = server.connectionsMap[connectionId] ?: return false
        return try {
            conn.send(bytes)
            true
        } catch (e: Exception) {
            listener?.onError(connectionId, e)
            false
        }
    }

    override fun disconnect(connectionId: String) {
        val server = internalServer ?: return
        val conn = server.connectionsMap[connectionId] ?: return
        try {
            conn.close()
        } catch (_: Exception) {}
    }

    override fun setListener(listener: HostServerListener?) {
        this.listener = listener
    }

    private inner class InternalServer(listenPort: Int) : WebSocketServer(InetSocketAddress(listenPort)) {
        val connectionsMap = ConcurrentHashMap<String, WebSocket>()

        override fun onOpen(conn: WebSocket, handshake: ClientHandshake?) {
            val connectionId = UUID.randomUUID().toString()
            conn.setAttachment(connectionId)
            connectionsMap[connectionId] = conn
            val remoteAddr = conn.remoteSocketAddress?.address?.hostAddress ?: "unknown"
            listener?.onClientConnected(connectionId, remoteAddr)
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
            val connectionId: String? = conn.getAttachment()
            if (connectionId != null) {
                connectionsMap.remove(connectionId)
                listener?.onClientDisconnected(connectionId)
            }
        }

        override fun onMessage(conn: WebSocket, message: String) {
            val connectionId: String? = conn.getAttachment()
            if (connectionId != null) {
                listener?.onMessageReceived(connectionId, message)
            }
        }

        override fun onMessage(conn: WebSocket, message: ByteBuffer) {
            val connectionId: String? = conn.getAttachment()
            if (connectionId != null) {
                val bytes = ByteArray(message.remaining())
                message.get(bytes)
                listener?.onBinaryReceived(connectionId, bytes)
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            val connectionId: String? = conn?.getAttachment()
            listener?.onError(connectionId, ex)
        }

        override fun onStart() {
            // Server bereit
        }
    }
}
