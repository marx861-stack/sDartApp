package com.sdart.app.connect.ui

import sdartapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sdart.app.connect.GameSnapshot
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.PlayerSnapshot
import com.sdart.app.ui.theme.GlassBorder
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.Res
import sdartapp.composeapp.generated.resources.ic_check_circle
import sdartapp.composeapp.generated.resources.ic_role_display
import sdartapp.composeapp.generated.resources.ic_trophy

/**
 * Vollbild-Scoreboard für das Companion-Gerät (Rolle DISPLAY).
 * Entwickelt für Tablets an der Wand, Zweitmonitore oder Handys im Zuschauer-Modus.
 * Reagiert in Echtzeit auf GameSnapshots vom Host.
 */
@Composable
fun CompanionScoreboardView(
    hostName: String,
    participantManager: ParticipantSessionManager,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameSnapshot by participantManager.gameSnapshot.collectAsState()
    val snapshot = gameSnapshot

    if (snapshot == null || !snapshot.isGameActive) {
        CompanionScoreboardWaitingView(
            hostName = hostName,
            onDisconnect = onDisconnect,
            modifier = modifier
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(12.dp)) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            // Landscape / Tablet / TV Layout: Split-Screen
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Linke Spalte: Aktiver Spieler, riesiger Score & aktuelle 3 Darts
                Column(
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ScoreboardTopBar(
                        hostName = hostName,
                        gameMode = snapshot.gameMode,
                        currentLeg = snapshot.currentLeg,
                        totalLegs = snapshot.totalLegs,
                        onDisconnect = onDisconnect
                    )

                    ActivePlayerHeroCard(
                        snapshot = snapshot,
                        modifier = Modifier.weight(1f)
                    )

                    CurrentTurnDartsCard(
                        snapshot = snapshot,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Rechte Spalte: Gesamttabelle aller Spieler
                Column(
                    modifier = Modifier.weight(0.9f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AllPlayersScoreboardCard(
                        snapshot = snapshot,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Portrait / Smartphone Layout: Vertikal gestapelt
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScoreboardTopBar(
                    hostName = hostName,
                    gameMode = snapshot.gameMode,
                    currentLeg = snapshot.currentLeg,
                    totalLegs = snapshot.totalLegs,
                    onDisconnect = onDisconnect
                )

                ActivePlayerHeroCard(
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxWidth()
                )

                CurrentTurnDartsCard(
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxWidth()
                )

                AllPlayersScoreboardCard(
                    snapshot = snapshot,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Kompakte obere Leiste mit Host-Status und Spielmodus.
 */
@Composable
private fun ScoreboardTopBar(
    hostName: String,
    gameMode: String,
    currentLeg: Int,
    totalLegs: Int,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF4CAF50), CircleShape)
            )
            Column {
                Text(
                    text = gameMode.ifBlank { stringResource(Res.string.connect_scoreboard_active) },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = stringResource(Res.string.connect_live_from_host, hostName) + if (totalLegs > 1) " • Leg $currentLeg/$totalLegs" else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                )
            }
        }

        OutlinedButton(
            onClick = onDisconnect,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(stringResource(Res.string.connect_disconnect_short), color = Color.White, fontSize = 12.sp)
        }
    }
}

/**
 * Hero-Karte des aktiven Spielers mit weithin sichtbarer Punktzahl & Checkout.
 */
@Composable
private fun ActivePlayerHeroCard(
    snapshot: GameSnapshot,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        snapshot.isFinished -> Color(0xFF4CAF50)
        snapshot.isBust -> Color(0xFFFF5252)
        else -> PrimaryOrange.copy(alpha = 0.8f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Oberer Bereich: Spielername & Status-Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (snapshot.isFinished) stringResource(Res.string.connect_match_winner) else stringResource(Res.string.connect_on_turn_caps),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (snapshot.isFinished) Color(0xFF4CAF50) else PrimaryOrange,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = snapshot.currentPlayerName,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 32.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (snapshot.totalLegs > 1) {
                    val activePlayer = snapshot.players.getOrNull(snapshot.currentPlayerIndex)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF333333),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(
                            text = stringResource(Res.string.connect_legs_won, (activePlayer?.legsWon ?: 0).toString()),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // Mittlerer Bereich: Gigantischer Score
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${snapshot.currentPlayerScore}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Black,
                        color = if (snapshot.isFinished) Color(0xFF4CAF50) else PrimaryOrange
                    ),
                    textAlign = TextAlign.Center
                )

                if (snapshot.gameMode.contains("Cricket", ignoreCase = true)) {
                    Text(
                        text = stringResource(Res.string.connect_cricket_points),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )
                } else if (snapshot.gameMode.contains("Clock", ignoreCase = true)) {
                    val target = snapshot.players.getOrNull(snapshot.currentPlayerIndex)?.atcTarget ?: 1
                    Text(
                        text = stringResource(Res.string.connect_current_target, target.toString()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF448AFF)
                        )
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.connect_remaining),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                    )
                }
            }

            // Unterer Bereich: Checkout-Empfehlung oder Runden-Info
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    snapshot.isFinished -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Text(
                                text = stringResource(Res.string.connect_game_won),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }
                    snapshot.isBust -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF5252).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                        ) {
                            Text(
                                text = stringResource(Res.string.connect_bust),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            )
                        }
                    }
                    snapshot.checkoutSuggestion != null -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF448AFF).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF448AFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.connect_checkout_label),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = snapshot.checkoutSuggestion,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF64B5F6)
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(Res.string.connect_round_points, snapshot.currentRoundTotal.toString()),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Zeigt die 3 Darts der aktuellen Aufnahme groß und gut lesbar an.
 */
