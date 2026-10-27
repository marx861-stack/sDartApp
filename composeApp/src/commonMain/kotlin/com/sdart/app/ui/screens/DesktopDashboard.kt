package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.connect.HostState
import com.sdart.app.connect.SessionManager
import com.sdart.app.tournament.TournamentState
import com.sdart.app.ui.components.*
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

/**
 * Startseite mit Schnellstart, Turnierstatus, Bestenliste, Geräten und Bestwerten.
 * Breite Fenster (Desktop, Tablet quer) zeigen die Karten als Raster neben der Navigationsleiste,
 * schmale Fenster ([compact], Handy) untereinander mit eigener Kopfzeile.
 */
@Composable
fun DesktopDashboard(
    vm: DartViewModel,
    repository: PlayerRepository,
    sessionManager: SessionManager,
    onGamesClick: () -> Unit,
    onRematchClick: (LastGameSetup) -> Unit,
    onTournamentClick: () -> Unit,
    onResumeTournamentClick: () -> Unit,
    onPcConnect: () -> Unit,
    onHostOverviewClick: () -> Unit,
    onManagePlayersClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    compact: Boolean = false
) {
    val players by produceState(initialValue = emptyList<SavedPlayer>(), repository) {
        value = repository.loadAllPlayers()
    }
    val lastGame = remember { AppPreferences.lastGameSetup }
    val hostState by sessionManager.hostState.collectAsState()
    val tournament = vm.tournamentManager.currentTournament?.takeIf { !it.isFinished }

    if (compact) {
        CompactDashboardPage(onSettingsClick = onSettingsClick) {
            QuickStartCard(
                lastGame = lastGame,
                onRematchClick = onRematchClick,
                onGamesClick = onGamesClick,
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
            CompactTiles(
                tournament = tournament,
                hostState = hostState,
                players = players,
                onTournamentClick = if (tournament != null) onResumeTournamentClick else onTournamentClick,
                onDevicesClick = onPcConnect,
                onPlayersClick = onManagePlayersClick
            )
        }
        return
    }

    DesktopPage(title = stringResource(Res.string.nav_overview)) {
        // Die Kachelreihen teilen sich die Fensterhöhe, damit auch große Monitore gefüllt sind
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            QuickStartCard(
                lastGame = lastGame,
                onRematchClick = onRematchClick,
                onGamesClick = onGamesClick,
                modifier = Modifier.weight(2f).fillMaxHeight()
            )
            TournamentStatusCard(
                tournament = tournament,
                onClick = if (tournament != null) onResumeTournamentClick else onTournamentClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LeaderboardCard(
                players = players,
                onManagePlayersClick = onManagePlayersClick,
                modifier = Modifier.weight(2f).fillMaxHeight()
            )
            DevicesCard(
                hostState = hostState,
                onHostOverviewClick = onHostOverviewClick,
                onPcConnect = onPcConnect,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        HighlightsCard(players = players)
    }
}

/**
 * Seite des Dashboards auf schmalen Fenstern: Kopfzeile mit Logo und Einstellungen,
 * darunter die Karten in einer scrollbaren Spalte.
 */
@Composable
private fun CompactDashboardPage(
    onSettingsClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // Systemleisten-Abstand außen, sonst scrollt der Inhalt unter die Statusleiste
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_main_logo),
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "S'Dart App",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(Res.string.settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            content()
        }
    }
}

/**
 * Übersicht auf dem Handy als Kachelraster: Turnier und Geräte nebeneinander, darunter die Bestenliste
 * über die ganze Breite und die Bestwerte als einzelne Kacheln. Jede Kachel führt in ihren Bereich.
 */
@Composable
private fun CompactTiles(
    tournament: TournamentState?,
    hostState: HostState,
    players: List<SavedPlayer>,
    onTournamentClick: () -> Unit,
    onDevicesClick: () -> Unit,
    onPlayersClick: () -> Unit
) {
    val ranked = players.filter { it.totalRounds > 0 }.sortedByDescending { it.roundAverage() }
    val host = hostState as? HostState.Active

    TileRow {
        if (tournament != null) {
            val finished = tournament.matches.count { it.isFinished }
            val total = tournament.matches.size.coerceAtLeast(1)
            DashboardTile(
                icon = Res.drawable.ic_trophy,
                title = stringResource(Res.string.dash_tournament_running),
                value = tournament.name,
                valueColor = PrimaryOrange,
                highlighted = true,
                onClick = onTournamentClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GlassControlBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(finished.toFloat() / total)
                            .fillMaxHeight()
                            .background(PrimaryOrange)
                    )
                }
                TileDetail(stringResource(Res.string.dash_tile_matches_progress, finished, tournament.matches.size))
            }
        } else {
            DashboardTile(
                icon = Res.drawable.ic_trophy,
                title = stringResource(Res.string.main_title_tournament),
                value = stringResource(Res.string.dash_tile_new_tournament),
                onClick = onTournamentClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                TileDetail(stringResource(Res.string.dash_tile_tournament_hint))
            }
        }
        DashboardTile(
            icon = Res.drawable.ic_mobile_connect,
            title = stringResource(Res.string.nav_devices),
            value = if (host != null) stringResource(Res.string.dash_tile_connected, host.participants.size) else stringResource(Res.string.dash_tile_host_off),
            onClick = onDevicesClick,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (host != null) OnlineGreen else GlassControlBorder)
                )
                Spacer(Modifier.width(6.dp))
                TileDetail(if (host != null) stringResource(Res.string.dash_tile_port, host.port) else stringResource(Res.string.dash_tile_tap_to_connect), topPadding = 0.dp)
            }
        }
    }

    // Bestenliste über die ganze Breite: die ersten drei mit Verlauf und Schnitt
    DashboardTile(
        icon = Res.drawable.ic_chart_bar,
        title = stringResource(Res.string.dash_leaderboard_title),
        value = if (ranked.isEmpty()) stringResource(Res.string.dash_leaderboard_empty_value) else null,
        onClick = onPlayersClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (ranked.isEmpty()) {
            TileDetail(stringResource(Res.string.dash_leaderboard_empty_hint))
        } else {
            Spacer(Modifier.height(6.dp))
            ranked.take(3).forEachIndexed { index, player ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (index == 0) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Sparkline(
                        values = player.gameHistory.map { it.averageScore },
                        color = if (index == 0) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(72.dp).height(22.dp)
                    )
                    Text(
                        text = formatOneDecimal(player.roundAverage()),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(56.dp)
                    )
                }
            }
        }
    }

    val total180s = players.sumOf { it.highScores180s }
    val bestCheckout = players.maxOfOrNull { it.bestCheckout } ?: 0
    val highestRound = players.maxOfOrNull { it.highestRoundScore } ?: 0
    val fewestDarts = players.map { it.bestMatchDarts }.filter { it > 0 }.minOrNull()
    CardTitle(stringResource(Res.string.dash_highlights_title), modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    listOf(
        stringResource(Res.string.dash_stat_180s) to total180s.toString(),
        stringResource(Res.string.dash_stat_best_checkout) to if (bestCheckout > 0) bestCheckout.toString() else "–",
        stringResource(Res.string.dash_stat_highest_round) to if (highestRound > 0) highestRound.toString() else "–",
        stringResource(Res.string.dash_stat_fewest_darts) to (fewestDarts?.let { stringResource(Res.string.dash_darts_count, it) } ?: "–")
    ).chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f), compact = true) }
        }
    }
}

