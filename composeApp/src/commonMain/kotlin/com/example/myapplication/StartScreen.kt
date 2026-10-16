package com.example.myapplication

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
import com.example.myapplication.ui.AdaptiveContainer
import com.example.myapplication.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun StartScreen(
    vm: DartViewModel,
    onContinue: (GameMode) -> Unit = {},
    onTournamentClick: () -> Unit = {},
    onResumeTournamentClick: () -> Unit = {},
    onPcConnect: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onManagePlayersClick: () -> Unit = {}
) {
    var selectedMode by remember { mutableStateOf<GameMode?>(null) }
    val isDark = true

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

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // --- HEADER START ---
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 30.dp)
                ) {
                    // Debossed Logo
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_main_logo),
                            contentDescription = null,
                            tint = Color(0x26FFFFFF),
                            modifier = Modifier
                                .size(100.dp)
                                .offset(x = 0.5.dp, y = 0.5.dp)
                        )
                        Icon(
                            painter = painterResource(Res.drawable.ic_main_logo),
                            contentDescription = null,
                            tint = Color(0xCC000000),
                            modifier = Modifier
                                .size(100.dp)
                                .offset(x = (-0.5).dp, y = (-0.5).dp)
                                .graphicsLayer { alpha = 0.8f }
                        )
                        Icon(
                            painter = painterResource(Res.drawable.ic_main_logo),
                            contentDescription = "Dart Game Pro Logo",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // Debossed Main Headline "DART GAME PRO"
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "DART GAME PRO",
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
                        Text(
                            text = "DART GAME PRO",
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
                        Text(
                            text = "DART GAME PRO",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                letterSpacing = 2.sp,
                                color = Color(0xFF1A1A1A)
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                // --- HEADER END ---

                // Game Mode Cards Container (mit zentrierter Maximalbreite für Widescreen)
                Column(
                    modifier = Modifier
                        .widthIn(max = if (isWidescreen) 1000.dp else Dp.Unspecified)
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
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
                                title = "Standard (501)",
                                description = "Klassisches Dart-Spiel. Starten Sie mit 501 Punkten und versuchen Sie, mit einem Double-Out zu gewinnen.",
                                isSelected = selectedMode == GameMode.X501,
                                onClick = { onContinue(GameMode.X501) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            GameModeCard(
                                iconRes = Res.drawable.ic_cricket,
                                title = "Cricket",
                                description = "Spielen Sie Cricket mit den Zahlen 15-20 und Bull. Schließen Sie alle Segmente vor Ihrem Gegner.",
                                isSelected = selectedMode == GameMode.CRICKET,
                                onClick = { onContinue(GameMode.CRICKET) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GameModeCard(
                                iconRes = Res.drawable.ic_all_fives,
                                title = "Durch 5 teilbar (All Fives)",
                                description = "Nur Runden mit durch 5 teilbaren Totals zählen. Alle 3 Pfeile müssen getroffen werden.",
                                isSelected = selectedMode == GameMode.ALL_FIVES,
                                onClick = { onContinue(GameMode.ALL_FIVES) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            GameModeCard(
                                iconRes = Res.drawable.ic_atc,
                                title = "Around the Clock",
                                description = "Treffen Sie der Reihe nach die Segmente von 1 bis 20 und optional das Bullseye.",
                                isSelected = selectedMode == GameMode.AROUND_THE_CLOCK,
                                onClick = { onContinue(GameMode.AROUND_THE_CLOCK) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        val activeTourn = vm.tournamentManager.currentTournament
                        val isTournRunning = activeTourn != null && !activeTourn.isFinished

                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TournamentCard(
                                activeTournamentName = if (isTournRunning) activeTourn?.name else null,
                                isFinished = activeTourn?.isFinished ?: true,
                                onClick = {
                                    if (isTournRunning) onResumeTournamentClick() else onTournamentClick()
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                isGrid = true
                            )
                            PlayerManagementCard(
                                onClick = { onManagePlayersClick() },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PcIntegrationCard(
                                onClick = { onPcConnect() },
                                modifier = Modifier.fillMaxWidth(),
                                isGrid = true
                            )
                        }
                    } else {
                        // 1-SPALTEN LISTE (MOBILE PORTRAIT)
                        val activeTourn = vm.tournamentManager.currentTournament
                        val isTournRunning = activeTourn != null && !activeTourn.isFinished

                        GameModeCard(
                            iconRes = Res.drawable.ic_standard,
                            title = "Standard (501)",
                            description = "Klassisches Dart-Spiel. Starten Sie mit 501 Punkten und versuchen Sie, mit einem Double-Out zu gewinnen.",
                            isSelected = selectedMode == GameMode.X501,
                            onClick = { onContinue(GameMode.X501) }
                        )
                        GameModeCard(
                            iconRes = Res.drawable.ic_cricket,
                            title = "Cricket",
                            description = "Spielen Sie Cricket mit den Zahlen 15-20 und Bull. Schließen Sie alle Segmente vor Ihrem Gegner.",
                            isSelected = selectedMode == GameMode.CRICKET,
                            onClick = { onContinue(GameMode.CRICKET) }
                        )
                        GameModeCard(
                            iconRes = Res.drawable.ic_all_fives,
                            title = "Durch 5 teilbar (All Fives)",
                            description = "Nur Runden mit durch 5 teilbaren Totals zählen. Alle 3 Pfeile müssen getroffen werden.",
                            isSelected = selectedMode == GameMode.ALL_FIVES,
                            onClick = { onContinue(GameMode.ALL_FIVES) }
                        )
                        GameModeCard(
                            iconRes = Res.drawable.ic_atc,
                            title = "Around the Clock",
                            description = "Treffen Sie der Reihe nach die Segmente von 1 bis 20 und optional das Bullseye.",
                            isSelected = selectedMode == GameMode.AROUND_THE_CLOCK,
                            onClick = { onContinue(GameMode.AROUND_THE_CLOCK) }
                        )
                        TournamentCard(
                            activeTournamentName = if (isTournRunning) activeTourn?.name else null,
                            isFinished = activeTourn?.isFinished ?: true,
                            onClick = {
                                if (isTournRunning) onResumeTournamentClick() else onTournamentClick()
                            }
                        )
                        PlayerManagementCard(
                            onClick = { onManagePlayersClick() }
                        )
                        PcIntegrationCard(
                            onClick = { onPcConnect() }
                        )
                    }
                }
            }
            
            // Settings Button (Floating Top Right)
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        contentDescription = "Einstellungen",
                        modifier = Modifier.size(32.dp)
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
    isSelected: Boolean,
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
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
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

@Composable
fun PcIntegrationCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (isGrid) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    text = "Geräte verbinden",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Kamera, Anzeige oder Host im Netzwerk",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_mobile_connect),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Geräte verbinden",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Kamera, Anzeige oder Host im Netzwerk",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                
                Text(
                    text = "→",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
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
                text = "Spieler",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Spieler verwalten & Statistiken ansehen",
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
    modifier: Modifier = Modifier,
    isGrid: Boolean = false
) {
    val isRunning = activeTournamentName != null && !isFinished
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isRunning) 2.dp else 1.dp,
                color = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) Color(0xFF2E2218) else MaterialTheme.colorScheme.surface
        )
    ) {
        if (isGrid) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
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
                    text = if (isRunning) "Turnier fortsetzen" else "Turnier",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isRunning) "$activeTournamentName läuft" else "K.O.-System & Liga für Gruppen",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 20.dp),
                    tint = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRunning) "Turnier fortsetzen" else "Turnier",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isRunning) "$activeTournamentName läuft" else "K.O.-System & Liga für Gruppen",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Text(
                    text = "→",
                    fontSize = 24.sp,
                    color = if (isRunning) PrimaryOrange else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

