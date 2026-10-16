package com.example.myapplication

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
import com.example.myapplication.connect.ParticipantSessionManager
import com.example.myapplication.connect.SessionManager

/**
 * iOS-spezifischer Stub für CameraVideoFeed (AVFoundation).
 */
@Composable
actual fun CameraVideoFeed(
    modifier: Modifier,
    vm: DartViewModel,
    showCamera: Boolean,
    onDetectionsUpdated: (List<ObjectDetection>) -> Unit,
    onFrameSizeChanged: (Int, Int) -> Unit,
    sessionManager: SessionManager?,
    participantManager: ParticipantSessionManager?
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // iOS Status-Banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(5f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF28a745), CircleShape)
                )
                Text(
                    text = "iOS Kamera-Modus (AVFoundation Vorbereitung)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
