package com.sdart.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Einstellungen des zuletzt gestarteten Spiels, um es per "Revanche" erneut zu starten.
 */
@Serializable
data class LastGameSetup(
    val playerNames: List<String>,
    val mode: GameMode,
    val customScore: Int,
    val doubleIn: Boolean,
    val doubleOut: Boolean,
    val legs: Int,
    val randomizeOrder: Boolean,
    val atcVariant: ATCVariant? = null,
    val atcReverse: Boolean = false,
    val atcWithBulls: Boolean = false,
    val cricketVariant: CricketVariant? = null,
    val startedAtMillis: Long = 0L
) {
    val atcConfig: ATCGameState?
        get() = atcVariant?.let { ATCGameState(it, atcReverse, atcWithBulls) }

    val cricketConfig: CricketConfig?
        get() = cricketVariant?.let { CricketConfig(it) }
}

/**
 * Multiplatform-Speicher für Einstellungen und UI-Zustände.
 */
object AppPreferences {
    private const val KEY_LAST_PLAYERS = "last_player_names"
    private const val KEY_SOUND_ENABLED = "pref_sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "pref_vibration_enabled"
    private const val KEY_VOICE_CALLER_ENABLED = "pref_voice_caller_enabled"
    private const val KEY_ANNOUNCE_EACH_DART = "pref_announce_each_dart"
    private const val KEY_ANNOUNCE_TURN_TOTAL = "pref_announce_turn_total"
    private const val KEY_SHOW_DETECTION_OVERLAY = "pref_show_detection_overlay"
    private const val KEY_SHOW_DEBUG_HUD = "pref_show_debug_hud"
    private const val KEY_SHOW_DARTBOARD_OVERLAY = "pref_show_dartboard_overlay"
    private const val KEY_CAMERA_ZOOM_LEVEL = "pref_camera_zoom_level"
    private const val KEY_CAMERA_ZOOM_RATIO = "pref_camera_zoom_ratio"
    private const val KEY_CAMERA_SOURCE_TYPE = "pref_camera_source_type"
    private const val KEY_CAMERA_STREAM_URL = "pref_camera_stream_url"
    private const val KEY_SELECTED_WEBCAM_INDEX = "pref_selected_webcam_index"
    private const val KEY_LAST_GAME_SETUP = "pref_last_game_setup"

    private val lastGameJson = Json { ignoreUnknownKeys = true }

    fun getLastPlayerNames(): String? = loadAppPreference(KEY_LAST_PLAYERS)

    fun saveLastPlayerNames(names: String) {
        saveAppPreference(KEY_LAST_PLAYERS, names)
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        val raw = loadAppPreference(key) ?: return default
        return raw.toBooleanStrictOrNull() ?: default
    }

    fun saveBoolean(key: String, value: Boolean) {
        saveAppPreference(key, value.toString())
    }

    fun getFloat(key: String, default: Float): Float {
        val raw = loadAppPreference(key) ?: return default
        return raw.toFloatOrNull() ?: default
    }

    fun saveFloat(key: String, value: Float) {
        saveAppPreference(key, value.toString())
    }

    var cameraZoomLevel: Float
        get() = getFloat(KEY_CAMERA_ZOOM_LEVEL, 0f)
        set(value) = saveFloat(KEY_CAMERA_ZOOM_LEVEL, value)

    var cameraZoomRatio: Float
        get() = getFloat(KEY_CAMERA_ZOOM_RATIO, 1f)
        set(value) = saveFloat(KEY_CAMERA_ZOOM_RATIO, value)

    var isSoundEnabled: Boolean
        get() = getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = saveBoolean(KEY_SOUND_ENABLED, value)

    var isVibrationEnabled: Boolean
        get() = getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = saveBoolean(KEY_VIBRATION_ENABLED, value)

    var isVoiceCallerEnabled: Boolean
        get() = getBoolean(KEY_VOICE_CALLER_ENABLED, true)
        set(value) = saveBoolean(KEY_VOICE_CALLER_ENABLED, value)

    var audioAnnounceEachDart: Boolean
        get() = getBoolean(KEY_ANNOUNCE_EACH_DART, true)
        set(value) = saveBoolean(KEY_ANNOUNCE_EACH_DART, value)

    var audioAnnounceTurnTotal: Boolean
        get() = getBoolean(KEY_ANNOUNCE_TURN_TOTAL, true)
        set(value) = saveBoolean(KEY_ANNOUNCE_TURN_TOTAL, value)

    var showCameraDetectionOverlay: Boolean
        get() = getBoolean(KEY_SHOW_DETECTION_OVERLAY, false)
        set(value) = saveBoolean(KEY_SHOW_DETECTION_OVERLAY, value)

    var showCameraDebugHud: Boolean
        get() = getBoolean(KEY_SHOW_DEBUG_HUD, false)
        set(value) = saveBoolean(KEY_SHOW_DEBUG_HUD, value)

    var showDartboardOverlay: Boolean
        get() = getBoolean(KEY_SHOW_DARTBOARD_OVERLAY, false)
        set(value) = saveBoolean(KEY_SHOW_DARTBOARD_OVERLAY, value)

    var cameraSourceType: CameraSourceType
        get() {
            val name = loadAppPreference(KEY_CAMERA_SOURCE_TYPE) ?: return CameraSourceType.LOCAL
            return try {
                CameraSourceType.valueOf(name)
            } catch (_: Exception) {
                CameraSourceType.LOCAL
            }
        }
        set(value) = saveAppPreference(KEY_CAMERA_SOURCE_TYPE, value.name)

    var cameraStreamUrl: String
        get() = loadAppPreference(KEY_CAMERA_STREAM_URL) ?: "http://192.168.178.50:8080"
        set(value) = saveAppPreference(KEY_CAMERA_STREAM_URL, value)

    var selectedWebcamIndex: Int
        get() = loadAppPreference(KEY_SELECTED_WEBCAM_INDEX)?.toIntOrNull() ?: 0
        set(value) = saveAppPreference(KEY_SELECTED_WEBCAM_INDEX, value.toString())

    var lastGameSetup: LastGameSetup?
        get() {
            val raw = loadAppPreference(KEY_LAST_GAME_SETUP) ?: return null
            return try {
                lastGameJson.decodeFromString<LastGameSetup>(raw)
            } catch (_: Exception) {
                null
            }
        }
        set(value) = saveAppPreference(KEY_LAST_GAME_SETUP, value?.let { lastGameJson.encodeToString(it) } ?: "")
}