/** Zwei gleich hohe Kacheln nebeneinander. */
@Composable
private fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        content = content
    )
}

/** Kachel der Handy-Übersicht: Symbol und kleiner Titel, darunter der Hauptwert (falls vorhanden) und Details. */
@Composable
private fun DashboardTile(
    icon: DrawableResource,
    title: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    highlighted: Boolean = false,
    details: @Composable ColumnScope.() -> Unit
) {
    DashboardCard(
        title = null,
        highlighted = highlighted,
        onClick = onClick,
        modifier = modifier.heightIn(min = 128.dp),
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        details()
    }
}

@Composable
private fun TileDetail(text: String, topPadding: Dp = 2.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = topPadding)
    )
}

// --- Karten ---

@Composable
private fun QuickStartCard(
    lastGame: LastGameSetup?,
    onRematchClick: (LastGameSetup) -> Unit,
    onGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val onStart = { if (lastGame != null) onRematchClick(lastGame) else onGamesClick() }
    DashboardCard(title = stringResource(Res.string.dash_quick_start), modifier = modifier, contentPadding = cardPadding(compact)) {
        // Auf dem Handy steht der Button unter der Beschreibung, damit der Text Platz hat
        if (compact) {
            QuickStartSummary(lastGame, compact = true)
            QuickStartButton(lastGame, onStart, Modifier.fillMaxWidth().padding(top = 10.dp), compact = true)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuickStartSummary(lastGame, Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                QuickStartButton(lastGame, onStart)
            }
        }
    }
}

