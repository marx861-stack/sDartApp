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
import com.sdart.app.ui.LocalCompactLayout
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.sdart.app.ui.components.ActiveGlassBg
import com.sdart.app.ui.components.CardLink
import com.sdart.app.ui.components.DashboardCard
import com.sdart.app.ui.components.DesktopPage
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*
import kotlinx.coroutines.launch

/**
 * Verwaltungsseite für Spieler
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
    val repoInitFailedText = stringResource(Res.string.players_error_repository_init)
    val loadErrorFmt = stringResource(Res.string.players_error_load, "%MSG%")
    val saveErrorFmt = stringResource(Res.string.players_error_save, "%MSG%")
    val deleteErrorFmt = stringResource(Res.string.players_error_delete, "%MSG%")
    
    LaunchedEffect(Unit) {
        if (repository == null) {
            errorMessage = repoInitFailedText
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
            errorMessage = loadErrorFmt.replace("%MSG%", e.message.toString())
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
                    stringResource(Res.string.players_error_init_management),
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
                    Text(stringResource(Res.string.players_back), color = Color.White)
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
                errorMessage = loadErrorFmt.replace("%MSG%", e.message.toString())
                players = emptyList()
            } finally {
                isLoading = false
            }
        }
    }
    
    PlayerList(
        players = players,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onCreate = { showCreateDialog = true },
        onDelete = { playerToDelete = it },
        onPlayerClick = onPlayerClick
    )

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
                        errorMessage = saveErrorFmt.replace("%MSG%", e.message.toString())
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
                title = { Text(stringResource(Res.string.players_delete_title), color = if (isDark) Color.White else Color.Unspecified) },
                text = {
                    Text(
                        text = stringResource(Res.string.players_delete_confirm, player.name),
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
                                    errorMessage = deleteErrorFmt.replace("%MSG%", e.message.toString())
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFdc3545)
                        )
                    ) {
                        Text(stringResource(Res.string.players_delete), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { playerToDelete = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Unspecified
                        )
                    ) {
                        Text(stringResource(Res.string.players_cancel))
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
                    text = stringResource(Res.string.players_new_player),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Unspecified,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text(stringResource(Res.string.players_name_label)) },
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
                        Text(stringResource(Res.string.players_cancel))
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
                        Text(stringResource(Res.string.players_create), color = Color.White)
                    }
                }
            }
        }
    }
}



/**
 * Spielerliste. Breite Fenster sind aufgebaut wie "Neues Spiel": Spielerkarten links, rechts die Statistik
 * des gewählten Spielers. Schmale Fenster zeigen nur die Karten; ein Tipp öffnet das Spieler-Detail.
 */
@Composable
private fun PlayerList(
    players: List<SavedPlayer>,
    isLoading: Boolean,
    errorMessage: String?,
    onCreate: () -> Unit,
    onDelete: (SavedPlayer) -> Unit,
    onPlayerClick: (SavedPlayer) -> Unit
) {
    if (LocalCompactLayout.current) {
        DesktopPage(title = stringResource(Res.string.main_title_players)) {
            CardLink(
                text = stringResource(Res.string.players_new_player),
                onClick = onCreate,
                icon = Res.drawable.ic_plus,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                )
            }
            when {
                isLoading -> CircularProgressIndicator(color = PrimaryOrange)
                players.isEmpty() -> Text(
                    text = stringResource(Res.string.players_empty),
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                else -> players.forEach { player ->
                    PlayerCard(
                        player = player,
                        selected = false,
                        onClick = { onPlayerClick(player) },
                        onDelete = { onDelete(player) },
                        compact = true
                    )
                }
            }
        }
        return
    }

    var selectedName by remember { mutableStateOf<String?>(null) }
    // Nach dem Löschen oder beim ersten Laden springt die Auswahl auf den ersten Spieler
    val selected = players.firstOrNull { it.name == selectedName } ?: players.firstOrNull()

    // Seite genau fensterhoch: Spielerliste und Statistik scrollen jeweils für sich
    DesktopPage(title = stringResource(Res.string.main_title_players), minHeight = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CardLink(
                    text = stringResource(Res.string.players_new_player),
                    onClick = onCreate,
                    icon = Res.drawable.ic_plus,
                    modifier = Modifier.padding(start = 4.dp, bottom = 14.dp)
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
                when {
                    isLoading -> CircularProgressIndicator(color = PrimaryOrange)
                    players.isEmpty() -> Text(
                        text = stringResource(Res.string.players_empty),
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(players, key = { it.name }) { player ->
                            PlayerCard(
                                player = player,
                                selected = player.name == selected?.name,
                                onClick = { selectedName = player.name },
                                onDelete = { onDelete(player) }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(2.6f).fillMaxHeight()) {
                if (selected != null) {
                    DesktopPlayerStats(
                        player = selected,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: SavedPlayer,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    compact: Boolean = false
) {
    DashboardCard(
        title = null,
        highlighted = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = if (compact) 14.dp else 24.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ActiveGlassBg)
            ) {
                Text(
                    text = player.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = PrimaryOrange)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (player.totalGames > 0) {
                        if (player.totalGames == 1)
                            stringResource(Res.string.players_games_summary_one, player.totalGames, player.bestAverageScorePerRound.toString())
                        else
                            stringResource(Res.string.players_games_summary_other, player.totalGames, player.bestAverageScorePerRound.toString())
                    } else stringResource(Res.string.players_no_games_yet),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trash),
                    contentDescription = stringResource(Res.string.players_delete_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
