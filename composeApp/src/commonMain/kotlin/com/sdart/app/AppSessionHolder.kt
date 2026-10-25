package com.sdart.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.SessionManager

/**
 * Hält app-weite Objekte, die eine Neuerstellung der Activity (Drehung, Dark-Mode,
 * Fenstergröße, Foldables) überleben müssen.
 *
 * Mit `remember` gingen Repository, Netzwerk-Sessions und Splash-Status bei jeder
 * Konfigurationsänderung verloren; als ViewModel bleiben sie bis zum echten Beenden erhalten.
 */
class AppSessionHolder : ViewModel() {
    val repository: PlayerRepository = createPersistentPlayerRepository()
    val sessionManager = SessionManager()
    val participantManager = ParticipantSessionManager()

    /** Splash nur beim echten App-Start zeigen, nicht nach jeder Neuerstellung. */
    var showSplashScreen by mutableStateOf(true)

    override fun onCleared() {
        super.onCleared()
        // Erst beim endgültigen Beenden Verbindungen sauber schließen
        participantManager.disconnect()
        sessionManager.stopHost()
    }
}
