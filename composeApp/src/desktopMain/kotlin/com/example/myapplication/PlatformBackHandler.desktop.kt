package com.example.myapplication

import androidx.compose.runtime.Composable

/**
 * Desktop-Implementierung: Auf dem Desktop gibt es keine native Zurücktaste/Geste;
 * die Navigation erfolgt über UI-Buttons.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Auf Desktop ist kein nativer System-Back-Handler nötig
}
