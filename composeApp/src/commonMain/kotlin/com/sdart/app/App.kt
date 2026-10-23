package com.sdart.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.sdart.app.connect.SessionManager
import com.sdart.app.connect.ParticipantSessionManager
import com.sdart.app.connect.ui.CompanionConnectionScreen
import com.sdart.app.connect.ui.HostOverviewDialog
import com.sdart.app.tournament.ui.TournamentSetupScreen
import com.sdart.app.tournament.ui.TournamentViewScreen
import com.sdart.app.ui.AdaptiveContainer
import com.sdart.app.ui.ProvideAdaptiveScale
import com.sdart.app.ui.components.DesktopDestination
import com.sdart.app.ui.components.DesktopScaffold
import com.sdart.app.ui.screens.*
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.MyApplicationTheme
import com.sdart.app.ui.theme.ditheredBackground

@Composable
fun App() {
    val vm = remember { DartViewModel() }
    val navManager = remember { NavigationManager() }
    val repository = remember { createPersistentPlayerRepository() }
    val audioPlayer = rememberAudioPlayer()
    val sessionManager = remember { SessionManager() }
    val participantManager = remember { ParticipantSessionManager() }
    var showHostOverviewFromStart by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }

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
            participantManager.state.value !is com.sdart.app.connect.ParticipantState.Idle

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

    // Startet ein Spiel und merkt es sich für "Revanche" auf dem Desktop-Dashboard
    val startNewGame: (LastGameSetup) -> Unit = { setup ->
        AppPreferences.lastGameSetup = setup.copy(startedAtMillis = currentTimeMillis())
        navManager.setGameMode(setup.mode)
        vm.startGame(
            setup.playerNames, setup.mode, setup.customScore, setup.doubleIn, setup.doubleOut,
            setup.legs, setup.randomizeOrder, setup.atcConfig, setup.cricketConfig
        )
        navManager.navigateTo(Screen.Game)
    }

    // Callback der Setup-Screens (mobil und Desktop) zum Starten eines Spiels
    val startFromSetup: (List<String>, GameMode, Int, Boolean, Boolean, Int, Boolean, ATCGameState?, CricketConfig?) -> Unit =
        { players, mode, startScore, doubleIn, doubleOut, legs, randomize, atc, cricket ->
            startNewGame(
                LastGameSetup(
                    playerNames = players,
                    mode = mode,
                    customScore = startScore,
                    doubleIn = doubleIn,
                    doubleOut = doubleOut,
                    legs = legs,
                    randomizeOrder = randomize,
                    atcVariant = atc?.variant,
                    atcReverse = atc?.reverse ?: false,
                    atcWithBulls = atc?.withBulls ?: false,
                    cricketVariant = cricket?.variant
                )
            )
        }

    // Navigationsleiste der Desktop-Screens: Hauptbereiche ersetzen den Verlauf statt ihn zu stapeln
    val navigateTopLevel: (DesktopDestination) -> Unit = { destination ->
        navManager.reset()
        when (destination) {
            DesktopDestination.OVERVIEW -> Unit
            DesktopDestination.GAMES -> navManager.navigateTo(Screen.GameModes)
            DesktopDestination.TOURNAMENT -> {
                val running = vm.tournamentManager.currentTournament?.let { !it.isFinished } == true
                navManager.navigateTo(if (running) Screen.TournamentView else Screen.TournamentSetup)
            }
            DesktopDestination.PLAYERS -> navManager.navigateTo(Screen.PlayerManagement)
            DesktopDestination.DEVICES -> navManager.navigateTo(Screen.PcIntegration)
            DesktopDestination.SETTINGS -> navManager.navigateTo(Screen.Settings)
        }
    }

    val lastShellScreen = remember { ShellScreenMemory() }

    val startScreen: @Composable () -> Unit = {
        StartScreen(
            vm = vm,
            repository = repository,
            sessionManager = sessionManager,
            onGamesClick = { navManager.navigateTo(Screen.GameModes) },
            onGameModeClick = { mode ->
                navManager.setGameMode(mode)
                navManager.navigateTo(Screen.GameModes)
            },
            onRematchClick = startNewGame,
            onTournamentClick = { navManager.navigateTo(Screen.TournamentSetup) },
            onResumeTournamentClick = { navManager.navigateTo(Screen.TournamentView) },
            onPcConnect = { navManager.navigateTo(Screen.PcIntegration) },
            onHostOverviewClick = { showHostOverviewFromStart = true },
            onSettingsClick = { navManager.navigateTo(Screen.Settings) },
            onManagePlayersClick = { navManager.navigateTo(Screen.PlayerManagement) }
        )
    }

    // Inhalt eines Screens; Desktop-Hülle und Übergänge liegen darum herum
    val screenContent: @Composable (Screen) -> Unit = { screen ->
                when (screen) {
                Screen.Start -> startScreen()
                Screen.GameModes -> AdaptiveContainer { windowInfo ->
                    if (windowInfo.isExpanded) {
                        DesktopGamesScreen(
                            initialMode = navManager.selectedGameMode,
                            repository = repository,
                            onModeSelected = { mode -> navManager.setGameMode(mode) },
                            onStartGame = startFromSetup
                        )
                    } else {
                        GameModesScreen(
                            onGameModeSelected = { mode ->
                                navManager.setGameMode(mode)
                                navManager.navigateTo(Screen.PlayerSetup)
                            },
                            onBack = { navManager.goBack() }
                        )
                    }
                }
                Screen.PlayerSetup -> PlayerSetupScreen(
                    selectedGameMode = navManager.selectedGameMode ?: GameMode.X501,
                    repository = repository,
                    onBack = { navManager.goBack() },
                    onStartGame = startFromSetup,
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
                else -> startScreen()
            }
    }

    MyApplicationTheme {
      ProvideAdaptiveScale(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .ditheredBackground()
                .edgeSwipeBack(
                    enabled = canGoBack,
                    onBack = handleBack
                )
        ) {
            AdaptiveContainer(modifier = Modifier.fillMaxSize()) { windowInfo ->
                val currentScreen = navManager.currentScreen
                // Auf großen Fenstern liegen alle Bereiche der Navigationsleiste in einer festen Hülle;
                // der äußere Übergang sieht sie als einen Zustand, nur der Inhalt darin wechselt weich.
                val inDesktopShell = windowInfo.isExpanded && currentScreen.desktopDestination() != null
                AnimatedContent(
                    targetState = if (inDesktopShell) DesktopShellKey else currentScreen,
                    transitionSpec = {
                        val isBackward = navManager.navigationDirection == NavigationDirection.Backward ||
                                targetState == Screen.Start || targetState === DesktopShellKey
                        val slideOffset: (Int) -> Int = { fullWidth -> (fullWidth * 0.15f).toInt() }
                        if (isBackward) {
                            (slideInHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetX = { -slideOffset(it) }
                            ) + fadeIn(tween(220))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                                        targetOffsetX = { slideOffset(it) }
                                    ) + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetX = { slideOffset(it) }
                            ) + fadeIn(tween(220))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                                        targetOffsetX = { -slideOffset(it) }
                                    ) + fadeOut(tween(180)))
                        }
                    }
                ) { key ->
                    if (key === DesktopShellKey) {
                        // Beim Verlassen der Hülle (z. B. nach Einstellungen) bleibt zuletzt gezeigte Seite stehen
                        val shownScreen = navManager.currentScreen
                            .takeIf { it.desktopDestination() != null } ?: lastShellScreen.screen
                        lastShellScreen.screen = shownScreen
                        DesktopScaffold(
                            selected = shownScreen.desktopDestination() ?: DesktopDestination.OVERVIEW,
                            onNavigate = navigateTopLevel
                        ) {
                            AnimatedContent(
                                targetState = shownScreen,
                                transitionSpec = {
                                    fadeIn(tween(160, delayMillis = 90)) togetherWith fadeOut(tween(90))
                                }
                            ) { screen -> screenContent(screen) }
                        }
                    } else {
                        screenContent(key as Screen)
                    }
                }
            }



            // Host-Übersicht geöffnet aus dem Startmenü
            if (showHostOverviewFromStart) {
                HostOverviewDialog(
                    sessionManager = sessionManager,
                    onDismissRequest = { showHostOverviewFromStart = false },
                    vm = vm
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
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.sdart.app.ui.theme.PrimaryOrange)
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

            if (showSplashScreen) {
                SplashScreen(
                    onTimeout = { showSplashScreen = false }
                )
            }
        }
      }
    }
}

/** Zustand für alle Screens, die auf großen Fenstern in der Desktop-Hülle liegen. */
private data object DesktopShellKey

/** Merkt sich die zuletzt gezeigte Seite der Desktop-Hülle, damit sie beim Ausblenden stehen bleibt. */
private class ShellScreenMemory {
    var screen: Screen = Screen.Start
}

/** Bereich der Navigationsleiste, zu dem der Screen gehört; null für Screens ohne Leiste (z. B. das Spiel). */
private fun Screen.desktopDestination(): DesktopDestination? = when (this) {
    Screen.Start -> DesktopDestination.OVERVIEW
    Screen.GameModes -> DesktopDestination.GAMES
    Screen.TournamentSetup, Screen.TournamentView -> DesktopDestination.TOURNAMENT
    Screen.PlayerManagement -> DesktopDestination.PLAYERS
    Screen.PcIntegration -> DesktopDestination.DEVICES
    Screen.Settings -> DesktopDestination.SETTINGS
    else -> null
}
