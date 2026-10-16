package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.myapplication.connect.DeviceRole
import com.example.myapplication.connect.HostState
import com.example.myapplication.connect.ParticipantSessionManager
import com.example.myapplication.connect.ParticipantState
import com.example.myapplication.connect.SessionManager
import com.example.myapplication.ui.theme.PrimaryOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

enum class CameraSourceType {
    USB_WEBCAM,
    STREAM_URL,
    COMPANION_STREAM
}

/**
 * Desktop-spezifische Implementierung von CameraVideoFeed.
 * Verwaltet USB-Webcams, IP-MJPEG-Streams und eingehende Companion-Handy-Streams.
 * Führt Onnx-Inferenz im Hintergrund aus und liefert Detektionen & Frame-Dimensionen an commonMain.
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
    val hostState by (sessionManager?.hostState?.collectAsState() ?: remember { mutableStateOf(null) })
    val isHostActive = hostState is HostState.Active

    val participantState by (participantManager?.state?.collectAsState() ?: remember { mutableStateOf(null) })
    val isStreamingToHost = participantState is ParticipantState.Connected &&
            (participantState as ParticipantState.Connected).role == DeviceRole.CAMERA
    val shouldCapture = showCamera || isHostActive || isStreamingToHost

    val webcamManager = remember { DesktopWebcamManager() }
    val mjpegClient = remember { DesktopMjpegClient() }

    val initialCompanionBytes = sessionManager?.latestPreviewFrame?.value
    var selectedSourceType by remember {
        mutableStateOf(if (initialCompanionBytes != null) CameraSourceType.COMPANION_STREAM else CameraSourceType.USB_WEBCAM)
    }
    var selectedWebcamIndex by remember { mutableStateOf(0) }

    val availableWebcams by webcamManager.availableWebcams.collectAsState()
    val isWebcamRunning by webcamManager.isRunning.collectAsState()
    val webcamFrame by webcamManager.currentFrame.collectAsState()
    val webcamFps by webcamManager.fps.collectAsState()

    val streamState by mjpegClient.streamState.collectAsState()
    val streamFrame by mjpegClient.currentFrame.collectAsState()
    val streamFps by mjpegClient.fps.collectAsState()

    val webcamRawImage by webcamManager.currentBufferedImage.collectAsState()
    val streamRawJpegBytes by mjpegClient.currentRawJpegBytes.collectAsState()

    // Eingehende JPEG-Frames von einem verbundenen Companion-Handy
    val companionRawBytes by (sessionManager?.latestPreviewFrame?.collectAsState() ?: remember { mutableStateOf(null) })
    var companionFrame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    val onnxDetector = remember { OnnxDartDetector() }
    val isInferencing = remember { AtomicBoolean(false) }

    var streamUrlInput by remember { mutableStateOf("http://192.168.178.50:8080") }
    var showSourceDialog by remember { mutableStateOf(false) }

    // USB-Webcam nur suchen, wenn wir tatsächlich USB_WEBCAM als Quelle gewählt haben
    LaunchedEffect(Unit) {
        if (selectedSourceType == CameraSourceType.USB_WEBCAM) {
            webcamManager.refreshWebcams()
        }
    }

    // Wenn ein Companion-Handy Frames sendet, schalten wir sofort auf den Handy-Feed um und stoppen die lokale Webcam!
    LaunchedEffect(companionRawBytes) {
        companionRawBytes?.let { bytes ->
            if (bytes.isNotEmpty()) {
                if (selectedSourceType != CameraSourceType.COMPANION_STREAM) {
                    webcamManager.stopWebcam()
                    selectedSourceType = CameraSourceType.COMPANION_STREAM
                }
                try {
                    val skiaImg = Image.makeFromEncoded(bytes)
                    if (skiaImg != null) {
                        companionFrame = skiaImg.toComposeImageBitmap()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    LaunchedEffect(availableWebcams, shouldCapture, selectedSourceType) {
        if (shouldCapture && selectedSourceType == CameraSourceType.USB_WEBCAM) {
            if (availableWebcams.isNotEmpty() && !isWebcamRunning) {
                webcamManager.startWebcam(selectedWebcamIndex)
            }
        } else if ((!shouldCapture || selectedSourceType != CameraSourceType.USB_WEBCAM) && isWebcamRunning) {
            webcamManager.stopWebcam()
        }
    }

    // Streaming an Host: Wenn PC die Rolle CAMERA hat, Frames zum Host schicken
    var lastFrameSendTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(webcamRawImage, streamRawJpegBytes, isStreamingToHost, selectedSourceType) {
        if (!isStreamingToHost) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val minIntervalMs = 1000L / 24L // ~24 FPS Rate Limiting
        if (now - lastFrameSendTime < minIntervalMs) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val bytes = when (selectedSourceType) {
                CameraSourceType.USB_WEBCAM -> {
                    webcamRawImage?.let { bufferedImageToJpeg(it, 0.6f) }
                }
                CameraSourceType.STREAM_URL -> {
                    streamRawJpegBytes
                }
                CameraSourceType.COMPANION_STREAM -> null
            }
            if (bytes != null && bytes.isNotEmpty()) {
                lastFrameSendTime = System.currentTimeMillis()
                participantManager?.sendPreviewFrame(bytes)
            }
        }
    }

    // Frame-Inferenz Loop für Desktop: Führt Inferenz nur im Standalone- oder Host-Betrieb aus
    LaunchedEffect(webcamRawImage, streamRawJpegBytes, companionRawBytes, selectedSourceType, shouldCapture, isStreamingToHost) {
        if (!shouldCapture || isStreamingToHost) {
            onDetectionsUpdated(emptyList())
            return@LaunchedEffect
        }

        // Frame-Dropping: Wenn vorherige Inferenz noch rechnet, Frame überspringen
        if (!isInferencing.compareAndSet(false, true)) {
            return@LaunchedEffect
        }

        try {
            withContext(Dispatchers.Default) {
                val dets = try {
                    when (selectedSourceType) {
                        CameraSourceType.USB_WEBCAM -> {
                            webcamRawImage?.let { onnxDetector.detect(it) } ?: emptyList()
                        }
                        CameraSourceType.STREAM_URL -> {
                            streamRawJpegBytes?.let { onnxDetector.detect(it) } ?: emptyList()
                        }
                        CameraSourceType.COMPANION_STREAM -> {
                            companionRawBytes?.let { onnxDetector.detect(it) } ?: emptyList()
                        }
                    }
                } catch (t: Throwable) {
                    System.err.println("[CameraVideoFeed] Inferenz-Fehler abgefangen: ${t.message}")
                    emptyList()
                }
                onDetectionsUpdated(dets)
            }
        } finally {
            isInferencing.set(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webcamManager.stopWebcam()
            mjpegClient.disconnect()
            onnxDetector.close()
        }
    }

    val activeFrame = when (selectedSourceType) {
        CameraSourceType.USB_WEBCAM -> webcamFrame
        CameraSourceType.STREAM_URL -> streamFrame
        CameraSourceType.COMPANION_STREAM -> companionFrame
    }
    val activeFps = if (selectedSourceType == CameraSourceType.USB_WEBCAM) webcamFps else streamFps

    LaunchedEffect(activeFrame) {
        activeFrame?.let {
            onFrameSizeChanged(it.width, it.height)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Video Rendering (USB Webcam oder MJPEG Stream)
        if (showCamera && activeFrame != null) {
            Image(
                bitmap = activeFrame,
                contentDescription = "Live Video Feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Desktop Status-Banner unten (über der Aktionsleiste) - nur wenn Kameraansicht aktiv ist
        if (showCamera) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
                    .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .zIndex(20f)
            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isActive = if (selectedSourceType == CameraSourceType.USB_WEBCAM) isWebcamRunning else (streamState is StreamState.Connected)
                val statusDotColor = if (isActive) Color(0xFF28a745) else Color(0xFFffc107)

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusDotColor, CircleShape)
                )

                val statusText = if (isStreamingToHost) {
                    "Stream an Host aktiv ($activeFps FPS)"
                } else when (selectedSourceType) {
                    CameraSourceType.USB_WEBCAM -> {
                        if (isWebcamRunning && availableWebcams.isNotEmpty()) {
                            "USB: ${availableWebcams.getOrNull(selectedWebcamIndex) ?: "Kamera"} ($activeFps FPS)"
                        } else if (availableWebcams.isEmpty()) {
                            "Keine USB-Webcam gefunden"
                        } else {
                            "Webcam angehalten"
                        }
                    }
                    CameraSourceType.STREAM_URL -> {
                        when (streamState) {
                            is StreamState.Connected -> "Stream Verbunden ($activeFps FPS)"
                            is StreamState.Connecting -> "Verbinde mit Stream..."
                            is StreamState.Error -> "Stream-Verbindungsfehler"
                            else -> "Companion / IP-Stream bereit"
                        }
                    }
                    CameraSourceType.COMPANION_STREAM -> {
                        if (companionRawBytes != null) {
                            "Handy-Kamera live verbunden (Host-Inferenz aktiv)"
                        } else {
                            "Warte auf Handy-Kamera-Stream..."
                        }
                    }
                }

                Text(
                    text = statusText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = {
                        webcamManager.refreshWebcams()
                        showSourceDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "Quelle wählen",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

        // Real-time Debug HUD für Inferenzzeit und FPS
        if (showCamera && vm.showCameraDebugHud) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .zIndex(10f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Video-Feed",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "$activeFps FPS",
                        color = if (activeFps > 15) Color.Green else if (activeFps > 0) Color.Yellow else Color.Gray,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .width(40.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    Text(
                        text = "Quelle",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = when (selectedSourceType) {
                            CameraSourceType.USB_WEBCAM -> "USB WEBCAM"
                            CameraSourceType.STREAM_URL -> "MJPEG STREAM"
                            CameraSourceType.COMPANION_STREAM -> "COMPANION PHONE (HOST ML)"
                        },
                        color = Color.Cyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Dialog zur Kameraquellen-Auswahl (USB vs. Stream)
        if (showSourceDialog) {
            AlertDialog(
                onDismissRequest = { showSourceDialog = false },
                title = { Text("Kamera-Quelle auswählen", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Verfügbare USB-Webcams:", color = PrimaryOrange, fontWeight = FontWeight.Bold)

                        if (availableWebcams.isEmpty()) {
                            Text("Keine USB-Kameras gefunden (bitte USB-Verbindung prüfen).", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        } else {
                            availableWebcams.forEachIndexed { idx, name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (selectedSourceType == CameraSourceType.USB_WEBCAM && selectedWebcamIndex == idx) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedSourceType = CameraSourceType.USB_WEBCAM
                                            selectedWebcamIndex = idx
                                            mjpegClient.disconnect()
                                            webcamManager.startWebcam(idx)
                                            showSourceDialog = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (selectedSourceType == CameraSourceType.USB_WEBCAM && selectedWebcamIndex == idx),
                                        onClick = {
                                            selectedSourceType = CameraSourceType.USB_WEBCAM
                                            selectedWebcamIndex = idx
                                            mjpegClient.disconnect()
                                            webcamManager.startWebcam(idx)
                                            showSourceDialog = false
                                        }
                                    )
                                    Text(name, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                        Text("Oder Netzwerk-Stream (Smartphone/IP-Cam):", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = streamUrlInput,
                            onValueChange = { streamUrlInput = it },
                            label = { Text("MJPEG URL (z.B. http://192.168.x.x:8080)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                selectedSourceType = CameraSourceType.STREAM_URL
                                webcamManager.stopWebcam()
                                mjpegClient.connect(streamUrlInput.trim())
                                showSourceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Netzwerk-Stream starten", color = Color.White)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSourceDialog = false }) {
                        Text("Schließen", color = Color.White)
                    }
                }
            )
        }
    }
}

/**
 * Konvertiert ein AWT BufferedImage in komprimierte JPEG-Bytes für das Streaming an den Host.
 */
internal fun bufferedImageToJpeg(image: java.awt.image.BufferedImage, quality: Float = 0.6f): ByteArray? {
    return try {
        val baos = ByteArrayOutputStream(256 * 1024)
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", baos)
            return baos.toByteArray()
        }
        val writer = writers.next()
        val param = writer.defaultWriteParam
        if (param.canWriteCompressed()) {
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionQuality = quality
        }
        val ios = ImageIO.createImageOutputStream(baos)
        writer.output = ios
        writer.write(null, IIOImage(image, null, null), param)
        writer.dispose()
        ios.close()
        baos.toByteArray()
    } catch (_: Throwable) {
        null
    }
}

