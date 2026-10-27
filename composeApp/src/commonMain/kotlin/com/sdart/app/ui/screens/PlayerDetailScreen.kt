package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.LocalCompactLayout
import com.sdart.app.ui.components.CardLink
import com.sdart.app.ui.components.DashboardCard
import com.sdart.app.ui.components.DesktopPage
import com.sdart.app.ui.components.StatTile
import com.sdart.app.ui.HeatmapDartboard
import com.sdart.app.ui.HighScoresBarChart
import com.sdart.app.ui.WinLossDonutChart
import com.sdart.app.ui.CheckoutPyramidChart
import com.sdart.app.ui.HistoryLineChart
import com.sdart.app.ui.HitDistributionChart
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

/**
 * Detail-Seite für Spieler-Statistiken: Kennzahlen als Kachelreihen, darunter die Diagramme.
 * Spaltenzahl und Abstände richten sich nach der Fensterbreite.
 */
@Composable
fun PlayerDetailScreen(
    player: SavedPlayer,
    onBack: () -> Unit
) {
    val compact = LocalCompactLayout.current
    DesktopPage(title = player.name, fillHeight = false, onBack = onBack) {
        // Auf schmalen Fenstern steht der Zurück-Pfeil im Titel
        if (!compact) {
            CardLink(text = stringResource(Res.string.main_title_players), onClick = onBack, primary = false, modifier = Modifier.padding(bottom = 4.dp))
        }
        DesktopPlayerStats(player)
    }
}

/**
 * Statistik eines Spielers; wird auch neben der Spielerauswahl angezeigt.
 */
@Composable
internal fun DesktopPlayerStats(player: SavedPlayer, modifier: Modifier = Modifier) {
    val isDark = true
    val compact = LocalCompactLayout.current
    val gap = if (compact) 10.dp else 20.dp
    val cardPadding = if (compact) 16.dp else 24.dp
    BoxWithConstraints(modifier = modifier) {
    val availableWidth = maxWidth
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        if (player.totalGames == 0) {
            DashboardCard(title = null, modifier = Modifier.fillMaxWidth(), contentPadding = cardPadding) {
                Text(
                    text = stringResource(Res.string.players_detail_no_games),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        val avgScore = if (player.totalDartsThrown > 0) {
            String.format("%.0f", player.totalPoints.toFloat() / (player.totalDartsThrown / 3f).coerceAtLeast(1f))
        } else String.format("%.0f", 0f)
        val winRate = ((player.gamesWon.toFloat() / player.totalGames) * 100).toInt()
        val checkoutQuote = if (player.checkoutAttempts > 0) {
            String.format("%.0f", (player.checkoutHits.toFloat() / player.checkoutAttempts) * 100)
        } else String.format("%.0f", 0f)
        val first9Avg = if (player.first9Darts > 0) {
            String.format("%.0f", player.first9Points.toFloat() / (player.first9Darts / 3f).coerceAtLeast(1f))
        } else "-"
        val bestDouble = player.doubleHits.maxByOrNull { it.value }?.key?.let { "D$it" } ?: "-"

        val kpis = listOf(
            stringResource(Res.string.stats_games) to player.totalGames.toString(),
            stringResource(Res.string.stats_avg_score) to avgScore,
            stringResource(Res.string.stats_first9_avg) to first9Avg,
            stringResource(Res.string.stats_checkout_pct) to "$checkoutQuote%",
            stringResource(Res.string.stats_wins) to "$winRate%",
            stringResource(Res.string.stats_best_double) to bestDouble,
            "180s" to player.highScores180s.toString(),
            "140+" to player.highScores140s.toString(),
            stringResource(Res.string.stats_total_darts) to player.totalDartsThrown.toString(),
            stringResource(Res.string.stats_total_busts) to player.totalBusts.toString(),
            stringResource(Res.string.stats_max_checkout) to (if (player.bestCheckout > 0) player.bestCheckout.toString() else "-"),
            stringResource(Res.string.stats_best_match) to (if (player.bestMatchDarts > 0) stringResource(Res.string.stats_darts_count, player.bestMatchDarts) else "-")
        )
        // Spaltenzahl nach verfügbarer Breite, damit Beschriftungen und Werte nicht abgeschnitten werden
        val kpiColumns = when {
            availableWidth >= 1000.dp -> 6
            availableWidth >= 560.dp -> 4
            else -> 3
        }
        kpis.chunked(kpiColumns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp)) {
                row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f), compact = compact) }
            }
        }

        val winLoss: @Composable (Modifier) -> Unit = { m ->
            DashboardCard(title = stringResource(Res.string.stats_win_loss), modifier = m, contentPadding = cardPadding) {
                WinLossDonutChart(player.gamesWon, player.totalGames - player.gamesWon, isDark)
            }
        }
        val highScores: @Composable (Modifier) -> Unit = { m ->
            DashboardCard(title = stringResource(Res.string.stats_high_scores), modifier = m, contentPadding = cardPadding) {
                HighScoresBarChart(player.highScores100s, player.highScores140s, player.highScores180s, isDark)
            }
        }

        val charts = buildList<@Composable (Modifier) -> Unit> {
            if (player.savedDartPositions.isNotEmpty()) {
                add { m ->
                    DashboardCard(title = stringResource(Res.string.stats_hit_heatmap), modifier = m, contentPadding = cardPadding) {
                        HeatmapDartboard(
                            hits = player.savedDartPositions.map { Offset(it.x, it.y) },
                            modifier = Modifier.fillMaxWidth().height(320.dp)
                        )
                    }
                }
                // Neben der Heatmap stehen Win/Loss und High Scores übereinander
                add { m ->
                    Column(modifier = m, verticalArrangement = Arrangement.spacedBy(gap)) {
                        winLoss(Modifier.fillMaxWidth())
                        highScores(Modifier.fillMaxWidth())
                    }
                }
            } else {
                add(winLoss)
                add(highScores)
            }
            add { m ->
                DashboardCard(title = stringResource(Res.string.stats_average_score), modifier = m, contentPadding = cardPadding) {
                    HistoryLineChart(
                        history = player.gameHistory,
                        isDark = isDark,
                        title = "",
                        valueSelector = { it.averageScore }
                    )
                }
            }
            add { m ->
                DashboardCard(title = stringResource(Res.string.stats_darts_per_leg_trend), modifier = m, contentPadding = cardPadding) {
                    HistoryLineChart(
                        history = player.gameHistory,
                        isDark = isDark,
                        title = "",
                        valueSelector = { it.dartsPerLeg }
                    )
                }
            }
            add { m ->
                DashboardCard(title = stringResource(Res.string.stats_favorite_checkout), modifier = m, contentPadding = cardPadding) {
                    CheckoutPyramidChart(doubleHits = player.doubleHits, isDark = isDark)
                }
            }
            if (player.segmentHits.isNotEmpty()) {
                add { m ->
                    DashboardCard(title = stringResource(Res.string.stats_hit_distribution), modifier = m, contentPadding = cardPadding) {
                        HitDistributionChart(segmentHits = player.segmentHits, isDark = isDark)
                    }
                }
            }
        }

        // Diagramme paarweise nebeneinander; bei schmalem Platz untereinander
        val chartColumns = if (availableWidth >= 600.dp) 2 else 1
        charts.chunked(chartColumns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { chart -> chart(Modifier.weight(1f)) }
                repeat(chartColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    }
}
