package com.sdart.app

import androidx.compose.runtime.Composable

/**
 * Plattformübergreifendes Interface für Soundeffekte, Voice Caller und haptisches Feedback.
 */
interface AudioPlayer {
    /**
     * Spielt einen Soundeffekt oder eine Voice-Caller-Ansage ab.
     * Unterstützt sequenzielle Ansagen über den Delimiter '...' (z.B. 'buzzer ... Bust').
     */
    fun playSound(text: String, excitement: Int = 0)

    /**
     * Löst kurzes haptisches Feedback / Vibration aus (falls von der Plattform unterstützt).
     */
    fun vibrate()

    /**
     * Gibt Audio-Ressourcen und Player-Instanzen frei.
     */
    fun release()
}

/**
 * Erzeugt und verwaltet eine an den Compose-Lebenszyklus gebundene AudioPlayer-Instanz.
 */
@Composable
expect fun rememberAudioPlayer(): AudioPlayer
