package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.runtime.Composable
import com.sdart.app.connect.SessionManager
import com.sdart.app.ui.LocalCompactLayout

@Composable
fun StartScreen(
    vm: DartViewModel,
    repository: PlayerRepository,
    sessionManager: SessionManager,
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
    // Auf großen Fenstern kommen Navigationsleiste und Hintergrund von DesktopScaffold in App.kt,
    // damit sie beim Seitenwechsel stehen bleiben; schmale Fenster zeigen das Dashboard einspaltig
    DesktopDashboard(
        vm = vm,
        repository = repository,
        sessionManager = sessionManager,
        onGamesClick = onGamesClick,
        onGameModeClick = onGameModeClick,
        onRematchClick = onRematchClick,
        onTournamentClick = onTournamentClick,
        onResumeTournamentClick = onResumeTournamentClick,
        onPcConnect = onPcConnect,
        onHostOverviewClick = onHostOverviewClick,
        onManagePlayersClick = onManagePlayersClick,
        onSettingsClick = onSettingsClick,
        compact = LocalCompactLayout.current
    )
}
