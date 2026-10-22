package com.sdart.app

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.skia.Image
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class StreamState {
    object Disconnected : StreamState()
    object Connecting : StreamState()
    data class Connected(val url: String) : StreamState()
    data class Error(val message: String) : StreamState()
}

/**
 * Desktop MJPEG Stream Client.
 * Connects to HTTP MJPEG stream endpoints (such as Android MjpegServer or IP-Cameras)
 * and decodes JPEG frames into Skia/Compose ImageBitmaps with frame-dropping to guarantee zero lag.
 */
class DesktopMjpegClient {
    private val _streamState = MutableStateFlow<StreamState>(StreamState.Disconnected)
    val streamState: StateFlow<StreamState> = _streamState

    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentFrame: StateFlow<ImageBitmap?> = _currentFrame

    private val _currentRawJpegBytes = MutableStateFlow<ByteArray?>(null)
    val currentRawJpegBytes: StateFlow<ByteArray?> = _currentRawJpegBytes

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps

    private var streamJob: Job? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(streamUrl: String) {
        disconnect()
        _streamState.value = StreamState.Connecting

        streamJob = clientScope.launch {
            var connection: HttpURLConnection? = null
            var inputStream: BufferedInputStream? = null
            try {
                val url = URL(streamUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "sDartApp-Desktop/1.0")
                }
                connection.connect()

                if (connection.responseCode != 200) {
                    _streamState.value = StreamState.Error("HTTP-Fehler: ${connection.responseCode}")
                    return@launch
                }

                _streamState.value = StreamState.Connected(streamUrl)
                inputStream = BufferedInputStream(connection.inputStream, 65536)

                var lastFpsCalcTime = System.currentTimeMillis()
                var framesThisSecond = 0
                val jpegBuffer = ByteArrayOutputStream(128 * 1024)

                var prevByte = -1
                var inJpeg = false

                while (isActive) {
                    val b = inputStream.read()
                    if (b == -1) break

                    // JPEG Start of Image (SOI): 0xFF, 0xD8
                    if (!inJpeg) {
                        if (prevByte == 0xFF && b == 0xD8) {
                            inJpeg = true
                            jpegBuffer.reset()
                            jpegBuffer.write(0xFF)
                            jpegBuffer.write(0xD8)
                        }
                    } else {
                        jpegBuffer.write(b)
                        // JPEG End of Image (EOI): 0xFF, 0xD9
                        if (prevByte == 0xFF && b == 0xD9) {
                            inJpeg = false
                            val rawBytes = jpegBuffer.toByteArray()
                            try {
                                _currentRawJpegBytes.value = rawBytes
                                val skiaImage = Image.makeFromEncoded(rawBytes)
                                val composeBitmap = skiaImage.toComposeImageBitmap()
                                _currentFrame.value = composeBitmap

                                framesThisSecond++
                                val now = System.currentTimeMillis()
                                if (now - lastFpsCalcTime >= 1000) {
                                    _fps.value = framesThisSecond
                                    framesThisSecond = 0
                                    lastFpsCalcTime = now
                                }
                            } catch (e: Exception) {
                                // Frame decode error, skip frame
                            }
                        }
                    }
                    prevByte = b
                }
            } catch (e: CancellationException) {
                // Normal disconnect
            } catch (e: Exception) {
                _streamState.value = StreamState.Error(e.message ?: "Verbindungsfehler")
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}
                _currentRawJpegBytes.value = null
                if (_streamState.value !is StreamState.Error) {
                    _streamState.value = StreamState.Disconnected
                }
            }
        }
    }

    fun disconnect() {
        streamJob?.cancel()
        streamJob = null
        _currentFrame.value = null
        _currentRawJpegBytes.value = null
        _fps.value = 0
        _streamState.value = StreamState.Disconnected
    }
}
