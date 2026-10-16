package com.example.myapplication.tournament.web

import com.example.myapplication.tournament.TournamentManager

/**
 * Plattformunabhängige Schnittstelle für den integrierten Turnier-Webserver.
 * Ermöglicht externen Geräten (ohne App) den Zugriff via Web-Browser.
 */
interface TournamentWebServer {
    val port: Int
    val isRunning: Boolean
    val localUrl: String
    var allowScoreSubmission: Boolean
    var requireHostApproval: Boolean

    /**
     * Startet den HTTP-Webserver auf dem angegebenen Port (Standard: 8080; 0 = freier Port).
     * @return der tatsächlich gebundene Port.
     */
    fun start(port: Int = 8080): Int

    /**
     * Stoppt den Server und schließt alle aktiven Verbindungen.
     */
    fun stop()

    /**
     * Informiert alle verbundenen Browser via Server-Sent Events (SSE) über eine Statusänderung.
     */
    fun notifyUpdate()
}

/** Erzeugt die plattformspezifische Implementierung des [TournamentWebServer]. */
expect fun createTournamentWebServer(
    tournamentManager: TournamentManager
): TournamentWebServer

/**
 * Fake-Implementierung für Unit-Tests ohne Sockets.
 */
class FakeTournamentWebServer(
    private val tournamentManager: TournamentManager
) : TournamentWebServer {
    override var port: Int = 0
        private set
    override var isRunning: Boolean = false
        private set
    override val localUrl: String
        get() = if (isRunning) "http://127.0.0.1:$port" else ""
    override var allowScoreSubmission: Boolean = true
    override var requireHostApproval: Boolean = true

    override fun start(port: Int): Int {
        this.port = if (port == 0) 8080 else port
        isRunning = true
        return this.port
    }

    override fun stop() {
        isRunning = false
        port = 0
    }

    override fun notifyUpdate() {}
}
