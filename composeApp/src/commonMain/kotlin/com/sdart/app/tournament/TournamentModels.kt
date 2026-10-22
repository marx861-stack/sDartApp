package com.sdart.app.tournament

import com.sdart.app.ATCVariant
import com.sdart.app.CricketVariant
import com.sdart.app.GameMode
import com.sdart.app.currentTimeMillis
import kotlinx.serialization.Serializable

/**
 * Art des Turniersystems
 */
@Serializable
enum class TournamentType {
    SINGLE_ELIMINATION,
    ROUND_ROBIN
}

/**
 * Ein Teilnehmer am Turnier.
 * Jeder Teilnehmer besitzt eine eindeutige ID, um Namensdopplungen zu vermeiden.
 * [savedPlayerName] verweist optional auf ein Profil im PlayerRepository.
 */
@Serializable
data class TournamentPlayer(
    val id: String,
    val name: String,
    val savedPlayerName: String? = null
)

/**
 * Spielkonfiguration für alle Matches im Turnier
 */
@Serializable
data class TournamentGameConfig(
    val mode: GameMode = GameMode.X501,
    val customStartScore: Int = 501,
    val legs: Int = 1,
    val doubleIn: Boolean = false,
    val doubleOut: Boolean = true,
    val cricketVariant: CricketVariant = CricketVariant.STANDARD,
    val atcVariant: ATCVariant = ATCVariant.STANDARD
)

/**
 * Spieler-Einzelergebnis innerhalb eines Matches
 */
@Serializable
data class MatchPlayerResult(
    val legsWon: Int = 0,
    val average: Double = 0.0,
    val dartsThrown: Int = 0,
    val highestScore: Int = 0,
    val isForfeit: Boolean = false
)

/**
 * Ein einzelnes Match im Turnierverlauf
 */
@Serializable
data class TournamentMatch(
    val id: String,
    val round: Int,
    val matchIndexInRound: Int,
    val player1: TournamentPlayer? = null,
    val player2: TournamentPlayer? = null,
    val winner: TournamentPlayer? = null,
    val isFinished: Boolean = false,
    val nextMatchId: String? = null,
    val nextMatchSlot: Int = 1, // 1 für Spieler 1 im Folgematch, 2 für Spieler 2
    val player1Result: MatchPlayerResult = MatchPlayerResult(),
    val player2Result: MatchPlayerResult = MatchPlayerResult(),
    val isBye: Boolean = false // Freilos-Match
) {
    val isReady: Boolean
        get() = !isFinished && player1 != null && player2 != null
}

/**
 * Vollständiger, serialisierbarer Turnierstatus
 */
@Serializable
data class TournamentState(
    val id: String,
    val name: String,
    val type: TournamentType,
    val config: TournamentGameConfig,
    val players: List<TournamentPlayer>,
    val matches: List<TournamentMatch>,
    val isFinished: Boolean = false,
    val winner: TournamentPlayer? = null,
    val createdAt: Long = currentTimeMillis()
)

/**
 * Zeile in der Tabelle eines Jeder-gegen-Jeden-Turniers
 */
@Serializable
data class LeagueStandingRow(
    val rank: Int,
    val player: TournamentPlayer,
    val played: Int,
    val won: Int,
    val lost: Int,
    val legsWon: Int,
    val legsLost: Int,
    val legDifference: Int,
    val averageScore: Double
)
