package com.sdart.app.tournament.web

import com.sdart.app.tournament.TournamentManager
import com.sdart.app.tournament.TournamentType
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

actual fun createTournamentWebServer(
    tournamentManager: TournamentManager
): TournamentWebServer = DesktopTournamentWebServer(tournamentManager)

/**
 * JVM/Desktop-Implementierung von [TournamentWebServer] auf Basis von Java-Sockets und Coroutines.
 * Unterstützt HTTP/1.1, REST-Endpunkte, Server-Sent Events (SSE) und CORS ohne externe Abhängigkeiten.
 */
class DesktopTournamentWebServer(
    private val tournamentManager: TournamentManager
) : TournamentWebServer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override var port: Int = 0
        private set

    override var isRunning: Boolean = false
        private set

    override var allowScoreSubmission: Boolean = true
    override var requireHostApproval: Boolean = true

    override val localUrl: String
        get() = if (isRunning) "http://${findLocalIpAddress()}:$port" else ""

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val sseClients = CopyOnWriteArrayList<OutputStream>()

    init {
        tournamentManager.onTournamentUpdated = {
            notifyUpdate()
        }
    }

    override fun start(port: Int): Int {
        stop()

        val boundSocket = if (port == 0) {
            ServerSocket(0)
        } else {
            try {
                ServerSocket(port)
            } catch (e: Exception) {
                ServerSocket(0) // Fallback auf freien Port
            }
        }

        this.serverSocket = boundSocket
        this.port = boundSocket.localPort
        this.isRunning = true

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serverJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    val client = boundSocket.accept()
                    launch {
                        handleClient(client)
                    }
                } catch (e: Exception) {
                    if (!isRunning) break
                }
            }
        }

        return this.port
    }

    override fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        serverJob?.cancel()
        serverJob = null

        for (client in sseClients) {
            try {
                client.close()
            } catch (_: Exception) {}
        }
        sseClients.clear()
        port = 0
    }

    override fun notifyUpdate() {
        if (!isRunning || sseClients.isEmpty()) return

        val deadClients = mutableListOf<OutputStream>()
        val ssePayload = "data: {\"type\":\"update\",\"time\":${System.currentTimeMillis()}}\n\n"
        val bytes = ssePayload.toByteArray(Charsets.UTF_8)

        for (client in sseClients) {
            try {
                client.write(bytes)
                client.flush()
            } catch (e: Exception) {
                deadClients.add(client)
            }
        }
        sseClients.removeAll(deadClients)
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 10000
            val rawInput = socket.getInputStream()
            val bufferedIn = java.io.BufferedInputStream(rawInput)
            val output = socket.getOutputStream()

            // 1. Header einlesen (Bytes bis \r\n\r\n)
            val headerBytes = java.io.ByteArrayOutputStream()
            var b: Int
            var match = 0
            while (bufferedIn.read().also { b = it } != -1) {
                headerBytes.write(b)
                if (match == 0 && b == 13) match = 1
                else if (match == 1 && b == 10) match = 2
                else if (match == 2 && b == 13) match = 3
                else if (match == 3 && b == 10) {
                    break
                } else {
                    match = if (b == 13) 1 else 0
                }
            }

            val headerStr = headerBytes.toByteArray().toString(Charsets.ISO_8859_1)
            val lines = headerStr.split("\r\n").filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                socket.close()
                return@withContext
            }

            val requestLine = lines[0]
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return@withContext
            }

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")

            // Header extrahieren
            val headers = mutableMapOf<String, String>()
            var contentLength = 0
            for (i in 1 until lines.size) {
                val line = lines[i]
                val headerParts = line.split(":", limit = 2)
                if (headerParts.size == 2) {
                    val key = headerParts[0].trim().lowercase()
                    val value = headerParts[1].trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            // CORS Preflight
            if (method == "OPTIONS") {
                sendResponse(output, 200, "OK", "text/plain", "")
                socket.close()
                return@withContext
            }

            // 2. Body einlesen, falls Content-Length vorhanden (Exakt in Bytes!)
            val bodyStr = if (contentLength > 0) {
                val bodyBytes = ByteArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val count = bufferedIn.read(bodyBytes, totalRead, contentLength - totalRead)
                    if (count < 0) break
                    totalRead += count
                }
                String(bodyBytes, 0, totalRead, Charsets.UTF_8)
            } else ""

            when {
                // Hauptseite / Web-App
                method == "GET" && (path == "/" || path == "/index.html") -> {
                    val html = TournamentWebHtml.buildHtml(port)
                    sendResponse(output, 200, "OK", "text/html; charset=UTF-8", html)
                    socket.close()
                }

                // Deep-Link Fallback / Redirect
                method == "GET" && path == "/join" -> {
                    val html = TournamentWebHtml.buildHtml(port)
                    sendResponse(output, 200, "OK", "text/html; charset=UTF-8", html)
                    socket.close()
                }

                // JSON API: Aktueller Turnierstatus & Tabelle
                method == "GET" && path == "/api/tournament" -> {
                    val state = tournamentManager.currentTournament
                    val standings = if (state?.type == TournamentType.ROUND_ROBIN) {
                        tournamentManager.getLeagueStandings()
                    } else null

                    val pendingMatchIds = tournamentManager.pendingSubmissions.map { it.matchId }
                    val overview = TournamentWebOverview(
                        tournament = state,
                        standings = standings,
                        pendingMatchIds = pendingMatchIds,
                        allowScoreSubmission = allowScoreSubmission,
                        requireHostApproval = requireHostApproval
                    )
                    val jsonStr = json.encodeToString(TournamentWebOverview.serializer(), overview)
                    sendResponse(output, 200, "OK", "application/json; charset=UTF-8", jsonStr)
                    socket.close()
                }

                // Server-Sent Events (Live-Push)
                method == "GET" && path == "/api/events" -> {
                    socket.soTimeout = 0 // Kein Timeout für SSE-Verbindung
                    val header = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: text/event-stream\r\n")
                        append("Cache-Control: no-cache\r\n")
                        append("Connection: keep-alive\r\n")
                        append("Access-Control-Allow-Origin: *\r\n\r\n")
                    }
                    output.write(header.toByteArray(Charsets.UTF_8))
                    output.write("data: {\"type\":\"connected\"}\n\n".toByteArray(Charsets.UTF_8))
                    output.flush()

                    sseClients.add(output)
                    // Verbindung offen halten, solange socket lebt
                }

                // POST API: Match-Ergebnis einreichen
                method == "POST" && path.startsWith("/api/matches/") && path.endsWith("/submit") -> {
                    if (!allowScoreSubmission) {
                        val resp = json.encodeToString(WebApiResponse.serializer(), WebApiResponse(false, "Ergebniseingabe deaktiviert"))
                        sendResponse(output, 403, "Forbidden", "application/json; charset=UTF-8", resp)
                        socket.close()
                        return@withContext
                    }

                    try {
                        val request = json.decodeFromString(MatchSubmissionRequest.serializer(), bodyStr)
                        val success = tournamentManager.submitExternalResult(request, requireHostApproval)
                        val msg = if (success) {
                            if (requireHostApproval) "Ergebnis eingereicht, wartet auf Bestätigung" else "Ergebnis erfolgreich erfasst"
                        } else {
                            "Match nicht gefunden oder bereits beendet"
                        }
                        val resp = json.encodeToString(WebApiResponse.serializer(), WebApiResponse(success, msg))
                        sendResponse(output, if (success) 200 else 400, if (success) "OK" else "Bad Request", "application/json; charset=UTF-8", resp)
                    } catch (e: Exception) {
                        val resp = json.encodeToString(WebApiResponse.serializer(), WebApiResponse(false, "Ungültiges Format: ${e.message}"))
                        sendResponse(output, 400, "Bad Request", "application/json; charset=UTF-8", resp)
                    }
                    socket.close()
                }

                else -> {
                    sendResponse(output, 404, "Not Found", "text/plain", "Not Found")
                    socket.close()
                }
            }
        } catch (e: Exception) {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: String
    ) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Accept, Authorization\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bodyBytes)
        output.flush()
    }

    private fun findLocalIpAddress(): String {
        return try {
            val candidates = NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
            (candidates.firstOrNull { it.isSiteLocalAddress } ?: candidates.firstOrNull())?.hostAddress ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }
}
