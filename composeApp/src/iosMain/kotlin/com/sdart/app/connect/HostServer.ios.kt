package com.sdart.app.connect

actual fun createHostServer(): HostServer = IosHostServerStub()

/**
 * iOS-Stub für HostServer (wird bei nativer iOS-Netzwerk-Anbindung implementiert).
 */
class IosHostServerStub : HostServer {
    override val port: Int = 0
    override val isRunning: Boolean = false
    override fun start(port: Int): Int = 0
    override fun stop() {}
    override fun send(connectionId: String, text: String): Boolean = false
    override fun sendBinary(connectionId: String, bytes: ByteArray): Boolean = false
    override fun disconnect(connectionId: String) {}
    override fun setListener(listener: HostServerListener?) {}
}
