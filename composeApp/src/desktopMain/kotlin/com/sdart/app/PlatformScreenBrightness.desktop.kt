package com.sdart.app

import androidx.compose.runtime.Composable

/**
 * Desktop-Implementierung: Auf Desktop-Systemen (Windows/macOS/Linux) wird die
 * Monitor-Hardwarehelligkeit nicht anwendungsseitig manipuliert.
 */
@Composable
actual fun PlatformScreenBrightness(
    isDimmed: Boolean,
    keepScreenOn: Boolean
) {
    // Desktop: Hardware-Helligkeitssteuerung nicht unterstützt / no-op
}
