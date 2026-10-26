package com.sdart.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

class IosAudioPlayer : AudioPlayer {
    override fun playSound(text: String, excitement: Int) {
        // iOS Vorbereitung: AVFoundation Integration folgt bei Bedarf
    }

    override fun vibrate() {
        val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
        generator.prepare()
        generator.impactOccurred()
    }

    override fun release() {
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    return remember { IosAudioPlayer() }
}
