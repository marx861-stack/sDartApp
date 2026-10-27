package com.sdart.app.ui.screens

import com.sdart.app.ui.theme.ditheredBackground
import com.sdart.app.*

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.theme.DarkBgGradient
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.Res
import sdartapp.composeapp.generated.resources.ic_main_logo
import sdartapp.composeapp.generated.resources.dash_app_logo
import org.jetbrains.compose.resources.stringResource

/**
 * Premium startup animation screen for S'Dart App (KMP).
 * It displays the main logo and application title in the center in a static embossed style,
 * and then fades out after a short duration to reveal the start screen.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    // Animate overall opacity for a smooth fade out transition
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        finishedListener = {
            onTimeout()
        },
        label = "splashFade"
    )

    LaunchedEffect(Unit) {
        delay(1800) // Show the splash screen animation for 1.8 seconds
        isVisible = false // Trigger fade out
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .ditheredBackground()
            .graphicsLayer { this.alpha = alpha }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // --- EMBOSSED LOOK LAYERS (Static size 160dp) ---

                // Light Highlight (Bottom Right offset)
                Icon(
                    painter = painterResource(Res.drawable.ic_main_logo),
                    contentDescription = null,
                    tint = Color(0x26FFFFFF), // 15% White
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = 0.8.dp, y = 0.8.dp)
                )

                // Dark Shadow (Top Left offset)
                Icon(
                    painter = painterResource(Res.drawable.ic_main_logo),
                    contentDescription = null,
                    tint = Color(0xCC000000), // 80% Black
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = (-0.8).dp, y = (-0.8).dp)
                        .graphicsLayer {
                            this.alpha = 0.8f
                        }
                )

                // Main Logo (Matches Dark Background for embossed/debossed effect)
                Icon(
                    painter = painterResource(Res.drawable.ic_main_logo),
                    contentDescription = stringResource(Res.string.dash_app_logo),
                    tint = Color(0xFF1A1A1A), // Matches DarkBgStart color
                    modifier = Modifier
                        .size(160.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Debossed Main Headline "S'Dart App"
            Box(contentAlignment = Alignment.Center) {
                // Light Highlight (Bottom Right)
                Text(
                    text = "S'Dart App",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        letterSpacing = 2.sp,
                        color = Color(0x26FFFFFF)
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = 0.5.dp, y = 0.5.dp)
                )
                // Dark Shadow (Top Left)
                Text(
                    text = "S'Dart App",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        letterSpacing = 2.sp,
                        color = Color(0xCC000000)
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = (-0.5).dp, y = (-0.5).dp)
                )
                // Main Text (Matches Background for Embossed Effect)
                Text(
                    text = "S'Dart App",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        letterSpacing = 2.sp,
                        color = Color(0xFF1A1A1A) // Matches DarkBgStart color
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
