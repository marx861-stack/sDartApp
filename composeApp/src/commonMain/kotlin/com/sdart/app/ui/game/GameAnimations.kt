package com.sdart.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun RoundCompletionAnimation(
    isVisible: Boolean,
    roundSum: Int,
    playerName: String,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    if (isVisible && animationProgress > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f * animationProgress))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f * animationProgress))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10000f)
                    .graphicsLayer { alpha = animationProgress },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.8f + (0.2f * animationProgress)
                            scaleY = 0.8f + (0.2f * animationProgress)
                        }
                        .padding(horizontal = 40.dp, vertical = 40.dp)
                ) {
                    val digitCount = roundSum.toString().length
                    val fontSize = when {
                        digitCount <= 2 -> 180.sp
                        digitCount == 3 -> 140.sp
                        else -> 100.sp
                    }
                    Text(
                        text = "$roundSum",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(Res.string.game_play_points),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
        }
    }
}

@Composable
fun AllFivesRoundAnimation(
    isVisible: Boolean,
    roundSum: Int,
    isValid: Boolean,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    if (isVisible && animationProgress > 0f) {
        val color = if (isValid) Color(0xFF28a745) else Color(0xFFdc3545)
        Box(modifier = modifier.fillMaxSize().zIndex(9999f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * animationProgress))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10000f)
                    .graphicsLayer { alpha = animationProgress },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.8f + (0.2f * animationProgress)
                            scaleY = 0.8f + (0.2f * animationProgress)
                        }
                        .padding(horizontal = 40.dp, vertical = 40.dp)
                ) {
                    Text(
                        text = "$roundSum",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 120.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
