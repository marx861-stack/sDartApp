package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdart.app.ui.components.MaxPlayers
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

data class SetupPlayer(val name: String, val isLoaded: Boolean = false)

/**
 * Zustand und Start-Logik des Spiel-Setups auf der Seite "Neues Spiel".
 * Bleibt beim Wechsel des Spielmodus erhalten, damit eingetragene Spieler nicht verloren gehen.
 */
@Stable
class PlayerSetupState(initialPlayers: List<SetupPlayer>) {
    var playerNames by mutableStateOf(initialPlayers)
    var startScore by mutableStateOf(501)
    var customScore by mutableStateOf("")
    var doubleIn by mutableStateOf(false)
    var doubleOut by mutableStateOf(true)
    var legs by mutableStateOf(1)
    var randomizeOrder by mutableStateOf(true)
    var showError by mutableStateOf(false)
    // All Fives: 50, 75 oder 100
    var targetScore by mutableStateOf<Int?>(50)
    // Around the Clock
    var atcVariant by mutableStateOf(ATCVariant.STANDARD)
    var atcReverse by mutableStateOf(false)
    var atcWithBulls by mutableStateOf(false)
    // Cricket
    var cricketVariant by mutableStateOf(CricketVariant.STANDARD)

    val canStart: Boolean get() = playerNames.any { it.name.isNotBlank() }

    @Composable
    fun errorMessage(mode: GameMode): String = when {
        playerNames.isEmpty() -> stringResource(Res.string.game_setup_error_min_players)
        playerNames.size > MaxPlayers -> stringResource(Res.string.game_setup_error_max_players, MaxPlayers)
        mode == GameMode.ALL_FIVES && targetScore == null -> stringResource(Res.string.game_setup_error_target)
        mode == GameMode.ALL_FIVES && playerNames.size != playerNames.distinct().size -> stringResource(Res.string.game_setup_error_unique_names)
        else -> stringResource(Res.string.game_setup_error_generic)
    }

    fun addPlayer() {
        if (playerNames.size < MaxPlayers) playerNames = playerNames + SetupPlayer("", false)
    }

    fun renamePlayer(index: Int, name: String) {
        playerNames = playerNames.toMutableList().also { it[index] = SetupPlayer(name, false) }
    }

    fun removePlayer(index: Int) {
        playerNames = playerNames.toMutableList().also { it.removeAt(index) }
    }

    fun addLoadedPlayers(names: List<String>) {
        val newNames = playerNames.toMutableList()
        names.forEach { selectedName ->
            if (newNames.size < MaxPlayers && !newNames.any { it.name == selectedName }) {
                newNames.add(SetupPlayer(selectedName, true))
            }
        }
        playerNames = newNames
    }

    /**
     * Prüft die Eingaben und startet das Spiel. Setzt [showError], wenn das nicht möglich ist.
     */
    fun startGame(
        mode: GameMode,
        onStartGame: (List<String>, GameMode, Int, Boolean, Boolean, Int, Boolean, ATCGameState?, CricketConfig?) -> Unit,
        playerWord: String = "Player"
    ) {
        try {
            val validPlayers = playerNames.mapIndexed { i, p ->
                SetupPlayer(if (p.name.isBlank()) "$playerWord ${i + 1}" else p.name.trim(), p.isLoaded)
            }.filter { it.name.isNotBlank() }
            val names = validPlayers.map { it.name }

            if (names.isEmpty() || names.size > MaxPlayers) {
                showError = true
                return
            }

            if (mode == GameMode.ALL_FIVES) {
                val target = targetScore
                if (target == null || names.size != names.distinct().size) {
                    showError = true
                    return
                }
                AppPreferences.saveLastPlayerNames(validPlayers.joinToString("|") { "${it.name}:${it.isLoaded}" })
                onStartGame(names, mode, target, false, false, legs, randomizeOrder, null, null)
                showError = false
                return
            }

            val finalStartScore: Int = if (startScore > 0) {
                startScore
            } else {
                customScore.toIntOrNull()?.takeIf { it > 0 } ?: 501
            }
            val atcConfig = if (mode == GameMode.AROUND_THE_CLOCK) ATCGameState(atcVariant, atcReverse, atcWithBulls) else null
            val cricketConfig = if (mode == GameMode.CRICKET) CricketConfig(cricketVariant) else null

            AppPreferences.saveLastPlayerNames(validPlayers.joinToString("|") { "${it.name}:${it.isLoaded}" })
            onStartGame(names, mode, finalStartScore, doubleIn, doubleOut, legs, randomizeOrder, atcConfig, cricketConfig)
            showError = false
        } catch (e: Exception) {
            println("=== ERROR IN PLAYER SETUP: ${e.message} ===")
            e.printStackTrace()
            showError = true
        }
    }
}

@Composable
fun rememberPlayerSetupState(playerWord: String = "Player"): PlayerSetupState = remember {
    val saved = AppPreferences.getLastPlayerNames()
    val initialPlayers = if (!saved.isNullOrEmpty()) {
        saved.split("|").map {
            val parts = it.split(":")
            if (parts.size >= 2) SetupPlayer(parts[0], parts[1].toBooleanStrictOrNull() ?: false)
            else SetupPlayer(it, false)
        }
    } else {
        listOf(SetupPlayer("$playerWord 1", false))
    }
    PlayerSetupState(initialPlayers)
}
