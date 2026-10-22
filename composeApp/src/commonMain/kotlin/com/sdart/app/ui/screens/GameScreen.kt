package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.sdart.app.domain.calculateDartScore
import com.sdart.app.domain.CHECKOUT_PATHS
import com.sdart.app.domain.BOGEY_NUMBERS
import com.sdart.app.ui.game.InteractiveDartboard
import com.sdart.app.ui.game.MagnifierOverlay
import com.sdart.app.ui.game.ScoreInputSection
import com.sdart.app.ui.game.ActionButtonsSection
import com.sdart.app.ui.game.RoundCompletionAnimation
import com.sdart.app.ui.game.AllFivesRoundAnimation
import com.sdart.app.ui.game.PlayerStatsDialog
import com.sdart.app.ui.game.PlayerScoreRow
import com.sdart.app.ui.game.DesktopCurrentPlayerCard
import com.sdart.app.ui.game.DesktopPlayerScoreboardTable
import com.sdart.app.ui.game.DesktopCurrentTurnPanel
import com.sdart.app.ui.game.CricketScoreTable
import com.sdart.app.ui.game.AllFivesScoreTable
import com.sdart.app.ui.game.GameHeader
import com.sdart.app.connect.DeviceRole
import com.sdart.app.connect.SessionManager
import com.sdart.app.connect.HostState
import com.sdart.app.connect.ui.HostOverviewDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import com.sdart.app.ui.AdaptiveContainer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sdart.app.ui.HeatmapDartboard
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.GlassBorder
import com.sdart.app.ui.theme.GlassWhite
import com.sdart.app.ui.theme.PrimaryOrange
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*
import kotlin.math.*

// --- FARBDEFINITIONEN FÜR DIE DARTSCHEIBE ---
val dartboardBeige = Color(0xFFF5DEB3)
val dartboardBlack = Color.Black
val dartboardRed = Color(0xFFD32F2F)
val dartboardGreen = Color(0xFF388E3C)


