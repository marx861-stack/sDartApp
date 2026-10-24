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
