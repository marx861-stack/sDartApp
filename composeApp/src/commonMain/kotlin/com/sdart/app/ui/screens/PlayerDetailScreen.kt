package com.sdart.app.ui.screens

import com.sdart.app.ui.theme.ditheredBackground
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
import com.sdart.app.ui.AdaptiveContainer
import com.sdart.app.ui.ScreenHeader
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

/**
 * Detail-Seite für Spieler-Statistiken: auf großen Fenstern als Kachel-Layout in der Desktop-Hülle,
 * sonst als einspaltige Liste.
 */
@Composable
fun PlayerDetailScreen(
    player: SavedPlayer,
    isDark: Boolean = true,
    onBack: () -> Unit
) {
    AdaptiveContainer { windowInfo ->
        if (windowInfo.isExpanded) {
            DesktopPlayerDetail(player, onBack)
        } else {
            MobilePlayerDetail(player, isDark, onBack)
        }
    }
}

@Composable
private fun MobilePlayerDetail(
    player: SavedPlayer,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val backgroundModifier = Modifier.fillMaxSize().ditheredBackground()

    Box(
        modifier = backgroundModifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            ScreenHeader(
                title = player.name,
                onBack = onBack
            )
            
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (player.totalGames == 0) {
                    // Keine Statistiken vorhanden
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) com.sdart.app.ui.theme.GlassWhite else Color(0xFFf8f9fa)
                        ),
                        border = if (isDark) BorderStroke(1.dp, com.sdart.app.ui.theme.GlassBorder) else null
                    ) {
                        Text(
                            text = "Noch keine Spiele gespielt.\nStatistiken werden nach dem ersten Spiel angezeigt.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                            color = if (isDark) com.sdart.app.ui.theme.DarkTextSecondary else Color(0xFF6c757d)
                        )
                    }
                } else {
                    // --- Hit Heatmap ---
                    if (player.savedDartPositions.isNotEmpty()) {
                        StatsCard(isDark) {
                            SectionTitle("Hit Heatmap", isDark)
                            val offsets = player.savedDartPositions.map { Offset(it.x, it.y) }
                            HeatmapDartboard(
                                hits = offsets,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(vertical = 8.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Gesamt-Statistiken KPIs berechnen
                    val avgScore = if (player.totalDartsThrown > 0) {
                        String.format("%.0f", player.totalPoints.toFloat() / (player.totalDartsThrown / 3f).coerceAtLeast(1f))
                    } else String.format("%.0f", 0f)
                    
                    val winRate = if (player.totalGames > 0) {
                        ((player.gamesWon.toFloat() / player.totalGames) * 100).toInt().toString()
                    } else "0"
                    
                    val checkoutQuote = if (player.checkoutAttempts > 0) {
                        String.format("%.0f", (player.checkoutHits.toFloat() / player.checkoutAttempts) * 100)
                    } else String.format("%.0f", 0f)
                    
                    val first9Avg = if (player.first9Darts > 0) {
                        String.format("%.0f", player.first9Points.toFloat() / (player.first9Darts / 3f).coerceAtLeast(1f))
                    } else "-"
                    
                    val bestDoubleRaw = player.doubleHits.maxByOrNull { it.value }?.key?.toString() ?: "-"
                    val bestDouble = if (bestDoubleRaw != "-") "D$bestDoubleRaw" else "-"
                    
                    // KPI Grid (3 Spalten)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiBox("Spiele", player.totalGames.toString(), isDark, Modifier.weight(1f))
                        KpiBox("Avg. Score", avgScore, isDark, Modifier.weight(1f))
                        KpiBox("Checkout %", "$checkoutQuote%", isDark, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiBox("Siege", "$winRate%", isDark, Modifier.weight(1f))
                        KpiBox("First 9 Avg", first9Avg, isDark, Modifier.weight(1f))
                        KpiBox("Best Double", bestDouble, isDark, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiBox("180s", player.highScores180s.toString(), isDark, Modifier.weight(1f))
                        KpiBox("140+", player.highScores140s.toString(), isDark, Modifier.weight(1f))
                        KpiBox("Total Darts", player.totalDartsThrown.toString(), isDark, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiBox("Total Busts", player.totalBusts.toString(), isDark, Modifier.weight(1f))
                        KpiBox("Max Checkout", if (player.bestCheckout > 0) player.bestCheckout.toString() else "-", isDark, Modifier.weight(1f))
                        KpiBox("Best Match\n(Darts)", if (player.bestMatchDarts > 0) player.bestMatchDarts.toString() else "-", isDark, Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // ===== CHARTS SECTION =====
                    
                    // --- Win / Loss ---
                    StatsCard(isDark) {
                        SectionTitle("Win / Loss", isDark)
                        WinLossDonutChart(player.gamesWon, player.totalGames - player.gamesWon, isDark)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // --- High Scores ---
                    StatsCard(isDark) {
                        SectionTitle("High Scores", isDark)
                        HighScoresBarChart(player.highScores100s, player.highScores140s, player.highScores180s, isDark,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // --- Favorite Checkout ---
                    StatsCard(isDark) {
                        SectionTitle("Favorite Checkout", isDark)
                        CheckoutPyramidChart(
                            doubleHits = player.doubleHits,
                            isDark = isDark,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // --- Average Score (Trend) ---
                    StatsCard(isDark) {
                        SectionTitle("Average Score", isDark)
                        HistoryLineChart(
                            history = player.gameHistory,
                            isDark = isDark,
                            title = "",
                            valueSelector = { it.averageScore },
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // --- Darts Pro Leg (Trend) ---
                    StatsCard(isDark) {
                        SectionTitle("Darts Pro Leg (Trend)", isDark)
                        HistoryLineChart(
                            history = player.gameHistory,
                            isDark = isDark,
                            title = "",
                            valueSelector = { it.dartsPerLeg },
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // --- Trefferverteilung ---
                    if (player.segmentHits.isNotEmpty()) {
                        StatsCard(isDark) {
                            SectionTitle("Trefferverteilung", isDark)
                            HitDistributionChart(
                                segmentHits = player.segmentHits,
                                isDark = isDark,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun KpiBox(label: String, value: String, isDark: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) com.sdart.app.ui.theme.GlassWhite else Color(0xFFf8f9fa)
        ),
        border = if (isDark) BorderStroke(1.dp, com.sdart.app.ui.theme.GlassBorder) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label.uppercase(),
                    color = if (isDark) Color(0xFFAAAAAA) else Color.DarkGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = if (isDark) androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        ) else null
                    )
                )
            }
        }
    }
}

/**
 * Wiederverwendbare Stats-Card mit Glassmorphism-Stil.
 * Entspricht den stats-card Containern in der Windows-App.
 */
@Composable
private fun StatsCard(isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) com.sdart.app.ui.theme.GlassWhite else Color(0xFFf8f9fa)
        ),
        border = if (isDark) BorderStroke(1.dp, com.sdart.app.ui.theme.GlassBorder) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Sektions-Überschrift im Stil der Windows-App (<h3> innerhalb einer stats-card).
 */
@Composable
private fun SectionTitle(title: String, isDark: Boolean) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isDark) Color.White else Color(0xFF212529),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * Desktop-Layout im Dashboard-Stil: Kennzahlen als Kachelreihen, darunter die bekannten Diagramme
 * in zwei Spalten.
 */
@Composable
private fun DesktopPlayerDetail(player: SavedPlayer, onBack: () -> Unit) {
    DesktopPage(title = player.name, fillHeight = false) {
        CardLink(text = "Spieler", onClick = onBack, primary = false, modifier = Modifier.padding(bottom = 4.dp))
        DesktopPlayerStats(player)
    }
}

/**
 * Statistik eines Spielers für breite Fenster; wird auch neben der Spielerauswahl angezeigt.
 */
@Composable
internal fun DesktopPlayerStats(player: SavedPlayer, modifier: Modifier = Modifier) {
    val isDark = true
    BoxWithConstraints(modifier = modifier) {
    val availableWidth = maxWidth
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (player.totalGames == 0) {
            DashboardCard(title = null, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Noch keine Spiele gespielt.\nStatistiken werden nach dem ersten Spiel angezeigt.",
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
            "Spiele" to player.totalGames.toString(),
            "Avg. Score" to avgScore,
            "First 9 Avg" to first9Avg,
            "Checkout %" to "$checkoutQuote%",
            "Siege" to "$winRate%",
            "Best Double" to bestDouble,
            "180s" to player.highScores180s.toString(),
            "140+" to player.highScores140s.toString(),
            "Total Darts" to player.totalDartsThrown.toString(),
            "Total Busts" to player.totalBusts.toString(),
            "Max Checkout" to (if (player.bestCheckout > 0) player.bestCheckout.toString() else "-"),
            "Best Match" to (if (player.bestMatchDarts > 0) "${player.bestMatchDarts} Darts" else "-")
        )
        // Spaltenzahl nach verfügbarer Breite, damit Beschriftungen und Werte nicht abgeschnitten werden
        val kpiColumns = when {
            availableWidth >= 1000.dp -> 6
            availableWidth >= 560.dp -> 4
            else -> 3
        }
        kpis.chunked(kpiColumns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
            }
        }

        val winLoss: @Composable (Modifier) -> Unit = { m ->
            DashboardCard(title = "Win / Loss", modifier = m) {
                WinLossDonutChart(player.gamesWon, player.totalGames - player.gamesWon, isDark)
            }
        }
        val highScores: @Composable (Modifier) -> Unit = { m ->
            DashboardCard(title = "High Scores", modifier = m) {
                HighScoresBarChart(player.highScores100s, player.highScores140s, player.highScores180s, isDark)
            }
        }

        val charts = buildList<@Composable (Modifier) -> Unit> {
            if (player.savedDartPositions.isNotEmpty()) {
                add { m ->
                    DashboardCard(title = "Hit Heatmap", modifier = m) {
                        HeatmapDartboard(
                            hits = player.savedDartPositions.map { Offset(it.x, it.y) },
                            modifier = Modifier.fillMaxWidth().height(320.dp)
                        )
                    }
                }
                // Neben der Heatmap stehen Win/Loss und High Scores übereinander
                add { m ->
                    Column(modifier = m, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        winLoss(Modifier.fillMaxWidth())
                        highScores(Modifier.fillMaxWidth())
                    }
                }
            } else {
                add(winLoss)
                add(highScores)
            }
            add { m ->
                DashboardCard(title = "Average Score", modifier = m) {
                    HistoryLineChart(
                        history = player.gameHistory,
                        isDark = isDark,
                        title = "",
                        valueSelector = { it.averageScore }
                    )
                }
            }
            add { m ->
                DashboardCard(title = "Darts Pro Leg (Trend)", modifier = m) {
                    HistoryLineChart(
                        history = player.gameHistory,
                        isDark = isDark,
                        title = "",
                        valueSelector = { it.dartsPerLeg }
                    )
                }
            }
            add { m ->
                DashboardCard(title = "Favorite Checkout", modifier = m) {
                    CheckoutPyramidChart(doubleHits = player.doubleHits, isDark = isDark)
                }
            }
            if (player.segmentHits.isNotEmpty()) {
                add { m ->
                    DashboardCard(title = "Trefferverteilung", modifier = m) {
                        HitDistributionChart(segmentHits = player.segmentHits, isDark = isDark)
                    }
                }
            }
        }

        // Diagramme paarweise nebeneinander; bei schmalem Platz untereinander
        val chartColumns = if (availableWidth >= 600.dp) 2 else 1
        charts.chunked(chartColumns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { chart -> chart(Modifier.weight(1f)) }
                repeat(chartColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    }
}
