package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.AdaptiveContainer
import com.sdart.app.ui.CenteredMenuLayout
import com.sdart.app.connect.SessionManager
import com.sdart.app.ui.components.DesktopDestination
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.PrimaryOrange
import com.sdart.app.ui.theme.ditheredBackground
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun StartScreen(
    vm: DartViewModel,
    repository: PlayerRepository,
    sessionManager: SessionManager,
    onNavigate: (DesktopDestination) -> Unit = {},
    onGamesClick: () -> Unit = {},
    onGameModeClick: (GameMode) -> Unit = {},
    onRematchClick: (LastGameSetup) -> Unit = {},
    onTournamentClick: () -> Unit = {},
    onResumeTournamentClick: () -> Unit = {},
    onPcConnect: () -> Unit = {},
    onHostOverviewClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onManagePlayersClick: () -> Unit = {}
) {
    val backgroundModifier = Modifier.fillMaxSize().ditheredBackground()

    AdaptiveContainer(modifier = backgroundModifier) { windowInfo ->
        // Große Fenster (Desktop, Tablet quer) bekommen das Dashboard mit Navigationsleiste
        if (windowInfo.isExpanded) {
            DesktopDashboard(
                vm = vm,
                repository = repository,
                sessionManager = sessionManager,
                onNavigate = onNavigate,
                onGamesClick = onGamesClick,
                onGameModeClick = onGameModeClick,
                onRematchClick = onRematchClick,
                onTournamentClick = onTournamentClick,
                onResumeTournamentClick = onResumeTournamentClick,
                onPcConnect = onPcConnect,
                onHostOverviewClick = onHostOverviewClick,
                onManagePlayersClick = onManagePlayersClick
            )
            return@AdaptiveContainer
        }

        val isWidescreen = windowInfo.isWidescreen

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
                CenteredMenuLayout {
                // Main Cards Container (skaliert zentriert und harmonisch bis 4K)
                Column(
                    modifier = Modifier
                        .widthIn(max = windowInfo.maxContentWidth)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val activeTourn = vm.tournamentManager.currentTournament
                    val isTournRunning = activeTourn != null && !activeTourn.isFinished

                    if (isWidescreen) {
                        // 2-SPALTEN GRID (WIDESCREEN / DESKTOP)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GamesCard(
                                onClick = { onGamesClick() },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            TournamentCard(
                                activeTournamentName = if (isTournRunning) activeTourn?.name else null,
                                isFinished = activeTourn?.isFinished ?: true,
                                onClick = {
                                    if (isTournRunning) onResumeTournamentClick() else onTournamentClick()
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PlayerManagementCard(
                                onClick = { onManagePlayersClick() },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            PcIntegrationCard(
                                onClick = { onPcConnect() },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    } else {
                        // MOBILE PORTRAIT: Games & Turnier volle Breite, Spieler & Geräte verbinden nebeneinander
                        GamesCard(
                            onClick = { onGamesClick() }
                        )
                        TournamentCard(
                            activeTournamentName = if (isTournRunning) activeTourn?.name else null,
                            isFinished = activeTourn?.isFinished ?: true,
                            onClick = {
                                if (isTournRunning) onResumeTournamentClick() else onTournamentClick()
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PlayerManagementCard(
                                onClick = { onManagePlayersClick() },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            PcIntegrationCard(
                                onClick = { onPcConnect() },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // Settings Button (Floating Top Right)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onSettingsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(Res.string.settings),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GamesCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
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
                painter = painterResource(Res.drawable.ic_main_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 12.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(Res.string.main_title_games),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(Res.string.main_desc_games),
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

@Composable
fun PcIntegrationCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_mobile_connect),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(Res.string.main_title_connect),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = stringResource(Res.string.main_desc_connect),
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

@Composable
fun PlayerManagementCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
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
                painter = painterResource(Res.drawable.ic_player_management),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 12.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(Res.string.main_title_players),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(Res.string.main_desc_players),
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

@Composable
fun TournamentCard(
    activeTournamentName: String?,
    isFinished: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = activeTournamentName != null && !isFinished
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) Color(0xFF2E2218) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_trophy),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 12.dp),
                tint = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isRunning) stringResource(Res.string.main_title_tournament_resume) else stringResource(Res.string.main_title_tournament),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = if (isRunning) stringResource(Res.string.main_desc_tournament_running, activeTournamentName ?: "") else stringResource(Res.string.main_desc_tournament),
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
