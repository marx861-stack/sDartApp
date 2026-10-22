package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.PrimaryOrange
import com.sdart.app.ui.HeatmapDartboard
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

/**
 * Screen zur Anzeige der Spielstatistiken nach einem beendeten Spiel
 */
@Composable
fun GameSummaryScreen(
    vm: DartViewModel,
    onBack: () -> Unit
) {
    val game = vm.game
    val isDark = true

    val backgroundModifier = Modifier.fillMaxSize().background(DarkBgGradient)

    Scaffold(
        containerColor = Color.Transparent,
        modifier = backgroundModifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val isTournament = vm.activeTournamentMatchId != null
                Button(
                    onClick = {
                        if (isTournament) {
                            vm.finishTournamentMatch()
                        }
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isTournament) "Ergebnis übernehmen & zum Turnier" else "Zurück zum Hauptmenü",
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spiel beendet",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Spielmodus: ${game?.mode?.name ?: "Unbekannt"}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            game?.let { currentGame ->
                val finishedPlayers = remember(currentGame) {
                    if (currentGame.mode == GameMode.ALL_FIVES) {
                        currentGame.players.sortedByDescending { it.wonLegs }
                    } else {
                        currentGame.players.filter { player ->
                            when (currentGame.mode) {
                                GameMode.CRICKET -> player.hasAllSegmentsClosed()
                                GameMode.AROUND_THE_CLOCK -> player.atcCurrentTarget == 0
                                else -> player.currentScore == 0
                            }
                        }.sortedBy { player ->
                            player.dartThrows.lastOrNull()?.timestamp ?: 0L
                        }
                    }
                }

                // Statistiken für jeden Spieler
                currentGame.players.forEach { player ->
                    val finishIndex = finishedPlayers.indexOf(player)
                    val trophyColor = when (finishIndex) {
                        0 -> Color(0xFFFFD700) // Gold
                        1 -> Color(0xFFC0C0C0) // Silber
                        2 -> Color(0xFFCD7F32) // Bronze
                        else -> null
                    }
                    PlayerSummaryCard(player, currentGame, isDark, trophyColor, currentGame.atcConfig)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}



@Composable
fun PlayerSummaryCard(player: Player, game: Game, isDark: Boolean, trophyColor: Color?, atcConfig: ATCGameState? = null) {
    val stats = PlayerStatistics.getAllStatistics(player, game)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF262626).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF2d3748)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            
            // Heatmap anzeigen
            val hitPositions = player.dartThrows.map { it.position }.filter { it != androidx.compose.ui.geometry.Offset.Unspecified }
            if (hitPositions.isNotEmpty()) {
                HeatmapDartboard(
                    hits = hitPositions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(bottom = 16.dp)
                )
            }
            
            val totalThrows = player.scores.size
            // Dividiert durch die tatsächliche Anzahl geworfener Darts, nicht totalRounds * 3
            val avgPerThrow = if (totalThrows > 0) String.format("%.1f", stats.totalPointsFromRounds.toFloat() / totalThrows) else "0"
            
            // Mode-spezifische Statistiken für höhere Relevanz
            val mode = game.mode
            when (mode) {
                GameMode.CRICKET -> {
                    val mpr = PlayerStatistics.getCricketMPR(player)
                    val closed = PlayerStatistics.getCricketClosedCount(player)
                    val totalMarks = PlayerStatistics.getCricketTotalMarks(player)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("MPR", String.format("%.2f", mpr), isDark)
                        StatItem("Geschlossen", "$closed/7", isDark)
                        StatItem("Legs", "${stats.wonLegs}", isDark)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Total Marks", "$totalMarks", isDark)
                        StatItem("Darts", "$totalThrows", isDark)
                        StatItem("Runden", "${stats.totalRounds}", isDark)
                    }
                }
                GameMode.AROUND_THE_CLOCK -> {
                    val hpr = PlayerStatistics.getAverageHitsPerRound(player, atcConfig)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Hits/Runde", String.format("%.2f", hpr), isDark)
                        StatItem("Präzision", String.format("%.1f%%", (hpr / 3f) * 100), isDark)
                        StatItem("Legs", "${stats.wonLegs}", isDark)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Total Darts", "$totalThrows", isDark)
                        StatItem("Runden", "${stats.totalRounds}", isDark)
                        StatItem("Best Rnd", "${stats.highestRoundScore}", isDark)
                    }
                }
                else -> {
                    // Standard X01 und All Fives
                    val isAllFives = mode == GameMode.ALL_FIVES
                    val displayAvg = if (isAllFives) (stats.averageScorePerRound / 5) else stats.averageScorePerRound
                    val displayBest = if (isAllFives) (stats.highestRoundScore / 5) else stats.highestRoundScore
                    val displayTotal = if (isAllFives) (stats.totalPointsFromRounds / 5) else stats.totalPointsFromRounds
                    val displayAvgPerThrow = if (isAllFives) {
                        if (totalThrows > 0) String.format("%.1f", (stats.totalPointsFromRounds.toFloat() / 5) / totalThrows) else "0"
                    } else avgPerThrow

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Ø/Runde", "$displayAvg", isDark)
                        StatItem("Best Rnd", "$displayBest", isDark)
                        StatItem("Legs", "${stats.wonLegs}", isDark)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Total Pts", "$displayTotal", isDark)
                        StatItem("Runden", "${stats.totalRounds}", isDark)
                        StatItem("Ø/Pfeil", displayAvgPerThrow, isDark)
                    }
                }
            }
        }
        
        if (trophyColor != null) {
            Icon(
                painter = painterResource(Res.drawable.ic_trophy),
                contentDescription = "Platzierung",
                tint = trophyColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(24.dp)
            )
        }
    }
}
}

@Composable
fun RowScope.StatItem(label: String, value: String, isDark: Boolean) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isDark) PrimaryOrange else Color(0xFFE67E00)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Color.LightGray else Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}



