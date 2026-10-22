package com.example.myapplication.connect

import com.example.myapplication.currentTimeMillis
import com.example.myapplication.loadAppPreference
import com.example.myapplication.saveAppPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_TRUSTED_DEVICES_KEY = "companion_trusted_devices_v1"
private const val PREFS_TRUSTED_DEVICES_V2_KEY = "companion_trusted_devices_v2"
private const val PREFS_HOST_ID_KEY = "companion_host_id_v1"

/**
 * Ein gemerktes, vertrauenswürdiges Gerät.
 */
@Serializable
data class TrustedDevice(
    val deviceId: String,
    val name: String,
    val platform: String,
    val addedAtMillis: Long = currentTimeMillis()
)

/**
 * Ein erfolgreich verbundener und autorisierter Teilnehmer.
 */
data class ConnectedParticipant(
    val connectionId: String,
    val deviceId: String,
    val name: String,
    val platform: String,
    val role: DeviceRole,
    val capabilities: Set<DeviceCapability>,
    val remoteAddress: String,
    val connectedAtMillis: Long = currentTimeMillis()
)

/**
 * Ausstehende Autorisierungs-Anfrage für ein unbekanntes Gerät.
 */
data class PendingAuthorization(
    val connectionId: String,
    val hello: ConnectMessage.Hello,
    val remoteAddress: String
)

/**
 * Zustand des Hosts.
 */
sealed interface HostState {
    data object Inactive : HostState
    data class Active(
        val hostId: String,
        val hostName: String,
        val port: Int,
        val participants: List<ConnectedParticipant>
    ) : HostState
}

/**
 * Zentraler Manager für den Host-Modus (Spielerdaten & Spielstand-Inhaber).
 * Steuert WebSocket-Server, mDNS-Ankündigung, Teilnehmer-Autorisierung und Nachrichten-Routing.
 */
