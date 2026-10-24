package com.sdart.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Navigation-Screens der S'Dart App
 */
sealed class Screen {
    object Start : Screen()
    object GameModes : Screen()
    object Game : Screen()
    object Camera : Screen()
    object Settings : Screen()
    object PlayerManagement : Screen()
    data class PlayerDetail(val player: SavedPlayer) : Screen()
    object PcIntegration : Screen()
    object GameSummary : Screen()
    object TournamentSetup : Screen()
    object TournamentView : Screen()
}

/**
 * Navigation-Richtungen für Screen-Übergänge
 */
enum class NavigationDirection {
    Forward, Backward, Up, Down
}

/**
 * Plattformunabhängiger NavigationManager zur Steuerung der App-Screens.
 */
class NavigationManager : ViewModel() {
    var currentScreen by mutableStateOf<Screen>(Screen.Start)
        private set
    
    private var navigationStack = mutableListOf<Screen>()
    
    var navigationDirection by mutableStateOf(NavigationDirection.Forward)
        private set
    
    var isNavigating by mutableStateOf(false)
        private set
    
    var selectedGameMode by mutableStateOf<GameMode?>(null)
        private set
    
    var showGameExitDialog by mutableStateOf(false)
        private set

    fun navigateTo(screen: Screen, direction: NavigationDirection = NavigationDirection.Forward) {
        if (currentScreen != screen) {
            if (direction == NavigationDirection.Forward) {
                navigationStack.add(currentScreen)
            }
            currentScreen = screen
            navigationDirection = direction
            isNavigating = true
            
            MainScope().launch {
                delay(300)
                isNavigating = false
            }
        }
    }
    
    fun goBack(): Boolean {
        if (showGameExitDialog) {
            cancelGameExit()
            return true
        }

        if (currentScreen == Screen.GameSummary) {
            reset()
            return true
        }

        if (currentScreen == Screen.Game) {
            showGameExitDialog = true
            return false
        }
        
        if (navigationStack.isEmpty()) {
            if (currentScreen != Screen.Start) {
                reset()
                return true
            }
            return false
        }
        
        val previous = navigationStack.removeAt(navigationStack.size - 1)
        currentScreen = previous
        navigationDirection = NavigationDirection.Backward
        isNavigating = true
        
        MainScope().launch {
            delay(300)
            isNavigating = false
        }
        
        return true
    }
    
    fun canGoBack(): Boolean = showGameExitDialog || currentScreen != Screen.Start || navigationStack.isNotEmpty()
    
    fun confirmGameExit() {
        showGameExitDialog = false
        reset()
    }
    
    fun cancelGameExit() {
        showGameExitDialog = false
    }
    
    fun reset() {
        currentScreen = Screen.Start
        navigationStack.clear()
        navigationDirection = NavigationDirection.Forward
        isNavigating = false
        selectedGameMode = null
        showGameExitDialog = false
    }
    
    fun setGameMode(gameMode: GameMode) {
        selectedGameMode = gameMode
    }
}

/**
 * Dialog der angezeigt wird wenn ein Spieler fertig wird oder das Spiel abgeschlossen ist.
 */
@androidx.compose.runtime.Composable
fun GameCompletionDialog(
    winnerName: String,
    isDark: Boolean,
    showContinueOption: Boolean = true,
    onContinuePlaying: () -> Unit,
    onViewStatistics: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { /* Dialog kann nicht geschlossen werden, muss eine Aktion wählen */ },
        title = {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Text(
                    text = winnerName,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    color = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF2d3748),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            androidx.compose.material3.Text(
                text = if (showContinueOption) {
                    "Möchten Sie weiterspielen, damit die anderen Spieler fertig spielen können, oder die Statistiken ansehen?"
                } else {
                    "Das Spiel ist beendet. Alle Spieler haben fertig gespielt. Möchten Sie sich die Statistiken ansehen?"
                },
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = if (isDark) com.sdart.app.ui.theme.DarkTextSecondary else androidx.compose.ui.graphics.Color(0xFF4a5568),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onViewStatistics,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = com.sdart.app.ui.theme.PrimaryOrange
                ),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Text("Statistiken ansehen", color = androidx.compose.ui.graphics.Color.White)
            }
        },
        dismissButton = if (showContinueOption) {
            {
                androidx.compose.material3.OutlinedButton(
                    onClick = onContinuePlaying,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Text("Weiter spielen")
                }
            }
        } else null,
        containerColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF262626).copy(alpha = 0.92f) else androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium
    )
}
