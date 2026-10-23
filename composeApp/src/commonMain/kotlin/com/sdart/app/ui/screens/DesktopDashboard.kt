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
import com.sdart.app.ui.components.*
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

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
    onManagePlayersClick: () -> Unit
) {
    val players by produceState(initialValue = emptyList<SavedPlayer>(), repository) {
        value = repository.loadAllPlayers()
    }
    val lastGame = remember { AppPreferences.lastGameSetup }
    val hostState by sessionManager.hostState.collectAsState()
    val tournament = vm.tournamentManager.currentTournament?.takeIf { !it.isFinished }

    DesktopPage(title = "Übersicht") {
        // Die Kachelreihen teilen sich die Fensterhöhe, damit auch große Monitore gefüllt sind
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
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

// --- Karten ---

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
                        style = MaterialTheme.typography.headlineSmall.copy(
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
                        style = MaterialTheme.typography.headlineSmall.copy(
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

        Spacer(Modifier.weight(1f).heightIn(min = 18.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(58.dp)
        ) {
            ModeTile(Res.drawable.ic_standard, stringResource(Res.string.game_mode_standard), Modifier.weight(1f)) { onGameModeClick(GameMode.X501) }
            ModeTile(Res.drawable.ic_cricket, stringResource(Res.string.game_mode_cricket), Modifier.weight(1f)) { onGameModeClick(GameMode.CRICKET) }
            ModeTile(Res.drawable.ic_all_fives, stringResource(Res.string.game_mode_all_fives), Modifier.weight(1f)) { onGameModeClick(GameMode.ALL_FIVES) }
            ModeTile(Res.drawable.ic_atc, stringResource(Res.string.game_mode_atc), Modifier.weight(1f)) { onGameModeClick(GameMode.AROUND_THE_CLOCK) }
        }
    }
}

@Composable
private fun ModeTile(icon: DrawableResource, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassCardBg)
            .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Icon(painterResource(icon), null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = PrimaryOrange),
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
        .take(8)

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
