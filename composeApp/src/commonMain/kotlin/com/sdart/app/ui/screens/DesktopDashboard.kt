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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.connect.HostState
import com.sdart.app.connect.SessionManager
import com.sdart.app.tournament.TournamentState
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

private val CardShape = RoundedCornerShape(15.dp)
private val ActiveOrangeBg = Color(0xFF2E2218)
private val OnlineGreen = Color(0xFF4CAF50)

/**
 * Startseite für breite Fenster (Desktop, Tablet quer): feste Navigationsleiste links,
 * daneben Schnellstart, Turnierstatus, Bestenliste, Geräte und Bestwerte.
 */
@Composable
fun DesktopDashboard(
    vm: DartViewModel,
    repository: PlayerRepository,
    sessionManager: SessionManager,
    onGamesClick: () -> Unit,
    onGameModeClick: (GameMode) -> Unit,
    onRematchClick: (LastGameSetup) -> Unit,
    onTournamentClick: () -> Unit,
    onResumeTournamentClick: () -> Unit,
    onPcConnect: () -> Unit,
    onHostOverviewClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onManagePlayersClick: () -> Unit
) {
    val players by produceState(initialValue = emptyList<SavedPlayer>(), repository) {
        value = repository.loadAllPlayers()
    }
    val lastGame = remember { AppPreferences.lastGameSetup }
    val hostState by sessionManager.hostState.collectAsState()
    val tournament = vm.tournamentManager.currentTournament?.takeIf { !it.isFinished }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        DashboardNavRail(
            onGamesClick = onGamesClick,
            onTournamentClick = if (tournament != null) onResumeTournamentClick else onTournamentClick,
            onManagePlayersClick = onManagePlayersClick,
            onPcConnect = onPcConnect,
            onSettingsClick = onSettingsClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Übersicht",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickStartCard(
                    lastGame = lastGame,
                    onRematchClick = onRematchClick,
                    onGameModeClick = onGameModeClick,
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
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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
}

// --- Navigation ---

@Composable
private fun DashboardNavRail(
    onGamesClick: () -> Unit,
    onTournamentClick: () -> Unit,
    onManagePlayersClick: () -> Unit,
    onPcConnect: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, bottom = 20.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_main_logo),
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "S'Dart App",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        NavRailItem(icon = Res.drawable.ic_chart_bar, label = "Übersicht", selected = true, onClick = {})
        NavRailItem(icon = Res.drawable.ic_target, label = stringResource(Res.string.main_title_games), onClick = onGamesClick)
        NavRailItem(icon = Res.drawable.ic_trophy, label = stringResource(Res.string.main_title_tournament), onClick = onTournamentClick)
        NavRailItem(icon = Res.drawable.ic_player_management, label = stringResource(Res.string.main_title_players), onClick = onManagePlayersClick)
        NavRailItem(icon = Res.drawable.ic_mobile_connect, label = stringResource(Res.string.main_title_connect), onClick = onPcConnect)

        Spacer(Modifier.weight(1f))

        NavRailItem(
            vector = Icons.Default.Settings,
            label = stringResource(Res.string.settings),
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun NavRailItem(
    label: String,
    onClick: () -> Unit,
    icon: DrawableResource? = null,
    vector: ImageVector? = null,
    selected: Boolean = false
) {
    val contentColor = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ActiveOrangeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        when {
            icon != null -> Icon(painterResource(icon), null, tint = contentColor, modifier = Modifier.size(22.dp))
            vector != null -> Icon(vector, null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- Karten ---

@Composable
private fun DashboardCard(
    title: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(if (highlighted) ActiveOrangeBg else MaterialTheme.colorScheme.surface)
            .border(
                width = 2.dp,
                color = if (highlighted) PrimaryOrange else MaterialTheme.colorScheme.outline,
                shape = CardShape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun QuickStartCard(
    lastGame: LastGameSetup?,
    onRematchClick: (LastGameSetup) -> Unit,
    onGameModeClick: (GameMode) -> Unit,
    onGamesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(title = "Schnellstart", modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (lastGame != null) {
                    Text(
                        text = describeGame(lastGame),
                        style = MaterialTheme.typography.titleLarge.copy(
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
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Noch kein Spiel gespielt",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Wähle unten einen Spielmodus, um loszulegen.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { if (lastGame != null) onRematchClick(lastGame) else onGamesClick() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (lastGame != null) "Revanche" else "Spiel starten",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(Res.drawable.ic_standard, stringResource(Res.string.game_mode_standard)) { onGameModeClick(GameMode.X501) }
            ModeChip(Res.drawable.ic_cricket, stringResource(Res.string.game_mode_cricket)) { onGameModeClick(GameMode.CRICKET) }
            ModeChip(Res.drawable.ic_all_fives, stringResource(Res.string.game_mode_all_fives)) { onGameModeClick(GameMode.ALL_FIVES) }
            ModeChip(Res.drawable.ic_atc, stringResource(Res.string.game_mode_atc)) { onGameModeClick(GameMode.AROUND_THE_CLOCK) }
        }
    }
}

@Composable
private fun ModeChip(icon: DrawableResource, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(painterResource(icon), null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1
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
        title = if (tournament != null) "Turnier läuft" else stringResource(Res.string.main_title_tournament),
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = PrimaryOrange),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = next?.let { "Nächstes: ${it.player1?.name} vs. ${it.player2?.name}" } ?: "Warte auf nächste Runde",
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
                    .background(MaterialTheme.colorScheme.outline)
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
                text = "$finished von ${tournament.matches.size} Matches gespielt",
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
                text = "Turnier erstellen →",
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
        .take(5)

    DashboardCard(title = "Bestenliste · Ø pro Aufnahme", modifier = modifier) {
        if (ranked.isEmpty()) {
            Text(
                text = "Sobald gespeicherte Spieler ein Spiel beendet haben, erscheinen sie hier.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Spieler verwalten →",
                style = MaterialTheme.typography.labelLarge.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 12.dp).clickable(onClick = onManagePlayersClick)
            )
        } else {
            ranked.forEachIndexed { index, player ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onManagePlayersClick)
                        .padding(vertical = 7.dp, horizontal = 4.dp)
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
                        modifier = Modifier.width(120.dp).height(24.dp)
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

@Composable
private fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val range = (values.max() - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val point = Offset(i * stepX, size.height - (v - min) / range * size.height)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun DevicesCard(
    hostState: HostState,
    onHostOverviewClick: () -> Unit,
    onPcConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(title = "Geräte", modifier = modifier) {
        when (hostState) {
            is HostState.Active -> {
                StatusLine(online = true, label = "Host aktiv", detail = "Port ${hostState.port}")
                if (hostState.participants.isEmpty()) {
                    Text(
                        text = "Noch kein Gerät verbunden",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    hostState.participants.forEach { participant ->
                        StatusLine(online = true, label = participant.name, detail = participant.platform)
                    }
                }
            }
            HostState.Inactive -> {
                StatusLine(online = false, label = "Host inaktiv", detail = null)
                Text(
                    text = "Starte den Host, damit sich Handys als Kamera oder Eingabe verbinden können.",
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
                text = "Host-Übersicht →",
                style = MaterialTheme.typography.labelLarge.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clickable(onClick = onHostOverviewClick)
            )
            Text(
                text = "Verbinden →",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.clickable(onClick = onPcConnect)
            )
        }
    }
}

@Composable
private fun StatusLine(online: Boolean, label: String, detail: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (online) OnlineGreen else MaterialTheme.colorScheme.outline)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    DashboardCard(title = "Bestwerte aller Spieler", modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("180er", total180s.toString(), Modifier.weight(1f))
            StatTile("Bestes Checkout", if (bestCheckout > 0) bestCheckout.toString() else "–", Modifier.weight(1f))
            StatTile("Höchste Aufnahme", if (highestRound > 0) highestRound.toString() else "–", Modifier.weight(1f))
            StatTile("Kürzestes Leg", fewestDarts?.let { "$it Darts" } ?: "–", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// --- Hilfsfunktionen ---

private fun SavedPlayer.roundAverage(): Float =
    if (totalRounds > 0) totalPoints.toFloat() / totalRounds else 0f

private fun formatOneDecimal(value: Float): String {
    val tenths = (value * 10).toInt()
    return "${tenths / 10},${tenths % 10}"
}

private fun describeGame(setup: LastGameSetup): String = when (setup.mode) {
    GameMode.CRICKET -> "Cricket"
    GameMode.ALL_FIVES -> "All Fives"
    GameMode.AROUND_THE_CLOCK -> "Around the Clock"
    else -> {
        val score = if (setup.customScore > 0) setup.customScore else setup.mode.startScore
        val out = if (setup.doubleOut) "Double Out" else "Single Out"
        val legs = if (setup.legs > 1) " · ${setup.legs} Legs" else ""
        "$score · $out$legs"
    }
}

private fun relativeDay(millis: Long): String {
    val dayMillis = 24L * 60 * 60 * 1000
    val days = ((currentTimeMillis() - millis) / dayMillis).toInt()
    return when {
        days <= 0 -> "heute"
        days == 1 -> "gestern"
        else -> "vor $days Tagen"
    }
}
