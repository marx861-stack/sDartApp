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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    onTelemetryUpdated: (CameraTelemetry) -> Unit,
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
    var companionFps by remember { mutableIntStateOf(0) }
    var companionFramesThisSec by remember { mutableIntStateOf(0) }
    var lastCompanionFpsTime by remember { mutableLongStateOf(0L) }

    val onnxDetector = remember { OnnxDartDetector() }
    val isInferencing = remember { AtomicBoolean(false) }
    var dynamicRoi by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var inferenceTimeMs by remember { mutableLongStateOf(0L) }

    var streamUrlInput by remember { mutableStateOf("http://192.168.178.50:8080") }
    var showSourceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showSourceDialog) {
        vm.isCameraSourceDialogOpen = showSourceDialog
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.isCameraSourceDialogOpen = false
        }
    }

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
                val now = System.currentTimeMillis()
                companionFramesThisSec++
                if (lastCompanionFpsTime == 0L) {
                    lastCompanionFpsTime = now
                } else if (now - lastCompanionFpsTime >= 1000L) {
                    companionFps = companionFramesThisSec
                    companionFramesThisSec = 0
                    lastCompanionFpsTime = now
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
                dynamicRoi = onnxDetector.dynamicRoi
                inferenceTimeMs = onnxDetector.lastInferenceTimeMs
                onDetectionsUpdated(dets)
            }
        } finally {
            isInferencing.set(false)
        }
    }

    DisposableEffect(vm) {
        vm.onPinchZoom = { zoomMultiplier ->
            val currentZoom = vm.cameraZoomLevel
            val currentScale = 1f + currentZoom * 3f
            val newScale = (currentScale * zoomMultiplier).coerceIn(1f, 4f)
            val newZoom = (newScale - 1f) / 3f
            vm.cameraZoomLevel = newZoom
            vm.cameraZoomRatio = newScale
        }
        vm.onResetZoom = {
            vm.cameraZoomLevel = 0f
            vm.cameraZoomRatio = 1.0f
        }
        onDispose {
            vm.onPinchZoom = null
            vm.onResetZoom = null
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
    val activeFps = when (selectedSourceType) {
        CameraSourceType.USB_WEBCAM -> webcamFps
        CameraSourceType.STREAM_URL -> streamFps
        CameraSourceType.COMPANION_STREAM -> companionFps
    }

    LaunchedEffect(activeFrame) {
        activeFrame?.let {
            onFrameSizeChanged(it.width, it.height)
        }
    }

    // Digitaler Zoom: skaliert nur die Anzeige, die Overlays in CameraBackground skalieren über displayZoom mit
    val zoomFactor = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) {
        vm.cameraZoomRatio
    } else {
        1.0f + vm.cameraZoomLevel * 3.0f
    }

    LaunchedEffect(dynamicRoi, inferenceTimeMs, activeFps, selectedSourceType, zoomFactor) {
        val label = when (selectedSourceType) {
            CameraSourceType.USB_WEBCAM -> "CPU (ONNX USB)"
            CameraSourceType.STREAM_URL -> "CPU (ONNX STREAM)"
            CameraSourceType.COMPANION_STREAM -> "CPU (ONNX HOST)"
        }
        onTelemetryUpdated(
            CameraTelemetry(
                dynamicRoi = dynamicRoi,
                inferenceTimeMs = inferenceTimeMs,
                fps = activeFps,
                acceleratorLabel = label,
                gpuActive = false,
                gpuInitFailed = false,
                displayZoom = zoomFactor
            )
        )
    }

    Box(
        modifier = modifier.fillMaxSize().clipToBounds()
    ) {
        // Video Rendering (USB Webcam oder MJPEG Stream)
        if (showCamera && activeFrame != null) {
            Image(
                bitmap = activeFrame,
                contentDescription = "Live Video Feed",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomFactor
                        scaleY = zoomFactor
                    },
                contentScale = ContentScale.Crop
            )
        }

        // Quellen-Button oben rechts (dezent, ohne Text-Statusleiste) - nur wenn Kameraansicht aktiv ist
        if (showCamera) {
            Button(
                onClick = {
                    webcamManager.refreshWebcams()
                    showSourceDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 68.dp)
                    .height(34.dp)
                    .zIndex(20f)
            ) {
                Text(
                    text = "Quelle wählen",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
                },
                containerColor = Color(0xFF262626),
                shape = RoundedCornerShape(16.dp)
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

