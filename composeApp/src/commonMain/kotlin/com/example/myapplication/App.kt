package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.connect.SessionManager
import com.example.myapplication.connect.ParticipantSessionManager
import com.example.myapplication.connect.ui.CompanionConnectionScreen
import com.example.myapplication.connect.ui.HostOverviewDialog
import com.example.myapplication.tournament.ui.TournamentSetupScreen
import com.example.myapplication.tournament.ui.TournamentViewScreen
import com.example.myapplication.ui.theme.DarkBgGradient
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun App() {
    val vm = remember { DartViewModel() }
    val navManager = remember { NavigationManager() }
    val repository = remember { createPersistentPlayerRepository() }
    val audioPlayer = rememberAudioPlayer()
    val sessionManager = remember { SessionManager() }
    val participantManager = remember { ParticipantSessionManager() }
    var showHostOverviewFromStart by remember { mutableStateOf(false) }

    DisposableEffect(audioPlayer, vm) {
        vm.onPlaySound = { text, excitement ->
            audioPlayer.playSound(text, excitement)
        }
        vm.onVibrate = {
            audioPlayer.vibrate()
        }
        onDispose {
            vm.onPlaySound = null
            vm.onVibrate = null
        }
    }

    DisposableEffect(sessionManager, vm) {
        sessionManager.onDartHit = { hit ->
            val pos = if (hit.x > 0f || hit.y > 0f) androidx.compose.ui.geometry.Offset(hit.x, hit.y) else null
            vm.enterScoreML(
                score = hit.segment,
                confidence = hit.confidence.toDouble(),
                position = pos,
                multiplier = hit.multiplier
            )
        }
        sessionManager.onScoreInput = { input ->
            val pos = if (input.x != null && input.y != null) androidx.compose.ui.geometry.Offset(input.x, input.y) else null
            vm.enterScore(
                score = input.segment,
                position = pos,
                multiplier = input.multiplier
            )
        }
        sessionManager.onUndo = {
            vm.undo()
        }
        onDispose {
            sessionManager.onDartHit = null
            sessionManager.onScoreInput = null
            sessionManager.onUndo = null
        }
    }

    val isPcIntegrationSubstate = navManager.currentScreen is Screen.PcIntegration &&
            participantManager.state.value !is com.example.myapplication.connect.ParticipantState.Idle

    val canGoBack = navManager.canGoBack() || showHostOverviewFromStart || isPcIntegrationSubstate

    val handleBack: () -> Unit = {
        if (showHostOverviewFromStart) {
            showHostOverviewFromStart = false
        } else if (isPcIntegrationSubstate) {
            participantManager.disconnect()
        } else {
            navManager.goBack()
        }
    }

    PlatformBackHandler(
        enabled = canGoBack,
        onBack = handleBack
    )

    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBgGradient)
                .edgeSwipeBack(
                    enabled = canGoBack,
                    onBack = handleBack
                )
        ) {
            when (val screen = navManager.currentScreen) {
                Screen.Start -> StartScreen(
                    vm = vm,
                    onContinue = { mode ->
                        navManager.setGameMode(mode)
                        navManager.navigateTo(Screen.PlayerSetup)
                    },
                    onTournamentClick = {
                        navManager.navigateTo(Screen.TournamentSetup)
                    },
                    onResumeTournamentClick = {
                        navManager.navigateTo(Screen.TournamentView)
                    },
                    onPcConnect = {
                        navManager.navigateTo(Screen.PcIntegration)
                    },
                    onSettingsClick = {
                        navManager.navigateTo(Screen.Settings)
                    },
                    onManagePlayersClick = {
                        navManager.navigateTo(Screen.PlayerManagement)
                    }
                )
                Screen.PlayerSetup -> PlayerSetupScreen(
                    selectedGameMode = navManager.selectedGameMode ?: GameMode.X501,
                    repository = repository,
                    onBack = { navManager.goBack() },
                    onStartGame = { players, mode, startScore, doubleIn, doubleOut, legs, randomize, atc, cricket ->
                        vm.startGame(players, mode, startScore, doubleIn, doubleOut, legs, randomize, atc, cricket)
                        // In Phase 5 verbinden wir den GameScreen mit Kamera
                        navManager.navigateTo(Screen.Game)
                    },
                    onNavigateToManagement = {
                        navManager.navigateTo(Screen.PlayerManagement)
                    }
                )
                Screen.Settings -> SettingsScreen(
                    vm = vm,
                    onBack = { navManager.goBack() }
                )
                Screen.PlayerManagement -> PlayerManagementScreen(
                    repository = repository,
                    onBack = { navManager.goBack() },
                    onPlayerClick = { player ->
                        navManager.navigateTo(Screen.PlayerDetail(player))
                    }
                )
                is Screen.PlayerDetail -> PlayerDetailScreen(
                    player = screen.player,
                    onBack = { navManager.goBack() }
                )
                Screen.Game -> GameScreen(
                    vm = vm,
                    repository = repository,
                    navigationManager = navManager,
                    sessionManager = sessionManager,
                    onExit = {
                        if (vm.activeTournamentMatchId != null) {
                            vm.cancelTournamentMatch()
                            navManager.navigateTo(Screen.TournamentView)
                        } else {
                            navManager.reset()
                        }
                    },
                    onGameFinished = { navManager.navigateTo(Screen.GameSummary) }
                )
                Screen.GameSummary -> GameSummaryScreen(
                    vm = vm,
                    onBack = {
                        if (vm.tournamentManager.currentTournament != null && navManager.selectedGameMode == null) {
                            navManager.navigateTo(Screen.TournamentView)
                        } else {
                            navManager.reset()
                        }
                    }
                )
                Screen.TournamentSetup -> TournamentSetupScreen(
                    repository = repository,
                    onBack = { navManager.goBack() },
                    onStartTournament = { name, type, config, players, randomize ->
                        vm.tournamentManager.createTournament(name, type, config, players, randomize)
                        navManager.navigateTo(Screen.TournamentView)
                    }
                )
                Screen.TournamentView -> TournamentViewScreen(
                    tournamentManager = vm.tournamentManager,
                    webServer = vm.tournamentWebServer,
                    onBack = { navManager.navigateTo(Screen.Start) },
                    onStartMatch = { match ->
                        vm.startTournamentMatch(match) {
                            navManager.navigateTo(Screen.Game)
                        }
                    }
                )
                Screen.PcIntegration -> CompanionConnectionScreen(
                    participantManager = participantManager,
                    onBack = { navManager.goBack() },
                    onOpenHostOverview = { showHostOverviewFromStart = true },
                    vm = vm
                )
                else -> {
                    StartScreen(
                        vm = vm,
                        onContinue = { mode ->
                            navManager.setGameMode(mode)
                            navManager.navigateTo(Screen.PlayerSetup)
                        },
                        onTournamentClick = {
                            navManager.navigateTo(Screen.TournamentSetup)
                        },
                        onResumeTournamentClick = {
                            navManager.navigateTo(Screen.TournamentView)
                        },
                        onPcConnect = {
                            navManager.navigateTo(Screen.PcIntegration)
                        },
                        onSettingsClick = { navManager.navigateTo(Screen.Settings) },
                        onManagePlayersClick = { navManager.navigateTo(Screen.PlayerManagement) }
                    )
                }
            }



            // Host-Übersicht geöffnet aus dem Startmenü
            if (showHostOverviewFromStart) {
                HostOverviewDialog(
                    sessionManager = sessionManager,
                    onDismissRequest = { showHostOverviewFromStart = false }
                )
            }

            if (navManager.showGameExitDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { navManager.cancelGameExit() },
                    title = { androidx.compose.material3.Text("Spiel beenden?", color = androidx.compose.ui.graphics.Color.White) },
                    text = { androidx.compose.material3.Text("Möchtest du das aktuelle Spiel wirklich beenden?", color = androidx.compose.ui.graphics.Color.LightGray) },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                if (vm.activeTournamentMatchId != null) {
                                    vm.cancelTournamentMatch()
                                    navManager.confirmGameExit()
                                    navManager.navigateTo(Screen.TournamentView)
                                } else {
                                    navManager.confirmGameExit()
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.myapplication.ui.theme.PrimaryOrange)
                        ) {
                            androidx.compose.material3.Text("Beenden", color = androidx.compose.ui.graphics.Color.White)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.OutlinedButton(onClick = { navManager.cancelGameExit() }) {
                            androidx.compose.material3.Text("Weiterspielen", color = androidx.compose.ui.graphics.Color.White)
                        }
                    },
                    containerColor = androidx.compose.ui.graphics.Color(0xFF262626)
                )
            }
        }
    }
}
