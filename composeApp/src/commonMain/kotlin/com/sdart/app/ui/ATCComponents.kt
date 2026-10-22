package com.sdart.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.sdart.app.Player
import com.sdart.app.ATCGameState
import com.sdart.app.ATCVariant
import androidx.compose.ui.draw.clip

@Composable
fun ATCScoreTable(
    players: List<Player>,
    currentPlayerIndex: Int,
    atcConfig: ATCGameState?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Spieler-Liste
        players.forEachIndexed { index, player ->
            val isCurrentPlayer = index == currentPlayerIndex
            val isFinished = player.atcCurrentTarget == 0
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = if (isCurrentPlayer) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isFinished) {
                            Text(
                                text = "FERTIG!",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Ziel: ${getTargetText(player, atcConfig)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Progress Anzeige
                    if (!isFinished) {
                        ATCProgressBar(
                            player = player,
                            reverse = atcConfig?.reverse == true,
                             withBulls = atcConfig?.withBulls == true,
                             modifier = Modifier.width(100.dp)
                        )
                    } else {
                         Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                            contentDescription = "Finished",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ATCMainDisplay(
    player: Player,
    atcConfig: ATCGameState?,
    modifier: Modifier = Modifier
) {
    if (player.atcCurrentTarget == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
             Text(
                text = "FERTIG!",
                style = MaterialTheme.typography.displayMedium,
                color = Color.Green
            )
        }
        return
    }

    val isShanghai = atcConfig?.variant == ATCVariant.SHANGHAI
    val targetText = getTargetText(player, atcConfig)
    val fontSize = if (isShanghai) {
        if (targetText.length > 5) 20.sp else 28.sp
    } else {
        40.sp
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Großes Target
        Box(
            modifier = Modifier
                .height(90.dp)
                .then(
                    if (isShanghai) Modifier.wrapContentWidth().padding(horizontal = 16.dp)
                    else Modifier.width(90.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = if (isShanghai) RoundedCornerShape(45.dp) else CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = targetText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ATCProgressBar(
    player: Player,
    reverse: Boolean,
    withBulls: Boolean,
    modifier: Modifier = Modifier
) {
    // Berechne Progress
    // Standard: 1..20 (20 steps). With Bull: 21 steps.
    val maxSteps = if (withBulls) 21f else 20f
    
    // Aktueller Schritt berechnen
    // 1 -> 0 progress.
    val currentStep = if (reverse) {
         if (withBulls) {
             if (player.atcCurrentTarget == 25) 0f
             else (21 - player.atcCurrentTarget).toFloat()
         } else {
             (20 - player.atcCurrentTarget).toFloat()
         }
    } else {
         // 1 -> Start (0/20).
         // Target 25: 20/21.
         if (player.atcCurrentTarget == 25) {
             20f
         } else {
             (player.atcCurrentTarget - 1).toFloat()
         }
    }
    
    val progress = (currentStep / maxSteps).coerceIn(0f, 1f)
    
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier.height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun ShanghaiIndicator(label: String, active: Boolean, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                if (active) Color(0xFF4CAF50) else Color.LightGray,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.White else Color.Black,
            fontSize = (size.value * 0.5).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getTargetText(player: Player, atcConfig: ATCGameState? = null): String {
    val target = player.atcCurrentTarget
    if (target == 0) return "FERTIG!"
    
    if (target == 25) {
        return when (atcConfig?.variant) {
            ATCVariant.DOUBLE -> "DB"
            ATCVariant.TRIPLE -> "B"
            ATCVariant.SHANGHAI -> {
                val hits = player.atcShanghaiHits
                when {
                    hits.isEmpty() -> "B"
                    hits.contains(1) && !hits.contains(2) -> "DB"
                    else -> "B"
                }
            }
            else -> "B"
        }
    }
    
    return when (atcConfig?.variant) {
        ATCVariant.DOUBLE -> "D$target"
        ATCVariant.TRIPLE -> "T$target"
        ATCVariant.SHANGHAI -> {
            val hits = player.atcShanghaiHits
            when {
                hits.isEmpty() -> "$target"
                hits.contains(1) && !hits.contains(2) -> "D$target"
                hits.contains(1) && hits.contains(2) -> "T$target"
                else -> "$target"
            }
        }
        else -> target.toString()
    }
}
