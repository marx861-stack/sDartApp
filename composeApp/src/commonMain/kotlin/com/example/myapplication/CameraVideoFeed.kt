package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.connect.ParticipantSessionManager
import com.example.myapplication.connect.SessionManager

/**
 * Minimale Plattform-Abstraktion für den rohen Kamera-Video-Feed (Android: CameraX, Desktop: Webcam/Stream, iOS: AVFoundation).
 * Überträgt ausschließlich das Video-Rendering und meldet die erkannten Objekte sowie die Frame-Größe.
 * Sämtliche Overlays, Visier-Brackets und Scoring-Logik liegen zu 100% geteilt in commonMain.
 */
@Composable
expect fun CameraVideoFeed(
    modifier: Modifier = Modifier,
    vm: DartViewModel,
    showCamera: Boolean = true,
    onDetectionsUpdated: (List<ObjectDetection>) -> Unit,
    onFrameSizeChanged: (Int, Int) -> Unit,
    sessionManager: SessionManager? = null,
    participantManager: ParticipantSessionManager? = null
)
