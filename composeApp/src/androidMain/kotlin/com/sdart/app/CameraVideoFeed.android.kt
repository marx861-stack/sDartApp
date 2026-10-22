package com.sdart.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sdart.app.connect.DeviceRole
import com.sdart.app.connect.HostState
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.ParticipantState
import com.sdart.app.connect.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

actual suspend fun queryAvailableLocalCameras(): List<String> = listOf("Lokale Gerätekamera")

/**
 * Android-spezifische Implementierung von CameraVideoFeed.
 * Konzentriert sich ausschließlich auf die Video-Bereitstellung (CameraX / Companion Stream)
 * und die Weitergabe von Detektionen und Frame-Dimensionen an commonMain.
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val hostState by (sessionManager?.hostState?.collectAsState() ?: remember { mutableStateOf(null) })
    val isHostActive = hostState is HostState.Active

    val participantState by (participantManager?.state?.collectAsState() ?: remember { mutableStateOf(null) })
    val isStreamingToHost = participantState is ParticipantState.Connected &&
            (participantState as ParticipantState.Connected).role == DeviceRole.CAMERA

    val mlDetector = remember { MLDartDetector(context) }

    val companionRawBytes by (sessionManager?.latestPreviewFrame?.collectAsState() ?: remember { mutableStateOf(null) })
    var companionFrame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val isHostWithCompanionStream = (vm.cameraSourceType == CameraSourceType.COMPANION_STREAM) && (companionRawBytes != null)
    val isCompanionInferencing = remember { AtomicBoolean(false) }

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

    LaunchedEffect(companionRawBytes, isHostWithCompanionStream) {
        if (!isHostWithCompanionStream) {
            companionFrame = null
            return@LaunchedEffect
        }
        val bytes = companionRawBytes ?: return@LaunchedEffect
        if (bytes.isEmpty()) return@LaunchedEffect

        // Frame-Dropping: Wenn vorherige Inferenz noch läuft, Frame überspringen
        if (!isCompanionInferencing.compareAndSet(false, true)) {
            return@LaunchedEffect
        }

        try {
            withContext(Dispatchers.Default) {
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    companionFrame = bmp.asImageBitmap()
                    onFrameSizeChanged(bmp.width, bmp.height)
                    val dets = mlDetector.detectBitmap(bmp)
                    onDetectionsUpdated(dets)
                }
            }
        } catch (_: Throwable) {
        } finally {
            isCompanionInferencing.set(false)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val shouldCapture = showCamera || isHostActive || isStreamingToHost

    LaunchedEffect(isHostWithCompanionStream, shouldCapture) {
        if (!hasCameraPermission && !isHostWithCompanionStream && shouldCapture) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Im Streaming-Modus (Kamera schickt Video an Host) schalten wir die lokale Inferenz ab!
    // Im Standalone-Modus rechnet das Handy selbst mit NCNN Vulkan.
    LaunchedEffect(isStreamingToHost) {
        mlDetector.inferenceEnabled = !isStreamingToHost
        if (isStreamingToHost) {
            val jpegStream = ByteArrayOutputStream(256 * 1024)
            var lastFrameSendTime = 0L
            val minIntervalMs = 1000L / 24L // ~24 FPS Rate Limiting

            mlDetector.setFrameListener { bmp ->
                val now = System.currentTimeMillis()
                if (now - lastFrameSendTime >= minIntervalMs) {
                    lastFrameSendTime = now
                    try {
                        jpegStream.reset()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 60, jpegStream)
                        val bytes = jpegStream.toByteArray()
                        participantManager?.sendPreviewFrame(bytes)
                    } catch (_: Exception) {}
                }
            }
        } else {
            mlDetector.setFrameListener(null)
        }
    }

    val currentDetections by mlDetector.detections.collectAsState()
    val detectorFrameSize by mlDetector.frameSize.collectAsState()
    val dynamicRoi by mlDetector.dynamicRoiState.collectAsState()
    val inferenceTimeMs by mlDetector.inferenceTimeMs.collectAsState()
    val fps by mlDetector.framesPerSecond.collectAsState()
    val gpuActive by mlDetector.gpuActive.collectAsState()
    val gpuInitFailed by mlDetector.gpuInitFailed.collectAsState()

    LaunchedEffect(currentDetections) {
        onDetectionsUpdated(currentDetections)
    }

    LaunchedEffect(detectorFrameSize, isHostWithCompanionStream) {
        if (!isHostWithCompanionStream && detectorFrameSize.first > 0 && detectorFrameSize.second > 0) {
            onFrameSizeChanged(detectorFrameSize.first, detectorFrameSize.second)
        }
    }

    LaunchedEffect(dynamicRoi, inferenceTimeMs, fps, gpuActive, gpuInitFailed) {
        onTelemetryUpdated(
            CameraTelemetry(
                dynamicRoi = dynamicRoi,
                inferenceTimeMs = inferenceTimeMs,
                fps = fps,
                acceleratorLabel = if (gpuActive) "GPU (Vulkan)" else "CPU",
                gpuActive = gpuActive,
                gpuInitFailed = gpuInitFailed
            )
        )
    }

    val zoomState by mlDetector.zoomState.collectAsState()
    LaunchedEffect(zoomState) {
        zoomState?.let { state ->
            vm.cameraZoomRatio = state.zoomRatio
            vm.cameraZoomLevel = ((state.zoomRatio - 1.0f) / 3.0f).coerceIn(0f, 1f)
        }
    }

    DisposableEffect(mlDetector, vm) {
        vm.onPinchZoom = { zoomMultiplier ->
            val state = mlDetector.zoomState.value
            if (state != null) {
                val currentRatio = state.zoomRatio
                val maxRatio = state.maxZoomRatio
                val minRatio = state.minZoomRatio
                val newRatio = (currentRatio * zoomMultiplier).coerceIn(minRatio, maxRatio)
                mlDetector.setZoomRatio(newRatio)
                vm.cameraZoomRatio = newRatio
            } else {
                val currentZoom = vm.cameraZoomLevel
                val currentScale = 1f + currentZoom * 3f
                val newScale = (currentScale * zoomMultiplier).coerceIn(1f, 4f)
                val newZoom = (newScale - 1f) / 3f
                mlDetector.setZoom(newZoom)
                vm.cameraZoomLevel = newZoom
                vm.cameraZoomRatio = newScale
            }
        }
        vm.onResetZoom = {
            mlDetector.resetZoom()
            vm.cameraZoomLevel = 0f
            vm.cameraZoomRatio = 1.0f
        }
        onDispose {
            vm.onPinchZoom = null
            vm.onResetZoom = null
        }
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission, isHostWithCompanionStream, shouldCapture) {
        if (hasCameraPermission && shouldCapture && !isHostWithCompanionStream) {
            if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.01f) {
                mlDetector.setZoomRatio(vm.cameraZoomRatio)
            } else if (vm.cameraZoomLevel > 0.001f) {
                mlDetector.setZoomRatio(1.0f + vm.cameraZoomLevel * 3.0f)
            }
            mlDetector.startCamera(lifecycleOwner)
        }
        onDispose {
            mlDetector.stopCamera()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                if (!isHostWithCompanionStream && (detectorFrameSize.first <= 0 || detectorFrameSize.second <= 0)) {
                    val w = layoutCoordinates.size.width
                    val h = layoutCoordinates.size.height
                    val frameW = if (w < h) 720 else 1280
                    val frameH = if (w < h) 1280 else 720
                    onFrameSizeChanged(frameW, frameH)
                }
            }
    ) {
        if (showCamera && isHostWithCompanionStream && companionFrame != null) {
            Image(
                bitmap = companionFrame!!,
                contentDescription = "Companion Camera Stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (hasCameraPermission && showCamera && !isHostWithCompanionStream) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }.also { previewView ->
                        mlDetector.setPreviewView(previewView)
                    }
                },
                update = { previewView ->
                    mlDetector.setPreviewView(previewView)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
