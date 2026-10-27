package com.sdart.app

import org.jetbrains.compose.resources.getString
import sdartapp.composeapp.generated.resources.Res
import sdartapp.composeapp.generated.resources.camera_local_device
import com.sdart.app.core.detection.ObjectDetection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.SessionManager

/**
 * iOS-spezifischer Stub für CameraVideoFeed (AVFoundation).
 */
actual suspend fun queryAvailableLocalCameras(): List<String> = listOf(getString(Res.string.camera_local_device))

@Composable
actual fun CameraVideoFeed(
    modifier: Modifier,
    vm: DartViewModel,
    showCamera: Boolean,
    onDetectionsUpdated: (List<ObjectDetection>) -> Unit,
    onFrameSizeChanged: (Int, Int) -> Unit,
    onTelemetryUpdated: (CameraTelemetry) -> Unit,
    sessionManager: SessionManager?,
    participantManager: ParticipantSessionManager?
) {
    Box(
        modifier = modifier.fillMaxSize()
    )
}
