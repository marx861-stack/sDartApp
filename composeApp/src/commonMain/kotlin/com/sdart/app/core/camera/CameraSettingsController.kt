package com.sdart.app.core.camera

import androidx.compose.runtime.*
import com.sdart.app.AppPreferences
import com.sdart.app.CameraSourceType
import com.sdart.app.queryAvailableLocalCameras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ZOOM_PERSIST_DELAY_MS = 400L

/**
 * Verwaltet plattformübergreifende Kamera- und Video-Einstellungen:
 * Quellenauswahl (USB, IP-Stream, Companion), Zoomstufen, Entprellung der Speicherung und Webcam-Erkennung.
 */
class CameraSettingsController(
    private val scope: CoroutineScope
) {
    // Zoom-Level der Kamera (wird über alle Spiele hinweg gemerkt, bis die App geschlossen wird)
    private var _cameraZoomLevel by mutableFloatStateOf(AppPreferences.cameraZoomLevel)
    var cameraZoomLevel: Float
        get() = _cameraZoomLevel
        set(value) {
            _cameraZoomLevel = value
            scheduleZoomPersist()
        }

    private var _cameraZoomRatio by mutableFloatStateOf(AppPreferences.cameraZoomRatio)
    var cameraZoomRatio: Float
        get() = _cameraZoomRatio
        set(value) {
            _cameraZoomRatio = value
            scheduleZoomPersist()
        }

    private var zoomPersistJob: Job? = null

    private fun scheduleZoomPersist() {
        zoomPersistJob?.cancel()
        zoomPersistJob = scope.launch {
            delay(ZOOM_PERSIST_DELAY_MS)
            persistZoom()
        }
    }

    private fun persistZoom() {
        AppPreferences.cameraZoomLevel = _cameraZoomLevel
        AppPreferences.cameraZoomRatio = _cameraZoomRatio
    }

    // Direkte Zoom-Steuerung durch die Plattform (verhindert Ruckeln & Feedback-Loops)
    var onPinchZoom: ((Float) -> Unit)? = null
    var onResetZoom: (() -> Unit)? = null

    // Plattformübergreifende Kamera-Quellenauswahl
    var cameraSourceType by mutableStateOf(AppPreferences.cameraSourceType)
        private set

    var cameraStreamUrl by mutableStateOf(AppPreferences.cameraStreamUrl)
        private set

    var selectedWebcamIndex by mutableIntStateOf(AppPreferences.selectedWebcamIndex)
        private set

    var selectedCompanionConnectionId by mutableStateOf<String?>(null)
        private set

    var availableWebcams by mutableStateOf<List<String>>(emptyList())

    // Plattform-Callbacks für Kamera-Hardware-Steuerung
    var onRefreshWebcams: (() -> Unit)? = null
    var onCameraSourceChanged: ((CameraSourceType) -> Unit)? = null

    // Status ob der Kamera-Quellen-Auswahldialog geöffnet ist (für Hintergrund-Blur im GameScreen)
    var isCameraSourceDialogOpen by mutableStateOf(false)

    // Callback zum Anfordern des plattformspezifischen Quellen-Dialogs (z.B. Desktop)
    var onRequestOpenSourceDialog: (() -> Unit)? = null

    init {
        refreshWebcams()
    }

    fun setCameraSource(type: CameraSourceType) {
        cameraSourceType = type
        AppPreferences.cameraSourceType = type
        onCameraSourceChanged?.invoke(type)
    }

    fun selectCompanionCamera(connectionId: String?) {
        selectedCompanionConnectionId = connectionId
        setCameraSource(CameraSourceType.COMPANION_STREAM)
    }

    fun setStreamUrl(url: String) {
        cameraStreamUrl = url
        AppPreferences.cameraStreamUrl = url
    }

    fun setWebcamIndex(index: Int) {
        selectedWebcamIndex = index
        AppPreferences.selectedWebcamIndex = index
        onCameraSourceChanged?.invoke(cameraSourceType)
    }

    fun refreshWebcams() {
        onRefreshWebcams?.invoke()
        scope.launch {
            val cams = queryAvailableLocalCameras()
            availableWebcams = cams
            if (selectedWebcamIndex !in cams.indices && cams.isNotEmpty()) {
                selectedWebcamIndex = 0
            }
        }
    }

    fun resetZoom() {
        cameraZoomLevel = 0f
        cameraZoomRatio = 1.0f
        onResetZoom?.invoke()
    }
}
