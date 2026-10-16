package com.example.myapplication.tournament

import kotlin.math.ceil
import kotlin.math.log2

object TournamentScheduler {

    /**
     * Erstellt einen Single-Elimination (K.-o.-) Turnierbaum.
     * Unterstützt jede Teilnehmerzahl >= 2.
     * Freilose (Byes) werden automatisch und fair verteilt, ohne dass zwei Freilose
     * aufeinandertreffen. Spieler mit Freilos rücken automatisch in Runde 2 vor.
     */
    fun createSingleEliminationSchedule(
        tournamentId: String,
        players: List<TournamentPlayer>
    ): List<TournamentMatch> {
        require(players.size >= 2) { "Ein K.O.-Turnier benötigt mindestens 2 Spieler." }

        val numPlayers = players.size
        val numRounds = ceil(log2(numPlayers.toDouble())).toInt().coerceAtLeast(1)
        val totalSlots = 1 shl numRounds // 2^numRounds (z.B. 2, 4, 8, 16)

        val matches = mutableListOf<TournamentMatch>()

        // 1. Baumstruktur rückwärts vom Finale zu Runde 1 anlegen
        // Finale (Runde numRounds, Index 0)
        val finalMatch = TournamentMatch(
            id = "${tournamentId}_r${numRounds}_m0",
            round = numRounds,
            matchIndexInRound = 0,
            nextMatchId = null,
            nextMatchSlot = 1
        )
        matches.add(finalMatch)

        var currentRoundMatches = listOf(finalMatch)

        // Runde (numRounds - 1) bis Runde 1
        for (r in (numRounds - 1) downTo 1) {
            val previousRoundMatches = mutableListOf<TournamentMatch>()
            for ((parentIndex, parentMatch) in currentRoundMatches.withIndex()) {
                val m1 = TournamentMatch(
                    id = "${tournamentId}_r${r}_m${parentIndex * 2}",
                    round = r,
                    matchIndexInRound = parentIndex * 2,
                    nextMatchId = parentMatch.id,
                    nextMatchSlot = 1
                )
                val m2 = TournamentMatch(
                    id = "${tournamentId}_r${r}_m${parentIndex * 2 + 1}",
                    round = r,
                    matchIndexInRound = parentIndex * 2 + 1,
                    nextMatchId = parentMatch.id,
                    nextMatchSlot = 2
                )
                matches.add(m1)
                matches.add(m2)
                previousRoundMatches.add(m1)
                previousRoundMatches.add(m2)
            }
            currentRoundMatches = previousRoundMatches
        }

        // 2. Seeding-Reihenfolge nach Standard-Turnierverteilung berechnen
        val seeding = getSeedingOrder(totalSlots)
        // seeding ist 1-basiert, z.B. [1, 8, 4, 5, 2, 7, 3, 6] für 8 Slots

        // Zuordnung: Seed 1..numPlayers sind echte Spieler, alles darüber sind Freilose (null)
        val seededParticipants = seeding.map { seedNum ->
            if (seedNum <= numPlayers) players[seedNum - 1] else null
        }

        // 3. Teilnehmer in Runde 1 Matches eintragen
        val round1Matches = matches
            .filter { it.round == 1 }
            .sortedBy { it.matchIndexInRound }

        val updatedRound1Matches = mutableListOf<TournamentMatch>()

        for (i in round1Matches.indices) {
            val p1 = seededParticipants[i * 2]
            val p2 = seededParticipants[i * 2 + 1]
            val original = round1Matches[i]

            // Prüfen ob Freilos vorliegt
            val isByeMatch = (p1 != null && p2 == null) || (p1 == null && p2 != null)
            val byeWinner = if (isByeMatch) (p1 ?: p2) else null

            val filledMatch = original.copy(
                player1 = p1,
                player2 = p2,
                winner = byeWinner,
                isFinished = isByeMatch,
                isBye = isByeMatch
            )
            updatedRound1Matches.add(filledMatch)
        }

        // Ersetze Runde 1 in der Gesamtliste
        val finalMatches = matches.map { m ->
            if (m.round == 1) {
                updatedRound1Matches.first { it.id == m.id }
            } else {
                m
            }
        }.toMutableList()

        // 4. Spieler mit Freilos direkt in Runde 2 befördern
        for (m in updatedRound1Matches) {
            if (m.isBye && m.winner != null && m.nextMatchId != null) {
                val nextMatchIndex = finalMatches.indexOfFirst { it.id == m.nextMatchId }
                if (nextMatchIndex >= 0) {
                    val nextMatch = finalMatches[nextMatchIndex]
                    val updatedNext = if (m.nextMatchSlot == 1) {
                        nextMatch.copy(player1 = m.winner)
                    } else {
                        nextMatch.copy(player2 = m.winner)
                    }
                    finalMatches[nextMatchIndex] = updatedNext
                }
            }
        }

        return finalMatches.sortedWith(compareBy({ it.round }, { it.matchIndexInRound }))
    }

    /**
     * Erstellt den Spielplan für Jeder-gegen-Jeden (Round Robin).
     * Verwendet die klassische Circle-Methode.
     */
    fun createRoundRobinSchedule(
        tournamentId: String,
        players: List<TournamentPlayer>
    ): List<TournamentMatch> {
        require(players.size >= 2) { "Ein Liga-Turnier benötigt mindestens 2 Spieler." }

        val pool = players.toMutableList<TournamentPlayer?>()
        if (pool.size % 2 != 0) {
            pool.add(null) // Bye-Platzhalter für ungerade Anzahl
        }

        val numParticipants = pool.size
        val numRounds = numParticipants - 1
        val matchesPerRound = numParticipants / 2

        val matches = mutableListOf<TournamentMatch>()
        var matchCounter = 0

        for (round in 1..numRounds) {
            for (i in 0 until matchesPerRound) {
                val p1 = pool[i]
                val p2 = pool[numParticipants - 1 - i]

                // Nur echte Spiele (ohne Bye-Platzhalter) erzeugen
                if (p1 != null && p2 != null) {
                    matches.add(
                        TournamentMatch(
                            id = "${tournamentId}_rr_m${matchCounter++}",
                            round = round,
                            matchIndexInRound = i,
                            player1 = p1,
                            player2 = p2
                        )
                    )
                }
            }

            // Circle-Rotation: Erstes Element bleibt fest, die restlichen rotieren im Uhrzeigersinn
            val nextPool = mutableListOf<TournamentPlayer?>()
            nextPool.add(pool[0])
            nextPool.add(pool.last())
            for (k in 1 until pool.size - 1) {
                nextPool.add(pool[k])
            }
            pool.clear()
            pool.addAll(nextPool)
        }

        return matches.sortedWith(compareBy({ it.round }, { it.matchIndexInRound }))
    }

    /**
     * Berechnet die Standard-Setzreihenfolge für eine gegebene Zweierpotenz.
     * Beispiel für 8 Slots: [1, 8, 4, 5, 2, 7, 3, 6]
     */
    internal fun getSeedingOrder(size: Int): List<Int> {
        if (size <= 1) return listOf(1)
        var order = listOf(1, 2)
        while (order.size < size) {
            val next = mutableListOf<Int>()
            val matchSum = order.size * 2 + 1
            for (s in order) {
                next.add(s)
                next.add(matchSum - s)
            }
            order = next
        }
        return order
    }
}
