package com.sdart.app.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import com.sdart.app.DartViewModel
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.domain.player.PlayerStatistics
import com.sdart.app.DartThrow
import com.sdart.app.format
import com.sdart.app.domain.CHECKOUT_PATHS
import com.sdart.app.ui.LocalCompactLayout
import com.sdart.app.ui.theme.GlassBorder
import com.sdart.app.ui.theme.PrimaryOrange

@Composable
fun DesktopCurrentPlayerCard(
    game: Game,
    currentPlayer: Player?,
    currentPlayerIndex: Int,
    vm: DartViewModel,
    onPlayerClick: (Player) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        currentPlayer?.let { player ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LINKS: Riesiger Spielername & Stats/Checkout
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onPlayerClick(player) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val checkoutInfo = if (game.mode == GameMode.ALL_FIVES || game.mode == GameMode.AROUND_THE_CLOCK || game.mode == GameMode.CRICKET) null else when {
                        player.currentScore == 0 -> "🏆 Spiel gewonnen!"
                        player.currentScore in CHECKOUT_PATHS.keys -> {
                            val paths = CHECKOUT_PATHS[player.currentScore]
                            "Checkout: ${paths?.firstOrNull() ?: ""}"
                        }
                        player.currentScore <= 40 -> "Einfacher Checkout möglich"
                        else -> null
                    }

                    checkoutInfo?.let { info ->
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = when {
                                    player.currentScore == 0 -> Color(0xFF4CAF50)
                                    player.currentScore in CHECKOUT_PATHS.keys -> Color(0xFF448AFF)
                                    player.currentScore <= 40 -> Color(0xFF4CAF50)
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val avgText = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                        val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                        "Ø ${avgHits.format(2)} Treffer/Runde"
                    } else if (game.mode == GameMode.CRICKET) {
                        "MPR ${PlayerStatistics.getCricketMPR(player).format(2)}"
                    } else {
                        val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                        "Ø ${avgScore} Pkt./Runde"
                    }

                    Text(
                        text = avgText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // 2. RECHTS: RIESIGE PUNKTE (ohne Kachel)
                if (game.mode == GameMode.AROUND_THE_CLOCK) {
                    com.sdart.app.ui.ATCMainDisplay(
                        player = player,
                        atcConfig = game.atcConfig,
                        modifier = Modifier
                    )
                } else if (game.mode == GameMode.ALL_FIVES) {
                    val roundTotal = vm.getCurrentRoundSum()
                    com.sdart.app.ui.AllFivesMainDisplay(
                        currentRoundTotal = roundTotal,
                        missed = false,
                        modifier = Modifier
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = player.currentScore.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 96.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-2).sp
                            )
                        )
                        Text(
                            text = if (game.mode == GameMode.CRICKET) "PUNKTE" else "REST",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopPlayerScoreboardTable(
    game: Game,
    currentPlayer: Player?,
    currentPlayerIndex: Int,
    vm: DartViewModel,
    onPlayerClick: (Player) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
            // Header: Leg / Match Info wenn Legs > 1
            if (game.totalLegs > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Leg ${game.currentLeg}/${game.totalLegs}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = if (game.players.size == 1) "Legs: ${game.totalLegs}" else "Best of ${game.totalLegs}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    )
                }
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Tabellen-Spaltenüberschriften (proportional gewichtet für Desktop)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPIELER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = "SCHNITT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "LEGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "WÜRFE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "REST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Zeilen für alle Spieler untereinander; bei vielen Spielern scrollt die Liste in begrenzter
            // Höhe und folgt dem Spieler am Zug
            val listState = rememberFollowingListState(currentPlayerIndex)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).nestedScroll(KeepScrollInList),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(game.players) { index, player ->
                    val isCurrentPlayer = index == currentPlayerIndex
                    val isWinner = when (game.mode) {
                        GameMode.AROUND_THE_CLOCK -> player.atcCurrentTarget == 0
                        GameMode.ALL_FIVES -> false
                        else -> player.currentScore == 0
                    }
                    val wonLegs = PlayerStatistics.getWonLegs(player, game.mode)
                    val avgScore = PlayerStatistics.getAverageScorePerRound(player)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(player) },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isWinner -> Color(0xFF2E7D32).copy(alpha = 0.25f)
                            isCurrentPlayer -> PrimaryOrange.copy(alpha = 0.16f)
                            else -> Color.White.copy(alpha = 0.04f)
                        },
                        border = when {
                            isWinner -> BorderStroke(2.dp, Color(0xFF4CAF50))
                            isCurrentPlayer -> BorderStroke(2.dp, PrimaryOrange)
                            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Spalte 1: Spieler Name
                            Column(
                                modifier = Modifier.weight(1.3f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isCurrentPlayer) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = if (isCurrentPlayer) 20.sp else 17.sp,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Spalte 2: DURCHSCHNITT (Checkout, Average)
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val avgText = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                    val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                                    "Ø ${avgHits.format(2)}"
                                } else if (game.mode == GameMode.ALL_FIVES) {
                                    "Ø ${avgScore / 5}"
                                } else {
                                    "Ø $avgScore"
                                }

                                Text(
                                    text = avgText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                val checkoutInfo = if (game.mode == GameMode.ALL_FIVES || game.mode == GameMode.AROUND_THE_CLOCK) null else when {
                                    player.currentScore == 0 -> "🏆 Gewonnen"
                                    player.currentScore in CHECKOUT_PATHS.keys -> {
                                        CHECKOUT_PATHS[player.currentScore]?.firstOrNull()
                                    }
                                    player.currentScore <= 40 -> "Doppel möglich"
                                    else -> null
                                }

                                if (checkoutInfo != null) {
                                    Text(
                                        text = checkoutInfo,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Spalte 3: LEGS
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$wonLegs",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Spalte 4: WÜRFE (Keine Kachel, keine Summe, nur weiße Schrift)
                            val throwsForPlayer: List<DartThrow> = if (isCurrentPlayer) {
                                val count = game.throwsInRound
                                if (count > 0) {
                                    val list = mutableListOf<DartThrow>()
                                    for (i in game.throwHistory.size - 1 downTo 0) {
                                        if (list.size >= count) break
                                        val t = game.throwHistory[i]
                                        if (t.playerIndex == index) {
                                            list.add(0, t)
                                        } else break
                                    }
                                    list
                                } else emptyList()
                            } else {
                                player.dartThrows.takeLast(3)
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                for (dartIndex in 0..2) {
                                    val dt = throwsForPlayer.getOrNull(dartIndex)
                                    val isCurrent = isCurrentPlayer && dartIndex == game.throwsInRound && !game.isFinished

                                    val text = dt?.let {
                                        when {
                                            it.score == 50 -> "BULL"
                                            it.score == 25 -> "25"
                                            it.score == 0 -> "MISS"
                                            it.multiplier == 3 -> "T${it.score / 3}"
                                            it.multiplier == 2 -> "D${it.score / 2}"
                                            else -> "${it.score}"
                                        }
                                    } ?: if (isCurrent) "···" else "—"

                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = if (dt != null || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (dt != null || isCurrent) Color.White else Color.White.copy(alpha = 0.35f)
                                        )
                                    )
                                    if (dartIndex < 2) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                            }

                            // Spalte 5: Restpunkte / Score (nur weiße Schrift)
                            Column(
                                modifier = Modifier.weight(0.8f),
                                horizontalAlignment = Alignment.End
                            ) {
                                val scoreDisplay = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                    if (player.atcCurrentTarget == 0) "Fin" else "Ziel: ${player.atcCurrentTarget}"
                                } else if (game.mode == GameMode.ALL_FIVES) {
                                    (player.currentScore / 5).toString()
                                } else {
                                    player.currentScore.toString()
                                }

                                Text(
                                    text = scoreDisplay,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = if (isCurrentPlayer) 34.sp else 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun DesktopCurrentTurnPanel(
    game: Game,
    currentPlayer: Player?,
    vm: DartViewModel,
    modifier: Modifier = Modifier
) {
    val throwsInRound = game.throwsInRound
    val currentPlayerIndex = game.currentPlayerIndex

    val currentThrows = remember(game.throwHistory.size, throwsInRound, currentPlayerIndex) {
        val list = mutableListOf<DartThrow>()
        if (throwsInRound > 0) {
            for (i in game.throwHistory.size - 1 downTo 0) {
                if (list.size >= throwsInRound) break
                val t = game.throwHistory[i]
                if (t.playerIndex == currentPlayerIndex) {
                    list.add(0, t)
                } else {
                    break
                }
            }
        }
        list
    }

    val roundSum = currentThrows.sumOf { it.score }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AKTUELLE RUNDE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )
                )
                if (roundSum > 0) {
                    Text(
                        text = "Summe: $roundSum",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (dartIndex in 0..2) {
                    val dartThrow = currentThrows.getOrNull(dartIndex)
                    val isCurrent = dartIndex == throwsInRound && !game.isFinished

                    val text = dartThrow?.let { dt ->
                        when {
                            dt.score == 50 -> "BULL"
                            dt.score == 25 -> "25"
                            dt.score == 0 -> "MISS"
                            dt.multiplier == 3 -> "T${dt.score / 3}"
                            dt.multiplier == 2 -> "D${dt.score / 2}"
                            else -> "${dt.score}"
                        }
                    } ?: if (isCurrent) "..." else "—"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .background(
                                color = when {
                                    dartThrow != null -> Color.White.copy(alpha = 0.12f)
                                    isCurrent -> PrimaryOrange.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.04f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) PrimaryOrange else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Dart ${dartIndex + 1}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = text,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    dartThrow != null -> Color.White
                                    isCurrent -> PrimaryOrange
                                    else -> Color.White.copy(alpha = 0.3f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cricket-Tabelle für breite Fenster im Format der Standard-Tabelle ([DesktopPlayerScoreboardTable]):
 * Spaltenköpfe in Großbuchstaben, jeder Spieler als abgerundete Zeile, der Spieler am Zug orange umrandet.
 */
@Composable
fun DesktopCricketScoreboardTable(
    game: Game,
    currentPlayerIndex: Int,
    onPlayerClick: (Player) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val players = game.players
    val segments = listOf(15, 16, 17, 18, 19, 20, 25)
    val showLegs = game.totalLegs > 1
    val headerStyle = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.5f),
        letterSpacing = 1.sp,
        fontSize = 11.sp
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showLegs) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Leg ${game.currentLeg}/${game.totalLegs}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = if (players.size == 1) "Legs: ${game.totalLegs}" else "Best of ${game.totalLegs}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Color.LightGray)
                )
            }
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.12f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        // Spaltenköpfe
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SPIELER", style = headerStyle, modifier = Modifier.weight(1.6f))
            Text("P.", style = headerStyle, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
            if (showLegs) {
                Text("LEGS", style = headerStyle, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
            }
            segments.forEach { segment ->
                Text(
                    text = if (segment == 25) "BULL" else "$segment",
                    style = headerStyle,
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bei vielen Spielern scrollt die Liste im freien Platz und folgt dem Spieler am Zug
        val listState = rememberFollowingListState(currentPlayerIndex)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).nestedScroll(KeepScrollInList),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(players) { index, player ->
                val isCurrentPlayer = index == currentPlayerIndex
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onPlayerClick(player) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrentPlayer) PrimaryOrange.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
                    border = if (isCurrentPlayer) BorderStroke(2.dp, PrimaryOrange)
                        else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isCurrentPlayer) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = if (isCurrentPlayer) 20.sp else 17.sp,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.6f)
                        )
                        Text(
                            text = player.currentScore.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (isCurrentPlayer) 24.sp else 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            ),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center
                        )
                        if (showLegs) {
                            Text(
                                text = player.wonLegs.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (player.wonLegs > 0) Color(0xFF4CAF50) else Color.White
                                ),
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                        segments.forEach { segment ->
                            val marks = player.cricketMarks.getOrDefault(segment, 0)
                            val closedByAll = players.all { it.cricketMarks.getOrDefault(segment, 0) >= 3 }
                            Text(
                                text = when {
                                    closedByAll -> "●"
                                    marks >= 3 -> "⊗"
                                    marks == 2 -> "X"
                                    marks == 1 -> "/"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (closedByAll || marks >= 3) 22.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        closedByAll -> Color.White.copy(alpha = 0.35f)
                                        marks >= 3 -> PrimaryOrange
                                        else -> Color.White
                                    }
                                ),
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Höhe einer Spielerzeile in der Cricket-Tabelle. */
private val CricketRowHeight = 44.dp

@Composable
fun CricketScoreTable(
    players: List<Player>,
    currentPlayerIndex: Int,
    totalLegs: Int = 1,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean
) {
    val cricketSegments = listOf(15, 16, 17, 18, 19, 20)
    
    val headerBgColor = Color.Transparent
    val headerTextColor = if (isDarkTheme) Color(0xFFe0e0e0) else Color(0xFF495057)
    val dividerColor = if (isDarkTheme) Color(0xFF404040) else Color(0xFFdee2e6)
    val rowBgNormal = Color.Transparent
    val highlightColor = Color(0xFFFF9800)
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spieler",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                ),
                modifier = Modifier.weight(2f)
            )
            Text(
                text = "P.", // Punkte
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            if (totalLegs > 1) {
                Text(
                    text = "Legs",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center
                )
            }
            cricketSegments.forEach { segment ->
                Text(
                    text = segment.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = "Bull",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                ),
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.Center
            )
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = dividerColor,
            thickness = 1.dp
        )
        
        // Bei vielen Spielern scrollt die Liste in begrenzter Höhe und folgt dem Spieler am Zug
        val listState = rememberFollowingListState(currentPlayerIndex)
        // Feste Zeilenhöhe, damit genau 4 (Handy) bzw. 8 Spieler (Desktop) sichtbar sind
        val visibleRows = if (LocalCompactLayout.current) 4 else 8
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CricketRowHeight * visibleRows + 2.dp)
                .nestedScroll(KeepScrollInList)
        ) {
            itemsIndexed(players) { index, player ->
                val isCurrentPlayer = index == currentPlayerIndex
                val textColor = if (isDarkTheme) Color.White else Color(0xFF212529)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CricketRowHeight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .then(
                            if (isCurrentPlayer) {
                                Modifier.border(2.dp, highlightColor, RoundedCornerShape(10.dp))
                            } else Modifier
                        )
                        .background(rowBgNormal, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = if (isCurrentPlayer) FontWeight.ExtraBold else FontWeight.Medium,
                            color = textColor
                        ),
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = player.currentScore.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    if (totalLegs > 1) {
                        Text(
                            text = player.wonLegs.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = if (player.wonLegs > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (player.wonLegs > 0) Color(0xFF28a745) else textColor
                            ),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    cricketSegments.forEach { segment ->
                        val marks = player.cricketMarks.getOrDefault(segment, 0)
                        val isGloballyClosed = players.all { it.cricketMarks.getOrDefault(segment, 0) >= 3 }
                        val displayText = when {
                            isGloballyClosed -> "●"
                            marks >= 3 -> "⊗"
                            marks == 2 -> "X"
                            marks == 1 -> "/"
                            else -> ""
                        }
                        val displayColor = when {
                            isGloballyClosed || marks >= 2 -> Color.White
                            else -> textColor
                        }
                        val fontWeight = if (isGloballyClosed) FontWeight.Bold else FontWeight.Normal
                        val fontSize = if (isGloballyClosed || marks >= 3) 24.sp else 13.sp
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                color = displayColor
                            ),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    val bullMarks = player.cricketMarks.getOrDefault(25, 0)
                    val isBullGloballyClosed = players.all { it.cricketMarks.getOrDefault(25, 0) >= 3 }
                    val bullDisplayText = when {
                        isBullGloballyClosed -> "●"
                        bullMarks >= 3 -> "⊗"
                        bullMarks == 2 -> "X"
                        bullMarks == 1 -> "/"
                        else -> ""
                    }
                    val bullDisplayColor = if (isBullGloballyClosed || bullMarks >= 2) Color.White else textColor
                    val bullFontWeight = if (isBullGloballyClosed) FontWeight.Bold else FontWeight.Normal
                    val bullFontSize = if (isBullGloballyClosed || bullMarks >= 3) 24.sp else 13.sp
                    Text(
                        text = bullDisplayText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = bullFontSize,
                            fontWeight = bullFontWeight,
                            color = bullDisplayColor
                        ),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                if (index < players.size - 1) {
                    HorizontalDivider(
                        color = dividerColor,
                        thickness = 0.5.dp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Behält das Scrollen in der Spielertabelle: Am Listenende wird der Rest nicht an die umgebende,
 * scrollbare Seitenleiste weitergereicht, damit sie beim Scrollen durch die Spieler nicht mitläuft.
 */
private val KeepScrollInList = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

/**
 * Listenzustand für Spielertabellen: scrollt zum Spieler am Zug, sobald er nicht vollständig sichtbar ist.
 */
@Composable
private fun rememberFollowingListState(currentPlayerIndex: Int): LazyListState {
    val listState = rememberLazyListState()
    LaunchedEffect(currentPlayerIndex) {
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == currentPlayerIndex }
        val fullyVisible = item != null && item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible && currentPlayerIndex >= 0) {
            listState.animateScrollToItem(currentPlayerIndex)
        }
    }
    return listState
}
