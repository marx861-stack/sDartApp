package com.example.myapplication.tournament

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.myapplication.AppPreferences
import com.example.myapplication.currentTimeMillis
import com.example.myapplication.loadAppPreference
import com.example.myapplication.saveAppPreference
import kotlinx.serialization.json.Json

/**
 * Zentraler Manager für den Zustand, die Spiellogik und die Persistenz
 * von Dart-Turnieren.
 */
class TournamentManager {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val PREF_KEY_ACTIVE_TOURNAMENT = "active_tournament_v1"

    var currentTournament by mutableStateOf<TournamentState?>(null)
        private set

    val pendingSubmissions = androidx.compose.runtime.mutableStateListOf<com.example.myapplication.tournament.web.PendingMatchSubmission>()

    var onTournamentUpdated: (() -> Unit)? = null

    init {
        loadFromPreferences()
    }

    /**
     * Erstellt ein neues Turnier und speichert es ab.
     */
    fun createTournament(
        name: String,
        type: TournamentType,
        config: TournamentGameConfig,
        players: List<TournamentPlayer>,
        randomizeOrder: Boolean = false
    ): TournamentState {
        val preparedPlayers = if (randomizeOrder) players.shuffled() else players
        val tId = "tourn_${currentTimeMillis()}"

        val matches = when (type) {
            TournamentType.SINGLE_ELIMINATION -> {
                TournamentScheduler.createSingleEliminationSchedule(tId, preparedPlayers)
            }
            TournamentType.ROUND_ROBIN -> {
                TournamentScheduler.createRoundRobinSchedule(tId, preparedPlayers)
            }
        }

        val state = TournamentState(
            id = tId,
            name = name.ifBlank { "Dart-Turnier" },
            type = type,
            config = config,
            players = preparedPlayers,
            matches = matches,
            isFinished = false,
            winner = null
        )

        currentTournament = state
        saveToPreferences()
        return state
    }

    /**
     * Schließt ein Match mit dem übergebenen Gewinner und Einzelergebnissen ab.
     * Befördert den Sieger in die Folgerunde bzw. berechnet den Turniersieg.
     */
    fun resolveMatch(
        matchId: String,
        winnerPlayerId: String,
        player1Result: MatchPlayerResult = MatchPlayerResult(),
        player2Result: MatchPlayerResult = MatchPlayerResult()
    ): Boolean {
        val state = currentTournament ?: return false
        val matchIndex = state.matches.indexOfFirst { it.id == matchId }
        if (matchIndex < 0) return false

        val match = state.matches[matchIndex]
        if (match.isFinished) return false

        val winner = if (match.player1?.id == winnerPlayerId) {
            match.player1
        } else if (match.player2?.id == winnerPlayerId) {
            match.player2
        } else {
            null
        } ?: return false

        val updatedMatch = match.copy(
            winner = winner,
            isFinished = true,
            player1Result = player1Result,
            player2Result = player2Result
        )

        val updatedMatches = state.matches.toMutableList()
        updatedMatches[matchIndex] = updatedMatch

        var tournamentWinner = state.winner
        var isTournamentFinished = state.isFinished

        when (state.type) {
            TournamentType.SINGLE_ELIMINATION -> {
                if (match.nextMatchId != null) {
                    val nextIndex = updatedMatches.indexOfFirst { it.id == match.nextMatchId }
                    if (nextIndex >= 0) {
                        val nextMatch = updatedMatches[nextIndex]
                        val advanced = if (match.nextMatchSlot == 1) {
                            nextMatch.copy(player1 = winner)
                        } else {
                            nextMatch.copy(player2 = winner)
                        }
                        updatedMatches[nextIndex] = advanced
                    }
                } else {
                    // Final-Sieg!
                    tournamentWinner = winner
                    isTournamentFinished = true
                }
            }
            TournamentType.ROUND_ROBIN -> {
                val allDone = updatedMatches.all { it.isFinished }
                if (allDone) {
                    isTournamentFinished = true
                    val standings = calculateStandingsForMatches(state.players, updatedMatches)
                    tournamentWinner = standings.firstOrNull()?.player
                }
            }
        }

        currentTournament = state.copy(
            matches = updatedMatches,
            winner = tournamentWinner,
            isFinished = isTournamentFinished
        )
        saveToPreferences()
        return true
    }

    /**
     * Wertet ein Match als kampflos gewonnen (Aufgabe des Gegners).
     */
    fun forfeitMatch(matchId: String, forfeitingPlayerId: String): Boolean {
        val state = currentTournament ?: return false
        val match = state.matches.find { it.id == matchId } ?: return false
        if (match.isFinished) return false

        val winner = if (match.player1?.id == forfeitingPlayerId) match.player2 else match.player1
        if (winner == null) return false

        val requiredLegs = state.config.legs
        val p1Result = if (match.player1?.id == winner.id) {
            MatchPlayerResult(legsWon = requiredLegs, isForfeit = false)
        } else {
            MatchPlayerResult(legsWon = 0, isForfeit = true)
        }
        val p2Result = if (match.player2?.id == winner.id) {
            MatchPlayerResult(legsWon = requiredLegs, isForfeit = false)
        } else {
            MatchPlayerResult(legsWon = 0, isForfeit = true)
        }

        return resolveMatch(matchId, winner.id, p1Result, p2Result)
    }

