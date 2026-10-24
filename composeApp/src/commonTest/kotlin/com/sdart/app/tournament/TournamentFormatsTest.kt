package com.sdart.app.tournament

import com.sdart.app.GameMode
import kotlin.test.*

/**
 * Tests für Doppel-K.-o. und Gruppenphase mit anschließender K.-o.-Runde.
 */
class TournamentFormatsTest {

    private val config = TournamentGameConfig(mode = GameMode.X501, legs = 1)

    private fun makePlayers(count: Int): List<TournamentPlayer> =
        (1..count).map { TournamentPlayer(id = "p$it", name = "Spieler $it") }

    private fun seedOf(player: TournamentPlayer) = player.id.removePrefix("p").toInt()

    /**
     * Spielt das Turnier bis zum Ende. [pickWinner] bestimmt den Sieger jedes Matches.
     * Gibt die Niederlagen je Spieler zurück.
     */
    private fun playOut(
        manager: TournamentManager,
        pickWinner: (TournamentMatch) -> TournamentPlayer
    ): Map<String, Int> {
        val losses = mutableMapOf<String, Int>()
        var guard = 0
        while (manager.currentTournament?.isFinished == false) {
            val match = manager.currentTournament!!.matches.firstOrNull { it.isReady }
                ?: fail("Turnier hängt: kein spielbereites Match, aber nicht beendet")
            assertNotEquals(match.player1!!.id, match.player2!!.id, "Spieler darf nicht gegen sich selbst spielen")
            val winner = pickWinner(match)
            val loser = if (winner.id == match.player1.id) match.player2 else match.player1
            losses[loser.id] = (losses[loser.id] ?: 0) + 1
            assertTrue(manager.resolveMatch(match.id, winner.id))
            assertTrue(++guard < 500, "Zu viele Matches")
        }
        return losses
    }

    @Test
    fun doubleEliminationFinishesForAllPlayerCounts() {
        for (count in 3..17) {
            for (favoritesWin in listOf(true, false)) {
                val manager = TournamentManager(persistent = false)
                manager.createTournament("DKO $count", TournamentType.DOUBLE_ELIMINATION, config, makePlayers(count))
                val losses = playOut(manager) { m ->
                    val better = if (seedOf(m.player1!!) < seedOf(m.player2!!)) m.player1 else m.player2!!
                    val worse = if (better.id == m.player1.id) m.player2!! else m.player1
                    if (favoritesWin) better else worse
                }

                val state = manager.currentTournament!!
                val champion = state.winner ?: fail("Kein Sieger bei $count Spielern")
                // Alle außer dem Sieger scheiden mit genau zwei Niederlagen aus
                state.players.filter { it.id != champion.id }.forEach {
                    assertEquals(2, losses[it.id], "${it.name} bei $count Spielern")
                }
                assertTrue((losses[champion.id] ?: 0) <= 1, "Sieger hat höchstens eine Niederlage")
            }
        }
    }

    @Test
    fun doubleEliminationStructureForEightPlayers() {
        val matches = TournamentScheduler.createDoubleEliminationSchedule("t", makePlayers(8))
        assertEquals(7, matches.count { it.bracket == MatchBracket.MAIN })
        // Verliererrunde: 2 + 2 + 1 + 1 Matches
        assertEquals(6, matches.count { it.bracket == MatchBracket.LOSERS })
        assertEquals(1, matches.count { it.bracket == MatchBracket.GRAND_FINAL })
        // Jeder Verlierer der Gewinnerrunde hat ein Ziel in der Verliererrunde
        assertTrue(matches.filter { it.bracket == MatchBracket.MAIN }.all { it.loserNextMatchId != null })
    }

    @Test
    fun grandFinalResetWhenLoserBracketChampionWins() {
        val manager = TournamentManager(persistent = false)
        manager.createTournament("Reset", TournamentType.DOUBLE_ELIMINATION, config, makePlayers(4))

        // Spielen bis zum Grand Final, Favoriten gewinnen
        while (true) {
            val ready = manager.currentTournament!!.matches.first { it.isReady }
            if (ready.bracket == MatchBracket.GRAND_FINAL) break
            val winner = if (seedOf(ready.player1!!) < seedOf(ready.player2!!)) ready.player1 else ready.player2!!
            manager.resolveMatch(ready.id, winner.id)
        }

        val grandFinal = manager.currentTournament!!.matches.first { it.bracket == MatchBracket.GRAND_FINAL }
        // Spieler aus der Verliererrunde (Slot 2) gewinnt
        manager.resolveMatch(grandFinal.id, grandFinal.player2!!.id)

        val afterFirst = manager.currentTournament!!
        assertFalse(afterFirst.isFinished, "Nach dem ersten Grand Final folgt ein Entscheidungsspiel")
        val reset = afterFirst.matches.first { it.bracket == MatchBracket.GRAND_FINAL && it.round == 2 }
        assertTrue(reset.isReady)

        manager.resolveMatch(reset.id, reset.player2!!.id)
        assertTrue(manager.currentTournament!!.isFinished)
        assertEquals(grandFinal.player2!!.id, manager.currentTournament!!.winner?.id)
    }

