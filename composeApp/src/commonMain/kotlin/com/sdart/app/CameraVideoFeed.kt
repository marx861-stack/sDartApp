package com.sdart.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.SessionManager

/**
 * Plattformübergreifende Telemetrie-Daten der Kamera- und ML-Pipeline für das gemeinsame UI-Overlay in commonMain.
 */
data class CameraTelemetry(
    val dynamicRoi: Rect? = null,
    val inferenceTimeMs: Long = 0L,
    val fps: Int = 0,
    val acceleratorLabel: String = "",
    val gpuActive: Boolean = true,
    val gpuInitFailed: Boolean = false,
    // Digitaler Anzeige-Zoom des Videobilds (nur wenn die Plattform das Bild selbst skaliert, z.B. Desktop)
    val displayZoom: Float = 1f
)

/**
 * Art der ausgewählten Kamera-Quelle (plattformübergreifend).
 */
enum class CameraSourceType {
    LOCAL,             // Lokale USB-Webcam (Desktop) / Gerätekamera (Android/iOS)
    COMPANION_STREAM   // Live-Video vom über Host verbundenen Smartphone (Companion)
}

/**
 * Fragt die verfügbaren lokalen Kameras der Plattform ab (auch wenn der Kamera-Feed noch nicht läuft).
 */
expect suspend fun queryAvailableLocalCameras(): List<String>

/**
 * Minimale Plattform-Abstraktion für den rohen Kamera-Video-Feed (Android: CameraX, Desktop: Webcam/Stream, iOS: AVFoundation).
 * Überträgt ausschließlich das Video-Rendering und meldet die erkannten Objekte, Frame-Größe sowie Telemetrie.
 * Sämtliche Overlays, Visier-Brackets und Scoring-Logik liegen zu 100% geteilt in commonMain.
 */
@Composable
expect fun CameraVideoFeed(
    modifier: Modifier = Modifier,
    vm: DartViewModel,
    showCamera: Boolean = true,
    onDetectionsUpdated: (List<ObjectDetection>) -> Unit,
    onFrameSizeChanged: (Int, Int) -> Unit,
    onTelemetryUpdated: (CameraTelemetry) -> Unit = {},
    sessionManager: SessionManager? = null,
    participantManager: ParticipantSessionManager? = null
)