    /**
     * Erlaubt manuelle Korrektur eines Match-Ergebnisses (solange das Folge-Match noch nicht gespielt wurde).
     */
    fun overrideMatchResult(
        matchId: String,
        winnerPlayerId: String,
        p1LegsWon: Int,
        p2LegsWon: Int
    ): Boolean {
        val state = currentTournament ?: return false
        val match = state.matches.find { it.id == matchId } ?: return false

        // Prüfen, ob das Folgematch bereits gespielt wurde
        if (match.nextMatchId != null) {
            val nextMatch = state.matches.find { it.id == match.nextMatchId }
            if (nextMatch != null && nextMatch.isFinished) {
                return false // Kann nicht mehr überschrieben werden, wenn Folge-Match bereits fertig ist
            }
        }

        val winner = if (match.player1?.id == winnerPlayerId) match.player1 else match.player2
        if (winner == null) return false

        val p1Result = match.player1Result.copy(legsWon = p1LegsWon)
        val p2Result = match.player2Result.copy(legsWon = p2LegsWon)

        // Temporär auf unfertig setzen, um resolveMatch erneut aufzurufen
        val updatedMatches = state.matches.toMutableList()
        val idx = updatedMatches.indexOfFirst { it.id == matchId }
        updatedMatches[idx] = match.copy(isFinished = false)
        currentTournament = state.copy(matches = updatedMatches)

        return resolveMatch(matchId, winner.id, p1Result, p2Result)
    }

    /**
     * Erfasst das manuelle Ergebnis eines Matches mit den konkreten Leg-Ständen.
     * Funktioniert sowohl für neue als auch zur Korrektur bereits beendeter Matches.
     */
    fun submitManualScore(
        matchId: String,
        winnerPlayerId: String,
        player1Legs: Int,
        player2Legs: Int
    ): Boolean {
        val state = currentTournament ?: return false
        val match = state.matches.find { it.id == matchId } ?: return false
        return if (match.isFinished) {
            overrideMatchResult(matchId, winnerPlayerId, player1Legs, player2Legs)
        } else {
            val p1Result = match.player1Result.copy(legsWon = player1Legs)
            val p2Result = match.player2Result.copy(legsWon = player2Legs)
            resolveMatch(matchId, winnerPlayerId, p1Result, p2Result)
        }
    }

    /**
     * Berechnet die aktuelle Rangliste für das Round-Robin-Turnier mit Dart-Tie-Breakern.
     */
    fun getLeagueStandings(): List<LeagueStandingRow> {
        val state = currentTournament ?: return emptyList()
        return calculateStandingsForMatches(state.players, state.matches)
    }

    private fun calculateStandingsForMatches(
        players: List<TournamentPlayer>,
        matches: List<TournamentMatch>
    ): List<LeagueStandingRow> {
        data class RawStats(
            var played: Int = 0,
            var won: Int = 0,
            var lost: Int = 0,
            var legsWon: Int = 0,
            var legsLost: Int = 0,
            var totalAvgSum: Double = 0.0,
            var avgCount: Int = 0
        )

        val statsMap = players.associate { it.id to RawStats() }.toMutableMap()

        for (m in matches) {
            if (m.isFinished && m.winner != null && m.player1 != null && m.player2 != null) {
                val s1 = statsMap.getOrPut(m.player1.id) { RawStats() }
                val s2 = statsMap.getOrPut(m.player2.id) { RawStats() }

                s1.played++
                s2.played++

                s1.legsWon += m.player1Result.legsWon
                s1.legsLost += m.player2Result.legsWon
                s2.legsWon += m.player2Result.legsWon
                s2.legsLost += m.player1Result.legsWon

                if (m.player1Result.average > 0) {
                    s1.totalAvgSum += m.player1Result.average
                    s1.avgCount++
                }
                if (m.player2Result.average > 0) {
                    s2.totalAvgSum += m.player2Result.average
                    s2.avgCount++
                }

                if (m.winner.id == m.player1.id) {
                    s1.won++
                    s2.lost++
                } else {
                    s2.won++
                    s1.lost++
                }
            }
        }

        // Sortierung mit Tie-Breaks:
        // 1. Meiste Siege
        // 2. Direkter Vergleich (Head-to-Head)
        // 3. Leg-Differenz
        // 4. Bester Average
        val sortedPlayers = players.sortedWith { p1, p2 ->
            val st1 = statsMap[p1.id] ?: RawStats()
            val st2 = statsMap[p2.id] ?: RawStats()

            // 1. Siege
            if (st1.won != st2.won) {
                return@sortedWith st2.won.compareTo(st1.won)
            }

            // 2. Direkter Vergleich
            val headToHeadMatch = matches.find {
                it.isFinished &&
                ((it.player1?.id == p1.id && it.player2?.id == p2.id) ||
                 (it.player1?.id == p2.id && it.player2?.id == p1.id))
            }
            if (headToHeadMatch?.winner != null) {
                if (headToHeadMatch.winner.id == p1.id) return@sortedWith -1
                if (headToHeadMatch.winner.id == p2.id) return@sortedWith 1
            }

            // 3. Leg-Differenz
            val diff1 = st1.legsWon - st1.legsLost
            val diff2 = st2.legsWon - st2.legsLost
            if (diff1 != diff2) {
                return@sortedWith diff2.compareTo(diff1)
            }

            // 4. Average
            val avg1 = if (st1.avgCount > 0) st1.totalAvgSum / st1.avgCount else 0.0
            val avg2 = if (st2.avgCount > 0) st2.totalAvgSum / st2.avgCount else 0.0
            avg2.compareTo(avg1)
        }

        return sortedPlayers.mapIndexed { index, player ->
            val st = statsMap[player.id] ?: RawStats()
            val avg = if (st.avgCount > 0) st.totalAvgSum / st.avgCount else 0.0
            LeagueStandingRow(
                rank = index + 1,
                player = player,
                played = st.played,
                won = st.won,
                lost = st.lost,
                legsWon = st.legsWon,
                legsLost = st.legsLost,
                legDifference = st.legsWon - st.legsLost,
                averageScore = (avg * 10).toInt() / 10.0
            )
        }
    }

