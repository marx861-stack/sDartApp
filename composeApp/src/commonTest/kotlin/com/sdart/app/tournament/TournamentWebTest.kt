package com.sdart.app.tournament

import com.sdart.app.GameMode
import com.sdart.app.tournament.web.MatchSubmissionRequest
import com.sdart.app.tournament.web.SimpleQrCode
import kotlin.test.*

class TournamentWebTest {

    private fun makePlayers(count: Int): List<TournamentPlayer> {
        return (1..count).map {
            TournamentPlayer(id = "p$it", name = "Spieler $it")
        }
    }

    @Test
    fun testQrCodeGeneration() {
        val url = "http://192.168.1.105:8080"
        val matrix = SimpleQrCode.encode(url)

        assertTrue(matrix.isNotEmpty())
        assertEquals(matrix.size, matrix[0].size) // Quadratisch
        assertTrue(matrix.size >= 21) // Mindestens Version 1

        // Prüfe Finder Pattern oben links (7x7 Block mit schwarzem Rand)
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][6])
        assertTrue(matrix[6][0])
        assertTrue(matrix[6][6])
        assertTrue(matrix[2][2]) // Kern
    }

    @Test
    fun testExternalSubmissionWithHostApproval() {
        val manager = TournamentManager()
        val players = makePlayers(4)
        val state = manager.createTournament(
            name = "Test Cup",
            type = TournamentType.SINGLE_ELIMINATION,
            config = TournamentGameConfig(mode = GameMode.X501, legs = 3),
            players = players
        )

        val firstMatch = state.matches.first { it.round == 1 && it.isReady }
        val p1 = firstMatch.player1!!
        val p2 = firstMatch.player2!!

        var updateNotified = false
        manager.onTournamentUpdated = {
            updateNotified = true
        }

        // Spieler 1 meldet 3:1 Sieg
        val request = MatchSubmissionRequest(
            matchId = firstMatch.id,
            winnerPlayerId = p1.id,
            player1Legs = 3,
            player2Legs = 1,
            submittedByPlayerName = p1.name
        )

        val submitted = manager.submitExternalResult(request, requireApproval = true)
        assertTrue(submitted)
        assertTrue(updateNotified)
        assertEquals(1, manager.pendingSubmissions.size)

        val pending = manager.pendingSubmissions.first()
        assertEquals(firstMatch.id, pending.matchId)
        assertEquals(p1.name, pending.winnerName)

        // Match am Host darf noch nicht abgeschlossen sein
        val matchBeforeApproval = manager.currentTournament!!.matches.find { it.id == firstMatch.id }!!
        assertFalse(matchBeforeApproval.isFinished)

        // Host bestätigt die Meldung
        val approved = manager.approveSubmission(pending.id)
        assertTrue(approved)
        assertEquals(0, manager.pendingSubmissions.size)

        // Match muss nun abgeschlossen sein und p1 der Sieger
        val matchAfterApproval = manager.currentTournament!!.matches.find { it.id == firstMatch.id }!!
        assertTrue(matchAfterApproval.isFinished)
        assertEquals(p1.id, matchAfterApproval.winner?.id)
        assertEquals(3, matchAfterApproval.player1Result.legsWon)
        assertEquals(1, matchAfterApproval.player2Result.legsWon)

        // Sieger rückt ins Finale vor
        val finalMatch = manager.currentTournament!!.matches.find { it.id == firstMatch.nextMatchId }!!
        assertEquals(p1.id, finalMatch.player1?.id)
    }

    @Test
    fun testExternalSubmissionDirectAutoApply() {
        val manager = TournamentManager()
        val players = makePlayers(4)
        val state = manager.createTournament(
            name = "Test Cup Direct",
            type = TournamentType.SINGLE_ELIMINATION,
            config = TournamentGameConfig(mode = GameMode.X501, legs = 2),
            players = players
        )

        val firstMatch = state.matches.first { it.round == 1 && it.isReady }
        val p1 = firstMatch.player1!!

        // Direktübernahme (requireApproval = false)
        val request = MatchSubmissionRequest(
            matchId = firstMatch.id,
            winnerPlayerId = p1.id,
            player1Legs = 2,
            player2Legs = 0
        )

        val submitted = manager.submitExternalResult(request, requireApproval = false)
        assertTrue(submitted)
        assertEquals(0, manager.pendingSubmissions.size)

        val match = manager.currentTournament!!.matches.find { it.id == firstMatch.id }!!
        assertTrue(match.isFinished)
        assertEquals(p1.id, match.winner?.id)
    }

    @Test
    fun testRejectSubmission() {
        val manager = TournamentManager()
        val players = makePlayers(4)
        val state = manager.createTournament(
            name = "Test Cup Reject",
            type = TournamentType.SINGLE_ELIMINATION,
            config = TournamentGameConfig(legs = 3),
            players = players
        )

        val firstMatch = state.matches.first { it.round == 1 && it.isReady }
        val p1 = firstMatch.player1!!

        val request = MatchSubmissionRequest(
            matchId = firstMatch.id,
            winnerPlayerId = p1.id,
            player1Legs = 3,
            player2Legs = 2
        )

        manager.submitExternalResult(request, requireApproval = true)
        assertEquals(1, manager.pendingSubmissions.size)

        val pending = manager.pendingSubmissions.first()
        val rejected = manager.rejectSubmission(pending.id)
        assertTrue(rejected)
        assertEquals(0, manager.pendingSubmissions.size)

        // Match bleibt unfertig
        val match = manager.currentTournament!!.matches.find { it.id == firstMatch.id }!!
        assertFalse(match.isFinished)
    }

    @Test
    fun testHostManualScoreSubmissionWithLegs() {
        val manager = TournamentManager()
        val players = makePlayers(4)
        val state = manager.createTournament(
            name = "Manual Legs Cup",
            type = TournamentType.ROUND_ROBIN,
            config = TournamentGameConfig(legs = 5),
            players = players
        )

        val match = state.matches.first { it.isReady }
        val p1 = match.player1!!
        val p2 = match.player2!!

        // Host erfasst manuell 3:1 für Spieler 1
        val success = manager.submitManualScore(match.id, p1.id, 3, 1)
        assertTrue(success)

        val updated = manager.currentTournament!!.matches.find { it.id == match.id }!!
        assertTrue(updated.isFinished)
        assertEquals(p1.id, updated.winner?.id)
        assertEquals(3, updated.player1Result.legsWon)
        assertEquals(1, updated.player2Result.legsWon)

        // Liga-Tabelle prüfen: Spieler 1 muss 1 Sieg, 3:1 Legs, Diff +2 haben
        val standings = manager.getLeagueStandings()
        val p1Row = standings.find { it.player.id == p1.id }!!
        assertEquals(1, p1Row.won)
        assertEquals(3, p1Row.legsWon)
        assertEquals(1, p1Row.legsLost)
        assertEquals(2, p1Row.legDifference)

        // Host korrigiert manuell auf 3:2 für Spieler 1
        val corrected = manager.submitManualScore(match.id, p1.id, 3, 2)
        assertTrue(corrected)
        val correctedMatch = manager.currentTournament!!.matches.find { it.id == match.id }!!
        assertEquals(3, correctedMatch.player1Result.legsWon)
        assertEquals(2, correctedMatch.player2Result.legsWon)

        val updatedStandings = manager.getLeagueStandings()
        val correctedRow = updatedStandings.find { it.player.id == p1.id }!!
        assertEquals(1, correctedRow.legDifference)
    }
}
