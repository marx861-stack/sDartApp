package com.sdart.app.tournament.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.sdart.app.tournament.LeagueStandingRow
import com.sdart.app.tournament.MatchBracket
import com.sdart.app.tournament.TournamentMatch
import com.sdart.app.tournament.TournamentState
import com.sdart.app.tournament.groupLabel
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

/**
 * Doppel-K.-o.: Gewinnerrunde, Verliererrunde und Finale, umschaltbar über Tabs.
 */
@Composable
fun DoubleEliminationView(
    state: TournamentState,
    isWidescreen: Boolean,
    onMatchClick: (TournamentMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val winners = remember(state.matches) { state.matches.filter { it.bracket == MatchBracket.MAIN } }
    val losers = remember(state.matches) { state.matches.filter { it.bracket == MatchBracket.LOSERS } }
    val finals = remember(state.matches) { state.matches.filter { it.bracket == MatchBracket.GRAND_FINAL } }
    val stages = listOf(winners, losers, finals)

    // Startet dort, wo gerade gespielt wird
    var selected by remember {
        mutableStateOf(stages.indexOfFirst { stage -> stage.any { it.isReady } }.takeIf { it >= 0 } ?: 2)
    }

    Column(modifier = modifier.fillMaxSize()) {
        StageTabs(
            labels = listOf(stringResource(Res.string.tour_stage_main), stringResource(Res.string.tour_stage_second_chance), stringResource(Res.string.tour_round_final)),
            active = stages.map { stage -> stage.any { it.isReady } },
            selected = selected,
            onSelect = { selected = it }
        )
        when (selected) {
            0 -> TournamentBracketView(
                state = state.copy(matches = winners, winner = null),
                isWidescreen = isWidescreen,
                onMatchClick = onMatchClick,
                roundTitle = { round, maxRound ->
                    when (round) {
                        maxRound -> stringResource(Res.string.tour_stage_main_final)
                        maxRound - 1 -> stringResource(Res.string.tour_stage_main_semifinal)
                        else -> stringResource(Res.string.tour_stage_main_round, round)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            1 -> RoundColumnsView(
                matches = losers,
                roundTitle = { round, maxRound ->
                    if (round == maxRound) stringResource(Res.string.tour_stage_sc_final) else stringResource(Res.string.tour_stage_sc_round, round)
                },
                onMatchClick = onMatchClick,
                modifier = Modifier.fillMaxSize()
            )
            else -> GrandFinalView(state = state, finals = finals, onMatchClick = onMatchClick)
        }
    }
}

/**
 * Gruppenphase mit anschließender K.-o.-Runde: ein Tab je Gruppe plus die K.-o.-Runde.
 */
@Composable
fun GroupStageView(
    state: TournamentState,
    groupStandings: List<List<LeagueStandingRow>>,
    isWidescreen: Boolean,
    onMatchClick: (TournamentMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val knockout = remember(state.matches) { state.matches.filter { it.bracket == MatchBracket.MAIN } }
    val groupMatches = remember(state.matches) {
        state.groups.indices.map { g -> state.matches.filter { it.bracket == MatchBracket.GROUP && it.group == g } }
    }
    val koTab = state.groups.size

    var selected by remember { mutableStateOf(if (knockout.isNotEmpty()) koTab else 0) }
    // Sobald die K.-o.-Runde ausgelost ist, dorthin wechseln
    LaunchedEffect(knockout.isNotEmpty()) {
        if (knockout.isNotEmpty()) selected = koTab
    }

    Column(modifier = modifier.fillMaxSize()) {
        StageTabs(
            labels = state.groups.indices.map { stringResource(Res.string.tour_stage_group, groupLabel(it)) } + stringResource(Res.string.tour_stage_knockout),
            active = groupMatches.map { matches -> matches.any { it.isReady } } + knockout.any { it.isReady },
            selected = selected,
            onSelect = { selected = it }
        )
        if (selected < koTab) {
            TournamentLeagueView(
                state = state.copy(matches = groupMatches[selected]),
                standings = groupStandings.getOrElse(selected) { emptyList() },
                onMatchClick = onMatchClick,
                qualifierCount = state.qualifiersPerGroup,
                modifier = Modifier.fillMaxSize()
            )
        } else if (knockout.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.tour_stage_ko_pending, state.qualifiersPerGroup),
                    color = Color(0x99FFFFFF),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            TournamentBracketView(
                state = state.copy(matches = knockout),
                isWidescreen = isWidescreen,
                onMatchClick = onMatchClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Umschalter zwischen Turnierteilen im Stil der Runden-Tabs; ein Punkt markiert Teile mit spielbereiten Matches.
 */
@Composable
private fun StageTabs(
    labels: List<String>,
    active: List<Boolean>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x12FFFFFF),
        border = BorderStroke(0.5.dp, Color(0x1AFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 960.dp)
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) Color(0x2BFFFFFF) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0x80FFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (active.getOrElse(index) { false }) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(PrimaryOrange, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Runden als Spalten nebeneinander, ohne Verbindungslinien – für die Verliererrunde, deren Runden
 * nicht wie ein Binärbaum halbiert werden.
 */
@Composable
private fun RoundColumnsView(
    matches: List<TournamentMatch>,
    roundTitle: @Composable (Int, Int) -> String,
    onMatchClick: (TournamentMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val rounds = remember(matches) { matches.groupBy { it.round }.toSortedMap() }
    val maxRound = rounds.keys.maxOrNull() ?: 1

    BoxWithConstraints(modifier = modifier) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .sizeIn(minWidth = viewportWidth, minHeight = viewportHeight)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                rounds.forEach { (round, roundMatches) ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.width(260.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x14FFFFFF),
                            border = BorderStroke(0.5.dp, Color(0x1AFFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = roundTitle(round, maxRound),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        roundMatches.forEach { match ->
                            BracketMatchCard(
                                match = match,
                                onClick = { onMatchClick(match) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grand Final (und ggf. Entscheidungsspiel) mit Siegerkachel.
 */
@Composable
private fun GrandFinalView(
    state: TournamentState,
    finals: List<TournamentMatch>,
    onMatchClick: (TournamentMatch) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            finals.sortedBy { it.round }.forEach { match ->
                Text(
                    text = if (match.round == 1) stringResource(Res.string.tour_stage_grand_final) else stringResource(Res.string.tour_stage_decider),
                    color = Color(0x99FFFFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                BracketMatchCard(
                    match = match,
                    onClick = { onMatchClick(match) },
                    isLarge = true
                )
            }
            if (finals.size == 1 && !finals.first().isFinished) {
                Text(
                    text = stringResource(Res.string.tour_stage_grand_hint),
                    color = Color(0x66FFFFFF),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            state.winner?.let { champion ->
                Spacer(modifier = Modifier.height(8.dp))
                ChampionPodiumCard(champion = champion, isLarge = true)
            }
        }
    }
}
