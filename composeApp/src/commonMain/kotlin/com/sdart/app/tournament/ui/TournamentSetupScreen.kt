package com.sdart.app.tournament.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.*
import com.sdart.app.tournament.*
import com.sdart.app.ui.AdaptiveContainer
import com.sdart.app.ui.ScreenHeader
import com.sdart.app.ui.SetupTile
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun TournamentSetupScreen(
    repository: PlayerRepository = remember { createPersistentPlayerRepository() },
    onBack: () -> Unit,
    onStartTournament: (name: String, type: TournamentType, config: TournamentGameConfig, players: List<TournamentPlayer>, randomize: Boolean) -> Unit
) {
    var tournamentName by remember { mutableStateOf("Dart-Turnier") }
    var tournamentType by remember { mutableStateOf(TournamentType.SINGLE_ELIMINATION) }
    var selectedGameMode by remember { mutableStateOf(GameMode.X501) }
    var legs by remember { mutableStateOf(3) }
    var doubleIn by remember { mutableStateOf(false) }
    var doubleOut by remember { mutableStateOf(true) }
    var cricketVariant by remember { mutableStateOf(CricketVariant.STANDARD) }
    var randomizeOrder by remember { mutableStateOf(true) }

    var participants by remember {
        mutableStateOf(
            listOf(
                TournamentPlayer("p1", "Spieler 1"),
                TournamentPlayer("p2", "Spieler 2"),
                TournamentPlayer("p3", "Spieler 3"),
                TournamentPlayer("p4", "Spieler 4")
            )
        )
    }

    var showLoadDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AdaptiveContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgGradient)
    ) { windowInfo ->
        val isWidescreen = windowInfo.isWidescreen

        // Linke Spalte: Teilnehmer & Presets
        val playersContent: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Teilnehmer (${participants.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Schnell-Presets (4, 8, 16 Spieler)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(4, 8, 16).forEach { targetCount ->
                            val isCurrent = participants.size == targetCount
                            Surface(
                                onClick = {
                                    if (participants.size < targetCount) {
                                        val additional = (participants.size + 1..targetCount).map {
                                            TournamentPlayer("p_$it", "Spieler $it")
                                        }
                                        participants = participants + additional
                                    } else if (participants.size > targetCount) {
                                        participants = participants.take(targetCount)
                                    }
                                },
                                shape = RoundedCornerShape(7.dp),
                                color = if (isCurrent) Color(0x2BFF8800) else Color(0x12FFFFFF),
                                border = BorderStroke(0.5.dp, if (isCurrent) PrimaryOrange else Color(0x1FFFFFFF))
                            ) {
                                Text(
                                    text = "$targetCount",
                                    fontSize = 11.sp,
                                    color = if (isCurrent) PrimaryOrange else Color(0x99FFFFFF),
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Aktions-Buttons (Neu, Laden, Zufall)
                if (participants.size < 32) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Neu"-Button
                        Surface(
                            onClick = {
                                val nextIdx = participants.size + 1
                                participants = participants + TournamentPlayer("p_$nextIdx", "Spieler $nextIdx")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x0EFFFFFF),
                            border = BorderStroke(0.5.dp, Color(0x22FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_plus),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 6.dp).size(16.dp)
                                )
                                Text("Neu", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // "Laden"-Button
                        Surface(
                            onClick = { showLoadDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x0EFFFFFF),
                            border = BorderStroke(0.5.dp, Color(0x22FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_folder),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 6.dp).size(16.dp)
                                )
                                Text("Laden", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // "Zufall"-Button
                        Surface(
                            onClick = { randomizeOrder = !randomizeOrder },
                            shape = RoundedCornerShape(10.dp),
                            color = if (randomizeOrder) Color(0x24FF8800) else Color(0x0EFFFFFF),
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = if (randomizeOrder) PrimaryOrange else Color(0x22FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_refresh),
                                    contentDescription = null,
                                    tint = if (randomizeOrder) PrimaryOrange else Color.White,
                                    modifier = Modifier.padding(end = 6.dp).size(16.dp)
                                )
                                Text(
                                    "Zufall",
                                    color = if (randomizeOrder) Color.White else Color(0x80FFFFFF),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // Teilnehmer-Eingabezeilen mit dezentem Seed-Kreis
                participants.forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x14FFFFFF),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    color = Color(0x80FFFFFF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = player.name,
                            onValueChange = { newName ->
                                participants = participants.toMutableList().also {
                                    it[index] = player.copy(name = newName)
                                }
                            },
                            placeholder = { Text("Spieler ${index + 1}", color = Color(0x40FFFFFF), fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = Color(0x1AFFFFFF),
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color(0x05FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (participants.size > 2) {
                            IconButton(
                                onClick = {
                                    participants = participants.toMutableList().also { it.removeAt(index) }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_trash),
                                    contentDescription = "Spieler löschen",
                                    tint = Color(0x80FFFFFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rechte Spalte: Spiel-Einstellungen
        val settingsContent: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Name
                Text(
                    text = "Turniername",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = tournamentName,
                    onValueChange = { tournamentName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedContainerColor = Color(0x0AFFFFFF),
                        unfocusedContainerColor = Color(0x05FFFFFF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Turnier-Typ
                Text(
                    text = "Turnier-Modus",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SetupTile(
                        title = "Single K.O.",
                        subtitle = "Klassischer K.-o.-Baum",
                        selected = tournamentType == TournamentType.SINGLE_ELIMINATION,
                        onClick = {
                            tournamentType = TournamentType.SINGLE_ELIMINATION
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SetupTile(
                        title = "Jeder gg. Jeden",
                        subtitle = "Liga mit Punktetabelle",
                        selected = tournamentType == TournamentType.ROUND_ROBIN,
                        onClick = {
                            tournamentType = TournamentType.ROUND_ROBIN
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dart-Spielmodus
                Text(
                    text = "Dart-Spielmodus",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SetupTile(
                        title = "501",
                        subtitle = "Standard",
                        selected = selectedGameMode == GameMode.X501,
                        onClick = { selectedGameMode = GameMode.X501 },
                        modifier = Modifier.weight(1f)
                    )
                    SetupTile(
                        title = "301",
                        subtitle = "Kurz",
                        selected = selectedGameMode == GameMode.X301,
                        onClick = { selectedGameMode = GameMode.X301 },
                        modifier = Modifier.weight(1f)
                    )
                    SetupTile(
                        title = "Cricket",
                        subtitle = "Taktisch",
                        selected = selectedGameMode == GameMode.CRICKET,
                        onClick = { selectedGameMode = GameMode.CRICKET },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SetupTile(
                        title = "All Fives",
                        subtitle = "Teilbar durch 5",
                        selected = selectedGameMode == GameMode.ALL_FIVES,
                        onClick = { selectedGameMode = GameMode.ALL_FIVES },
                        modifier = Modifier.weight(1f)
                    )
                    SetupTile(
                        title = "Around Clock",
                        subtitle = "1 bis 20 & Bull",
                        selected = selectedGameMode == GameMode.AROUND_THE_CLOCK,
                        onClick = { selectedGameMode = GameMode.AROUND_THE_CLOCK },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Match-Distanz (Best of Legs)
                Text(
                    text = "Match-Distanz (Best of Legs)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 7).forEach { legOption ->
                        val isSelected = legs == legOption
                        Surface(
                            onClick = { legs = legOption },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0x26FF8800) else Color(0x0CFFFFFF),
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = if (isSelected) PrimaryOrange else Color(0x1FFFFFFF)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Bo$legOption",
                                    color = if (isSelected) PrimaryOrange else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Spielspezifische Regeln
                if (selectedGameMode == GameMode.X501 || selectedGameMode == GameMode.X301) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "X01 Regeln",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SetupTile(
                            title = "Double In",
                            subtitle = if (doubleIn) "Aktiv" else "Inaktiv",
                            selected = doubleIn,
                            onClick = { doubleIn = !doubleIn },
                            modifier = Modifier.weight(1f)
                        )
                        SetupTile(
                            title = "Double Out",
                            subtitle = if (doubleOut) "Aktiv" else "Single Out",
                            selected = doubleOut,
                            onClick = { doubleOut = !doubleOut },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (selectedGameMode == GameMode.CRICKET) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Cricket Variante",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SetupTile(
                            title = "Standard",
                            subtitle = "Eigene Punkte",
                            selected = cricketVariant == CricketVariant.STANDARD,
                            onClick = { cricketVariant = CricketVariant.STANDARD },
                            modifier = Modifier.weight(1f)
                        )
                        SetupTile(
                            title = "Cut-Throat",
                            subtitle = "Gegner Punkte",
                            selected = cricketVariant == CricketVariant.CUT_THROAT,
                            onClick = { cricketVariant = CricketVariant.CUT_THROAT },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Start-Button
        val startButtonContent: @Composable () -> Unit = {
            Button(
                onClick = {
                    val cleanPlayers = participants.map { it.copy(name = it.name.trim()) }
                    if (cleanPlayers.any { it.name.isBlank() }) {
                        errorMessage = "Bitte allen Spielern einen gültigen Namen geben."
                        return@Button
                    }
                    if (cleanPlayers.size < 2) {
                        errorMessage = "Mindestens 2 Spieler erforderlich."
                        return@Button
                    }
                    if (tournamentType == TournamentType.ROUND_ROBIN && cleanPlayers.size < 3) {
                        errorMessage = "Für den Jeder-gegen-Jeden-Modus sind mindestens 3 Spieler erforderlich."
                        return@Button
                    }

                    val config = TournamentGameConfig(
                        mode = selectedGameMode,
                        customStartScore = if (selectedGameMode == GameMode.X301) 301 else 501,
                        legs = legs,
                        doubleIn = doubleIn,
                        doubleOut = doubleOut,
                        cricketVariant = cricketVariant
                    )

                    onStartTournament(tournamentName, tournamentType, config, cleanPlayers, randomizeOrder)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text(
                    text = "Turnier starten",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = "Turnier-Setup",
                onBack = onBack
            )

            // Fehlermeldung
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1100.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x22E53935),
                    border = BorderStroke(0.5.dp, Color(0xFFE53935).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_alert_triangle),
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = error,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isWidescreen) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1100.dp)
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Linke Spalte: Spieler
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x0AFFFFFF),
                        border = BorderStroke(0.5.dp, Color(0x1FFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            playersContent()
                        }
                    }

                    // Rechte Spalte: Einstellungen & Button
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0x0AFFFFFF),
                            border = BorderStroke(0.5.dp, Color(0x1FFFFFFF))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                settingsContent()
                            }
                        }

                        startButtonContent()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(bottom = 30.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x0AFFFFFF),
                        border = BorderStroke(0.5.dp, Color(0x1FFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            playersContent()
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x0AFFFFFF),
                        border = BorderStroke(0.5.dp, Color(0x1FFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            settingsContent()
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    startButtonContent()
                }
            }
        }

        if (showLoadDialog) {
            PlayerLoadDialog(
                repository = repository,
                onDismiss = { showLoadDialog = false },
                onPlayersSelected = { selectedNames ->
                    showLoadDialog = false
                    val loaded = selectedNames.mapIndexed { idx, name ->
                        TournamentPlayer(id = "loaded_${currentTimeMillis()}_$idx", name = name, savedPlayerName = name)
                    }
                    participants = participants + loaded
                }
            )
        }
    }
}