@Composable
private fun CurrentTurnDartsCard(
    snapshot: GameSnapshot,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.connect_darts_this_round),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = stringResource(Res.string.connect_sum, snapshot.currentRoundTotal.toString()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange,
                        fontSize = 12.sp
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (i in 0 until 3) {
                    val dart = snapshot.currentRoundDarts.getOrNull(i)
                    val isNext = i == snapshot.throwsInRound && !snapshot.isFinished && !snapshot.isBust

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                dart?.isBust == true -> Color(0xFFFF5252).copy(alpha = 0.25f)
                                dart != null -> PrimaryOrange.copy(alpha = 0.2f)
                                isNext -> Color(0xFF2E2E2E)
                                else -> Color(0xFF1E1E1E)
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                dart?.isBust == true -> Color(0xFFFF5252)
                                dart != null -> PrimaryOrange
                                isNext -> Color.White.copy(alpha = 0.45f)
                                else -> GlassBorder
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(Res.string.connect_dart_n, (i + 1).toString()),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dart?.label ?: if (isNext) "..." else "-",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        dart != null -> Color.White
                                        isNext -> PrimaryOrange
                                        else -> Color.Gray
                                    },
                                    fontSize = 16.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Scoreboard-Tabelle aller Spieler im Match.
 */
@Composable
private fun AllPlayersScoreboardCard(
    snapshot: GameSnapshot,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Header-Zeile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.connect_player_overview),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )
                )

                if (snapshot.totalLegs > 1) {
                    Text(
                        text = stringResource(Res.string.connect_legs_caps),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Spielerliste
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snapshot.players) { player ->
                    PlayerScoreboardRow(
                        player = player,
                        isWinner = snapshot.isFinished && player.name == snapshot.winnerName,
                        totalLegs = snapshot.totalLegs,
                        isCricket = snapshot.gameMode.contains("Cricket", ignoreCase = true)
                    )
                }
            }
        }
    }
}

/**
 * Zeile für einen einzelnen Spieler in der Scoreboard-Tabelle.
 */
@Composable
private fun PlayerScoreboardRow(
    player: PlayerSnapshot,
    isWinner: Boolean,
    totalLegs: Int,
    isCricket: Boolean
) {
    val isCurrent = player.isCurrent
    val rowBg = when {
        isWinner -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        isCurrent -> PrimaryOrange.copy(alpha = 0.18f)
        else -> Color(0xFF282828)
    }
    val rowBorder = when {
        isWinner -> Color(0xFF4CAF50)
        isCurrent -> PrimaryOrange
        else -> GlassBorder
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = rowBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, rowBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PrimaryOrange, CircleShape)
                        )
                    }

                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    player.lastRoundSum?.let { last ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = stringResource(Res.string.connect_last, last.toString()),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (totalLegs > 1) {
                        Text(
                            text = "${player.legsWon}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Text(
                        text = "${player.score}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isWinner) Color(0xFF4CAF50) else if (isCurrent) PrimaryOrange else Color.White,
                            fontSize = 26.sp
                        )
                    )
                }
            }

            // Cricket Marken Übersicht (falls Modus Cricket ist)
            if (isCricket && player.cricketMarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val segments = listOf(15, 16, 17, 18, 19, 20, 25)
                    for (seg in segments) {
                        val marks = player.cricketMarks[seg] ?: 0
                        val markSymbol = when (marks) {
                            0 -> "-"
                            1 -> "/"
                            2 -> "X"
                            else -> "⨂"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (seg == 25) "B" else "$seg",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = markSymbol,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (marks >= 3) Color(0xFF4CAF50) else Color.White,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ansprechende Warte-Ansicht, wenn auf dem Host noch kein Spiel läuft.
 */
@Composable
private fun CompanionScoreboardWaitingView(
    hostName: String,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Obere Statusleiste
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Column {
                    Text(
                        text = stringResource(Res.string.connect_scoreboard_connected),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = stringResource(Res.string.connect_receiving_from, hostName),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(stringResource(Res.string.connect_disconnect_short), color = Color.White, fontSize = 12.sp)
            }
        }

        // Zentrierte Warte-Karte
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF448AFF).copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, Color(0xFF448AFF).copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_role_display),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = Color(0xFF448AFF)
                        )
                    }

                    Text(
                        text = stringResource(Res.string.connect_scoreboard_ready),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(Res.string.connect_scoreboard_ready_desc, hostName),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
