package com.sdart.app.tournament.ui

import com.sdart.app.ui.theme.ditheredBackground
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.*
import com.sdart.app.ui.screens.PlayerLoadDialog
import com.sdart.app.tournament.*
import com.sdart.app.ui.AdaptiveContainer
import com.sdart.app.ui.ScreenHeader
import com.sdart.app.ui.SetupTile
import com.sdart.app.ui.components.*
import org.jetbrains.compose.resources.DrawableResource
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun TournamentSetupScreen(
    repository: PlayerRepository = remember { createPersistentPlayerRepository() },
    onBack: () -> Unit,
    onStartTournament: (name: String, type: TournamentType, config: TournamentGameConfig, players: List<TournamentPlayer>, randomize: Boolean, groupCount: Int, qualifiersPerGroup: Int) -> Unit
) {
    var tournamentName by remember { mutableStateOf("Dart-Turnier") }
    var tournamentType by remember { mutableStateOf(TournamentType.SINGLE_ELIMINATION) }
    var groupCount by remember { mutableStateOf(2) }
    var qualifiersPerGroup by remember { mutableStateOf(2) }
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

    // Gewählte Gruppenzahl, passend zur aktuellen Teilnehmerzahl
    val groupOptions = groupCountOptions(participants.size)
    val effectiveGroupCount = groupOptions.lastOrNull { it <= groupCount } ?: groupOptions.firstOrNull() ?: groupCount

    val startTournament: () -> Unit = start@{
        val cleanPlayers = participants.map { it.copy(name = it.name.trim()) }
        if (cleanPlayers.any { it.name.isBlank() }) {
            errorMessage = "Bitte allen Spielern einen gültigen Namen geben."
            return@start
        }
        if (cleanPlayers.size < 2) {
            errorMessage = "Mindestens 2 Spieler erforderlich."
            return@start
        }
        if (tournamentType == TournamentType.ROUND_ROBIN && cleanPlayers.size < 3) {
            errorMessage = "Für den Jeder-gegen-Jeden-Modus sind mindestens 3 Spieler erforderlich."
            return@start
        }
        if (tournamentType == TournamentType.DOUBLE_ELIMINATION && cleanPlayers.size < 3) {
            errorMessage = "Für Doppel-K.-o. sind mindestens 3 Spieler erforderlich."
            return@start
        }
        if (tournamentType == TournamentType.GROUPS_KNOCKOUT && groupOptions.isEmpty()) {
            errorMessage = "Für Gruppenphase plus K.-o. sind mindestens 6 Spieler erforderlich."
            return@start
        }

        val config = TournamentGameConfig(
            mode = selectedGameMode,
            customStartScore = if (selectedGameMode == GameMode.X301) 301 else 501,
            legs = legs,
            doubleIn = doubleIn,
            doubleOut = doubleOut,
            cricketVariant = cricketVariant
        )

        onStartTournament(tournamentName, tournamentType, config, cleanPlayers, randomizeOrder, effectiveGroupCount, qualifiersPerGroup)
    }

    AdaptiveContainer(modifier = Modifier.fillMaxSize()) { windowInfo ->
        if (windowInfo.isExpanded) {
            DesktopTournamentSetup(
                tournamentName = tournamentName,
                onNameChange = { tournamentName = it },
                tournamentType = tournamentType,
                onTypeChange = {
                    tournamentType = it
                    errorMessage = null
                },
                gameMode = selectedGameMode,
                onGameModeChange = { selectedGameMode = it },
                legs = legs,
                onLegsChange = { legs = it },
                doubleIn = doubleIn,
                onDoubleInChange = { doubleIn = it },
                doubleOut = doubleOut,
                onDoubleOutChange = { doubleOut = it },
                groupOptions = groupOptions,
                groupCount = effectiveGroupCount,
                onGroupCountChange = { groupCount = it },
                qualifiersPerGroup = qualifiersPerGroup,
                onQualifiersChange = { qualifiersPerGroup = it },
                randomizeOrder = randomizeOrder,
                onRandomizeChange = { randomizeOrder = it },
                participants = participants,
                onParticipantsChange = {
                    participants = it
                    errorMessage = null
                },
                errorMessage = errorMessage,
                onLoadClick = { showLoadDialog = true },
                onStart = startTournament
            )
            return@AdaptiveContainer
        }

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
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SetupTile(
                        title = "Doppel-K.-o.",
                        subtitle = "Mit zweiter Chance",
                        selected = tournamentType == TournamentType.DOUBLE_ELIMINATION,
                        onClick = {
                            tournamentType = TournamentType.DOUBLE_ELIMINATION
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SetupTile(
                        title = "Gruppenphase",
                        subtitle = "Danach K.-o.-Runde",
                        selected = tournamentType == TournamentType.GROUPS_KNOCKOUT,
                        onClick = {
                            tournamentType = TournamentType.GROUPS_KNOCKOUT
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (tournamentType == TournamentType.GROUPS_KNOCKOUT) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Gruppen",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (groupOptions.isEmpty()) {
                        Text("Ab 6 Spielern möglich", color = Color(0x80FFFFFF), fontSize = 13.sp)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupOptions.forEach { option ->
                                SetupTile(
                                    title = "$option Gruppen",
                                    subtitle = groupSizeText(participants.size, option),
                                    selected = effectiveGroupCount == option,
                                    onClick = { groupCount = option },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Weiter pro Gruppe",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "Nur Gruppensieger", 2 to "Erste und Zweite").forEach { (count, sub) ->
                            SetupTile(
                                title = "$count",
                                subtitle = sub,
                                selected = qualifiersPerGroup == count,
                                onClick = { qualifiersPerGroup = count },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Turniere laufen immer im Standard-Modus; wählbar ist nur die Startpunktzahl
                Text(
                    text = "Startpunkte",
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

                // X01-Regeln
                run {
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
                }
            }
        }

        // Start-Button
        val startButtonContent: @Composable () -> Unit = {
            Button(
                onClick = startTournament,
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
                .ditheredBackground()
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

/**
 * Turnier-Setup für breite Fenster im Stil von "Neues Spiel": Turnier-Modus links, Teilnehmer und
 * Einstellungen kompakt in einer Karte daneben. Gespielt wird immer Standard (501 oder 301).
 */
@Composable
private fun DesktopTournamentSetup(
    tournamentName: String,
    onNameChange: (String) -> Unit,
    tournamentType: TournamentType,
    onTypeChange: (TournamentType) -> Unit,
    gameMode: GameMode,
    onGameModeChange: (GameMode) -> Unit,
    legs: Int,
    onLegsChange: (Int) -> Unit,
    doubleIn: Boolean,
    onDoubleInChange: (Boolean) -> Unit,
    doubleOut: Boolean,
    onDoubleOutChange: (Boolean) -> Unit,
    groupOptions: List<Int>,
    groupCount: Int,
    onGroupCountChange: (Int) -> Unit,
    qualifiersPerGroup: Int,
    onQualifiersChange: (Int) -> Unit,
    randomizeOrder: Boolean,
    onRandomizeChange: (Boolean) -> Unit,
    participants: List<TournamentPlayer>,
    onParticipantsChange: (List<TournamentPlayer>) -> Unit,
    errorMessage: String?,
    onLoadClick: () -> Unit,
    onStart: () -> Unit
) {
    DesktopPage(title = "Neues Turnier") {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TypeCard(
                    icon = Res.drawable.ic_trophy,
                    title = "K.-o.-System",
                    subtitle = "Turnierbaum",
                    selected = tournamentType == TournamentType.SINGLE_ELIMINATION,
                    onClick = { onTypeChange(TournamentType.SINGLE_ELIMINATION) },
                    modifier = Modifier.weight(1f)
                )
                TypeCard(
                    icon = Res.drawable.ic_refresh,
                    title = "Doppel-K.-o.",
                    subtitle = "Mit zweiter Chance",
                    selected = tournamentType == TournamentType.DOUBLE_ELIMINATION,
                    onClick = { onTypeChange(TournamentType.DOUBLE_ELIMINATION) },
                    modifier = Modifier.weight(1f)
                )
                TypeCard(
                    icon = Res.drawable.ic_player_management,
                    title = "Gruppenphase",
                    subtitle = "Danach K.-o.-Runde",
                    selected = tournamentType == TournamentType.GROUPS_KNOCKOUT,
                    onClick = { onTypeChange(TournamentType.GROUPS_KNOCKOUT) },
                    modifier = Modifier.weight(1f)
                )
                TypeCard(
                    icon = Res.drawable.ic_chart_bar,
                    title = "Liga",
                    subtitle = "Jeder gegen jeden",
                    selected = tournamentType == TournamentType.ROUND_ROBIN,
                    onClick = { onTypeChange(TournamentType.ROUND_ROBIN) },
                    modifier = Modifier.weight(1f)
                )
            }

            DashboardCard(title = null, modifier = Modifier.weight(2.6f).fillMaxHeight()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ParticipantsSection(
                        participants = participants,
                        onParticipantsChange = onParticipantsChange,
                        randomizeOrder = randomizeOrder,
                        onRandomizeChange = onRandomizeChange,
                        onLoadClick = onLoadClick,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )

                    Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        CardTitle("Turniername")
                        OutlinedTextField(
                            value = tournamentName,
                            onValueChange = onNameChange,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = desktopFieldColors(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        )

                        if (tournamentType == TournamentType.GROUPS_KNOCKOUT) {
                            CardTitle("Gruppen", modifier = Modifier.padding(top = 14.dp))
                            if (groupOptions.isEmpty()) {
                                Text(
                                    text = "Ab 6 Spielern möglich",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                ) {
                                    groupOptions.forEach { option ->
                                        SelectTile("$option", groupCount == option, { onGroupCountChange(option) }, Modifier.weight(1f), groupSizeText(participants.size, option))
                                    }
                                }
                            }
                            CardTitle("Weiter pro Gruppe", modifier = Modifier.padding(top = 14.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            ) {
                                SelectTile("1", qualifiersPerGroup == 1, { onQualifiersChange(1) }, Modifier.weight(1f), "Gruppensieger")
                                SelectTile("2", qualifiersPerGroup == 2, { onQualifiersChange(2) }, Modifier.weight(1f), "Erste und Zweite")
                            }
                        }

                        CardTitle("Startpunkte", modifier = Modifier.padding(top = 14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            SelectTile("501", gameMode == GameMode.X501, { onGameModeChange(GameMode.X501) }, Modifier.weight(1f), "Standard")
                            SelectTile("301", gameMode == GameMode.X301, { onGameModeChange(GameMode.X301) }, Modifier.weight(1f), "Kurz")
                        }
                        CardTitle("Regeln", modifier = Modifier.padding(top = 14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            SelectTile("Double In", doubleIn, { onDoubleInChange(!doubleIn) }, Modifier.weight(1f), "Start mit Double")
                            SelectTile("Double Out", doubleOut, { onDoubleOutChange(!doubleOut) }, Modifier.weight(1f), "Sieg mit Double")
                        }

                        CardTitle("Legs · Best of", modifier = Modifier.padding(top = 14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1, 3, 5, 7).forEach { option ->
                                SelectTile("$option", legs == option, { onLegsChange(option) }, Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = GlassCardBorder)
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = errorMessage ?: tournamentSummary(tournamentType, gameMode, legs, doubleOut, participants.size, groupCount),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(painterResource(Res.drawable.ic_trophy), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Turnier starten", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Auswahlkarte für den Turnier-Modus, gestaltet wie die Spielmodus-Karten in "Neues Spiel". */
@Composable
private fun TypeCard(
    icon: DrawableResource,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        title = null,
        highlighted = selected,
        onClick = onClick,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ParticipantsSection(
    participants: List<TournamentPlayer>,
    onParticipantsChange: (List<TournamentPlayer>) -> Unit,
    randomizeOrder: Boolean,
    onRandomizeChange: (Boolean) -> Unit,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        CardTitle("Teilnehmer · ${participants.size} von 32")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(start = 22.dp, bottom = 12.dp)
        ) {
            if (participants.size < 32) {
                CardLink("Hinzufügen", onClick = {
                    val next = participants.size + 1
                    onParticipantsChange(participants + TournamentPlayer("p_$next", "Spieler $next"))
                }, icon = Res.drawable.ic_plus)
            }
            CardLink("Laden", onClick = onLoadClick, primary = false, icon = Res.drawable.ic_folder)
        }

        // Teilnehmerzahl schnell auf eine Baumgröße bringen
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 22.dp, bottom = 12.dp)
        ) {
            listOf(4, 8, 16).forEach { count ->
                SelectChip(
                    label = "$count",
                    selected = participants.size == count,
                    onClick = {
                        onParticipantsChange(
                            if (participants.size < count) {
                                participants + (participants.size + 1..count).map { TournamentPlayer("p_$it", "Spieler $it") }
                            } else {
                                participants.take(count)
                            }
                        )
                    }
                )
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            participants.forEachIndexed { index, player ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.width(22.dp)
                    )
                    OutlinedTextField(
                        value = player.name,
                        onValueChange = { newName ->
                            onParticipantsChange(participants.toMutableList().also { it[index] = player.copy(name = newName) })
                        },
                        placeholder = { Text("Spieler ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        trailingIcon = if (player.savedPlayerName != null) {
                            {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_folder),
                                    contentDescription = "Gespeicherter Spieler",
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        colors = desktopFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    if (participants.size > 2) {
                        IconButton(onClick = { onParticipantsChange(participants.toMutableList().also { it.removeAt(index) }) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_trash),
                                contentDescription = "Spieler entfernen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SelectChip(
            label = "Zufällige Setzliste",
            selected = randomizeOrder,
            onClick = { onRandomizeChange(!randomizeOrder) },
            icon = Res.drawable.ic_refresh
        )
    }
}

@Composable
private fun desktopFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = PrimaryOrange,
    unfocusedBorderColor = GlassControlBorder
)

private fun tournamentSummary(type: TournamentType, mode: GameMode, legs: Int, doubleOut: Boolean, players: Int, groupCount: Int): String {
    val typeText = when (type) {
        TournamentType.SINGLE_ELIMINATION -> "K.-o."
        TournamentType.ROUND_ROBIN -> "Liga"
        TournamentType.DOUBLE_ELIMINATION -> "Doppel-K.-o."
        TournamentType.GROUPS_KNOCKOUT -> "$groupCount Gruppen + K.-o."
    }
    val modeText = "${if (mode == GameMode.X301) 301 else 501} · ${if (doubleOut) "Double Out" else "Single Out"}"
    return "$typeText · $modeText · Best of $legs · $players Spieler"
}

/** Gruppenzahlen, bei denen jede Gruppe mindestens drei Spieler hat. */
private fun groupCountOptions(players: Int): List<Int> = listOf(2, 4, 8).filter { players >= it * 3 }

/** Gruppengröße als Text, z. B. "je 4" oder "je 3–4". */
private fun groupSizeText(players: Int, groups: Int): String {
    val small = players / groups
    val large = (players + groups - 1) / groups
    return if (small == large) "je $small Spieler" else "je $small–$large Spieler"
}
