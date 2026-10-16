package com.example.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android-Implementierung: Verwendet androidx.activity.compose.BackHandler,
 * welcher sowohl die Dreiecks-Zurücktaste (3-Tasten-Navigation) als auch
 * die Wischgesten vom Bildschirmrand (Gesten-Navigation) automatisch abfängt.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
