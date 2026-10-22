package com.sdart.app.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.DartViewModel
import com.sdart.app.Game
import com.sdart.app.GameMode
import com.sdart.app.Player
import com.sdart.app.PlayerStatistics
import com.sdart.app.format
import com.sdart.app.domain.CHECKOUT_PATHS

@Composable
fun GameHeader(
    game: Game, 
    currentPlayer: Player?, 
    currentPlayerIndex: Int, 
    vm: DartViewModel,
    onPlayerClick: (Player) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxWidth().padding(16.dp)
) {
    val isDarkTheme = true

    if (game.mode == GameMode.CRICKET) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF262626) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (game.totalLegs > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Leg ${game.currentLeg}/${game.totalLegs}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (game.players.size == 1) "Legs: ${game.totalLegs}" else "Best of ${game.totalLegs}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 1.dp
                )
            }
            
            key(game.players.map { it.name }.joinToString()) {
                CricketScoreTable(
                    players = game.players,
                    currentPlayerIndex = currentPlayerIndex,
                    totalLegs = game.totalLegs,
                    modifier = Modifier.fillMaxWidth(),
                    isDarkTheme = isDarkTheme
                )
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF262626) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                currentPlayer?.let { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            
                            val checkoutInfo = if (game.mode == GameMode.ALL_FIVES || game.mode == GameMode.AROUND_THE_CLOCK) null else when {
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
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = when {
                                            player.currentScore == 0 -> Color(0xFF4CAF50)
                                            player.currentScore in CHECKOUT_PATHS.keys -> Color(0xFF448AFF)
                                            player.currentScore <= 40 -> Color(0xFF4CAF50)
                                            else -> Color.LightGray
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            
                            if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                                Text(
                                    text = "Ø ${avgHits.format(2)} Treffer pro Runde",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            } else {
                                val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                                Text(
                                    text = "Ø ${avgScore} Punkte pro Runde",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        
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
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(width = 120.dp, height = 72.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = player.currentScore.toString(),
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(game.players.size) { index ->
                        val player = game.players[index]
                        val isCurrentPlayer = index == currentPlayerIndex
                        val isWinner = when (game.mode) {
                            GameMode.AROUND_THE_CLOCK -> player.atcCurrentTarget == 0
                            GameMode.ALL_FIVES -> false
                            else -> player.currentScore == 0
                        }
                        
                        val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                        val wonLegs = PlayerStatistics.getWonLegs(player, game.mode)
                        
                        Card(
                            modifier = Modifier.clickable { onPlayerClick(player) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isWinner -> Color(0xFFd4edda)
                                    isDarkTheme -> Color(0xFF333333)
                                    else -> Color.White
                                }
                            ),
                            border = when {
                                isWinner -> BorderStroke(2.dp, Color(0xFF28a745))
                                isCurrentPlayer -> BorderStroke(2.dp, Color(0xFFFF9800))
                                else -> null
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isWinner -> Color.Black
                                            isDarkTheme -> Color.White
                                            else -> Color.Black
                                        }
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                        if (player.atcCurrentTarget == 0) "Fin" else player.atcCurrentTarget.toString()
                                    } else if (game.mode == GameMode.ALL_FIVES) {
                                        (player.currentScore / 5).toString()
                                    } else {
                                        player.currentScore.toString()
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isWinner -> Color(0xFF28a745)
                                            else -> Color.White
                                        }
                                    )
                                )
                                
                                val avgText = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                    val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                                    "Ø ${avgHits.format(1)}"
                                } else if (game.mode == GameMode.ALL_FIVES) {
                                    "Ø ${avgScore / 5}"
                                } else {
                                    "Ø $avgScore"
                                }
                                
                                Text(
                                    text = avgText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = when {
                                            isWinner -> Color(0xFF495057)
                                            isDarkTheme -> Color.LightGray
                                            else -> Color(0xFF495057)
                                        },
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                
                                if (wonLegs > 0) {
                                    Text(
                                        text = "🏆 $wonLegs",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = Color(0xFFffc107),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

