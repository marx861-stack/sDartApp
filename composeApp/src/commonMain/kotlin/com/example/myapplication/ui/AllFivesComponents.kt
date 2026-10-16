package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.example.myapplication.Player
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close

@Composable
fun AllFivesScoreTable(
    players: List<Player>,
    currentPlayerIndex: Int,
    targetScore: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        players.forEachIndexed { index, player ->
            val isCurrentPlayer = index == currentPlayerIndex
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentPlayer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = if (isCurrentPlayer) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                        Text(
                             text = "Legs: ${player.wonLegs}",
                             style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Score
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${player.currentScore / 5} / ${targetScore / 5}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllFivesMainDisplay(
    currentRoundTotal: Int,
    missed: Boolean,
    modifier: Modifier = Modifier
) {
    val isDivisible = currentRoundTotal > 0 && currentRoundTotal % 5 == 0 && !missed
    val displayText = currentRoundTotal.toString()
    
    val containerColor = if (missed) {
        MaterialTheme.colorScheme.errorContainer
    } else if (isDivisible) {
        Color(0xFF4CAF50) // Green for divisible
    } else {
        MaterialTheme.colorScheme.surfaceVariant // Standard neutral
    }
    
    val contentColor = if (missed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else if (isDivisible) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.size(width = 120.dp, height = 72.dp), // Feste Größe wie im Standard-Modus
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
