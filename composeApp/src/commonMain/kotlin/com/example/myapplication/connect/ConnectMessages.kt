package com.example.myapplication.connect

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Aktuelle Protokollversion. Wird beim Hello geprüft, damit alte/neue Apps sich sauber abweisen. */
const val CONNECT_PROTOCOL_VERSION = 1

/** mDNS Service-Typ, unter dem sich der Host ankündigt. */
const val CONNECT_SERVICE_TYPE = "_sdart._tcp"

/**
 * Alle JSON-Nachrichten zwischen Host und Teilnehmern.
 * Binäre Vorschau-Frames (JPEG) laufen separat als WebSocket-Binary-Frame und sind hier nicht enthalten.
 */
@Serializable
sealed interface ConnectMessage {

    /** Teilnehmer → Host: Erste Nachricht nach dem Verbinden. */
    @Serializable
    @SerialName("hello")
    data class Hello(
        val deviceId: String,
        val deviceName: String,
        val platform: String,
        val role: DeviceRole,
        val capabilities: Set<DeviceCapability>,
        val protocolVersion: Int = CONNECT_PROTOCOL_VERSION
    ) : ConnectMessage

    /** Host → Teilnehmer: Verbindung akzeptiert. */
    @Serializable
    @SerialName("welcome")
    data class Welcome(val sessionId: String, val hostName: String) : ConnectMessage

    /** Host → Teilnehmer: Verbindung abgelehnt (Nutzer hat abgelehnt, falsche Version, Rolle nicht möglich). */
    @Serializable
    @SerialName("rejected")
    data class Rejected(val reason: RejectReason) : ConnectMessage

    /** Kamera → Host: Ein Dart wurde erkannt. Koordinaten sind auf das Board normalisiert (0..1). */
    @Serializable
    @SerialName("dart_hit")
    data class DartHit(
        val segment: Int,
        val multiplier: Int,
        val dartIndex: Int,
        val confidence: Float,
        val x: Float,
        val y: Float
    ) : ConnectMessage

    /** Kamera → Host: Board ist leer (Darts wurden gezogen). */
    @Serializable
    @SerialName("board_cleared")
    data object BoardCleared : ConnectMessage

    /** Eingabe → Host: Manuelle Punkte-Eingabe. */
    @Serializable
    @SerialName("score_input")
    data class ScoreInput(
        val segment: Int,
        val multiplier: Int,
        val x: Float? = null,
        val y: Float? = null
    ) : ConnectMessage

    /** Eingabe → Host: Letzten Wurf rückgängig machen. */
    @Serializable
    @SerialName("undo")
    data object Undo : ConnectMessage

    /** Host → Anzeige/Eingabe: Aktueller Spielstand als JSON-Snapshot (Format folgt mit der Anzeige-Rolle). */
    @Serializable
    @SerialName("game_state")
    data class GameStateUpdate(val snapshotJson: String) : ConnectMessage

    /** Verbindungsüberwachung (beide Richtungen). */
    @Serializable
    @SerialName("ping")
    data class Ping(val timestampMs: Long) : ConnectMessage

    @Serializable
    @SerialName("pong")
    data class Pong(val timestampMs: Long) : ConnectMessage

    /** Sauberes Beenden der Verbindung. */
    @Serializable
    @SerialName("bye")
    data object Bye : ConnectMessage
}

/** Gründe, warum der Host einen Teilnehmer abweist. */
@Serializable
enum class RejectReason {
    DECLINED_BY_USER,
    PROTOCOL_MISMATCH,
    ROLE_NOT_SUPPORTED,
    ROLE_ALREADY_TAKEN
}

/** Encoding/Decoding der Nachrichten. Unbekannte Felder werden ignoriert (Vorwärtskompatibilität). */
object ConnectProtocol {
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    fun encode(message: ConnectMessage): String =
        json.encodeToString(ConnectMessage.serializer(), message)

    /** @return die Nachricht oder `null`, wenn der Text kein gültiges Protokoll ist. */
    fun decode(text: String): ConnectMessage? =
        try {
            json.decodeFromString(ConnectMessage.serializer(), text)
        } catch (e: Exception) {
            null
        }
}
