package com.sdart.app.ui.screens

import com.sdart.app.ui.theme.ditheredBackground
import com.sdart.app.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ATCVariant
import com.sdart.app.ATCGameState
import com.sdart.app.CricketVariant
import com.sdart.app.CricketConfig

import com.sdart.app.ui.*
import com.sdart.app.ui.components.DashboardCard
import com.sdart.app.ui.theme.PrimaryOrange
import com.sdart.app.ui.theme.PrimaryGlow
import com.sdart.app.ui.theme.DarkBgGradient
import androidx.compose.ui.graphics.vector.path

data class SetupPlayer(val name: String, val isLoaded: Boolean = false)

@Composable
fun PlayerSetupScreen(
    selectedGameMode: GameMode,
    repository: PlayerRepository = remember { createPersistentPlayerRepository() },
    onBack: () -> Unit,
    onStartGame: (List<String>, GameMode, Int, Boolean, Boolean, Int, Boolean, ATCGameState?, CricketConfig?) -> Unit,
    onNavigateToManagement: () -> Unit = {}
) {
    val backgroundModifier = Modifier.fillMaxSize().ditheredBackground()

    AdaptiveContainer(modifier = backgroundModifier) { windowInfo ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            ScreenHeader(
                title = "Game Setup",
                onBack = onBack
            )

            PlayerSetupContent(
                selectedGameMode = selectedGameMode,
                repository = repository,
                onStartGame = onStartGame,
                isWidescreen = windowInfo.isWidescreen
            )
        }
    }
}

/**
 * Spielernamen, Spiel-Einstellungen und Start-Button für einen Spielmodus.
 * Wird vom mobilen Game Setup und vom Desktop-Screen "Neues Spiel" genutzt.
 */