@Composable
fun GameScreen(
    vm: DartViewModel,
    repository: PlayerRepository? = null,
    navigationManager: NavigationManager? = null,
    sessionManager: SessionManager? = null,
    onExit: () -> Unit = {},
    onGameFinished: () -> Unit = {}
) {
    val game = vm.game
    var showCamera by remember { mutableStateOf(false) }
    var showHostOverviewDialog by remember { mutableStateOf(false) }
    var dartboardBounds by remember { mutableStateOf<Rect?>(null) }
    var dartboardBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    val autoDetectionSystem = remember { AutoDetectionSystem() }
    val calibrationSystem = remember { DartboardCalibrationSystem() }
    
    val currentRepo = repository ?: remember { InMemoryPlayerRepository() }
    var statisticsUpdated by remember { mutableStateOf(false) }
    var shownFinishedPlayers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val hostState by (sessionManager?.hostState?.collectAsState() ?: remember { mutableStateOf(null) })
    val isHostActive = hostState is HostState.Active
    val hasConnectedCamera = (hostState as? HostState.Active)?.participants?.any { it.role == DeviceRole.CAMERA } == true
    val isCameraConnected = isHostActive && hasConnectedCamera
    val shouldKeepCameraRunning = showCamera || isHostActive || isCameraConnected

    val handleBack: () -> Unit = {
        if (navigationManager != null) {
            navigationManager.goBack()
        } else {
            showExitDialog = true
        }
    }
    
    val isDark = true
    
    // Prüfe ob ein Spieler gerade fertig geworden ist
    val finishedPlayers = game?.players?.filter { 
        when (game.mode) {
            GameMode.CRICKET -> it.hasAllSegmentsClosed()
            GameMode.ALL_FIVES -> false
            GameMode.AROUND_THE_CLOCK -> it.atcCurrentTarget == 0
            else -> it.currentScore == 0
        }
    } ?: emptyList()
    
    // Prüfe ob alle Spieler fertig sind
    val allPlayersFinished = game?.let { 
        when (it.mode) {
            GameMode.CRICKET -> it.players.all { player -> player.hasAllSegmentsClosed() }
            GameMode.ALL_FIVES -> it.isFinished
            GameMode.AROUND_THE_CLOCK -> it.players.all { player -> player.atcCurrentTarget == 0 }
            else -> it.players.all { player -> player.currentScore == 0 }
        }
    } ?: false
    
    val newlyFinishedPlayers = finishedPlayers.filter { it.name !in shownFinishedPlayers }
    
    LaunchedEffect(newlyFinishedPlayers.map { it.name }, allPlayersFinished) {
        if (allPlayersFinished) {
            showCompletionDialog = false
            vm.updatePlayerStatisticsAfterGame(currentRepo, forceSave = true)
            onGameFinished()
            navigationManager?.navigateTo(Screen.GameSummary, NavigationDirection.Forward)
            return@LaunchedEffect
        }
        
        val shouldShowDialog = when (game?.mode) {
            GameMode.ALL_FIVES -> false
            else -> newlyFinishedPlayers.isNotEmpty()
        }
        
        if (shouldShowDialog) {
            showCompletionDialog = true
            shownFinishedPlayers = finishedPlayers.map { it.name }.toSet()
        }
    }
    
    LaunchedEffect(game) {
        if (game == null) {
            shownFinishedPlayers = emptySet()
            showCompletionDialog = false
        }
    }
    
    val winnerText = when {
        allPlayersFinished && game?.mode == GameMode.ALL_FIVES -> {
            val maxWonLegs = game.players.maxOfOrNull { it.wonLegs } ?: 0
            val winners = game.players.filter { it.wonLegs == maxWonLegs }
            if (winners.size > 1) {
                "Unentschieden: ${winners.joinToString(", ") { it.name }}"
            } else {
                "${winners.firstOrNull()?.name ?: "Unbekannt"} hat gewonnen!"
            }
        }
        allPlayersFinished && finishedPlayers.size > 1 -> {
            "Unentschieden: ${finishedPlayers.joinToString(", ") { it.name }}"
        }
        allPlayersFinished && finishedPlayers.size == 1 -> {
            "${finishedPlayers.first().name} hat gewonnen!"
        }
        newlyFinishedPlayers.isNotEmpty() -> {
            "${newlyFinishedPlayers.joinToString(", ") { it.name }} ${if (newlyFinishedPlayers.size == 1) "hat" else "haben"} gewonnen!"
        }
        finishedPlayers.isNotEmpty() -> {
            "${finishedPlayers.joinToString(", ") { it.name }} ${if (finishedPlayers.size == 1) "hat" else "haben"} gewonnen!"
        }
        else -> ""
    }
    
    LaunchedEffect(game?.isFinished) {
        if (game?.isFinished == true && !statisticsUpdated) {
            statisticsUpdated = true
            vm.updatePlayerStatisticsAfterGame(currentRepo, forceSave = true)
        }
    }
    
    LaunchedEffect(game) {
        if (game?.isFinished != true) {
            statisticsUpdated = false
        }
    }

    if (game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBgGradient),
            contentAlignment = Alignment.Center
        ) {
            Text("Spiel wird geladen...", color = Color.White)
        }
        return
    }

    val currentPlayerIndex = game.currentPlayerIndex.coerceIn(0, game.players.size - 1)
    val currentPlayer = game.players.getOrNull(currentPlayerIndex)
    val currentScore = currentPlayer?.currentScore ?: 0

    var activeStatsPlayer by remember { mutableStateOf<Player?>(null) }

    var showRoundAnimation by remember { mutableStateOf(false) }
    var roundAnimationProgress by remember { mutableStateOf(0f) }
    var lastCompletedPlayerIndex by remember { mutableStateOf(-1) }
    var lastCompletedRoundSum by remember { mutableStateOf(0) }
    var lastCompletedRoundIsValid by remember { mutableStateOf(false) }
    var previousThrowHistorySize by remember { mutableStateOf(game.throwHistory.size) }
    
    LaunchedEffect(showRoundAnimation) {
        vm.isInputBlocked = showRoundAnimation
    }
    
    LaunchedEffect(game.throwHistory.size, game.currentPlayerIndex, game.throwsInRound) {
        val throwHistoryGrew = game.throwHistory.size > previousThrowHistorySize
        
        if (game.mode != GameMode.CRICKET && game.mode != GameMode.AROUND_THE_CLOCK && 
            game.throwsInRound == 0 && game.throwHistory.isNotEmpty() && throwHistoryGrew) {
            val completedRoundSum = vm.getLastCompletedRoundSum()
            
            if (completedRoundSum > 0) {
                lastCompletedPlayerIndex = (game.currentPlayerIndex - 1 + game.players.size) % game.players.size
                lastCompletedRoundSum = completedRoundSum
                lastCompletedRoundIsValid = if (game.mode == GameMode.ALL_FIVES) vm.isLastCompletedRoundValid() else true
                showRoundAnimation = true
            }
        }
        
        previousThrowHistorySize = game.throwHistory.size
    }

    LaunchedEffect(showRoundAnimation) {
        if (showRoundAnimation) {
            for (i in 1..20) {
                roundAnimationProgress = i / 20f
                delay(25)
            }
            delay(2000)
            for (i in 20 downTo 0) {
                roundAnimationProgress = i / 20f
                delay(25)
            }
            showRoundAnimation = false
        } else {
            roundAnimationProgress = 0f
        }
    }

    AdaptiveContainer(modifier = Modifier.fillMaxSize()) { windowInfo ->
        val isWidescreen = windowInfo.isWidescreen
        val isAnyDialogOpen = (showCompletionDialog && winnerText.isNotEmpty()) ||
                activeStatsPlayer != null ||
                showExitDialog ||
                (navigationManager?.showGameExitDialog == true) ||
                (showHostOverviewDialog && sessionManager != null) ||
                vm.isCameraSourceDialogOpen

        val blurRadius = if (isAnyDialogOpen) {
            20.dp
        } else if (roundAnimationProgress > 0f) {
            (20.dp * roundAnimationProgress)
        } else {
            0.dp
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            ) {
                if (isWidescreen) {
                    // ==========================================
                    // DESKTOP / TABLET WIDESCREEN (Side-by-Side)
                    // ==========================================
                    Box(modifier = Modifier.fillMaxSize().background(DarkBgGradient)) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Linker Bereich: Dartboard & Live-Kamera Feed (50/50 Aufteilung)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                    .then(
                                        if (showCamera && !showRoundAnimation) {
                                            Modifier
                                                .pointerInput(Unit) {
                                                    detectTransformGestures { _, _, zoom, _ ->
                                                        if (zoom != 1f) {
                                                            if (vm.onPinchZoom != null) {
                                                                vm.onPinchZoom?.invoke(zoom)
                                                            } else {
                                                                val currentRatio = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) vm.cameraZoomRatio else (1f + vm.cameraZoomLevel * 3f)
                                                                val newScale = (currentRatio * zoom).coerceIn(1f, 4f)
                                                                val newZoom = (newScale - 1f) / 3f
                                                                vm.cameraZoomLevel = newZoom
                                                                vm.cameraZoomRatio = newScale
                                                            }
                                                        }
                                                    }
                                                }
                                                .pointerInput(Unit) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {
                                                                val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                                if (scrollY != 0f) {
                                                                    event.changes.forEach { it.consume() }
                                                                    val zoomMultiplier = if (scrollY < 0f) 1.15f else 0.87f
                                                                    if (vm.onPinchZoom != null) {
                                                                        vm.onPinchZoom?.invoke(zoomMultiplier)
                                                                    } else {
                                                                        val currentRatio = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) vm.cameraZoomRatio else (1f + vm.cameraZoomLevel * 3f)
                                                                        val newScale = (currentRatio * zoomMultiplier).coerceIn(1f, 4f)
                                                                        val newZoom = (newScale - 1f) / 3f
                                                                        vm.cameraZoomLevel = newZoom
                                                                        vm.cameraZoomRatio = newScale
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (shouldKeepCameraRunning) {
                                    CameraBackground(
                                        modifier = Modifier.fillMaxSize(),
                                        vm = vm,
                                        showCamera = showCamera,
                                        showOverlay = showCamera,
                                        dartboardBounds = dartboardBounds,
                                        sessionManager = sessionManager,
                                        autoDetectionSystem = autoDetectionSystem,
                                        calibrationSystem = calibrationSystem
                                    )
                                }

                                ScoreInputSection(
                                    game = game,
                                    currentScore = currentScore,
                                    onScoreEntered = { score -> 
                                        if (!showRoundAnimation) {
                                            vm.enterScore(score) 
                                        }
                                    },
                                    showCamera = showCamera,
                                    isCameraConnected = isCameraConnected,
                                    vm = vm,
                                    dartboardBounds = dartboardBounds,
                                    onDartboardBoundsChanged = { bounds ->
                                        dartboardBounds = bounds
                                    },
                                    onDartboardBoundsInRootChanged = { boundsInRoot ->
                                        dartboardBoundsInRoot = boundsInRoot
                                    },
                                    enabled = !showRoundAnimation,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Zurück-Button direkt in der Dartboard-Kachel
                                IconButton(
                                    onClick = handleBack,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .size(46.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        .border(1.dp, GlassBorder, CircleShape)
                                        .zIndex(100f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Spiel beenden",
                                        tint = Color.White
                                    )
                                }

                                // Companion/Host-Button direkt in der Dartboard-Kachel
                                if (sessionManager != null) {
                                    val participantCount = (hostState as? HostState.Active)?.participants?.size ?: 0

                                    IconButton(
                                        onClick = { showHostOverviewDialog = true },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .size(46.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                            .border(
                                                1.dp,
                                                if (participantCount > 0) Color(0xFF4CAF50) else if (isHostActive) PrimaryOrange else GlassBorder,
                                                CircleShape
                                            )
                                            .zIndex(100f)
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (participantCount > 0) {
                                                    Badge(containerColor = Color(0xFF4CAF50)) {
                                                        Text("$participantCount", color = Color.White, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_mobile_connect),
                                                contentDescription = "Geräte-Verbindung",
                                                tint = if (participantCount > 0) Color(0xFF4CAF50) else if (isHostActive) PrimaryOrange else Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Rechter Bereich: Sidebar mit Scores, Stats & Aktionen (50/50 Aufteilung)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (game.mode == GameMode.CRICKET) {
                                        GameHeader(
                                            game = game, 
                                            currentPlayer = currentPlayer, 
                                            currentPlayerIndex = currentPlayerIndex, 
                                            vm = vm,
                                            onPlayerClick = { activeStatsPlayer = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        // 1. Große aktuelle Spieler-Anzeige inkl. dezenter Würfe dazwischen (ohne Kachel)
                                        DesktopCurrentPlayerCard(
                                            game = game,
                                            currentPlayer = currentPlayer,
                                            currentPlayerIndex = currentPlayerIndex,
                                            vm = vm,
                                            onPlayerClick = { activeStatsPlayer = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        HorizontalDivider(
                                            color = Color.White.copy(alpha = 0.12f),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        // 2. Alle Spieler als Liste untereinander mit Spalten (ohne Kachel)
                                        DesktopPlayerScoreboardTable(
                                            game = game,
                                            currentPlayer = currentPlayer,
                                            currentPlayerIndex = currentPlayerIndex,
                                            vm = vm,
                                            onPlayerClick = { activeStatsPlayer = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                ActionButtonsSection(
                                    onCameraClick = { 
                                        if (!showRoundAnimation) {
                                            showCamera = !showCamera 
                                        }
                                    },
                                    onUndo = { 
                                        if (!showRoundAnimation) {
                                            vm.undo() 
                                        }
                                    },
                                    enabled = !showRoundAnimation,
                                    showCamera = showCamera
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // MOBILE PORTRAIT (Vertikaler Stack)
                    // ==========================================
                    if (shouldKeepCameraRunning) {
                        CameraBackground(
                            modifier = Modifier.fillMaxSize(),
                            vm = vm,
                            showCamera = showCamera,
                            showOverlay = showCamera,
                            dartboardBounds = dartboardBoundsInRoot,
                            sessionManager = sessionManager,
                            autoDetectionSystem = autoDetectionSystem,
                            calibrationSystem = calibrationSystem
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(DarkBgGradient))
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        GameHeader(
                            game = game, 
                            currentPlayer = currentPlayer, 
                            currentPlayerIndex = currentPlayerIndex, 
                            vm = vm,
                            onPlayerClick = { activeStatsPlayer = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .then(
                                    if (showCamera && !showRoundAnimation) {
                                        Modifier
                                            .pointerInput(Unit) {
                                                detectTransformGestures { _, _, zoom, _ ->
                                                    if (zoom != 1f) {
                                                        if (vm.onPinchZoom != null) {
                                                            vm.onPinchZoom?.invoke(zoom)
                                                        } else {
                                                            val currentRatio = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) vm.cameraZoomRatio else (1f + vm.cameraZoomLevel * 3f)
                                                            val newScale = (currentRatio * zoom).coerceIn(1f, 4f)
                                                            val newZoom = (newScale - 1f) / 3f
                                                            vm.cameraZoomLevel = newZoom
                                                            vm.cameraZoomRatio = newScale
                                                        }
                                                    }
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Scroll) {
                                                            val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                            if (scrollY != 0f) {
                                                                event.changes.forEach { it.consume() }
                                                                val zoomMultiplier = if (scrollY < 0f) 1.15f else 0.87f
                                                                if (vm.onPinchZoom != null) {
                                                                    vm.onPinchZoom?.invoke(zoomMultiplier)
                                                                } else {
                                                                    val currentRatio = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) vm.cameraZoomRatio else (1f + vm.cameraZoomLevel * 3f)
                                                                    val newScale = (currentRatio * zoomMultiplier).coerceIn(1f, 4f)
                                                                    val newZoom = (newScale - 1f) / 3f
                                                                    vm.cameraZoomLevel = newZoom
                                                                    vm.cameraZoomRatio = newScale
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ScoreInputSection(
                                game = game,
                                currentScore = currentScore,
                                onScoreEntered = { score -> 
                                    if (!showRoundAnimation) {
                                        vm.enterScore(score) 
                                    }
                                },
                                showCamera = showCamera,
                                isCameraConnected = isCameraConnected,
                                vm = vm,
                                dartboardBounds = dartboardBounds,
                                onDartboardBoundsChanged = { bounds ->
                                    dartboardBounds = bounds
                                },
                                onDartboardBoundsInRootChanged = { boundsInRoot ->
                                    dartboardBoundsInRoot = boundsInRoot
                                },
                                enabled = !showRoundAnimation,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Zurück-Button direkt in der Dartboard-Kachel
                            IconButton(
                                onClick = handleBack,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .size(46.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .zIndex(100f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Spiel beenden",
                                    tint = Color.White
                                )
                            }

                            // Companion/Host-Button direkt in der Dartboard-Kachel (Mobile)
                            if (sessionManager != null) {
                                val participantCount = (hostState as? HostState.Active)?.participants?.size ?: 0

                                IconButton(
                                    onClick = { showHostOverviewDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .size(46.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        .border(
                                            1.dp,
                                            if (participantCount > 0) Color(0xFF4CAF50) else if (isHostActive) PrimaryOrange else GlassBorder,
                                            CircleShape
                                        )
                                        .zIndex(100f)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (participantCount > 0) {
                                                Badge(containerColor = Color(0xFF4CAF50)) {
                                                    Text("$participantCount", color = Color.White, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_mobile_connect),
                                            contentDescription = "Geräte-Verbindung",
                                            tint = if (participantCount > 0) Color(0xFF4CAF50) else if (isHostActive) PrimaryOrange else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ActionButtonsSection(
                                onCameraClick = { 
                                    if (!showRoundAnimation) {
                                        showCamera = !showCamera 
                                    }
                                },
                                onUndo = { 
                                    if (!showRoundAnimation) {
                                        vm.undo() 
                                    }
                                },
                                enabled = !showRoundAnimation,
                                showCamera = showCamera
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

        // Interaktive Zoom-Steuerung ([-], [ 1.0x ], [+])
        // Erlaubt direktes Zoomen per Klick, Scrollrad oder Gesten; Klick auf den Text setzt auf 1.0x zurück
        val currentDisplayZoom = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) {
            vm.cameraZoomRatio
        } else if (vm.cameraZoomLevel > 0.01f) {
            1.0f + vm.cameraZoomLevel * 3.0f
        } else {
            1.0f
        }
        val isZoomActive = kotlin.math.abs(currentDisplayZoom - 1.0f) > 0.05f

        if (showCamera && !isAnyDialogOpen) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (isWidescreen) 0.5f else 1.0f)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = if (isWidescreen) 48.dp else 96.dp)
                    .zIndex(20000f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.8f),
                    border = BorderStroke(
                        1.5.dp,
                        if (isZoomActive) PrimaryOrange.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f)
                    ),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        // Zoom Out (-) Button
                        IconButton(
                            onClick = {
                                val currentScale = currentDisplayZoom
                                val newScale = (currentScale - 0.2f).coerceIn(1.0f, 4.0f)
                                if (vm.onPinchZoom != null) {
                                    val factor = newScale / currentScale
                                    vm.onPinchZoom?.invoke(factor)
                                } else {
                                    vm.cameraZoomRatio = newScale
                                    vm.cameraZoomLevel = (newScale - 1f) / 3f
                                }
                            },
                            enabled = currentDisplayZoom > 1.01f,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_minus),
                                contentDescription = "Zoom verkleinern",
                                tint = if (currentDisplayZoom > 1.01f) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Zoom-Wert Pill (Klick setzt auf 1.0x zurück)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isZoomActive) PrimaryOrange.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    vm.resetZoom()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${currentDisplayZoom.format(1)}x",
                                color = if (isZoomActive) PrimaryOrange else Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        // Zoom In (+) Button
                        IconButton(
                            onClick = {
                                val currentScale = currentDisplayZoom
                                val newScale = (currentScale + 0.2f).coerceIn(1.0f, 4.0f)
                                if (vm.onPinchZoom != null) {
                                    val factor = newScale / currentScale
                                    vm.onPinchZoom?.invoke(factor)
                                } else {
                                    vm.cameraZoomRatio = newScale
                                    vm.cameraZoomLevel = (newScale - 1f) / 3f
                                }
                            },
                            enabled = currentDisplayZoom < 3.99f,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_plus),
                                contentDescription = "Zoom vergrößern",
                                tint = if (currentDisplayZoom < 3.99f) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Runden-Abschluss Animation
        if (showRoundAnimation && lastCompletedPlayerIndex >= 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            showRoundAnimation = false
                        }
                    }
                    .zIndex(10000f)
            ) {
                if (game.mode == GameMode.ALL_FIVES) {
                    AllFivesRoundAnimation(
                        isVisible = showRoundAnimation,
                        roundSum = lastCompletedRoundSum,
                        isValid = lastCompletedRoundIsValid,
                        animationProgress = roundAnimationProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    RoundCompletionAnimation(
                        isVisible = showRoundAnimation,
                        roundSum = lastCompletedRoundSum,
                        playerName = "",
                        animationProgress = roundAnimationProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // Modal-Hintergrund-Abdunkelung (Scrim) bei geöffneten Dialogen
        // Auf Desktop haben AlertDialogs keine native Abdunkelung; dieser Scrim sorgt zusammen mit dem Blur für perfekten Kontrast
        if (isAnyDialogOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .zIndex(8000f)
            )
        }

        // Spielabschluss-Dialog
        if (showCompletionDialog && winnerText.isNotEmpty()) {
            GameCompletionDialog(
                winnerName = winnerText,
                isDark = isDark,
                showContinueOption = !allPlayersFinished,
                onContinuePlaying = {
                    showCompletionDialog = false
                    vm.continueGame()
                },
                onViewStatistics = {
                    showCompletionDialog = false
                    vm.updatePlayerStatisticsAfterGame(currentRepo, forceSave = true)
                    onGameFinished()
                    navigationManager?.navigateTo(Screen.GameSummary, NavigationDirection.Forward)
                }
            )
        }

        activeStatsPlayer?.let { statsPlayer ->
            val isCurrentPlayerStats = statsPlayer == currentPlayer
            val isWinnerStats = when (game.mode) {
                GameMode.AROUND_THE_CLOCK -> statsPlayer.atcCurrentTarget == 0
                GameMode.ALL_FIVES -> false
                else -> statsPlayer.currentScore == 0
            }
            PlayerStatsDialog(
                player = statsPlayer,
                isCurrentPlayer = isCurrentPlayerStats,
                isWinner = isWinnerStats,
                gameMode = game.mode,
                isDarkTheme = isDark,
                onDismiss = { activeStatsPlayer = null }
            )
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Spiel beenden?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Möchtest du das aktuelle Spiel wirklich beenden und zum Hauptmenü zurückkehren?", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            onExit()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Beenden", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showExitDialog = false }) {
                        Text("Weiterspielen", color = Color.White)
                    }
                },
                containerColor = Color(0xFF262626),
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showHostOverviewDialog && sessionManager != null) {
            HostOverviewDialog(
                sessionManager = sessionManager,
                onDismissRequest = { showHostOverviewDialog = false },
                vm = vm
            )
        }
    }
    }
}

