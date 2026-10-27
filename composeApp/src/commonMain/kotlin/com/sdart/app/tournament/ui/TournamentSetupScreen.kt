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
import com.sdart.app.ui.LocalCompactLayout
import com.sdart.app.ui.components.*
import org.jetbrains.compose.resources.DrawableResource
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun TournamentSetupScreen(
    repository: PlayerRepository = remember { createPersistentPlayerRepository() },
    onBack: () -> Unit,
    onStartTournament: (name: String, type: TournamentType, config: TournamentGameConfig, players: List<TournamentPlayer>, randomize: Boolean, groupCount: Int, qualifiersPerGroup: Int) -> Unit
) {
    val defaultTournamentName = stringResource(Res.string.tour_default_name)
    val playerPrefix = stringResource(Res.string.tour_player_prefix)
    var tournamentName by remember { mutableStateOf(defaultTournamentName) }
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
                TournamentPlayer("p1", "$playerPrefix 1"),
                TournamentPlayer("p2", "$playerPrefix 2"),
                TournamentPlayer("p3", "$playerPrefix 3"),
                TournamentPlayer("p4", "$playerPrefix 4")
            )
        )
    }

    var showLoadDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Gewählte Gruppenzahl, passend zur aktuellen Teilnehmerzahl
    val groupOptions = groupCountOptions(participants.size)
    val effectiveGroupCount = groupOptions.lastOrNull { it <= groupCount } ?: groupOptions.firstOrNull() ?: groupCount

    val errName = stringResource(Res.string.tour_setup_err_name)
    val errMinPlayers = stringResource(Res.string.tour_setup_err_min_players)
    val errLeague = stringResource(Res.string.tour_setup_err_league)
    val errDouble = stringResource(Res.string.tour_setup_err_double)
    val errGroups = stringResource(Res.string.tour_setup_err_groups)

    val startTournament: () -> Unit = start@{
        val cleanPlayers = participants.map { it.copy(name = it.name.trim()) }
        if (cleanPlayers.any { it.name.isBlank() }) {
            errorMessage = errName
            return@start
        }
        if (cleanPlayers.size < 2) {
            errorMessage = errMinPlayers
            return@start
        }
        if (tournamentType == TournamentType.ROUND_ROBIN && cleanPlayers.size < 3) {
            errorMessage = errLeague
            return@start
        }
        if (tournamentType == TournamentType.DOUBLE_ELIMINATION && cleanPlayers.size < 3) {
            errorMessage = errDouble
            return@start
        }
        if (tournamentType == TournamentType.GROUPS_KNOCKOUT && groupOptions.isEmpty()) {
            errorMessage = errGroups
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

        onStartTournament(tournamentName.ifBlank { defaultTournamentName }, tournamentType, config, cleanPlayers, randomizeOrder, effectiveGroupCount, qualifiersPerGroup)
    }

    TournamentSetupContent(
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

    if (showLoadDialog) {
        PlayerLoadDialog(
            repository = repository,
            onDismiss = { showLoadDialog = false },
            onPlayersSelected = { selectedNames ->
                showLoadDialog = false
                val loaded = selectedNames.mapIndexed { idx, name ->
                    TournamentPlayer(id = "loaded_${currentTimeMillis()}_$idx", name = name, savedPlayerName = name)
                }
                participants = (participants + loaded).take(MaxPlayers)
            }
        )
    }
}

/**
 * Turnier-Setup im Stil von "Neues Spiel": Turnier-Modus, Teilnehmer und Einstellungen.
 * Breite Fenster zeigen die Modi links und Teilnehmer und Einstellungen kompakt in einer Karte daneben,
 * schmale Fenster stapeln alles untereinander. Gespielt wird immer Standard (501 oder 301).
 */
@Composable
private fun TournamentSetupContent(
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
    val compact = LocalCompactLayout.current
    val typeOptions = rememberTypeOptions()
    val typeCard: @Composable (TypeOption, Modifier) -> Unit = { option, modifier ->
        TypeCard(
            icon = option.icon,
            title = option.title,
            rules = option.rules,
            selected = tournamentType == option.type,
            onClick = { onTypeChange(option.type) },
            modifier = modifier
        )
    }
    val participantsSection: @Composable (Modifier) -> Unit = { modifier ->
        ParticipantsSection(
            participants = participants,
            onParticipantsChange = onParticipantsChange,
            randomizeOrder = randomizeOrder,
            onRandomizeChange = onRandomizeChange,
            onLoadClick = onLoadClick,
            modifier = modifier
        )
    }
    val settingsFields: @Composable ColumnScope.() -> Unit = {
        CardTitle(stringResource(Res.string.tour_setup_name))
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
            CardTitle(stringResource(Res.string.tour_setup_groups), modifier = Modifier.padding(top = 14.dp))
            if (groupOptions.isEmpty()) {
                Text(
                    text = stringResource(Res.string.tour_setup_groups_min),
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
            CardTitle(stringResource(Res.string.tour_setup_qualifiers), modifier = Modifier.padding(top = 14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                SelectTile("1", qualifiersPerGroup == 1, { onQualifiersChange(1) }, Modifier.weight(1f), stringResource(Res.string.tour_setup_qual_winner))
                SelectTile("2", qualifiersPerGroup == 2, { onQualifiersChange(2) }, Modifier.weight(1f), stringResource(Res.string.tour_setup_qual_top2))
            }
        }

        CardTitle(stringResource(Res.string.tour_setup_start_points), modifier = Modifier.padding(top = 14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            SelectTile("501", gameMode == GameMode.X501, { onGameModeChange(GameMode.X501) }, Modifier.weight(1f), stringResource(Res.string.game_mode_standard))
            SelectTile("301", gameMode == GameMode.X301, { onGameModeChange(GameMode.X301) }, Modifier.weight(1f), stringResource(Res.string.tour_setup_short))
        }
        CardTitle(stringResource(Res.string.tour_setup_rules), modifier = Modifier.padding(top = 14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            SelectTile("Double In", doubleIn, { onDoubleInChange(!doubleIn) }, Modifier.weight(1f), stringResource(Res.string.tour_setup_double_in_desc))
            SelectTile("Double Out", doubleOut, { onDoubleOutChange(!doubleOut) }, Modifier.weight(1f), stringResource(Res.string.tour_setup_double_out_desc))
        }

        CardTitle(stringResource(Res.string.tour_setup_legs), modifier = Modifier.padding(top = 14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(1, 3, 5, 7).forEach { option ->
                SelectTile("$option", legs == option, { onLegsChange(option) }, Modifier.weight(1f))
            }
        }
    }
    val summary: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = errorMessage ?: tournamentSummary(tournamentType, gameMode, legs, doubleOut, participants.size, groupCount),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = modifier
        )
    }
    val startButton: @Composable (Modifier) -> Unit = { modifier ->
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            modifier = modifier
        ) {
            Icon(painterResource(Res.drawable.ic_trophy), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.tour_setup_start), fontWeight = FontWeight.SemiBold)
        }
    }

    // Genau fensterhoch: Teilnehmer und Einstellungen scrollen für sich, die Seite selbst nicht
    DesktopPage(title = stringResource(Res.string.tour_setup_title), minHeight = 0.dp) {
        if (compact) {
            // Karten einer Reihe gleich hoch, auch wenn ein Untertitel zweizeilig wird
            typeOptions.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    row.forEach { option -> typeCard(option, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
            DashboardCard(title = null, modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                participantsSection(Modifier.fillMaxWidth())
            }
            DashboardCard(title = null, modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp, content = settingsFields)
            summary(Modifier.padding(horizontal = 4.dp))
            startButton(Modifier.fillMaxWidth())
            return@DesktopPage
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                typeOptions.forEach { option -> typeCard(option, Modifier.weight(1f)) }
            }

            DashboardCard(title = null, modifier = Modifier.weight(2.6f).fillMaxHeight()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    participantsSection(Modifier.weight(1f).fillMaxHeight())
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        content = settingsFields
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = GlassCardBorder)
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    summary(Modifier.weight(1f))
                    startButton(Modifier)
                }
            }
        }
    }
}

// Regeln als kurze Absätze, angezeigt über das Info-Symbol der Karte
private class TypeOption(val icon: DrawableResource, val title: String, val rules: List<String>, val type: TournamentType)

@Composable
private fun rememberTypeOptions(): List<TypeOption> = listOf(
    TypeOption(
        Res.drawable.ic_trophy, stringResource(Res.string.tour_type_knockout), listOf(
            stringResource(Res.string.tour_rules_ko_1),
            stringResource(Res.string.tour_rules_ko_2)
        ), TournamentType.SINGLE_ELIMINATION
    ),
    TypeOption(
        Res.drawable.ic_refresh, stringResource(Res.string.tour_type_double), listOf(
            stringResource(Res.string.tour_rules_double_1),
            stringResource(Res.string.tour_rules_double_2),
            stringResource(Res.string.tour_rules_double_3)
        ), TournamentType.DOUBLE_ELIMINATION
    ),
    TypeOption(
        Res.drawable.ic_player_management, stringResource(Res.string.tour_type_groups), listOf(
            stringResource(Res.string.tour_rules_groups_1),
            stringResource(Res.string.tour_rules_groups_2),
            stringResource(Res.string.tour_rules_groups_3)
        ), TournamentType.GROUPS_KNOCKOUT
    ),
    TypeOption(
        Res.drawable.ic_chart_bar, stringResource(Res.string.tour_type_league), listOf(
            stringResource(Res.string.tour_rules_league_1),
            stringResource(Res.string.tour_rules_league_2),
            stringResource(Res.string.tour_rules_league_3)
        ), TournamentType.ROUND_ROBIN
    )
)

/** Auswahlkarte für den Turnier-Modus, gestaltet wie die Spielmodus-Karten in "Neues Spiel". */
@Composable
private fun TypeCard(
    icon: DrawableResource,
    title: String,
    rules: List<String>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = LocalCompactLayout.current
    Box(modifier = modifier.fillMaxWidth()) {
        DashboardCard(
            title = null,
            highlighted = selected,
            onClick = onClick,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentPadding = if (compact) 14.dp else 24.dp
        ) {
            // Auf dem Handy sind die Karten flach: rechts Platz lassen für das Info-Symbol in der Ecke
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = if (compact) 18.dp else 0.dp)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(if (compact) 26.dp else 40.dp)
                )
                Spacer(Modifier.width(if (compact) 10.dp else 18.dp))
                Text(
                    text = title,
                    style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge).copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        RulesInfoButton(title = title, rules = rules, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
    }
}

/** Teilnehmerliste, aufgebaut wie die Spielerliste in "Neues Spiel"; am Desktop scrollt die lange Liste für sich. */
@Composable
private fun ParticipantsSection(
    participants: List<TournamentPlayer>,
    onParticipantsChange: (List<TournamentPlayer>) -> Unit,
    randomizeOrder: Boolean,
    onRandomizeChange: (Boolean) -> Unit,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerPrefix = stringResource(Res.string.tour_player_prefix)
    PlayerListSection(
        players = participants.map { PlayerListEntry(it.name, isLoaded = it.savedPlayerName != null) },
        minPlayers = 2,
        onAdd = {
            val next = participants.size + 1
            onParticipantsChange(participants + TournamentPlayer("p_$next", "$playerPrefix $next"))
        },
        onLoad = onLoadClick,
        onRename = { index, name ->
            onParticipantsChange(participants.toMutableList().also { it[index] = it[index].copy(name = name) })
        },
        onRemove = { index -> onParticipantsChange(participants.toMutableList().also { it.removeAt(index) }) },
        randomize = randomizeOrder,
        onRandomizeChange = onRandomizeChange,
        modifier = modifier,
        scrollList = true
    )
}

@Composable
private fun desktopFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = PrimaryOrange,
    unfocusedBorderColor = GlassControlBorder
)