class SessionManager(
    private val hostServer: HostServer = createHostServer(),
    private val discovery: ServiceDiscovery = createServiceDiscovery()
) : HostServerListener {

    private val _hostState = MutableStateFlow<HostState>(HostState.Inactive)
    val hostState: StateFlow<HostState> = _hostState.asStateFlow()

    private val _pendingAuth = MutableStateFlow<PendingAuthorization?>(null)
    val pendingAuthorization: StateFlow<PendingAuthorization?> = _pendingAuth.asStateFlow()

    private val _trustedDevices = MutableStateFlow<List<TrustedDevice>>(loadTrustedDevices())
    val trustedDevices: StateFlow<List<TrustedDevice>> = _trustedDevices.asStateFlow()

    private val _trustedDeviceIds = MutableStateFlow<Set<String>>(_trustedDevices.value.map { it.deviceId }.toSet())
    val trustedDeviceIds: StateFlow<Set<String>> = _trustedDeviceIds.asStateFlow()

    private val _latestPreviewFrame = MutableStateFlow<ByteArray?>(null)
    val latestPreviewFrame: StateFlow<ByteArray?> = _latestPreviewFrame.asStateFlow()

    var activeCameraConnectionId: String? = null

    // Temporäre Speicherung von Verbindungsadressen vor dem Hello
    private val connectionAddresses = mutableMapOf<String, String>()

    // Event-Callbacks für Spiellogik (DartViewModel)
    var onDartHit: ((ConnectMessage.DartHit) -> Unit)? = null
    var onBoardCleared: (() -> Unit)? = null
    var onScoreInput: ((ConnectMessage.ScoreInput) -> Unit)? = null
    var onUndo: (() -> Unit)? = null

    init {
        hostServer.setListener(this)
    }

    /**
     * Startet den Host: Bindet den WebSocket-Server und kündigt ihn via mDNS an.
     * Erst ab diesem Zeitpunkt ist das Gerät im lokalen Netzwerk sichtbar.
     */
    fun startHost(
        hostName: String,
        preferredPort: Int = 0,
        platform: String = "desktop"
    ): Int {
        stopHost()

        val hostId = getOrCreateHostId()
        val boundPort = hostServer.start(preferredPort)

        discovery.startAdvertising(
            HostAdvertisement(
                hostId = hostId,
                name = hostName,
                port = boundPort,
                platform = platform,
                protocolVersion = CONNECT_PROTOCOL_VERSION
            )
        )

        _hostState.value = HostState.Active(
            hostId = hostId,
            hostName = hostName,
            port = boundPort,
            participants = emptyList()
        )

        return boundPort
    }

    /**
     * Beendet den Host vollständig: Beendet mDNS-Ankündigung und trennt alle Verbindungen mit Bye.
     */
    fun stopHost() {
        val current = _hostState.value
        if (current is HostState.Active) {
            current.participants.forEach { p ->
                hostServer.send(p.connectionId, ConnectProtocol.encode(ConnectMessage.Bye))
            }
        }

        discovery.stopAdvertising()
        hostServer.stop()
        connectionAddresses.clear()
        _pendingAuth.value = null
        _latestPreviewFrame.value = null
        _hostState.value = HostState.Inactive
    }

    /**
     * Bestätigt die ausstehende Autorisierung für das verbundene Gerät.
     */
    fun approvePendingAuthorization(remember: Boolean = true) {
        val pending = _pendingAuth.value ?: return
        _pendingAuth.value = null

        if (remember) {
            trustDevice(
                deviceId = pending.hello.deviceId,
                name = pending.hello.deviceName,
                platform = pending.hello.platform
            )
        }

        acceptParticipant(pending.connectionId, pending.hello, pending.remoteAddress)
    }

    /**
     * Weist die ausstehende Autorisierung ab.
     */
    fun rejectPendingAuthorization(reason: RejectReason = RejectReason.DECLINED_BY_USER) {
        val pending = _pendingAuth.value ?: return
        _pendingAuth.value = null

        hostServer.send(pending.connectionId, ConnectProtocol.encode(ConnectMessage.Rejected(reason)))
        hostServer.disconnect(pending.connectionId)
    }

    /**
     * Trennt einen Teilnehmer manuell vom Host.
     */
    fun disconnectParticipant(connectionId: String) {
        hostServer.send(connectionId, ConnectProtocol.encode(ConnectMessage.Bye))
        hostServer.disconnect(connectionId)
        removeParticipant(connectionId)
    }

    /**
     * Sendet den aktuellen Spielstand an alle Anzeige- und Eingabe-Geräte.
     */
    fun broadcastGameState(snapshotJson: String) {
        val current = _hostState.value as? HostState.Active ?: return
        val messageText = ConnectProtocol.encode(ConnectMessage.GameStateUpdate(snapshotJson))
        current.participants
            .filter { it.role == DeviceRole.DISPLAY || it.role == DeviceRole.INPUT }
            .forEach { participant ->
                hostServer.send(participant.connectionId, messageText)
            }
    }

    // --- HostServerListener Implementierung ---

    override fun onClientConnected(connectionId: String, remoteAddress: String) {
        println("[SessionManager] Client connected: conn=$connectionId from $remoteAddress")
        connectionAddresses[connectionId] = remoteAddress
    }

    override fun onClientDisconnected(connectionId: String) {
        println("[SessionManager] Client disconnected: conn=$connectionId")
        connectionAddresses.remove(connectionId)
        if (_pendingAuth.value?.connectionId == connectionId) {
            _pendingAuth.value = null
        }
        removeParticipant(connectionId)
    }

    override fun onMessageReceived(connectionId: String, text: String) {
        val message = ConnectProtocol.decode(text)
        if (message == null) {
            println("[SessionManager] WARN: Failed to decode message from conn=$connectionId: $text")
            return
        }

        println("[SessionManager] Message received (conn=$connectionId): ${message::class.simpleName}")

        when (message) {
            is ConnectMessage.Hello -> handleHello(connectionId, message)
            is ConnectMessage.DartHit -> onDartHit?.invoke(message)
            is ConnectMessage.BoardCleared -> onBoardCleared?.invoke()
            is ConnectMessage.ScoreInput -> onScoreInput?.invoke(message)
            is ConnectMessage.Undo -> onUndo?.invoke()
            is ConnectMessage.Ping -> {
                hostServer.send(connectionId, ConnectProtocol.encode(ConnectMessage.Pong(currentTimeMillis())))
            }
            is ConnectMessage.Bye -> disconnectParticipant(connectionId)
            else -> {
                // Sonstige Nachrichten
            }
        }
    }

    override fun onBinaryReceived(connectionId: String, bytes: ByteArray) {
        // Empfangene Vorschau-Frames (JPEG) vom Kamera-Teilnehmer
        if (activeCameraConnectionId == null || activeCameraConnectionId == connectionId) {
            _latestPreviewFrame.value = bytes
        }
    }

    override fun onError(connectionId: String?, error: Throwable) {
        println("[SessionManager] Companion HostServer error (conn=$connectionId): ${error.message}")
    }

    // --- Interne Autorisierungs- und Session-Methoden ---

    private fun handleHello(connectionId: String, hello: ConnectMessage.Hello) {
        println("[SessionManager] Hello from '${hello.deviceName}' (${hello.platform}, role=${hello.role}, id=${hello.deviceId}, v=${hello.protocolVersion})")
        if (hello.protocolVersion != CONNECT_PROTOCOL_VERSION) {
            println("[SessionManager] Protocol version mismatch: ${hello.protocolVersion} != $CONNECT_PROTOCOL_VERSION")
            hostServer.send(connectionId, ConnectProtocol.encode(ConnectMessage.Rejected(RejectReason.PROTOCOL_MISMATCH)))
            hostServer.disconnect(connectionId)
            return
        }

        val remoteAddr = connectionAddresses[connectionId] ?: "unknown"

        if (isDeviceTrusted(hello.deviceId)) {
            println("[SessionManager] Device is trusted, accepting immediately: ${hello.deviceName}")
            acceptParticipant(connectionId, hello, remoteAddr)
        } else {
            println("[SessionManager] Device is UNTRUSTED -> setting pending authorization for: ${hello.deviceName}")
            _pendingAuth.value = PendingAuthorization(connectionId, hello, remoteAddr)
        }
    }

    private fun acceptParticipant(connectionId: String, hello: ConnectMessage.Hello, remoteAddr: String) {
        val current = _hostState.value as? HostState.Active ?: return

        val participant = ConnectedParticipant(
            connectionId = connectionId,
            deviceId = hello.deviceId,
            name = hello.deviceName,
            platform = hello.platform,
            role = hello.role,
            capabilities = hello.capabilities,
            remoteAddress = remoteAddr
        )

        if (hello.role == DeviceRole.CAMERA && activeCameraConnectionId == null) {
            activeCameraConnectionId = connectionId
        }

        val welcomeMsg = ConnectProtocol.encode(
            ConnectMessage.Welcome(
                sessionId = current.hostId,
                hostName = current.hostName
            )
        )
        hostServer.send(connectionId, welcomeMsg)

        _hostState.update { state ->
            if (state is HostState.Active) {
                state.copy(participants = state.participants.filterNot { it.connectionId == connectionId } + participant)
            } else state
        }
    }

    private fun removeParticipant(connectionId: String) {
        _hostState.update { state ->
            if (state is HostState.Active) {
                val updated = state.copy(participants = state.participants.filterNot { it.connectionId == connectionId })
                val cameraParticipants = updated.participants.filter { it.role == DeviceRole.CAMERA }
                if (activeCameraConnectionId == connectionId) {
                    activeCameraConnectionId = cameraParticipants.firstOrNull()?.connectionId
                }
                if (cameraParticipants.isEmpty()) {
                    _latestPreviewFrame.value = null
                    activeCameraConnectionId = null
                }
                updated
            } else state
        }
    }

    // --- Trust-List Persistenz ---

    fun isDeviceTrusted(deviceId: String): Boolean =
        _trustedDeviceIds.value.contains(deviceId)

    fun trustDevice(deviceId: String, name: String = "Gerät", platform: String = "companion") {
        val entry = TrustedDevice(
            deviceId = deviceId,
            name = name,
            platform = platform,
            addedAtMillis = currentTimeMillis()
        )
        val filtered = _trustedDevices.value.filterNot { it.deviceId == deviceId } + entry
        updateTrustedDevices(filtered)
    }

    fun revokeTrust(deviceId: String) {
        val filtered = _trustedDevices.value.filterNot { it.deviceId == deviceId }
        updateTrustedDevices(filtered)
    }

    fun clearAllTrustedDevices() {
        updateTrustedDevices(emptyList())
    }

    private fun updateTrustedDevices(newList: List<TrustedDevice>) {
        _trustedDevices.value = newList
        _trustedDeviceIds.value = newList.map { it.deviceId }.toSet()
        saveTrustedDevices(newList)
    }

    private fun loadTrustedDevices(): List<TrustedDevice> {
        val jsonStr = loadAppPreference(PREFS_TRUSTED_DEVICES_V2_KEY)
        if (!jsonStr.isNullOrBlank()) {
            try {
                return Json.decodeFromString<List<TrustedDevice>>(jsonStr)
            } catch (e: Exception) {
                println("[SessionManager] Error parsing trusted devices v2: ${e.message}")
            }
        }
        val legacy = loadAppPreference(PREFS_TRUSTED_DEVICES_KEY) ?: return emptyList()
        return legacy.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { id -> TrustedDevice(deviceId = id, name = "Gerät ($id)", platform = "companion") }
    }

    private fun saveTrustedDevices(list: List<TrustedDevice>) {
        try {
            val jsonStr = Json.encodeToString(list)
            saveAppPreference(PREFS_TRUSTED_DEVICES_V2_KEY, jsonStr)
            saveAppPreference(PREFS_TRUSTED_DEVICES_KEY, list.joinToString(",") { it.deviceId })
        } catch (e: Exception) {
            println("[SessionManager] Error saving trusted devices: ${e.message}")
        }
    }

    private fun getOrCreateHostId(): String {
        val existing = loadAppPreference(PREFS_HOST_ID_KEY)
        if (!existing.isNullOrBlank()) return existing
        val newId = "host-${currentTimeMillis()}"
        saveAppPreference(PREFS_HOST_ID_KEY, newId)
        return newId
    }
}
