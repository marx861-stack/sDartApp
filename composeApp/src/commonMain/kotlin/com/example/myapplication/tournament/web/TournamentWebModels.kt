package com.example.myapplication.tournament.web

import com.example.myapplication.currentTimeMillis
import com.example.myapplication.tournament.LeagueStandingRow
import com.example.myapplication.tournament.TournamentState
import kotlinx.serialization.Serializable

/**
 * Anfrage zur Übermittlung eines Match-Ergebnisses (aus dem Web oder der Companion-App).
 */
@Serializable
data class MatchSubmissionRequest(
    val matchId: String,
    val winnerPlayerId: String,
    val player1Legs: Int,
    val player2Legs: Int,
    val player1Average: Double? = null,
    val player2Average: Double? = null,
    val submittedByPlayerName: String? = null
)

/**
 * Ein eingereichtes Ergebnis, das auf die Freigabe des Hosts wartet.
 */
@Serializable
data class PendingMatchSubmission(
    val id: String = "sub_${currentTimeMillis()}",
    val matchId: String,
    val submission: MatchSubmissionRequest,
    val player1Name: String,
    val player2Name: String,
    val winnerName: String,
    val timestamp: Long = currentTimeMillis()
)

/**
 * Vollständiger JSON-Überblick für den Web-Client.
 */
@Serializable
data class TournamentWebOverview(
    val tournament: TournamentState?,
    val standings: List<LeagueStandingRow>? = null,
    val pendingMatchIds: List<String> = emptyList(),
    val serverTime: Long = currentTimeMillis(),
    val allowScoreSubmission: Boolean = true,
    val requireHostApproval: Boolean = true
)

/**
 * Statusantwort für POST-Anfragen.
 */
@Serializable
data class WebApiResponse(
    val success: Boolean,
    val message: String? = null
)
