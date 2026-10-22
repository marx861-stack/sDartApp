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
import com.sdart.app.ui.ScreenHeader
import com.sdart.app.ui.HeatmapDartboard
import com.sdart.app.ui.HighScoresBarChart
import com.sdart.app.ui.WinLossDonutChart
import com.sdart.app.ui.CheckoutPyramidChart
import com.sdart.app.ui.HistoryLineChart
import com.sdart.app.ui.HitDistributionChart

/**
 * Detail-Seite für Spieler-Statistiken
 */
@Composable
fun PlayerDetailScreen(
    player: SavedPlayer,
    isDark: Boolean = true,
    onBack: () -> Unit
) {
    val backgroundModifier = Modifier.fillMaxSize().background(com.sdart.app.ui.theme.DarkBgGradient)

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
                    String.format("%.2f", player.totalPoints.toFloat() / (player.totalDartsThrown / 3f).coerceAtLeast(1f))
                } else "0.0"
                
                val winRate = if (player.totalGames > 0) {
                    ((player.gamesWon.toFloat() / player.totalGames) * 100).toInt().toString()
                } else "0"
                
                val checkoutQuote = if (player.checkoutAttempts > 0) {
                    String.format("%.1f", (player.checkoutHits.toFloat() / player.checkoutAttempts) * 100)
                } else "0.0"
                
                val first9Avg = if (player.first9Darts > 0) {
                    String.format("%.2f", player.first9Points.toFloat() / (player.first9Darts / 3f).coerceAtLeast(1f))
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
