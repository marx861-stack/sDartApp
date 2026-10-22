package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.AdaptiveContainer
import com.example.myapplication.ui.ScreenHeader
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun GameModesScreen(
    onGameModeSelected: (GameMode) -> Unit,
    onBack: () -> Unit
) {
    val backgroundModifier = Modifier.fillMaxSize().background(
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A1A1A),
                Color(0xFF2A2A2A),
                Color(0xFF1F1F1F)
            )
        )
    )

    AdaptiveContainer(modifier = backgroundModifier) { windowInfo ->
        val isWidescreen = windowInfo.isWidescreen

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Einheitlicher Screen-Header (exakt gleiche Position wie in PlayerSetupScreen etc.)
            ScreenHeader(
                title = stringResource(Res.string.main_title_games),
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .widthIn(max = windowInfo.maxContentWidth)
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isWidescreen) {
                    // 2-SPALTEN GRID (WIDESCREEN / DESKTOP)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GameModeCard(
                            iconRes = Res.drawable.ic_standard,
                            title = stringResource(Res.string.game_mode_standard),
                            description = stringResource(Res.string.game_mode_standard_desc),
                            onClick = { onGameModeSelected(GameMode.X501) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        GameModeCard(
                            iconRes = Res.drawable.ic_cricket,
                            title = stringResource(Res.string.game_mode_cricket),
                            description = stringResource(Res.string.game_mode_cricket_desc),
                            onClick = { onGameModeSelected(GameMode.CRICKET) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GameModeCard(
                            iconRes = Res.drawable.ic_all_fives,
                            title = stringResource(Res.string.game_mode_all_fives),
                            description = stringResource(Res.string.game_mode_all_fives_desc),
                            onClick = { onGameModeSelected(GameMode.ALL_FIVES) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        GameModeCard(
                            iconRes = Res.drawable.ic_atc,
                            title = stringResource(Res.string.game_mode_atc),
                            description = stringResource(Res.string.game_mode_atc_desc),
                            onClick = { onGameModeSelected(GameMode.AROUND_THE_CLOCK) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                } else {
                    // 1-SPALTEN LISTE (MOBILE PORTRAIT)
                    GameModeCard(
                        iconRes = Res.drawable.ic_standard,
                        title = stringResource(Res.string.game_mode_standard),
                        description = stringResource(Res.string.game_mode_standard_desc),
                        onClick = { onGameModeSelected(GameMode.X501) }
                    )
                    GameModeCard(
                        iconRes = Res.drawable.ic_cricket,
                        title = stringResource(Res.string.game_mode_cricket),
                        description = stringResource(Res.string.game_mode_cricket_desc),
                        onClick = { onGameModeSelected(GameMode.CRICKET) }
                    )
                    GameModeCard(
                        iconRes = Res.drawable.ic_all_fives,
                        title = stringResource(Res.string.game_mode_all_fives),
                        description = stringResource(Res.string.game_mode_all_fives_desc),
                        onClick = { onGameModeSelected(GameMode.ALL_FIVES) }
                    )
                    GameModeCard(
                        iconRes = Res.drawable.ic_atc,
                        title = stringResource(Res.string.game_mode_atc),
                        description = stringResource(Res.string.game_mode_atc_desc),
                        onClick = { onGameModeSelected(GameMode.AROUND_THE_CLOCK) }
                    )
                }
            }
        }
    }
}

@Composable
fun GameModeCard(
    iconRes: DrawableResource,
    title: String,
    description: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 12.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
