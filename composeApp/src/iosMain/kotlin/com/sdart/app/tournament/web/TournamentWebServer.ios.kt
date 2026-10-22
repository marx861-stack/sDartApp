package com.sdart.app.tournament.web

import com.sdart.app.tournament.TournamentManager

actual fun createTournamentWebServer(
    tournamentManager: TournamentManager
): TournamentWebServer = IosTournamentWebServerStub(tournamentManager)

/**
 * iOS-Stub für den [TournamentWebServer].
 */
class IosTournamentWebServerStub(
    private val tournamentManager: TournamentManager
) : TournamentWebServer {
    override var port: Int = 0
        private set
    override var isRunning: Boolean = false
        private set
    override val localUrl: String = ""
    override var allowScoreSubmission: Boolean = true
    override var requireHostApproval: Boolean = true

    override fun start(port: Int): Int {
        this.port = if (port == 0) 8080 else port
        this.isRunning = true
        return this.port
    }

    override fun stop() {
        this.isRunning = false
        this.port = 0
    }

    override fun notifyUpdate() {}
}
