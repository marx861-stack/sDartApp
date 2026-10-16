package com.example.myapplication.connect

import com.example.myapplication.currentTimeMillis
import com.example.myapplication.loadAppPreference
import com.example.myapplication.saveAppPreference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_DEVICE_ID_KEY = "companion_device_id_v1"

/**
 * Zustand eines Teilnehmers.
 */
sealed interface ParticipantState {
    data object Idle : ParticipantState

    data class Searching(val role: DeviceRole) : ParticipantState

    data class Connecting(val host: DiscoveredHost, val role: DeviceRole) : ParticipantState

    data class Connected(
        val host: DiscoveredHost,
        val role: DeviceRole,
        val sessionId: String,
        val hostName: String
    ) : ParticipantState

    data class Rejected(
        val host: DiscoveredHost,
        val role: DeviceRole,
        val reason: RejectReason
    ) : ParticipantState

    data class Disconnected(
        val role: DeviceRole,
        val message: String
    ) : ParticipantState
}

/**
 * Manager für Teilnehmer (Handy/Tablet), die sich mit einem Host verbinden.
 * Sucht per mDNS nach dem Host und verbindet sich bei genau 1 gefundenen Host vollautomatisch.
 */
class ParticipantSessionManager(
    private val client: CompanionWebSocketClient = createWebSocketClient(),
    val discovery: ServiceDiscovery = createServiceDiscovery(),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : WebSocketClientListener {

    private val _state = MutableStateFlow<ParticipantState>(ParticipantState.Idle)
    val state: StateFlow<ParticipantState> = _state.asStateFlow()

    private val _latestGameState = MutableStateFlow<String?>(null)
    val latestGameState: StateFlow<String?> = _latestGameState.asStateFlow()

    private var activeRole: DeviceRole = DeviceRole.CAMERA
    private var activeDeviceName: String = "Mein Gerät"
    private var activePlatform: String = "android"
    private var activeCapabilities: Set<DeviceCapability> = emptySet()
    private var targetHost: DiscoveredHost? = null

    private var discoveryJob: Job? = null

    /**
     * Startet die Suche nach Hosts im lokalen Netzwerk für die gewählte Rolle.
     * Kündigt sich selbst NIE an (bleibt für andere unsichtbar).
     */
    fun selectRole(
        role: DeviceRole,
        deviceName: String = "Handy",
        platform: String = "android",
        capabilities: Set<DeviceCapability> = setOf(DeviceCapability.CAMERA, DeviceCapability.ML_DETECTION)
    ) {
        disconnect()

        activeRole = role
        activeDeviceName = deviceName
        activePlatform = platform
        activeCapabilities = capabilities
        _state.value = ParticipantState.Searching(role)

        discovery.startBrowsing()

        discoveryJob = coroutineScope.launch {
            discovery.hosts.collect { hostList ->
                val currentState = _state.value
                if (currentState is ParticipantState.Searching && hostList.size == 1) {
                    // Genau 1 Host gefunden -> Automatisch und nahtlos verbinden!
                    val singleHost = hostList.first()
                    connectToHost(singleHost, currentState.role)
                }
            }
        }
    }

    /**
     * Verbindet sich mit einem spezifischen Host (entweder automatisch ausgelöst oder manuell gewählt).
     */
    fun connectToHost(host: DiscoveredHost, role: DeviceRole) {
        discoveryJob?.cancel()
        targetHost = host
        activeRole = role
        _state.value = ParticipantState.Connecting(host, role)

        val wsUrl = "ws://${host.address}:${host.port}"
        client.connect(wsUrl, this)
    }

    /**
     * Trennt die Verbindung und versetzt den Client in den Leerlauf.
     */
    fun disconnect() {
        discoveryJob?.cancel()
        discoveryJob = null
        discovery.stopBrowsing()

        if (client.isConnected) {
            client.send(ConnectProtocol.encode(ConnectMessage.Bye))
        }
        client.disconnect()
        targetHost = null
        _latestGameState.value = null
        _state.value = ParticipantState.Idle
    }

    // --- Sende-Methoden für Rollen ---

    fun sendDartHit(segment: Int, multiplier: Int, dartIndex: Int, confidence: Float, x: Float, y: Float): Boolean {
        if (_state.value !is ParticipantState.Connected) return false
        val msg = ConnectMessage.DartHit(segment, multiplier, dartIndex, confidence, x, y)
        return client.send(ConnectProtocol.encode(msg))
    }

    fun sendBoardCleared(): Boolean {
        if (_state.value !is ParticipantState.Connected) return false
        return client.send(ConnectProtocol.encode(ConnectMessage.BoardCleared))
    }

    fun sendScoreInput(segment: Int, multiplier: Int, x: Float? = null, y: Float? = null): Boolean {
        if (_state.value !is ParticipantState.Connected) return false
        val msg = ConnectMessage.ScoreInput(segment, multiplier, x, y)
        return client.send(ConnectProtocol.encode(msg))
    }

    fun sendUndo(): Boolean {
        if (_state.value !is ParticipantState.Connected) return false
        return client.send(ConnectProtocol.encode(ConnectMessage.Undo))
    }

    fun sendPreviewFrame(jpegBytes: ByteArray): Boolean {
        if (_state.value !is ParticipantState.Connected) return false
        return client.sendBinary(jpegBytes)
    }

    // --- WebSocketClientListener Implementierung ---

    override fun onOpen() {
        println("[ParticipantSessionManager] WebSocket opened to host: ${targetHost?.name} (${targetHost?.address}:${targetHost?.port})")
        val deviceId = getOrCreateDeviceId()
        val hello = ConnectMessage.Hello(
            deviceId = deviceId,
            deviceName = activeDeviceName,
            platform = activePlatform,
            role = activeRole,
            capabilities = activeCapabilities,
            protocolVersion = CONNECT_PROTOCOL_VERSION
        )
        println("[ParticipantSessionManager] Sending Hello: deviceId=$deviceId, role=$activeRole, devName=$activeDeviceName")
        client.send(ConnectProtocol.encode(hello))
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        val currentState = _state.value
        println("[ParticipantSessionManager] Connection closed: code=$code, reason=$reason, remote=$remote, state=${currentState::class.simpleName}")
        if (currentState is ParticipantState.Connected || currentState is ParticipantState.Connecting) {
            _state.value = ParticipantState.Disconnected(
                role = activeRole,
                message = if (remote) "Verbindung vom Host beendet." else (reason ?: "Getrennt.")
            )
        }
    }

    override fun onMessage(text: String) {
        val message = ConnectProtocol.decode(text)
        if (message == null) {
            println("[ParticipantSessionManager] WARN: Failed to decode message from host: $text")
            return
        }
        val currentHost = targetHost ?: return
        println("[ParticipantSessionManager] Message from host: ${message::class.simpleName}")

        when (message) {
            is ConnectMessage.Welcome -> {
                println("[ParticipantSessionManager] Welcome received! Connected to session: ${message.sessionId}")
                _state.value = ParticipantState.Connected(
                    host = currentHost,
                    role = activeRole,
                    sessionId = message.sessionId,
                    hostName = message.hostName
                )
            }
            is ConnectMessage.Rejected -> {
                println("[ParticipantSessionManager] Rejected by host: ${message.reason}")
                _state.value = ParticipantState.Rejected(
                    host = currentHost,
                    role = activeRole,
                    reason = message.reason
                )
                client.disconnect()
            }
            is ConnectMessage.GameStateUpdate -> {
                _latestGameState.value = message.snapshotJson
            }
            is ConnectMessage.Ping -> {
                client.send(ConnectProtocol.encode(ConnectMessage.Pong(currentTimeMillis())))
            }
            is ConnectMessage.Bye -> {
                println("[ParticipantSessionManager] Bye received from host.")
                _state.value = ParticipantState.Disconnected(
                    role = activeRole,
                    message = "Host hat die Session beendet."
                )
                client.disconnect()
            }
            else -> {}
        }
    }

    override fun onBinary(bytes: ByteArray) {}

    override fun onError(error: Throwable) {
        println("[ParticipantSessionManager] WebSocket error: ${error.message}")
        val currentState = _state.value
        if (currentState is ParticipantState.Connecting) {
            _state.value = ParticipantState.Disconnected(
                role = currentState.role,
                message = "Verbindungsfehler: ${error.message ?: "Server nicht erreichbar"}"
            )
        }
    }

    private fun getOrCreateDeviceId(): String {
        val existing = loadAppPreference(PREFS_DEVICE_ID_KEY)
        if (!existing.isNullOrBlank()) return existing
        val newId = "dev-${currentTimeMillis()}"
        saveAppPreference(PREFS_DEVICE_ID_KEY, newId)
        return newId
    }
}
