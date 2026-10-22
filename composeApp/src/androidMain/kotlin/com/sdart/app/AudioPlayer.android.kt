package com.sdart.app

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private val tag = "AndroidAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val soundPool: SoundPool
    private var manifestMapping: Map<String, String> = emptyMap()
    private val soundIdMap = ConcurrentHashMap<String, Int>()
    private val lastPlayedTime = ConcurrentHashMap<String, Long>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        loadManifest()
        preloadSoundsAsync()
    }

    private fun loadManifest() {
        try {
            val jsonText = context.assets.open("audio/manifest.json").bufferedReader().use { it.readText() }
            manifestMapping = Json.decodeFromString<Map<String, String>>(jsonText)
            Log.d(tag, "Loaded manifest with ${manifestMapping.size} audio entries")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load audio manifest", e)
        }
    }

    private fun preloadSoundsAsync() {
        scope.launch(Dispatchers.IO) {
            val uniqueFiles = manifestMapping.values.distinct()
            for (filename in uniqueFiles) {
                loadSoundFile(filename)
            }
        }
    }

    private fun loadSoundFile(filename: String): Int? {
        val existing = soundIdMap[filename]
        if (existing != null) return existing

        return try {
            val afd: AssetFileDescriptor = context.assets.openFd("audio/$filename")
            val soundId = soundPool.load(afd, 1)
            soundIdMap[filename] = soundId
            soundId
        } catch (e: Exception) {
            Log.e(tag, "Failed to load sound: $filename", e)
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
            val soundId = soundIdMap[filename] ?: loadSoundFile(filename)
            if (soundId != null) {
                val currentTime = System.currentTimeMillis()
                val lastTime = lastPlayedTime[filename] ?: 0L

                // Debounce 150ms gegen extremes Nachhallen
                if (currentTime - lastTime > 150) {
                    soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                    lastPlayedTime[filename] = currentTime
                }
            } else {
                Log.w(tag, "Sound ID not found for file: $filename")
            }
        } else {
            Log.d(tag, "No audio mapping found for key: $key")
        }
    }

    override fun vibrate() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 15, 10, 20)
                    val amplitudes = intArrayOf(255, 80)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 15, 10, 20), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Vibration feedback failed", e)
        }
    }

    override fun release() {
        scope.cancel()
        soundPool.release()
        soundIdMap.clear()
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { AndroidAudioPlayer(context) }
    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }
    return player
}
