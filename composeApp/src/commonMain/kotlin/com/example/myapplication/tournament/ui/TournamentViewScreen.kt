package com.example.myapplication.tournament.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.tournament.TournamentManager
import com.example.myapplication.tournament.TournamentMatch
import com.example.myapplication.tournament.TournamentType
import com.example.myapplication.ui.AdaptiveContainer
import com.example.myapplication.ui.ScreenHeader
import com.example.myapplication.ui.theme.DarkBgGradient
import com.example.myapplication.ui.theme.GlassBorder
import com.example.myapplication.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun TournamentViewScreen(
    tournamentManager: TournamentManager,
    onBack: () -> Unit,
    onStartMatch: (TournamentMatch) -> Unit,
    webServer: com.example.myapplication.tournament.web.TournamentWebServer? = null
) {
    val state = tournamentManager.currentTournament
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showWebShareDialog by remember { mutableStateOf(false) }
    var showCelebrationDialog by remember { mutableStateOf(state?.isFinished == true && state.winner != null) }
    var selectedMatchForAction by remember { mutableStateOf<TournamentMatch?>(null) }

    // Web-Server automatisch starten, sobald das Turnier geöffnet wird
    LaunchedEffect(state?.id) {
        if (state != null && webServer != null && !webServer.isRunning) {
            webServer.start(8080)
        }
    }

    AdaptiveContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgGradient)
    ) { windowInfo ->
        val isWidescreen = windowInfo.isWidescreen

        if (state == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Kein aktives Turnier vorhanden.",
                        color = Color.LightGray,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Zum Hauptmenü", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@AdaptiveContainer
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ScreenHeader(
                        title = state.name,
                        onBack = onBack,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Apple-style Actions: Live-Ticker / QR-Code & Beenden
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (webServer != null) {
                            Surface(
                                onClick = {
                                    if (!webServer.isRunning) {
                                        webServer.start(8080)
                                    }
                                    showWebShareDialog = true
                                },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = if (webServer.isRunning) Color(0x33FF8800) else Color(0x12FFFFFF),
                                border = BorderStroke(0.5.dp, if (webServer.isRunning) PrimaryOrange.copy(alpha = 0.6f) else Color(0x24FFFFFF)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_mobile_connect),
                                        contentDescription = "Live-Ticker & QR-Code",
                                        tint = if (webServer.isRunning) PrimaryOrange else Color(0xCCFFFFFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { showExitConfirmDialog = true },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(0x12FFFFFF),
                            border = BorderStroke(0.5.dp, Color(0x24FFFFFF)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_trash),
                                    contentDescription = "Turnier beenden",
                                    tint = Color(0xCCFFFFFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Banner für eingehende Web-Meldungen
                PendingSubmissionsBanner(
                    tournamentManager = tournamentManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1500.dp)
                        .align(Alignment.CenterHorizontally)
                )

                // Apple Inset Hero Tournament Status & Progress
                val realMatches = remember(state.matches) { state.matches.filter { !it.isBye } }
                val totalMatches = realMatches.size
                val completedMatches = remember(state.matches) { realMatches.count { it.isFinished } }
                val progress = if (totalMatches > 0) completedMatches.toFloat() / totalMatches else 0f

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x0AFFFFFF),
                    border = BorderStroke(0.5.dp, Color(0x1FFFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1500.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Linke Seite: Dezent abgerundete Cupertino-Pillen
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val modeLabel = when (state.type) {
                                    TournamentType.SINGLE_ELIMINATION -> "K.O.-System"
                                    TournamentType.ROUND_ROBIN -> "Liga"
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x14FFFFFF)
                                ) {
                                    Text(
                                        text = modeLabel,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x0DFFFFFF)
                                ) {
                                    Text(
                                        text = "${state.config.mode.name} • Best of ${state.config.legs}",
                                        color = Color(0xB3FFFFFF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Rechte Seite: Match-Zähler oder Champion-Kapsel
                            if (state.isFinished && state.winner != null) {
                                Surface(
                                    onClick = { showCelebrationDialog = true },
                                    color = Color(0x26FF8800),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.5.dp, PrimaryOrange.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_trophy),
                                            contentDescription = null,
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sieger: ${state.winner.name}",
                                            color = PrimaryOrange,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "$completedMatches / $totalMatches gespielt",
                                    color = Color(0x80FFFFFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Feine 2.5dp Haarlinien-Fortschrittsanzeige
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(Color(0x14FFFFFF), RoundedCornerShape(1.5.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(PrimaryOrange, RoundedCornerShape(1.5.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Hauptbereich je nach Turniertyp
                when (state.type) {
                    TournamentType.SINGLE_ELIMINATION -> {
                        TournamentBracketView(
                            state = state,
                            isWidescreen = isWidescreen,
                            onMatchClick = { match ->
                                selectedMatchForAction = match
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TournamentType.ROUND_ROBIN -> {
                        val standings = remember(state.matches) {
                            tournamentManager.getLeagueStandings()
                        }
                        TournamentLeagueView(
                            state = state,
                            standings = standings,
                            onMatchClick = { match ->
                                selectedMatchForAction = match
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Dialog: Match Aktion & manuelle Ergebniseingabe mit Legs
        selectedMatchForAction?.let { match ->
            TournamentMatchActionDialog(
                match = match,
                tournamentConfig = state.config,
                onStartMatch = { m ->
                    selectedMatchForAction = null
                    onStartMatch(m)
                },
                onSubmitResult = { matchId, winnerId, p1Legs, p2Legs ->
                    tournamentManager.submitManualScore(matchId, winnerId, p1Legs, p2Legs)
                    selectedMatchForAction = null
                },
                onDismiss = {
                    selectedMatchForAction = null
                }
            )
        }

        // Dialog: Champion-Siegerehrung
        if (showCelebrationDialog && state.winner != null) {
            AlertDialog(
                onDismissRequest = { showCelebrationDialog = false },
                containerColor = Color.Transparent,
                text = {
                    TournamentCelebrationCard(
                        winner = state.winner,
                        tournamentName = state.name,
                        onDismiss = { showCelebrationDialog = false }
                    )
                },
                confirmButton = {}
            )
        }

        // Dialog: Turnier abbrechen (Apple Inset Dialog)
        if (showExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                containerColor = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Turnier beenden?",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Möchtest du das gesamte Turnier wirklich abbrechen und löschen? Der aktuelle Stand geht dabei verloren.",
                        color = Color(0xB3FFFFFF),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitConfirmDialog = false
                            tournamentManager.clearTournament()
                            onBack()
                        }
                    ) {
                        Text(
                            text = "Turnier löschen",
                            color = Color(0xFFFF453A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExitConfirmDialog = false }
                    ) {
                        Text(
                            text = "Abbrechen",
                            color = Color(0x99FFFFFF),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            )
        }

        // Dialog: QR-Code & Web-Freigabe
        if (showWebShareDialog && webServer != null) {
            TournamentWebShareDialog(
                webServer = webServer,
                onDismiss = { showWebShareDialog = false }
            )
        }
    }
}
