package com.sdart.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import com.sdart.app.tournament.*
import com.sdart.app.tournament.ui.TournamentViewScreen
import com.sdart.app.ui.ProvideAdaptiveScale
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.MyApplicationTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

fun runScreenshotCapture(screenType: String, outputFilePath: String) {
    val isLeague = screenType.contains("league", ignoreCase = true)
    val manager = TournamentManager().apply {
        val p = listOf(
            TournamentPlayer("p1", "Max M."),
            TournamentPlayer("p2", "Lukas K."),
            TournamentPlayer("p3", "Sarah B."),
            TournamentPlayer("p4", "Dennis W."),
            TournamentPlayer("p5", "Felix H."),
            TournamentPlayer("p6", "Laura S."),
            TournamentPlayer("p7", "Tim R."),
            TournamentPlayer("p8", "Anna P.")
        )
        createTournament(
            name = if (isLeague) "Darts Premier League" else "Club Championship 2026",
            type = if (isLeague) TournamentType.ROUND_ROBIN else TournamentType.SINGLE_ELIMINATION,
            config = TournamentGameConfig(mode = GameMode.X501, legs = 3),
            players = if (isLeague) p.take(4) else if (screenType.contains("4")) p.take(4) else p
        )
        val m = currentTournament?.matches ?: emptyList()
        if (m.isNotEmpty()) {
            resolveMatch(
                m[0].id,
                m[0].player1!!.id,
                MatchPlayerResult(legsWon = 3, average = 78.4),
                MatchPlayerResult(legsWon = 1, average = 71.2)
            )
        }
        if (m.size > 1) {
            resolveMatch(
                m[1].id,
                m[1].player1!!.id,
                MatchPlayerResult(legsWon = 3, average = 82.1),
                MatchPlayerResult(legsWon = 0, average = 69.5)
            )
        }
        if (screenType.contains("winner") && m.size > 2) {
            val finalMatch = currentTournament?.matches?.find { it.round == 2 }
            if (finalMatch != null && finalMatch.player1 != null) {
                resolveMatch(
                    finalMatch.id,
                    finalMatch.player1!!.id,
                    MatchPlayerResult(legsWon = 3, average = 85.0),
                    MatchPlayerResult(legsWon = 2, average = 83.2)
                )
            }
        }
    }

    val sceneWidth = when {
        screenType.contains("1440") -> 2560
        screenType.contains("1080") || screenType.contains("wide") -> 1920
        screenType.contains("800") -> 800
        else -> 1280
    }
    val sceneHeight = when {
        screenType.contains("1440") -> 1440
        screenType.contains("1080") || screenType.contains("wide") -> 1080
        screenType.contains("800") -> 600
        else -> 720
    }

    val scene = ImageComposeScene(
        width = sceneWidth,
        height = sceneHeight
    ) {
        MyApplicationTheme {
            ProvideAdaptiveScale(modifier = Modifier.fillMaxSize().background(DarkBgGradient)) {
                TournamentViewScreen(
                    tournamentManager = manager,
                    onBack = {},
                    onStartMatch = {}
                )
            }
        }
    }

    try {
        val image = scene.render()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        if (data != null) {
            val targetFile = File(outputFilePath)
            targetFile.parentFile?.mkdirs()
            targetFile.writeBytes(data.bytes)
            println("HEADLESS_CAPTURE_SUCCESS: ${targetFile.absolutePath} (size: ${data.bytes.size} bytes)")
        } else {
            println("ERROR: encodeToData returned null")
        }
    } finally {
        scene.close()
    }
}