    @Test
    fun overridingFirstGrandFinalRemovesResetMatch() {
        val manager = TournamentManager(persistent = false)
        manager.createTournament("Korrektur", TournamentType.DOUBLE_ELIMINATION, config, makePlayers(3))
        while (true) {
            val ready = manager.currentTournament!!.matches.first { it.isReady }
            if (ready.bracket == MatchBracket.GRAND_FINAL) break
            manager.resolveMatch(ready.id, ready.player1!!.id)
        }
        val grandFinal = manager.currentTournament!!.matches.first { it.bracket == MatchBracket.GRAND_FINAL }
        manager.resolveMatch(grandFinal.id, grandFinal.player2!!.id)
        assertTrue(manager.currentTournament!!.matches.any { it.id.endsWith("_gf_m1") })

        // Korrektur: doch der Spieler aus der Gewinnerrunde gewinnt
        assertTrue(manager.submitManualScore(grandFinal.id, grandFinal.player1!!.id, 1, 0))
        val state = manager.currentTournament!!
        assertTrue(state.matches.none { it.id.endsWith("_gf_m1") })
        assertTrue(state.isFinished)
        assertEquals(grandFinal.player1!!.id, state.winner?.id)
    }

    @Test
    fun groupsAreAssignedInSnakeOrder() {
        val groups = TournamentScheduler.assignGroups(makePlayers(8), 2)
        assertEquals(listOf("p1", "p4", "p5", "p8"), groups[0].map { it.id })
        assertEquals(listOf("p2", "p3", "p6", "p7"), groups[1].map { it.id })
    }

    @Test
    fun groupStageThenKnockout() {
        val manager = TournamentManager(persistent = false)
        val state = manager.createTournament(
            "Gruppen", TournamentType.GROUPS_KNOCKOUT, config, makePlayers(8),
            groupCount = 2, qualifiersPerGroup = 2
        )
        assertEquals(2, state.groups.size)
        // Je Gruppe 4 Spieler → 6 Spiele, zusammen 12
        assertEquals(12, state.matches.count { it.bracket == MatchBracket.GROUP })
        assertTrue(state.matches.none { it.bracket == MatchBracket.MAIN })

        // Gruppenphase: der Spieler mit der kleineren Nummer gewinnt
        while (manager.currentTournament!!.matches.any { it.bracket == MatchBracket.GROUP && !it.isFinished }) {
            val m = manager.currentTournament!!.matches.first { it.bracket == MatchBracket.GROUP && it.isReady }
            val winner = if (seedOf(m.player1!!) < seedOf(m.player2!!)) m.player1 else m.player2!!
            manager.resolveMatch(m.id, winner.id)
        }

        val knockout = manager.currentTournament!!.matches.filter { it.bracket == MatchBracket.MAIN }
        assertEquals(3, knockout.size, "4 Qualifikanten → 2 Halbfinals + Finale")
        val semiPlayers = knockout.filter { it.round == 1 }.flatMap { listOfNotNull(it.player1, it.player2) }.map { it.id }.toSet()
        // Gruppe A: 1,4,5,8 → 1 und 4 weiter; Gruppe B: 2,3,6,7 → 2 und 3 weiter
        assertEquals(setOf("p1", "p2", "p3", "p4"), semiPlayers)
        assertNoSameGroupPairing(manager.currentTournament!!)

        // Gruppenspiele lassen sich nach der Auslosung nicht mehr ändern
        val groupMatch = manager.currentTournament!!.matches.first { it.bracket == MatchBracket.GROUP }
        assertFalse(manager.submitManualScore(groupMatch.id, groupMatch.player2!!.id, 0, 1))

        playOut(manager) { it.player1!! }
        assertTrue(manager.currentTournament!!.isFinished)
        assertNotNull(manager.currentTournament!!.winner)
    }

    @Test
    fun knockoutFromThreeGroupsAvoidsSameGroupInFirstRound() {
        val manager = TournamentManager(persistent = false)
        manager.createTournament(
            "Drei Gruppen", TournamentType.GROUPS_KNOCKOUT, config, makePlayers(9),
            groupCount = 3, qualifiersPerGroup = 2
        )
        while (manager.currentTournament!!.matches.any { it.bracket == MatchBracket.GROUP && !it.isFinished }) {
            val m = manager.currentTournament!!.matches.first { it.bracket == MatchBracket.GROUP && it.isReady }
            manager.resolveMatch(m.id, m.player1!!.id)
        }
        val state = manager.currentTournament!!
        val qualifiers = state.matches.filter { it.bracket == MatchBracket.MAIN && it.round == 1 }
            .flatMap { listOfNotNull(it.player1, it.player2) } +
            state.matches.filter { it.bracket == MatchBracket.MAIN && it.round == 2 }
                .flatMap { listOfNotNull(it.player1, it.player2) }
        assertEquals(6, qualifiers.map { it.id }.toSet().size)
        assertNoSameGroupPairing(state)
    }

    private fun assertNoSameGroupPairing(state: TournamentState) {
        val groupOf = state.groups.flatMapIndexed { g, ids -> ids.map { it to g } }.toMap()
        state.matches
            .filter { it.bracket == MatchBracket.MAIN && it.round == 1 && it.player1 != null && it.player2 != null }
            .forEach {
                assertNotEquals(groupOf[it.player1!!.id], groupOf[it.player2!!.id], "Gleiche Gruppe in Runde 1: ${it.id}")
            }
    }
}
