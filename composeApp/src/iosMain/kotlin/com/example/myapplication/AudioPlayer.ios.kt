package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class IosAudioPlayer : AudioPlayer {
    override fun playSound(text: String, excitement: Int) {
        // iOS Vorbereitung: AVFoundation Integration folgt bei Bedarf
    }

    override fun vibrate() {
        // iOS Vorbereitung: AudioToolbox / UIImpactFeedbackGenerator
    }

    override fun release() {
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    return remember { IosAudioPlayer() }
}