@Composable
private fun tournamentSummary(type: TournamentType, mode: GameMode, legs: Int, doubleOut: Boolean, players: Int, groupCount: Int): String {
    val typeText = when (type) {
        TournamentType.SINGLE_ELIMINATION -> stringResource(Res.string.tour_summary_ko)
        TournamentType.ROUND_ROBIN -> stringResource(Res.string.tour_type_league)
        TournamentType.DOUBLE_ELIMINATION -> stringResource(Res.string.tour_type_double)
        TournamentType.GROUPS_KNOCKOUT -> stringResource(Res.string.tour_summary_groups, groupCount)
    }
    val modeText = "${if (mode == GameMode.X301) 301 else 501} · ${if (doubleOut) "Double Out" else "Single Out"}"
    return stringResource(Res.string.tour_summary_line, typeText, modeText, legs, players)
}

/** Gruppenzahlen, bei denen jede Gruppe mindestens drei Spieler hat. */
private fun groupCountOptions(players: Int): List<Int> = listOf(2, 4, 8).filter { players >= it * 3 }

/** Gruppengröße als Text, z. B. "je 4" oder "je 3–4". */
@Composable
private fun groupSizeText(players: Int, groups: Int): String {
    val small = players / groups
    val large = (players + groups - 1) / groups
    return if (small == large) stringResource(Res.string.tour_group_size_each, small) else stringResource(Res.string.tour_group_size_range, small, large)
}
