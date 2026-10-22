package com.sdart.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class DesktopAudioPlayer : AudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var manifestMapping: Map<String, String> = emptyMap()
    private val audioByteCache = ConcurrentHashMap<String, ByteArray>()
    private val lastPlayedTime = ConcurrentHashMap<String, Long>()

    init {
        loadManifest()
    }

    private fun loadManifest() {
        try {
            val classLoader = Thread.currentThread().contextClassLoader ?: DesktopAudioPlayer::class.java.classLoader
            val stream = classLoader?.getResourceAsStream("audio/manifest.json")
            if (stream != null) {
                val jsonText = stream.bufferedReader().use { it.readText() }
                manifestMapping = Json.decodeFromString<Map<String, String>>(jsonText)
                println("[DesktopAudioPlayer] Loaded manifest with ${manifestMapping.size} audio entries")
            } else {
                println("[DesktopAudioPlayer] Warning: audio/manifest.json not found in classpath")
            }
        } catch (e: Exception) {
            println("[DesktopAudioPlayer] Failed to load audio manifest: ${e.message}")
        }
    }

    private fun getAudioBytes(filename: String): ByteArray? {
        val cached = audioByteCache[filename]
        if (cached != null) return cached

        return try {
            val classLoader = Thread.currentThread().contextClassLoader ?: DesktopAudioPlayer::class.java.classLoader
            val stream = classLoader?.getResourceAsStream("audio/$filename")
            if (stream != null) {
                val bytes = stream.use { it.readBytes() }
                audioByteCache[filename] = bytes
                bytes
            } else {
                null
            }
        } catch (e: Exception) {
            println("[DesktopAudioPlayer] Failed to load audio file $filename: ${e.message}")
            null
        }
    }

    override fun playSound(text: String, excitement: Int) {
        if (text.contains("...")) {
            val parts = text.split("...")
            scope.launch {
                for (i in parts.indices) {
                    val part = parts[i].trim()
                    if (part.isNotEmpty()) {
                        playSingle(part, excitement)
                    }
                    if (i < parts.size - 1) {
                        delay(850)
                    }
                }
            }
        } else {
            playSingle(text, excitement)
        }
    }

    private fun playSingle(text: String, excitement: Int) {
        val key = "${text}_$excitement"
        var filename = manifestMapping[key]

        // Fallback auf excitement 0, falls spezifischer key fehlt
        if (filename == null && excitement != 0) {
            filename = manifestMapping["${text}_0"]
        }

        if (filename != null) {
            val currentTime = System.currentTimeMillis()
            val lastTime = lastPlayedTime[filename] ?: 0L

            // Debounce 150ms gegen zu schnelles mehrfaches Triggern
            if (currentTime - lastTime > 150) {
                lastPlayedTime[filename] = currentTime
                scope.launch(Dispatchers.IO) {
                    try {
                        val bytes = getAudioBytes(filename) ?: return@launch
                        val bais = ByteArrayInputStream(bytes)
                        val player = javazoom.jl.player.Player(bais)
                        player.play()
                        player.close()
                    } catch (e: Exception) {
                        println("[DesktopAudioPlayer] Playback error for $filename: ${e.message}")
                    }
                }
            }
        }
    }

    override fun vibrate() {
        // Desktop besitzt keine Vibrations-Hardware
    }

    override fun release() {
        scope.cancel()
        audioByteCache.clear()
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val player = remember { DesktopAudioPlayer() }
    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }
    return player
}
