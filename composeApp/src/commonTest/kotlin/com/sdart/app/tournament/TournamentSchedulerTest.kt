package com.sdart.app.tournament

import com.sdart.app.GameMode
import kotlinx.serialization.json.Json
import kotlin.test.*

class TournamentSchedulerTest {

    private fun makePlayers(count: Int): List<TournamentPlayer> {
        return (1..count).map {
            TournamentPlayer(id = "p$it", name = "Spieler $it")
        }
    }

    @Test
    fun testSingleEliminationExactPowerOfTwo() {
        val players4 = makePlayers(4)
        val matches4 = TournamentScheduler.createSingleEliminationSchedule("t4", players4)

        // Bei 4 Spielern: 2 Halbfinals (Runde 1) + 1 Finale (Runde 2) = 3 Matches
        assertEquals(3, matches4.size)

        val round1 = matches4.filter { it.round == 1 }
        val round2 = matches4.filter { it.round == 2 }

        assertEquals(2, round1.size)
        assertEquals(1, round2.size)

        // Keine Byes bei 4 Spielern
        assertTrue(round1.none { it.isBye })
        assertTrue(round1.all { it.player1 != null && it.player2 != null })

        // Alle Round 1 Matches verweisen aufs Finale
        val finalMatch = round2.first()
        assertTrue(round1.all { it.nextMatchId == finalMatch.id })
        assertEquals(1, round1[0].nextMatchSlot)
        assertEquals(2, round1[1].nextMatchSlot)
    }

    @Test
    fun testSingleEliminationWithByesOddPlayerCount() {
        // 5 Spieler -> 8 Slots -> 3 Runden (QF, SF, F)
        // 5 echte Spieler, 3 Freilose (Byes)
        val players5 = makePlayers(5)
        val matches5 = TournamentScheduler.createSingleEliminationSchedule("t5", players5)

        // 8er Baum: 4 QF + 2 SF + 1 F = 7 Matches
        assertEquals(7, matches5.size)

        val round1 = matches5.filter { it.round == 1 }
        val round2 = matches5.filter { it.round == 2 }
        assertEquals(4, round1.size)
        assertEquals(2, round2.size)

        // Genau 3 Byes in Runde 1
        val byeMatches = round1.filter { it.isBye }
        assertEquals(3, byeMatches.size)

        // Niemals 2 Byes gegeneinander
        assertTrue(round1.none { it.player1 == null && it.player2 == null })

        // Alle Bye-Matches sind bereits als beendet markiert mit Sieger
        assertTrue(byeMatches.all { it.isFinished && it.winner != null })

        // Spieler mit Freilos müssen bereits in Runde 2 eingetragen sein
        val round2PlayerCount = round2.count { it.player1 != null } + round2.count { it.player2 != null }
        assertEquals(3, round2PlayerCount, "Die 3 Freilos-Gewinner müssen direkt in Runde 2 stehen")
    }

    @Test
    fun testTournamentManagerKOProgression() {
        val manager = TournamentManager()
        val players = makePlayers(4)
        val config = TournamentGameConfig(mode = GameMode.X501, legs = 3)

        val state = manager.createTournament("Test Cup", TournamentType.SINGLE_ELIMINATION, config, players)
        assertFalse(state.isFinished)
        assertNull(state.winner)

        // Halbfinale 1 spielen
        val sf1 = manager.currentTournament!!.matches.first { it.round == 1 && it.matchIndexInRound == 0 }
        assertTrue(sf1.isReady)

        manager.resolveMatch(
            matchId = sf1.id,
            winnerPlayerId = sf1.player1!!.id,
            player1Result = MatchPlayerResult(legsWon = 3),
            player2Result = MatchPlayerResult(legsWon = 1)
        )

        // Finale sollte nun Spieler 1 im Slot 1 haben
        val final1 = manager.currentTournament!!.matches.first { it.round == 2 }
        assertEquals(sf1.player1!!.id, final1.player1?.id)
        assertNull(final1.player2)

        // Halbfinale 2 spielen
        val sf2 = manager.currentTournament!!.matches.first { it.round == 1 && it.matchIndexInRound == 1 }
        manager.resolveMatch(
            matchId = sf2.id,
            winnerPlayerId = sf2.player2!!.id,
            player1Result = MatchPlayerResult(legsWon = 0),
            player2Result = MatchPlayerResult(legsWon = 3)
        )

        // Finale sollte nun spielbereit sein
        val finalReady = manager.currentTournament!!.matches.first { it.round == 2 }
        assertTrue(finalReady.isReady)
        assertEquals(sf2.player2!!.id, finalReady.player2?.id)

        // Finale spielen
        manager.resolveMatch(
            matchId = finalReady.id,
            winnerPlayerId = finalReady.player1!!.id,
            player1Result = MatchPlayerResult(legsWon = 3),
            player2Result = MatchPlayerResult(legsWon = 2)
        )

        val finishedState = manager.currentTournament!!
        assertTrue(finishedState.isFinished)
        assertEquals(sf1.player1!!.id, finishedState.winner?.id)
    }