@Composable
private fun QuickStartSummary(lastGame: LastGameSetup?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val titleStyle = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall
    Column(modifier = modifier) {
        if (lastGame != null) {
            Text(
                text = describeGame(lastGame),
                style = titleStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = lastGame.playerNames.joinToString(", ") +
                        (if (lastGame.startedAtMillis > 0) " · ${relativeDay(lastGame.startedAtMillis)}" else ""),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            Text(
                text = stringResource(Res.string.dash_no_game_yet),
                style = titleStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = stringResource(Res.string.dash_start_first_game),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun QuickStartButton(
    lastGame: LastGameSetup?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.Black),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = if (compact) 8.dp else 12.dp),
        modifier = modifier
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (lastGame != null) stringResource(Res.string.dash_rematch) else stringResource(Res.string.dash_start_game),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TournamentStatusCard(
    tournament: TournamentState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        title = if (tournament != null) stringResource(Res.string.dash_tournament_running) else stringResource(Res.string.main_title_tournament),
        modifier = modifier,
        highlighted = tournament != null,
        onClick = onClick
    ) {
        if (tournament != null) {
            val finished = tournament.matches.count { it.isFinished }
            val total = tournament.matches.size.coerceAtLeast(1)
            val next = tournament.matches.firstOrNull { !it.isFinished && it.player1 != null && it.player2 != null }

            Text(
                text = tournament.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = PrimaryOrange),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = next?.let { stringResource(Res.string.dash_next_match, it.player1?.name.orEmpty(), it.player2?.name.orEmpty()) } ?: stringResource(Res.string.dash_waiting_next_round),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GlassControlBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(finished.toFloat() / total)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(PrimaryOrange)
                )
            }
            Text(
                text = stringResource(Res.string.dash_matches_played, finished, tournament.matches.size),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_trophy),
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(36.dp).padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(Res.string.main_desc_tournament),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.dash_create_tournament),
                style = MaterialTheme.typography.labelLarge.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun LeaderboardCard(
    players: List<SavedPlayer>,
    onManagePlayersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ranked = players
        .filter { it.totalRounds > 0 }
        .sortedByDescending { it.roundAverage() }
        .take(8)

    DashboardCard(title = stringResource(Res.string.dash_leaderboard_title), modifier = modifier) {
        if (ranked.isEmpty()) {
            Text(
                text = stringResource(Res.string.dash_leaderboard_empty_long),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.dash_manage_players),
                style = MaterialTheme.typography.labelLarge.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 12.dp).clickable(onClick = onManagePlayersClick)
            )
        } else {
            // Bei kleinem Fenster scrollen die Zeilen in der Karte, statt abgeschnitten zu werden
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                ranked.forEachIndexed { index, player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onManagePlayersClick)
                            .padding(vertical = 9.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (index == 0) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Sparkline(
                            values = player.gameHistory.map { it.averageScore },
                            color = if (index == 0) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(160.dp).height(26.dp)
                        )
                        Text(
                            text = formatOneDecimal(player.roundAverage()),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.width(64.dp).padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicesCard(
    hostState: HostState,
    onHostOverviewClick: () -> Unit,
    onPcConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(title = stringResource(Res.string.nav_devices), modifier = modifier) {
        when (hostState) {
            is HostState.Active -> {
                StatusLine(online = true, label = stringResource(Res.string.dash_host_active), detail = stringResource(Res.string.dash_tile_port, hostState.port))
                if (hostState.participants.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.dash_no_device_connected),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    // Viele Geräte scrollen in der Karte, die Links darunter bleiben sichtbar
                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        hostState.participants.forEach { participant ->
                            StatusLine(online = true, label = participant.name, detail = participant.platform)
                        }
                    }
                }
            }
            HostState.Inactive -> {
                StatusLine(online = false, label = stringResource(Res.string.dash_host_inactive), detail = null)
                Text(
                    text = stringResource(Res.string.dash_host_inactive_hint),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.dash_host_overview_link),
                style = MaterialTheme.typography.labelLarge.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clickable(onClick = onHostOverviewClick)
            )
            Text(
                text = stringResource(Res.string.dash_connect_link),
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.clickable(onClick = onPcConnect)
            )
        }
    }
}

@Composable
private fun HighlightsCard(players: List<SavedPlayer>) {
    val total180s = players.sumOf { it.highScores180s }
    val bestCheckout = players.maxOfOrNull { it.bestCheckout } ?: 0
    val highestRound = players.maxOfOrNull { it.highestRoundScore } ?: 0
    val fewestDarts = players.map { it.bestMatchDarts }.filter { it > 0 }.minOrNull()

    val stats = listOf(
        stringResource(Res.string.dash_stat_180s) to total180s.toString(),
        stringResource(Res.string.dash_stat_best_checkout) to if (bestCheckout > 0) bestCheckout.toString() else "–",
        stringResource(Res.string.dash_stat_highest_round) to if (highestRound > 0) highestRound.toString() else "–",
        stringResource(Res.string.dash_stat_fewest_darts) to (fewestDarts?.let { stringResource(Res.string.dash_darts_count, it) } ?: "–")
    )
    DashboardCard(title = stringResource(Res.string.dash_highlights_title), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            stats.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
        }
    }
}

// --- Hilfsfunktionen ---

private fun cardPadding(compact: Boolean) = if (compact) 16.dp else 24.dp

private fun SavedPlayer.roundAverage(): Float =
    if (totalRounds > 0) totalPoints.toFloat() / totalRounds else 0f

@Composable
private fun formatOneDecimal(value: Float): String {
    val tenths = (value * 10).toInt()
    return "${tenths / 10}${stringResource(Res.string.dash_decimal_separator)}${tenths % 10}"
}

@Composable
private fun describeGame(setup: LastGameSetup): String = when (setup.mode) {
    GameMode.CRICKET -> stringResource(Res.string.game_mode_cricket)
    GameMode.ALL_FIVES -> stringResource(Res.string.game_mode_all_fives)
    GameMode.AROUND_THE_CLOCK -> stringResource(Res.string.game_mode_atc)
    else -> {
        val score = if (setup.customScore > 0) setup.customScore else setup.mode.startScore
        val out = if (setup.doubleOut) stringResource(Res.string.dash_double_out) else stringResource(Res.string.dash_single_out)
        val legs = if (setup.legs > 1) " · " + stringResource(Res.string.dash_legs_suffix, setup.legs) else ""
        "$score · $out$legs"
    }
}

@Composable
private fun relativeDay(millis: Long): String {
    val dayMillis = 24L * 60 * 60 * 1000
    val days = ((currentTimeMillis() - millis) / dayMillis).toInt()
    return when {
        days <= 0 -> stringResource(Res.string.dash_today)
        days == 1 -> stringResource(Res.string.dash_yesterday)
        else -> stringResource(Res.string.dash_days_ago, days)
    }
}
