package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

/**
 * Dialog zum Laden von gespeicherten Spielern
 */
@Composable
fun PlayerLoadDialog(
    repository: PlayerRepository = remember { createPersistentPlayerRepository() },
    onDismiss: () -> Unit,
    onPlayersSelected: (List<String>) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var players by remember { mutableStateOf<List<SavedPlayer>>(emptyList()) }
    var selectedPlayers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            players = repository.loadAllPlayers()
        } catch (e: Exception) {
            println("Error loading players in PlayerLoadDialog: ${e.message}")
            e.printStackTrace()
            players = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF262626)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.myapplication.ui.theme.GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Gespeicherte Spieler",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Wähle Spieler für das Spiel aus:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = com.example.myapplication.ui.theme.PrimaryOrange)
                    }
                } else if (players.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine gespeicherten Spieler gefunden",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(players) { player ->
                            val isSelected = selectedPlayers.contains(player.name)
                            Surface(
                                onClick = {
                                    selectedPlayers = if (isSelected) {
                                        selectedPlayers - player.name
                                    } else {
                                        selectedPlayers + player.name
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) Color(0xFF332A22) else Color(0x1AFFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) com.example.myapplication.ui.theme.PrimaryOrange else Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = com.example.myapplication.ui.theme.PrimaryOrange,
                                            uncheckedColor = Color.White.copy(alpha = 0.6f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "Spiele: ${player.totalGames} | Siege: ${player.gamesWon}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onPlayersSelected(selectedPlayers.toList())
                            onDismiss()
                        },
                        enabled = selectedPlayers.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.myapplication.ui.theme.PrimaryOrange
                        )
                    ) {
                        Text("Übernehmen (${selectedPlayers.size})")
                    }
                }
            }
        }
    }
}