    @Test
    fun testRoundRobinScheduleAndStandings() {
        val manager = TournamentManager()
        val players = makePlayers(3)
        val config = TournamentGameConfig(mode = GameMode.X501, legs = 1)

        val state = manager.createTournament("Liga", TournamentType.ROUND_ROBIN, config, players)
        // Bei 3 Spielern (mit Pause/Bye): 3 Runden mit je 1 Spiel = 3 Spiele gesamt
        assertEquals(3, state.matches.size)

        // Jedes Paar muss genau 1 Mal vorkommen
        val p1 = players[0]
        val p2 = players[1]
        val p3 = players[2]

        val m12 = state.matches.find { (it.player1?.id == p1.id && it.player2?.id == p2.id) || (it.player1?.id == p2.id && it.player2?.id == p1.id) }
        val m13 = state.matches.find { (it.player1?.id == p1.id && it.player2?.id == p3.id) || (it.player1?.id == p3.id && it.player2?.id == p1.id) }
        val m23 = state.matches.find { (it.player1?.id == p2.id && it.player2?.id == p3.id) || (it.player1?.id == p3.id && it.player2?.id == p2.id) }

        assertNotNull(m12)
        assertNotNull(m13)
        assertNotNull(m23)

        // Spiele austragen: p1 schlägt p2, p2 schlägt p3, p1 schlägt p3
        manager.resolveMatch(m12.id, p1.id, MatchPlayerResult(legsWon = 1, average = 60.0), MatchPlayerResult(legsWon = 0, average = 50.0))
        manager.resolveMatch(m23.id, p2.id, MatchPlayerResult(legsWon = 1, average = 55.0), MatchPlayerResult(legsWon = 0, average = 45.0))
        manager.resolveMatch(m13.id, p1.id, MatchPlayerResult(legsWon = 1, average = 65.0), MatchPlayerResult(legsWon = 0, average = 40.0))

        val finalState = manager.currentTournament!!
        assertTrue(finalState.isFinished)
        assertEquals(p1.id, finalState.winner?.id, "p1 hat 2 Siege und muss Erster sein")

        val standings = manager.getLeagueStandings()
        assertEquals(3, standings.size)
        assertEquals(p1.id, standings[0].player.id)
        assertEquals(2, standings[0].won)
        assertEquals(p2.id, standings[1].player.id)
        assertEquals(1, standings[1].won)
        assertEquals(p3.id, standings[2].player.id)
        assertEquals(0, standings[2].won)
    }

    @Test
    fun testSerializationDeserialization() {
        val players = makePlayers(4)
        val config = TournamentGameConfig(mode = GameMode.X301, legs = 3)
        val matches = TournamentScheduler.createSingleEliminationSchedule("t_serial", players)
        val original = TournamentState(
            id = "t_serial",
            name = "Meisterschaft",
            type = TournamentType.SINGLE_ELIMINATION,
            config = config,
            players = players,
            matches = matches
        )

        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val encoded = json.encodeToString(TournamentState.serializer(), original)
        val decoded = json.decodeFromString(TournamentState.serializer(), encoded)

        assertEquals(original.id, decoded.id)
        assertEquals(original.name, decoded.name)
        assertEquals(original.players.size, decoded.players.size)
        assertEquals(original.matches.size, decoded.matches.size)
    }
}
