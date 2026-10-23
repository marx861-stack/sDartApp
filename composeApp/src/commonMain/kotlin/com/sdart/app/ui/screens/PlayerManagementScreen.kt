package com.sdart.app.ui.screens

import com.sdart.app.ui.theme.ditheredBackground
import com.sdart.app.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sdart.app.ui.ScreenHeader
import kotlinx.coroutines.launch

/**
 * Verwaltungsseite fÃ¼r Spieler
 */
@Composable
fun PlayerManagementScreen(
    isDark: Boolean = true,
    repository: PlayerRepository = remember { createPersistentPlayerRepository() },
    onBack: () -> Unit,
    onPlayerClick: (SavedPlayer) -> Unit = {}
) {
    
    val scope = rememberCoroutineScope()
    
    var players by remember { mutableStateOf<List<SavedPlayer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<SavedPlayer?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        if (repository == null) {
            errorMessage = "Repository konnte nicht initialisiert werden"
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            isLoading = true
            errorMessage = null
            players = repository.loadAllPlayers()
        } catch (e: Exception) {
            println("Error loading players in PlayerManagementScreen: ${e.message}")
            e.printStackTrace()
            errorMessage = "Fehler beim Laden der Spieler: ${e.message}"
            players = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    if (repository == null) {
        // Fallback UI wenn Repository nicht erstellt werden kann
        val backgroundModifier = Modifier.fillMaxSize().ditheredBackground()
        
        Box(
            modifier = backgroundModifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Fehler beim Initialisieren der Spieler-Verwaltung",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.sdart.app.ui.theme.PrimaryOrange
                    )
                ) {
                    Text("ZurÃ¼ck", color = Color.White)
                }
            }
        }
        return
    }
    
    fun loadPlayers() {
        if (repository == null) return
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                players = repository.loadAllPlayers()
            } catch (e: Exception) {
                println("Error reloading players: ${e.message}")
                e.printStackTrace()
                errorMessage = "Fehler beim Laden der Spieler: ${e.message}"
                players = emptyList()
            } finally {
                isLoading = false
            }
        }
    }
    
    val backgroundModifier = Modifier.fillMaxSize().ditheredBackground()
    
    Box(
        modifier = backgroundModifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Einheitlicher Screen-Header
        ScreenHeader(
            title = "Spieler",
            onBack = onBack
        )

        // Inhalt auf dieselbe Maximalbreite begrenzt wie die Einstellungen
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Neuer Spieler Button
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.sdart.app.ui.theme.PrimaryOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Neuer Spieler", color = Color.White)
                }
            }
            
            // Fehlermeldung anzeigen
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF5c191e) else Color(0xFFf8d7da)
                        ),
                        border = if (isDark) BorderStroke(1.dp, Color(0xFFff6b6b).copy(alpha = 0.5f)) else null
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFFffb3b3) else Color(0xFF721c24),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // Spieler-Liste
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(24.dp),
                            color = if (isDark) com.sdart.app.ui.theme.PrimaryOrange else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else if (players.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) com.sdart.app.ui.theme.GlassWhite else Color(0xFFf8f9fa)
                        ),
                        border = if (isDark) BorderStroke(1.dp, com.sdart.app.ui.theme.GlassBorder) else null
                    ) {
                        Text(
                            text = "Keine Spieler vorhanden.\nErstellen Sie einen neuen Spieler.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) com.sdart.app.ui.theme.DarkTextSecondary else Color(0xFF6c757d),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) com.sdart.app.ui.theme.GlassWhite else Color(0xFFf8f9fa)
                        ),
                        border = if (isDark) BorderStroke(1.dp, com.sdart.app.ui.theme.GlassBorder) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            players.forEachIndexed { index, player ->
                                PlayerItem(
                                    player = player,
                                    isDark = isDark,
                                    onClick = {
                                        onPlayerClick(player)
                                    },
                                    onDelete = {
                                        playerToDelete = player
                                    }
                                )
                                if (index < players.size - 1) {
                                    HorizontalDivider(color = if (isDark) com.sdart.app.ui.theme.GlassBorder else Color(0xFFdee2e6))
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Create Dialog
    if (showCreateDialog && repository != null) {
        CreatePlayerDialog(
            isDark = isDark,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                scope.launch {
                    try {
                        val newPlayer = SavedPlayer(name = name)
                        repository?.savePlayer(newPlayer)
                        loadPlayers()
                        showCreateDialog = false
                    } catch (e: Exception) {
                        println("Error saving player: ${e.message}")
                        e.printStackTrace()
                        errorMessage = "Fehler beim Speichern des Spielers: ${e.message}"
                    }
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    playerToDelete?.let { player ->
        if (repository != null) {
            AlertDialog(
                onDismissRequest = { playerToDelete = null },
                title = { Text("Spieler lÃ¶schen", color = if (isDark) Color.White else Color.Unspecified) },
                text = {
                    Text(
                        text = "MÃ¶chten Sie '${player.name}' wirklich lÃ¶schen? Alle Statistiken gehen verloren.",
                        color = if (isDark) com.sdart.app.ui.theme.DarkTextSecondary else Color.Unspecified
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    repository?.deletePlayer(player.name)
                                    loadPlayers()
                                    playerToDelete = null
                                } catch (e: Exception) {
                                    println("Error deleting player: ${e.message}")
                                    e.printStackTrace()
                                    errorMessage = "Fehler beim LÃ¶schen des Spielers: ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFdc3545)
                        )
                    ) {
                        Text("LÃ¶schen", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { playerToDelete = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Unspecified
                        )
                    ) {
                        Text("Abbrechen")
                    }
                },
                containerColor = if (isDark) Color(0xFF262626) else MaterialTheme.colorScheme.surface,
                titleContentColor = if (isDark) Color.White else Color.Unspecified,
                textContentColor = if (isDark) com.sdart.app.ui.theme.DarkTextSecondary else Color.Unspecified
            )
        }
    }
}

@Composable
private fun PlayerItem(
    player: SavedPlayer,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x15FFFFFF) else Color.White
        ),
        border = if (isDark) BorderStroke(1.dp, com.sdart.app.ui.theme.GlassBorder) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF212529)
                )
                
                if (player.totalGames > 0) {
                    Text(
                        text = "${player.totalGames} Spiele  â€¢  Ã˜ ${player.bestAverageScorePerRound}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) com.sdart.app.ui.theme.DarkTextSecondary else Color(0xFF6c757d)
                    )
                } else {
                    Text(
                        text = "Noch keine Spiele gespielt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) com.sdart.app.ui.theme.DarkTextSecondary else Color(0xFF6c757d)
                    )
                }
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "LÃ¶schen",
                    tint = if (isDark) Color(0xFFff6b6b) else Color(0xFFdc3545),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CreatePlayerDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playerName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF262626) else MaterialTheme.colorScheme.surface
            ),
            border = if (isDark) BorderStroke(1.dp, com.sdart.app.ui.theme.GlassBorder) else null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Neuer Spieler",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Unspecified,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Spielername") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black,
                        focusedBorderColor = com.sdart.app.ui.theme.PrimaryOrange,
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFdee2e6),
                        focusedLabelColor = com.sdart.app.ui.theme.PrimaryOrange,
                        unfocusedLabelColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6c757d)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Unspecified
                        )
                    ) {
                        Text("Abbrechen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (playerName.isNotBlank()) {
                                onConfirm(playerName.trim())
                            }
                        },
                        enabled = playerName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.sdart.app.ui.theme.PrimaryOrange,
                            disabledContainerColor = if (isDark) Color(0xFF444444) else Color(0xFFe0e0e0)
                        )
                    ) {
                        Text("Erstellen", color = Color.White)
                    }
                }
            }
        }
    }
}

