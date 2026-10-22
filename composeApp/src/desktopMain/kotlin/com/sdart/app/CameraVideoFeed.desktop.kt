package com.sdart.app

import com.sdart.app.core.detection.ObjectDetection

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
import com.sdart.app.connect.DeviceRole
import com.sdart.app.connect.HostState
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.ParticipantState
import com.sdart.app.connect.SessionManager
import com.sdart.app.ui.theme.PrimaryOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import com.github.sarxos.webcam.Webcam

actual suspend fun queryAvailableLocalCameras(): List<String> = withContext(Dispatchers.IO) {
    try {
        Webcam.getWebcams().map { it.name }
    } catch (_: Exception) {
        emptyList()
    }
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

    val availableWebcams by webcamManager.availableWebcams.collectAsState()
    val isWebcamRunning by webcamManager.isRunning.collectAsState()
    val webcamFrame by webcamManager.currentFrame.collectAsState()
    val webcamFps by webcamManager.fps.collectAsState()

    val webcamRawImage by webcamManager.currentBufferedImage.collectAsState()

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

    // Sync available webcams mit ViewModel (nicht mit leerer Initial-Liste überschreiben)
    LaunchedEffect(availableWebcams) {
        if (availableWebcams.isNotEmpty()) {
            vm.availableWebcams = availableWebcams
        }
    }

    // Webcams beim ersten Start suchen
    LaunchedEffect(Unit) {
        webcamManager.refreshWebcams()
    }

    // Automatischer Wechsel bei neu verbundener oder getrennter Host-Kamera
    val cameraParticipants = (hostState as? HostState.Active)?.participants?.filter { it.role == DeviceRole.CAMERA } ?: emptyList()
    var lastCameraParticipantIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(cameraParticipants) {
        val currentIds = cameraParticipants.map { it.connectionId }.toSet()
        val newlyJoined = currentIds - lastCameraParticipantIds
        if (newlyJoined.isNotEmpty()) {
            val newId = newlyJoined.last()
            sessionManager?.activeCameraConnectionId = newId
            vm.selectCompanionCamera(newId)
        } else if (currentIds.isEmpty() && vm.cameraSourceType == CameraSourceType.COMPANION_STREAM) {
            vm.setCameraSource(CameraSourceType.LOCAL)
        }
        lastCameraParticipantIds = currentIds
    }

    // Wenn ein Companion-Handy Frames sendet
    LaunchedEffect(companionRawBytes) {
        companionRawBytes?.let { bytes ->
            if (bytes.isNotEmpty()) {
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

    // Starten & Stoppen der Quellen je nach ausgewählter Quelle im ViewModel
    val knownWebcams = if (availableWebcams.isNotEmpty()) availableWebcams else vm.availableWebcams
    LaunchedEffect(knownWebcams, shouldCapture, vm.cameraSourceType, vm.selectedWebcamIndex) {
        if (!shouldCapture) {
            if (isWebcamRunning) webcamManager.stopWebcam()
            return@LaunchedEffect
        }

        when (vm.cameraSourceType) {
            CameraSourceType.LOCAL -> {
                if (knownWebcams.isNotEmpty()) {
                    webcamManager.startWebcam(vm.selectedWebcamIndex)
                }
            }
            CameraSourceType.COMPANION_STREAM -> {
                if (isWebcamRunning) webcamManager.stopWebcam()
            }
        }
    }

    // Streaming an Host: Wenn PC die Rolle CAMERA hat, Frames zum Host schicken
    var lastFrameSendTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(webcamRawImage, isStreamingToHost, vm.cameraSourceType) {
        if (!isStreamingToHost) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val minIntervalMs = 1000L / 24L // ~24 FPS Rate Limiting
        if (now - lastFrameSendTime < minIntervalMs) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val bytes = when (vm.cameraSourceType) {
                CameraSourceType.LOCAL -> {
                    webcamRawImage?.let { bufferedImageToJpeg(it, 0.6f) }
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
    LaunchedEffect(webcamRawImage, companionRawBytes, vm.cameraSourceType, shouldCapture, isStreamingToHost) {
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
                    when (vm.cameraSourceType) {
                        CameraSourceType.LOCAL -> {
                            webcamRawImage?.let { onnxDetector.detect(it) } ?: emptyList()
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
            val currentRatio = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) {
                vm.cameraZoomRatio
            } else {
                1f + vm.cameraZoomLevel * 3f
            }
            val newScale = (currentRatio * zoomMultiplier).coerceIn(1f, 4f)
            val newZoom = (newScale - 1f) / 3f
            vm.cameraZoomLevel = newZoom
            vm.cameraZoomRatio = newScale
        }
        vm.onResetZoom = {
            vm.cameraZoomLevel = 0f
            vm.cameraZoomRatio = 1.0f
        }
        vm.onRefreshWebcams = {
            webcamManager.refreshWebcams()
        }
        onDispose {
            vm.onPinchZoom = null
            vm.onResetZoom = null
            vm.onRefreshWebcams = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webcamManager.stopWebcam()
            onnxDetector.close()
        }
    }

    val activeFrame = when (vm.cameraSourceType) {
        CameraSourceType.LOCAL -> webcamFrame
        CameraSourceType.COMPANION_STREAM -> companionFrame
    }
    val activeFps = when (vm.cameraSourceType) {
        CameraSourceType.LOCAL -> webcamFps
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

    LaunchedEffect(dynamicRoi, inferenceTimeMs, activeFps, vm.cameraSourceType, zoomFactor) {
        val label = when (vm.cameraSourceType) {
            CameraSourceType.LOCAL -> "CPU (ONNX USB)"
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
        // Video Rendering (USB Webcam oder Companion)
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