@Composable
fun PlayerSetupContent(
    selectedGameMode: GameMode,
    repository: PlayerRepository,
    onStartGame: (List<String>, GameMode, Int, Boolean, Boolean, Int, Boolean, ATCGameState?, CricketConfig?) -> Unit,
    isWidescreen: Boolean,
    modifier: Modifier = Modifier,
    state: PlayerSetupState = rememberPlayerSetupState()
) {
    var playerNames by state::playerNames
    var startScore by state::startScore
    var customScore by state::customScore
    var doubleIn by state::doubleIn
    var doubleOut by state::doubleOut
    var legs by state::legs
    val showError by state::showError
    var showLoadDialog by remember { mutableStateOf(false) }
    // All Fives spezifische Einstellungen
    var targetScore by state::targetScore
    var randomizeOrder by state::randomizeOrder
    // ATC spezifische Einstellungen
    var atcVariant by state::atcVariant
    var atcReverse by state::atcReverse
    var atcWithBulls by state::atcWithBulls
    // Cricket spezifische Einstellungen
    var cricketVariant by state::cricketVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error Message
        if (showError) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFf8d7da)
                )
            ) {
                Text(
                    text = state.errorMessage(selectedGameMode),
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF721c24),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        val playerNamesContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                SetupSectionHeader("Spielernamen")
                
                // Aktions-Buttons oben auf (Neu, Laden, Zufall)
                if (playerNames.size < 8) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = { playerNames = playerNames + SetupPlayer("", false) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_plus),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                                )
                                Text(
                                    "Neu",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        
                        Surface(
                            onClick = { showLoadDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_folder),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                                )
                                Text("Laden", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Surface(
                            onClick = { randomizeOrder = !randomizeOrder },
                            shape = RoundedCornerShape(12.dp),
                            color = if (randomizeOrder) Color(0xFF332A22) else Color(0x1AFFFFFF),
                            border = BorderStroke(
                                width = if (randomizeOrder) 2.dp else 1.dp,
                                color = if (randomizeOrder) PrimaryOrange else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_refresh),
                                    contentDescription = null,
                                    tint = if (randomizeOrder) PrimaryOrange else Color.White,
                                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                                )
                                Text(
                                    "Zufall",
                                    color = if (randomizeOrder) Color.White else Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = { showLoadDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .weight(2f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_folder),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                                )
                                Text("Spieler laden", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Surface(
                            onClick = { randomizeOrder = !randomizeOrder },
                            shape = RoundedCornerShape(12.dp),
                            color = if (randomizeOrder) Color(0xFF332A22) else Color(0x1AFFFFFF),
                            border = BorderStroke(
                                width = if (randomizeOrder) 2.dp else 1.dp,
                                color = if (randomizeOrder) PrimaryOrange else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_refresh),
                                    contentDescription = null,
                                    tint = if (randomizeOrder) PrimaryOrange else Color.White,
                                    modifier = Modifier.padding(end = 8.dp).size(20.dp)
                                )
                                Text(
                                    "Zufall",
                                    color = if (randomizeOrder) Color.White else Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // Spieler-Liste darunter
                playerNames.forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = player.name,
                            onValueChange = { newName -> 
                                playerNames = playerNames.toMutableList().also { it[index] = SetupPlayer(newName, false) }
                            },
                            label = { Text("Spieler ${index + 1}") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            trailingIcon = if (player.isLoaded) {
                                {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_folder),
                                        contentDescription = "Spieler geladen",
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = PrimaryOrange,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        
                        // Delete button
                        if (playerNames.size > 1) {
                            IconButton(
                                onClick = {
                                    val newList = playerNames.toMutableList()
                                    newList.removeAt(index)
                                    playerNames = newList
                                }
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_trash),
                                    contentDescription = "Spieler löschen",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val gameSettingsContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                if (selectedGameMode == GameMode.X501 || selectedGameMode == GameMode.X301 || selectedGameMode == GameMode.X701) {
                    SetupSectionHeader("Startpunktzahl")
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SetupTile("101", "Sehr schnell", startScore == 101, { startScore = 101; customScore = "" }, Modifier.weight(1f))
                        SetupTile("301", "Schnell", startScore == 301, { startScore = 301; customScore = "" }, Modifier.weight(1f))
                        SetupTile("501", "Standard", startScore == 501, { startScore = 501; customScore = "" }, Modifier.weight(1f))
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SetupTile("701", "Lang", startScore == 701, { startScore = 701; customScore = "" }, Modifier.weight(1f))
                        SetupTile("Custom", if (startScore == 0 && customScore.isNotEmpty()) customScore else "", startScore == 0, { startScore = 0 }, Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    if (startScore == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customScore,
                            onValueChange = { customScore = it },
                            label = { Text("Custom Startpunktzahl", color = Color.White.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }

                    SetupSectionHeader("Regeln")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ToggleTile("Double In", "Start mit Double", doubleIn, { doubleIn = !doubleIn }, Modifier.weight(1f))
                        ToggleTile("Double Out", "Sieg mit Double", doubleOut, { doubleOut = !doubleOut }, Modifier.weight(1f))
                    }

                    SetupSectionHeader("Legs")
                    CounterTile(if (playerNames.size <= 1) "Legs" else "Legs (Best of)", legs, { legs = it })
                }
                
                if (selectedGameMode == GameMode.CRICKET) {
                    SetupSectionHeader("Modus Variante")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ToggleTile("Standard", "Punkte für Dich", cricketVariant == CricketVariant.STANDARD, { cricketVariant = CricketVariant.STANDARD }, Modifier.weight(1f))
                        ToggleTile("Cut Throat", "Punkte an Gegner", cricketVariant == CricketVariant.CUT_THROAT, { cricketVariant = CricketVariant.CUT_THROAT }, Modifier.weight(1f))
                    }
                    
                    SetupSectionHeader("Legs")
                    CounterTile(if (playerNames.size <= 1) "Legs" else "Legs (Best of)", legs, { legs = it })
                }
                
                if (selectedGameMode == GameMode.ALL_FIVES) {
                    SetupSectionHeader("Ziel-Punktzahl")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SetupTile("50", "Kurz", targetScore == 50, { targetScore = 50 }, Modifier.weight(1f))
                        SetupTile("75", "Standard", targetScore == 75, { targetScore = 75 }, Modifier.weight(1f))
                        SetupTile("100", "Lang", targetScore == 100, { targetScore = 100 }, Modifier.weight(1f))
                    }

                    SetupSectionHeader("Legs")
                    CounterTile("Legs", legs, { legs = it })
                }

                if (selectedGameMode == GameMode.AROUND_THE_CLOCK) {
                    SetupSectionHeader("Variante")
                    val atcVariants = listOf(
                        Triple(ATCVariant.STANDARD, "Standard", "Einfaches Treffen"),
                        Triple(ATCVariant.DOUBLE, "Double Only", "Nur Doubles zählen"),
                        Triple(ATCVariant.TRIPLE, "Triple Only", "Nur Triples zählen")
                    )
                    val atcVariants2 = listOf(
                        Triple(ATCVariant.QUICKLY, "Quickly", "S=1, D=2, T=3"),
                        Triple(ATCVariant.SHANGHAI, "Shanghai", "S, D, T nötig")
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        atcVariants.forEach { (variant, title, subtitle) ->
                            SetupTile(title, subtitle, atcVariant == variant, { atcVariant = variant }, Modifier.weight(1f))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        atcVariants2.forEach { (variant, title, subtitle) ->
                            SetupTile(title, subtitle, atcVariant == variant, { atcVariant = variant }, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    SetupSectionHeader("Optionen")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ToggleTile("Rückwärts", "20 -> 1", atcReverse, { atcReverse = !atcReverse }, Modifier.weight(1f))
                        ToggleTile("Bullseye", "Mit Bull", atcWithBulls, { atcWithBulls = !atcWithBulls }, Modifier.weight(1f))
                    }
                }
            }
        }

        val startButtonContent: @Composable () -> Unit = {
            Button(
                onClick = { state.startGame(selectedGameMode, onStartGame) },
                enabled = playerNames.any { it.name.isNotBlank() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (playerNames.any { it.name.isNotBlank() }) PrimaryOrange else Color(0xFF444444)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(
                    "SPIEL STARTEN",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }
        }

        if (isWidescreen) {
            // ==========================================
            // DESKTOP WIDESCREEN 2-SPALTEN LAYOUT
            // ==========================================
            Row(
                modifier = Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Linke Spalte: Spieler & Reihenfolge
                DashboardCard(title = null, modifier = Modifier.weight(1f)) {
                    playerNamesContent()
                }

                // Rechte Spalte: Spiel-Einstellungen & Start-Button
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DashboardCard(title = null, modifier = Modifier.fillMaxWidth()) {
                        gameSettingsContent()
                    }

                    startButtonContent()
                }
            }
        } else {
            // ==========================================
            // MOBILE PORTRAIT (Vertikaler Ein-Spalten-Fluss)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                playerNamesContent()
                Spacer(modifier = Modifier.height(20.dp))
                gameSettingsContent()
                Spacer(modifier = Modifier.height(20.dp))
                startButtonContent()
            }
        }
    }

    // Player Load Dialog
    if (showLoadDialog) {
        PlayerLoadDialog(
            repository = repository,
            onDismiss = { showLoadDialog = false },
            onPlayersSelected = { selectedNames -> state.addLoadedPlayers(selectedNames) }
        )
    }
}