    /**
     * Speichert den aktuellen Turnierstatus persistent ab.
     */
    fun saveToPreferences() {
        val state = currentTournament
        if (state != null) {
            try {
                val str = json.encodeToString(TournamentState.serializer(), state)
                saveAppPreference(PREF_KEY_ACTIVE_TOURNAMENT, str)
            } catch (e: Exception) {
                println("Fehler beim Speichern des Turniers: ${e.message}")
            }
        }
        onTournamentUpdated?.invoke()
    }

    /**
     * Lädt den persistenten Turnierstatus falls vorhanden.
     */
    fun loadFromPreferences(): Boolean {
        try {
            val str = loadAppPreference(PREF_KEY_ACTIVE_TOURNAMENT)
            if (!str.isNullOrBlank()) {
                currentTournament = json.decodeFromString(TournamentState.serializer(), str)
                onTournamentUpdated?.invoke()
                return true
            }
        } catch (e: Exception) {
            println("Fehler beim Laden des Turniers: ${e.message}")
        }
        return false
    }

    /**
     * Löscht das aktuelle Turnier aus Speicher und Persistenz.
     */
    fun clearTournament() {
        currentTournament = null
        pendingSubmissions.clear()
        saveAppPreference(PREF_KEY_ACTIVE_TOURNAMENT, "")
        onTournamentUpdated?.invoke()
    }

    /**
     * Verarbeitet ein über die Web-Schnittstelle oder Companion-App eingereichtes Ergebnis.
     */
    fun submitExternalResult(
        submission: com.example.myapplication.tournament.web.MatchSubmissionRequest,
        requireApproval: Boolean
    ): Boolean {
        val state = currentTournament ?: return false
        val match = state.matches.find { it.id == submission.matchId } ?: return false
        if (match.isFinished || match.player1 == null || match.player2 == null) return false

        if (!requireApproval) {
            val p1Result = MatchPlayerResult(
                legsWon = submission.player1Legs,
                average = submission.player1Average ?: 0.0
            )
            val p2Result = MatchPlayerResult(
                legsWon = submission.player2Legs,
                average = submission.player2Average ?: 0.0
            )
            return resolveMatch(submission.matchId, submission.winnerPlayerId, p1Result, p2Result)
        } else {
            val winnerName = if (submission.winnerPlayerId == match.player1.id) match.player1.name else match.player2.name
            val pending = com.example.myapplication.tournament.web.PendingMatchSubmission(
                matchId = submission.matchId,
                submission = submission,
                player1Name = match.player1.name,
                player2Name = match.player2.name,
                winnerName = winnerName
            )
            pendingSubmissions.removeAll { it.matchId == submission.matchId }
            pendingSubmissions.add(pending)
            onTournamentUpdated?.invoke()
            return true
        }
    }

    /**
     * Bestätigt ein ausstehendes externes Ergebnis und schaltet das Match scharf.
     */
    fun approveSubmission(submissionId: String): Boolean {
        val pending = pendingSubmissions.find { it.id == submissionId } ?: return false
        val sub = pending.submission
        val p1Result = MatchPlayerResult(
            legsWon = sub.player1Legs,
            average = sub.player1Average ?: 0.0
        )
        val p2Result = MatchPlayerResult(
            legsWon = sub.player2Legs,
            average = sub.player2Average ?: 0.0
        )
        val success = resolveMatch(sub.matchId, sub.winnerPlayerId, p1Result, p2Result)
        if (success) {
            pendingSubmissions.removeAll { it.id == submissionId }
            onTournamentUpdated?.invoke()
        }
        return success
    }

    /**
     * Weist ein fehlerhaftes externes Ergebnis ab.
     */
    fun rejectSubmission(submissionId: String): Boolean {
        val removed = pendingSubmissions.removeAll { it.id == submissionId }
        if (removed) {
            onTournamentUpdated?.invoke()
        }
        return removed
    }
}
