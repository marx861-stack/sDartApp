package com.sdart.app

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

sealed class DesktopCameraSource {
    data class UsbWebcam(val name: String, val index: Int) : DesktopCameraSource()
    data class MjpegStream(val url: String) : DesktopCameraSource()
}

/**
 * Controller für lokale USB-Webcams unter Desktop (Windows / Linux).
 * Liest Frames im Hintergrundthread und konvertiert BufferedImages zu Compose ImageBitmaps.
 * Führt Öffnen und Schließen asynchron via Mutex aus, um AWT-UI-Freezes zu verhindern.
 */
class DesktopWebcamManager {
    private val _availableWebcams = MutableStateFlow<List<String>>(emptyList())
    val availableWebcams: StateFlow<List<String>> = _availableWebcams

    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentFrame: StateFlow<ImageBitmap?> = _currentFrame

    private val _currentBufferedImage = MutableStateFlow<BufferedImage?>(null)
    val currentBufferedImage: StateFlow<BufferedImage?> = _currentBufferedImage

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private var activeWebcam: Webcam? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val webcamMutex = Mutex()

    fun refreshWebcams() {
        scope.launch {
            webcamMutex.withLock {
                try {
                    val cams = Webcam.getWebcams()
                    _availableWebcams.value = cams.map { it.name }
                } catch (e: Exception) {
                    _availableWebcams.value = emptyList()
                }
            }
        }
    }

    fun startWebcam(index: Int = 0) {
        stopWebcam()
        captureJob = scope.launch {
            webcamMutex.withLock {
                try {
                    val cams = Webcam.getWebcams()
                    if (cams.isEmpty() || index !in cams.indices) {
                        _isRunning.value = false
                        return@withLock
                    }

                    val cam = cams[index]
                    val supported = cam.viewSizes
                    val hdSize = supported.firstOrNull { it.width == 1280 && it.height == 720 }
                        ?: supported.firstOrNull { it.width == 640 && it.height == 480 }
                        ?: supported.firstOrNull()

                    if (hdSize != null) {
                        cam.viewSize = hdSize
                    }
                    cam.open(true)

                    activeWebcam = cam
                    _isRunning.value = true

                    var framesCount = 0
                    var lastFpsCalc = System.currentTimeMillis()

                    while (isActive && cam.isOpen) {
                        val img: BufferedImage? = cam.image
                        if (img != null) {
                            _currentBufferedImage.value = img
                            val composeBitmap = img.toComposeImageBitmap()
                            _currentFrame.value = composeBitmap

                            framesCount++
                            val now = System.currentTimeMillis()
                            if (now - lastFpsCalc >= 1000) {
                                _fps.value = framesCount
                                framesCount = 0
                                lastFpsCalc = now
                            }
                        }
                        delay(33) // ~30 FPS Begrenzung zur Ressourcenschonung
                    }
                } catch (e: CancellationException) {
                    // Regulärer Stopp
                } catch (e: Exception) {
                    _isRunning.value = false
                } finally {
                    try {
                        if (activeWebcam?.isOpen == true) {
                            activeWebcam?.close()
                        }
                    } catch (_: Exception) {}
                    activeWebcam = null
                    _isRunning.value = false
                    _fps.value = 0
                    _currentBufferedImage.value = null
                }
            }
        }
    }

    /**
     * Beendet die Webcam vollständig asynchron.
     * Blockiert zu keinem Zeitpunkt den UI-Thread (AWT EDT).
     */
    fun stopWebcam() {
        val jobToCancel = captureJob
        captureJob = null
        val camToClose = activeWebcam
        activeWebcam = null

        _isRunning.value = false
        _currentFrame.value = null
        _currentBufferedImage.value = null
        _fps.value = 0

        scope.launch {
            try {
                jobToCancel?.cancelAndJoin()
            } catch (_: Exception) {}
            webcamMutex.withLock {
                try {
                    if (camToClose != null && camToClose.isOpen) {
                        camToClose.close()
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
