package com.sdart.app.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*
import com.sdart.app.ui.theme.GlassBorder
import com.sdart.app.ui.theme.GlassWhite
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.domain.player.PlayerStatistics
import com.sdart.app.ui.HeatmapDartboard
import com.sdart.app.ui.theme.PrimaryOrange

@Composable
fun PlayerStatsDialog(
    player: Player,
    isCurrentPlayer: Boolean,
    isWinner: Boolean,
    gameMode: GameMode? = null,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Statistiken, 1 = Heatmap
    val hitPositions = remember(player.dartThrows) {
        player.dartThrows.map { it.position }.filter { it != Offset.Unspecified }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color(0xFF2D3748),
                    textAlign = TextAlign.Center
                )
                if (isWinner) {
                    Text(
                        text = "Gewinner! 🎉",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF28a745),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab-Auswahl (Statistiken / Heatmap) direkt im gleichen Fenster
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) PrimaryOrange else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Statistiken",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else (if (isDarkTheme) Color.LightGray else Color.DarkGray),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) PrimaryOrange else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Heatmap",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else (if (isDarkTheme) Color.LightGray else Color.DarkGray),
                            fontSize = 13.sp
                        )
                    }
                }

                if (selectedTab == 0) {
                    MainStatCard(
                        title = "Aktueller Score",
                        value = if (isWinner) "0" else player.currentScore.toString(),
                        subtitle = if (isWinner) "Gewonnen!" else "Punkte übrig",
                        color = if (isWinner) Color(0xFF28a745) else PrimaryOrange,
                        icon = if (isWinner) Res.drawable.ic_trophy else Res.drawable.ic_target,
                        isDarkTheme = isDarkTheme,
                        hits = hitPositions,
                        onHeatmapClick = { selectedTab = 1 }
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                        CompactStatCard(
                            title = "Ø Punkte/Runde",
                            value = "$avgScore",
                            color = if (isDarkTheme) Color.White else Color(0xFF495057),
                            icon = Res.drawable.ic_chart_bar,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                        val wonLegs = PlayerStatistics.getWonLegs(player, gameMode)
                        CompactStatCard(
                            title = "Gewonnene Legs",
                            value = "$wonLegs",
                            color = Color(0xFFffc107),
                            icon = Res.drawable.ic_trophy,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val highestRoundScore = PlayerStatistics.getHighestRoundScore(player)
                        CompactStatCard(
                            title = "Höchste Runde",
                            value = "$highestRoundScore",
                            color = Color(0xFF17a2b8),
                            icon = Res.drawable.ic_target,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                        val highestSingleThrow = if (player.scores.isNotEmpty()) {
                            when {
                                highestRoundScore >= 180 -> 60
                                highestRoundScore >= 140 -> 50
                                highestRoundScore >= 100 -> 40
                                highestRoundScore >= 60 -> 20
                                else -> highestRoundScore / 3
                            }
                        } else 0
                        CompactStatCard(
                            title = "Höchster Wurf",
                            value = "$highestSingleThrow",
                            color = Color(0xFF6f42c1),
                            icon = Res.drawable.ic_lightning,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    val totalRounds = PlayerStatistics.getTotalRounds(player)
                    val totalThrows = player.dartThrows.size
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactStatCard(
                            title = "Anzahl Runden",
                            value = "$totalRounds",
                            color = Color(0xFFfd7e14),
                            icon = Res.drawable.ic_refresh,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                        CompactStatCard(
                            title = "Anzahl Würfe",
                            value = "$totalThrows",
                            color = Color(0xFF20c997),
                            icon = Res.drawable.ic_target,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Große Heatmap-Anzeige direkt im gleichen Fenster
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        if (hitPositions.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 300.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(
                                        if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color(0xFFF0F0F0),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                HeatmapDartboard(
                                    hits = hitPositions,
                                    modifier = Modifier.fillMaxSize(),
                                    showNumbers = true
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${hitPositions.size} Treffer (${player.dartThrows.size} Darts geworfen)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color.LightGray else Color(0xFF6c757d)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Noch keine Würfe für ${player.name} vorhanden",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDarkTheme) Color.LightGray else Color(0xFF6c757d),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Schließen", fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp), color = Color.White)
            }
        },
        containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(16.dp).widthIn(min = 280.dp, max = 460.dp)
    )
}

@Composable
fun MainStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: DrawableResource,
    isDarkTheme: Boolean,
    hits: List<Offset>? = null,
    onHeatmapClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) GlassWhite else Color(0xFFf8f9fa)
        ),
        border = if (isDarkTheme) BorderStroke(1.dp, GlassBorder) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title.uppercase(),
                        color = if (isDarkTheme) Color(0xFFAAAAAA) else Color.DarkGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        color = color,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = if (isDarkTheme) androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            ) else null
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        color = if (isDarkTheme) Color.LightGray else Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!hits.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clickable { onHeatmapClick() }
                            .padding(start = 8.dp)
                    ) {
                        HeatmapDartboard(
                            hits = hits,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).padding(start = 8.dp),
                        tint = color.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStatCard(
    title: String,
    value: String,
    color: Color,
    icon: DrawableResource,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) GlassWhite else Color(0xFFf8f9fa)
        ),
        border = if (isDarkTheme) BorderStroke(1.dp, GlassBorder) else null
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
                    text = title.uppercase(),
                    color = if (isDarkTheme) Color(0xFFAAAAAA) else Color.DarkGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = if (isDarkTheme) androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        ) else null
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PlayerScoreRow(player: Player, isCurrentPlayer: Boolean, isWinner: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = when {
        isWinner -> Color(0xFFd4edda)
        isCurrentPlayer -> Color(0xFFfff8e1)
        else -> Color.White
    }
    val borderColor = when {
        isWinner -> Color(0xFF28a745)
        isCurrentPlayer -> Color(0xFFffd700)
        else -> Color(0xFFdee2e6)
    }
    val borderWidth = if (isCurrentPlayer || isWinner) 2.dp else 1.dp
    Card(
        modifier = modifier.fillMaxWidth().border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrentPlayer) Text("▶️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                else if (isWinner) Text("🏆", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                else Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Medium,
                        color = Color(0xFF495057)
                    )
                )
            }
            Text(
                text = if (isWinner) "Gewonnen" else player.currentScore.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWinner) Color(0xFF28a745) else PrimaryOrange
                )
            )
        }
    }
}

