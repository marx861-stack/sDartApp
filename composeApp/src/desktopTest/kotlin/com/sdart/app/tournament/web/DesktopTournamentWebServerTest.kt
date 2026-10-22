package com.sdart.app.tournament.web

import com.sdart.app.GameMode
import com.sdart.app.tournament.TournamentGameConfig
import com.sdart.app.tournament.TournamentManager
import com.sdart.app.tournament.TournamentPlayer
import com.sdart.app.tournament.TournamentType
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.*

class DesktopTournamentWebServerTest {

    private fun makePlayers(count: Int): List<TournamentPlayer> {
        return (1..count).map {
            TournamentPlayer(id = "p$it", name = "Player $it")
        }
    }

    @Test
    fun testServerLifecycleAndHttpEndpoints() {
        val manager = TournamentManager()
        val players = makePlayers(4)
        val state = manager.createTournament(
            name = "Live Test Cup",
            type = TournamentType.SINGLE_ELIMINATION,
            config = TournamentGameConfig(mode = GameMode.X501, legs = 3),
            players = players
        )

        val server = DesktopTournamentWebServer(manager)
        val port = server.start(0) // Port 0 = automatischer freier Port
        assertTrue(server.isRunning)
        assertTrue(port > 0)

        val baseUrl = "http://127.0.0.1:$port"

        try {
            // 1. GET / -> HTML Web App
            val urlIndex = URL("$baseUrl/")
            val connIndex = urlIndex.openConnection() as HttpURLConnection
            connIndex.connectTimeout = 3000
            connIndex.readTimeout = 3000
            assertEquals(200, connIndex.responseCode)
            val htmlContent = connIndex.inputStream.bufferedReader().readText()
            assertTrue(htmlContent.contains("s'Dart Turnier"))
            assertTrue(htmlContent.contains("Endstand melden"))
            connIndex.disconnect()

            // 2. GET /api/tournament -> JSON Übersicht
            val urlApi = URL("$baseUrl/api/tournament")
            val connApi = urlApi.openConnection() as HttpURLConnection
            connApi.connectTimeout = 3000
            connApi.readTimeout = 3000
            assertEquals(200, connApi.responseCode)
            val jsonContent = connApi.inputStream.bufferedReader().readText()
            assertTrue(jsonContent.contains("Live Test Cup"))
            connApi.disconnect()

            // 3. POST /api/matches/{id}/submit
            val match = state.matches.first { it.round == 1 && it.isReady }
            val urlSubmit = URL("$baseUrl/api/matches/${match.id}/submit")
            val connSubmit = urlSubmit.openConnection() as HttpURLConnection
            connSubmit.requestMethod = "POST"
            connSubmit.doOutput = true
            connSubmit.setRequestProperty("Content-Type", "application/json")
            connSubmit.connectTimeout = 3000
            connSubmit.readTimeout = 3000

            val postJson = """
                {
                    "matchId": "${match.id}",
                    "winnerPlayerId": "${match.player1!!.id}",
                    "player1Legs": 3,
                    "player2Legs": 1,
                    "submittedByPlayerName": "${match.player1!!.name}"
                }
            """.trimIndent()

            OutputStreamWriter(connSubmit.outputStream).use { it.write(postJson) }

            assertEquals(200, connSubmit.responseCode)
            val submitResp = connSubmit.inputStream.bufferedReader().readText()
            assertTrue(submitResp.contains("\"success\":true"))
            connSubmit.disconnect()

            // Überprüfe, dass Submission in manager.pendingSubmissions angekommen ist
            assertEquals(1, manager.pendingSubmissions.size)
            val pending = manager.pendingSubmissions.first()
            assertEquals(match.id, pending.matchId)

        } finally {
            server.stop()
            assertFalse(server.isRunning)
        }
    }

    @Test
    fun testSubmitWithUmlautsAndContentLength() {
        val manager = TournamentManager()
        val players = listOf(
            TournamentPlayer(id = "p1", name = "Jürgen Müller"),
            TournamentPlayer(id = "p2", name = "Björn Schütz")
        )
        val state = manager.createTournament(
            name = "Umlaut Cup",
            type = TournamentType.SINGLE_ELIMINATION,
            config = TournamentGameConfig(mode = GameMode.X501, legs = 3),
            players = players
        )

        val server = DesktopTournamentWebServer(manager)
        val port = server.start(0)
        val baseUrl = "http://127.0.0.1:$port"

        try {
            val match = state.matches.first { it.round == 1 && it.isReady }
            val urlSubmit = URL("$baseUrl/api/matches/${match.id}/submit")
            val connSubmit = urlSubmit.openConnection() as HttpURLConnection
            connSubmit.requestMethod = "POST"
            connSubmit.doOutput = true
            connSubmit.setRequestProperty("Content-Type", "application/json")
            connSubmit.connectTimeout = 3000
            connSubmit.readTimeout = 3000

            val postJson = """
                {
                    "matchId": "${match.id}",
                    "winnerPlayerId": "p1",
                    "player1Legs": 3,
                    "player2Legs": 1,
                    "submittedByPlayerName": "Jürgen Müller"
                }
            """.trimIndent()
            val utf8Bytes = postJson.toByteArray(Charsets.UTF_8)
            connSubmit.setRequestProperty("Content-Length", utf8Bytes.size.toString())

            connSubmit.outputStream.use { it.write(utf8Bytes) }

            assertEquals(200, connSubmit.responseCode)
            val submitResp = connSubmit.inputStream.bufferedReader().readText()
            assertTrue(submitResp.contains("\"success\":true"))
            connSubmit.disconnect()
        } finally {
            server.stop()
        }
    }
}
