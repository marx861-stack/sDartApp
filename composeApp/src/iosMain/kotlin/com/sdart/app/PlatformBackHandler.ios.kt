package com.sdart.app

import androidx.compose.runtime.Composable

/**
 * iOS-Implementierung: Auf iOS existiert kein Android-ähnlicher Activity-Dispatcher.
 * Gesten-Navigation wird über den Compose edgeSwipeBack Modifier abgedeckt.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Auf Plattform-Ebene no-op; Gesten werden via edgeSwipeBack gehandhabt
}
