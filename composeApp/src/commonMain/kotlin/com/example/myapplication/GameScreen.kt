package com.example.myapplication

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
import com.example.myapplication.connect.DeviceRole
import com.example.myapplication.connect.SessionManager
import com.example.myapplication.connect.HostState
import com.example.myapplication.connect.ui.HostOverviewDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.myapplication.ui.AdaptiveContainer
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
import com.example.myapplication.ui.HeatmapDartboard
import com.example.myapplication.ui.theme.DarkBgGradient
import com.example.myapplication.ui.theme.GlassBorder
import com.example.myapplication.ui.theme.GlassWhite
import com.example.myapplication.ui.theme.PrimaryOrange
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

// --- DART-CHECKOUT-KONSTANTEN ---
val BOGEY_NUMBERS = setOf(169, 168, 166, 165, 163, 162, 159)

val CHECKOUT_PATHS = mapOf(
    170 to listOf("T20 T20 Bull"),
    167 to listOf("T20 T19 Bull"),
    164 to listOf("T20 T18 Bull"),
    161 to listOf("T20 T17 Bull"),
    160 to listOf("T20 T20 D20"),
    158 to listOf("T20 T18 D22"),
    157 to listOf("T20 T19 D20"),
    155 to listOf("T20 T19 D19"),
    154 to listOf("T20 T18 D20"),
    152 to listOf("T20 T16 D22"),
    151 to listOf("T20 T17 D20"),
    149 to listOf("T20 T19 D16"),
    148 to listOf("T20 T16 D20"),
    146 to listOf("T20 T18 D16"),
    145 to listOf("T20 T15 D20"),
    143 to listOf("T20 T17 D16"),
    142 to listOf("T20 T14 D20"),
    140 to listOf("T20 T20 D20"),
    139 to listOf("T19 T14 D20"),
    137 to listOf("T20 T19 D10"),
    136 to listOf("T20 T20 D8"),
    134 to listOf("T20 T14 D20"),
    133 to listOf("T20 T19 D8"),
    131 to listOf("T20 T17 D10"),
    130 to listOf("T20 T20 D5"),
    128 to listOf("T18 T14 D16"),
    127 to listOf("T20 T17 D8"),
    125 to listOf("T20 T19 D4"),
    124 to listOf("T20 T16 D8"),
    122 to listOf("T18 T18 D7"),
    121 to listOf("T20 T11 D14"),
    120 to listOf("T20 20 D20"),
    118 to listOf("T20 18 D20"),
    116 to listOf("T20 16 D20"),
    115 to listOf("T20 15 D20"),
    113 to listOf("T20 13 D20"),
    112 to listOf("T20 12 D20"),
    110 to listOf("T20 10 D20"),
    109 to listOf("T20 9 D20"),
    107 to listOf("T19 10 D20"),
    106 to listOf("T20 6 D20"),
    104 to listOf("T18 10 D20"),
    103 to listOf("T20 3 D20"),
    101 to listOf("T20 1 D20"),
    100 to listOf("T20 D20"),
    98 to listOf("T20 D19"),
    96 to listOf("T20 D18"),
    94 to listOf("T20 D17"),
    92 to listOf("T20 D16"),
    90 to listOf("T20 D15"),
    88 to listOf("T20 D14"),
    86 to listOf("T20 D13"),
    84 to listOf("T20 D12"),
    82 to listOf("T20 D11"),
    80 to listOf("T20 D10"),
    78 to listOf("T20 D9"),
    76 to listOf("T20 D8"),
    74 to listOf("T20 D7"),
    72 to listOf("T20 D6"),
    70 to listOf("T20 D5"),
    68 to listOf("T20 D4"),
    66 to listOf("T20 D3"),
    64 to listOf("T20 D2"),
    62 to listOf("T20 D1"),
    60 to listOf("T20"),
    58 to listOf("T18 D2"),
    56 to listOf("T16 D4"),
    54 to listOf("T18 D0"),
    52 to listOf("T12 D8"),
    50 to listOf("Bull"),
    48 to listOf("T16"),
    46 to listOf("T14 D2"),
    44 to listOf("T12 D4"),
    42 to listOf("T10 D6"),
    40 to listOf("D20"),
    38 to listOf("D19"),
    36 to listOf("D18"),
    34 to listOf("D17"),
    32 to listOf("D16"),
    30 to listOf("D15"),
    28 to listOf("D14"),
    26 to listOf("D13"),
    24 to listOf("D12"),
    22 to listOf("D11"),
    20 to listOf("D10"),
    18 to listOf("D9"),
    16 to listOf("D8"),
    14 to listOf("D7"),
    12 to listOf("D6"),
    10 to listOf("D5"),
    8 to listOf("D4"),
    6 to listOf("D3"),
    4 to listOf("D2"),
    2 to listOf("D1")
)

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

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val isCompleted = showCompletionDialog && winnerText.isNotEmpty()
                        val isStatsOpen = activeStatsPlayer != null
                        if (roundAnimationProgress > 0f || isCompleted || isStatsOpen) {
                            val blurPx = if (isCompleted || isStatsOpen) 30f else (30f * roundAnimationProgress)
                            renderEffect = BlurEffect(
                                radiusX = blurPx,
                                radiusY = blurPx,
                                edgeTreatment = TileMode.Clamp
                            )
                        }
                    }
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
                                    .padding(8.dp)
                                    .then(
                                        if (showCamera && !showRoundAnimation) {
                                            Modifier.pointerInput(Unit) {
                                                detectTransformGestures { _, _, zoom, _ ->
                                                    if (zoom != 1f) {
                                                        if (vm.onPinchZoom != null) {
                                                            vm.onPinchZoom?.invoke(zoom)
                                                        } else {
                                                            val currentZoom = vm.cameraZoomLevel
                                                            val currentScale = 1f + currentZoom * 3f
                                                            val newScale = (currentScale * zoom).coerceIn(1f, 4f)
                                                            val newZoom = (newScale - 1f) / 3f
                                                            vm.cameraZoomLevel = newZoom
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
                                        sessionManager = sessionManager
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
                            sessionManager = sessionManager
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
                                        Modifier.pointerInput(Unit) {
                                            detectTransformGestures { _, _, zoom, _ ->
                                                if (zoom != 1f) {
                                                    if (vm.onPinchZoom != null) {
                                                        vm.onPinchZoom?.invoke(zoom)
                                                    } else {
                                                        val currentZoom = vm.cameraZoomLevel
                                                        val currentScale = 1f + currentZoom * 3f
                                                        val newScale = (currentScale * zoom).coerceIn(1f, 4f)
                                                        val newZoom = (newScale - 1f) / 3f
                                                        vm.cameraZoomLevel = newZoom
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

        // Zoom-Indikator
        val isZoomActive = kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f || vm.cameraZoomLevel > 0.01f
        if (showCamera && isZoomActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .then(
                        if (isWidescreen) {
                            Modifier.fillMaxWidth(0.5f).padding(bottom = 24.dp)
                        } else {
                            Modifier.padding(bottom = 96.dp)
                        }
                    )
                    .zIndex(20000f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                        .border(1.5.dp, PrimaryOrange.copy(alpha = 0.85f), CircleShape)
                        .clickable {
                            vm.resetZoom()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    val zoomText = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) {
                        "${vm.cameraZoomRatio.format(1)}x"
                    } else {
                        val zoomFactor = 1.0f + vm.cameraZoomLevel * 3.0f
                        "${zoomFactor.format(1)}x"
                    }
                    Text(
                        text = zoomText,
                        color = PrimaryOrange,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
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
                onDismissRequest = { showHostOverviewDialog = false }
            )
        }
    }
    }
}

@Composable
fun GameHeader(
    game: Game, 
    currentPlayer: Player?, 
    currentPlayerIndex: Int, 
    vm: DartViewModel,
    onPlayerClick: (Player) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxWidth().padding(16.dp)
) {
    val isDarkTheme = true

    if (game.mode == GameMode.CRICKET) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF262626) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (game.totalLegs > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Leg ${game.currentLeg}/${game.totalLegs}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (game.players.size == 1) "Legs: ${game.totalLegs}" else "Best of ${game.totalLegs}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 1.dp
                )
            }
            
            key(game.players.map { it.name }.joinToString()) {
                CricketScoreTable(
                    players = game.players,
                    currentPlayerIndex = currentPlayerIndex,
                    totalLegs = game.totalLegs,
                    modifier = Modifier.fillMaxWidth(),
                    isDarkTheme = isDarkTheme
                )
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF262626) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                currentPlayer?.let { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            
                            val checkoutInfo = if (game.mode == GameMode.ALL_FIVES || game.mode == GameMode.AROUND_THE_CLOCK) null else when {
                                player.currentScore == 0 -> "🏆 Spiel gewonnen!"
                                player.currentScore in CHECKOUT_PATHS.keys -> {
                                    val paths = CHECKOUT_PATHS[player.currentScore]
                                    "Checkout: ${paths?.firstOrNull() ?: ""}"
                                }
                                player.currentScore <= 40 -> "Einfacher Checkout möglich"
                                else -> null
                            }
                            
                            checkoutInfo?.let { info ->
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = when {
                                            player.currentScore == 0 -> Color(0xFF4CAF50)
                                            player.currentScore in CHECKOUT_PATHS.keys -> Color(0xFF448AFF)
                                            player.currentScore <= 40 -> Color(0xFF4CAF50)
                                            else -> Color.LightGray
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            
                            if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                                Text(
                                    text = "Ø ${avgHits.format(2)} Treffer pro Runde",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            } else {
                                val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                                Text(
                                    text = "Ø ${avgScore} Punkte pro Runde",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        
                        if (game.mode == GameMode.AROUND_THE_CLOCK) {
                            com.example.myapplication.ui.ATCMainDisplay(
                                player = player,
                                atcConfig = game.atcConfig,
                                modifier = Modifier
                            )
                        } else if (game.mode == GameMode.ALL_FIVES) {
                            val roundTotal = vm.getCurrentRoundSum()
                            com.example.myapplication.ui.AllFivesMainDisplay(
                                currentRoundTotal = roundTotal,
                                missed = false,
                                modifier = Modifier
                            )
                        } else {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(width = 120.dp, height = 72.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = player.currentScore.toString(),
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(game.players.size) { index ->
                        val player = game.players[index]
                        val isCurrentPlayer = index == currentPlayerIndex
                        val isWinner = when (game.mode) {
                            GameMode.AROUND_THE_CLOCK -> player.atcCurrentTarget == 0
                            GameMode.ALL_FIVES -> false
                            else -> player.currentScore == 0
                        }
                        
                        val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                        val wonLegs = PlayerStatistics.getWonLegs(player, game.mode)
                        
                        Card(
                            modifier = Modifier.clickable { onPlayerClick(player) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isWinner -> Color(0xFFd4edda)
                                    isDarkTheme -> Color(0xFF333333)
                                    else -> Color.White
                                }
                            ),
                            border = when {
                                isWinner -> BorderStroke(2.dp, Color(0xFF28a745))
                                isCurrentPlayer -> BorderStroke(2.dp, Color(0xFFFF9800))
                                else -> null
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isWinner -> Color.Black
                                            isDarkTheme -> Color.White
                                            else -> Color.Black
                                        }
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                        if (player.atcCurrentTarget == 0) "Fin" else player.atcCurrentTarget.toString()
                                    } else if (game.mode == GameMode.ALL_FIVES) {
                                        (player.currentScore / 5).toString()
                                    } else {
                                        player.currentScore.toString()
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isWinner -> Color(0xFF28a745)
                                            else -> Color.White
                                        }
                                    )
                                )
                                
                                val avgText = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                    val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                                    "Ø ${avgHits.format(1)}"
                                } else if (game.mode == GameMode.ALL_FIVES) {
                                    "Ø ${avgScore / 5}"
                                } else {
                                    "Ø $avgScore"
                                }
                                
                                Text(
                                    text = avgText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = when {
                                            isWinner -> Color(0xFF495057)
                                            isDarkTheme -> Color.LightGray
                                            else -> Color(0xFF495057)
                                        },
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                
                                if (wonLegs > 0) {
                                    Text(
                                        text = "🏆 $wonLegs",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = Color(0xFFffc107),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopCurrentPlayerCard(
    game: Game,
    currentPlayer: Player?,
    currentPlayerIndex: Int,
    vm: DartViewModel,
    onPlayerClick: (Player) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        currentPlayer?.let { player ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LINKS: Riesiger Spielername & Stats/Checkout
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onPlayerClick(player) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val checkoutInfo = if (game.mode == GameMode.ALL_FIVES || game.mode == GameMode.AROUND_THE_CLOCK) null else when {
                        player.currentScore == 0 -> "🏆 Spiel gewonnen!"
                        player.currentScore in CHECKOUT_PATHS.keys -> {
                            val paths = CHECKOUT_PATHS[player.currentScore]
                            "Checkout: ${paths?.firstOrNull() ?: ""}"
                        }
                        player.currentScore <= 40 -> "Einfacher Checkout möglich"
                        else -> null
                    }

                    checkoutInfo?.let { info ->
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = when {
                                    player.currentScore == 0 -> Color(0xFF4CAF50)
                                    player.currentScore in CHECKOUT_PATHS.keys -> Color(0xFF448AFF)
                                    player.currentScore <= 40 -> Color(0xFF4CAF50)
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val avgText = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                        val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                        "Ø ${avgHits.format(2)} Treffer/Runde"
                    } else {
                        val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                        "Ø ${avgScore} Pkt./Runde"
                    }

                    Text(
                        text = avgText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // 2. RECHTS: RIESIGE PUNKTE (ohne Kachel)
                if (game.mode == GameMode.AROUND_THE_CLOCK) {
                    com.example.myapplication.ui.ATCMainDisplay(
                        player = player,
                        atcConfig = game.atcConfig,
                        modifier = Modifier
                    )
                } else if (game.mode == GameMode.ALL_FIVES) {
                    val roundTotal = vm.getCurrentRoundSum()
                    com.example.myapplication.ui.AllFivesMainDisplay(
                        currentRoundTotal = roundTotal,
                        missed = false,
                        modifier = Modifier
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = player.currentScore.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 96.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-2).sp
                            )
                        )
                        Text(
                            text = "REST",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopPlayerScoreboardTable(
    game: Game,
    currentPlayer: Player?,
    currentPlayerIndex: Int,
    vm: DartViewModel,
    onPlayerClick: (Player) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
            // Header: Leg / Match Info wenn Legs > 1
            if (game.totalLegs > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Leg ${game.currentLeg}/${game.totalLegs}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = if (game.players.size == 1) "Legs: ${game.totalLegs}" else "Best of ${game.totalLegs}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    )
                }
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Tabellen-Spaltenüberschriften (proportional gewichtet für Desktop)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPIELER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = "SCHNITT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1.1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "LEGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "WÜRFE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "REST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Zeilen für alle Spieler untereinander
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                game.players.forEachIndexed { index, player ->
                    val isCurrentPlayer = index == currentPlayerIndex
                    val isWinner = when (game.mode) {
                        GameMode.AROUND_THE_CLOCK -> player.atcCurrentTarget == 0
                        GameMode.ALL_FIVES -> false
                        else -> player.currentScore == 0
                    }
                    val wonLegs = PlayerStatistics.getWonLegs(player, game.mode)
                    val avgScore = PlayerStatistics.getAverageScorePerRound(player)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(player) },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isWinner -> Color(0xFF2E7D32).copy(alpha = 0.25f)
                            isCurrentPlayer -> PrimaryOrange.copy(alpha = 0.16f)
                            else -> Color.White.copy(alpha = 0.04f)
                        },
                        border = when {
                            isWinner -> BorderStroke(2.dp, Color(0xFF4CAF50))
                            isCurrentPlayer -> BorderStroke(2.dp, PrimaryOrange)
                            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Spalte 1: Spieler Name
                            Column(
                                modifier = Modifier.weight(1.3f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isCurrentPlayer) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = if (isCurrentPlayer) 20.sp else 17.sp,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Spalte 2: DURCHSCHNITT (Checkout, Average)
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val avgText = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                    val avgHits = PlayerStatistics.getAverageHitsPerRound(player, game.atcConfig)
                                    "Ø ${avgHits.format(2)}"
                                } else if (game.mode == GameMode.ALL_FIVES) {
                                    "Ø ${avgScore / 5}"
                                } else {
                                    "Ø $avgScore"
                                }

                                Text(
                                    text = avgText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                val checkoutInfo = if (game.mode == GameMode.ALL_FIVES || game.mode == GameMode.AROUND_THE_CLOCK) null else when {
                                    player.currentScore == 0 -> "🏆 Gewonnen"
                                    player.currentScore in CHECKOUT_PATHS.keys -> {
                                        CHECKOUT_PATHS[player.currentScore]?.firstOrNull()
                                    }
                                    player.currentScore <= 40 -> "Doppel möglich"
                                    else -> null
                                }

                                if (checkoutInfo != null) {
                                    Text(
                                        text = checkoutInfo,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Spalte 3: LEGS
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$wonLegs",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Spalte 4: WÜRFE (Keine Kachel, keine Summe, nur weiße Schrift)
                            val throwsForPlayer: List<DartThrow> = if (isCurrentPlayer) {
                                val count = game.throwsInRound
                                if (count > 0) {
                                    val list = mutableListOf<DartThrow>()
                                    for (i in game.throwHistory.size - 1 downTo 0) {
                                        if (list.size >= count) break
                                        val t = game.throwHistory[i]
                                        if (t.playerIndex == index) {
                                            list.add(0, t)
                                        } else break
                                    }
                                    list
                                } else emptyList()
                            } else {
                                player.dartThrows.takeLast(3)
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                for (dartIndex in 0..2) {
                                    val dt = throwsForPlayer.getOrNull(dartIndex)
                                    val isCurrent = isCurrentPlayer && dartIndex == game.throwsInRound && !game.isFinished

                                    val text = dt?.let {
                                        when {
                                            it.score == 50 -> "BULL"
                                            it.score == 25 -> "25"
                                            it.score == 0 -> "MISS"
                                            it.multiplier == 3 -> "T${it.score / 3}"
                                            it.multiplier == 2 -> "D${it.score / 2}"
                                            else -> "${it.score}"
                                        }
                                    } ?: if (isCurrent) "···" else "—"

                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = if (dt != null || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (dt != null || isCurrent) Color.White else Color.White.copy(alpha = 0.35f)
                                        )
                                    )
                                    if (dartIndex < 2) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                            }

                            // Spalte 5: Restpunkte / Score (nur weiße Schrift)
                            Column(
                                modifier = Modifier.weight(0.8f),
                                horizontalAlignment = Alignment.End
                            ) {
                                val scoreDisplay = if (game.mode == GameMode.AROUND_THE_CLOCK) {
                                    if (player.atcCurrentTarget == 0) "Fin" else "Ziel: ${player.atcCurrentTarget}"
                                } else if (game.mode == GameMode.ALL_FIVES) {
                                    (player.currentScore / 5).toString()
                                } else {
                                    player.currentScore.toString()
                                }

                                Text(
                                    text = scoreDisplay,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = if (isCurrentPlayer) 34.sp else 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun DesktopCurrentTurnPanel(
    game: Game,
    currentPlayer: Player?,
    vm: DartViewModel,
    modifier: Modifier = Modifier
) {
    val throwsInRound = game.throwsInRound
    val currentPlayerIndex = game.currentPlayerIndex

    val currentThrows = remember(game.throwHistory.size, throwsInRound, currentPlayerIndex) {
        val list = mutableListOf<DartThrow>()
        if (throwsInRound > 0) {
            for (i in game.throwHistory.size - 1 downTo 0) {
                if (list.size >= throwsInRound) break
                val t = game.throwHistory[i]
                if (t.playerIndex == currentPlayerIndex) {
                    list.add(0, t)
                } else {
                    break
                }
            }
        }
        list
    }

    val roundSum = currentThrows.sumOf { it.score }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AKTUELLE RUNDE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )
                )
                if (roundSum > 0) {
                    Text(
                        text = "Summe: $roundSum",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (dartIndex in 0..2) {
                    val dartThrow = currentThrows.getOrNull(dartIndex)
                    val isCurrent = dartIndex == throwsInRound && !game.isFinished

                    val text = dartThrow?.let { dt ->
                        when {
                            dt.score == 50 -> "BULL"
                            dt.score == 25 -> "25"
                            dt.score == 0 -> "MISS"
                            dt.multiplier == 3 -> "T${dt.score / 3}"
                            dt.multiplier == 2 -> "D${dt.score / 2}"
                            else -> "${dt.score}"
                        }
                    } ?: if (isCurrent) "..." else "—"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .background(
                                color = when {
                                    dartThrow != null -> Color.White.copy(alpha = 0.12f)
                                    isCurrent -> PrimaryOrange.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.04f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) PrimaryOrange else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Dart ${dartIndex + 1}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = text,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    dartThrow != null -> Color.White
                                    isCurrent -> PrimaryOrange
                                    else -> Color.White.copy(alpha = 0.3f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CricketScoreTable(
    players: List<Player>,
    currentPlayerIndex: Int,
    totalLegs: Int = 1,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean
) {
    val cricketSegments = listOf(15, 16, 17, 18, 19, 20)
    
    val headerBgColor = Color.Transparent
    val headerTextColor = if (isDarkTheme) Color(0xFFe0e0e0) else Color(0xFF495057)
    val dividerColor = if (isDarkTheme) Color(0xFF404040) else Color(0xFFdee2e6)
    val rowBgNormal = Color.Transparent
    val highlightColor = Color(0xFFFF9800)
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spieler",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                ),
                modifier = Modifier.weight(2f)
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            if (totalLegs > 1) {
                Text(
                    text = "Legs",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center
                )
            }
            cricketSegments.forEach { segment ->
                Text(
                    text = segment.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = "Bull",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor
                ),
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.Center
            )
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = dividerColor,
            thickness = 1.dp
        )
        
        players.forEachIndexed { index, player ->
            val isCurrentPlayer = index == currentPlayerIndex
            val textColor = if (isDarkTheme) Color.White else Color(0xFF212529)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .then(
                        if (isCurrentPlayer) {
                            Modifier.border(2.dp, highlightColor, RoundedCornerShape(10.dp))
                        } else Modifier
                    )
                    .background(rowBgNormal, RoundedCornerShape(10.dp))
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = if (isCurrentPlayer) FontWeight.ExtraBold else FontWeight.Medium,
                        color = textColor
                    ),
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = player.currentScore.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                if (totalLegs > 1) {
                    Text(
                        text = player.wonLegs.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = if (player.wonLegs > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (player.wonLegs > 0) Color(0xFF28a745) else textColor
                        ),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                cricketSegments.forEach { segment ->
                    val marks = player.cricketMarks.getOrDefault(segment, 0)
                    val isGloballyClosed = players.all { it.cricketMarks.getOrDefault(segment, 0) >= 3 }
                    val displayText = when {
                        isGloballyClosed -> "●"
                        marks >= 3 -> "⊗"
                        marks == 2 -> "X"
                        marks == 1 -> "/"
                        else -> ""
                    }
                    val displayColor = when {
                        isGloballyClosed || marks >= 2 -> Color.White
                        else -> textColor
                    }
                    val fontWeight = if (isGloballyClosed) FontWeight.Bold else FontWeight.Normal
                    val fontSize = if (isGloballyClosed || marks >= 3) 24.sp else 13.sp
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = displayColor
                        ),
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                val bullMarks = player.cricketMarks.getOrDefault(25, 0)
                val isBullGloballyClosed = players.all { it.cricketMarks.getOrDefault(25, 0) >= 3 }
                val bullDisplayText = when {
                    isBullGloballyClosed -> "●"
                    bullMarks >= 3 -> "⊗"
                    bullMarks == 2 -> "X"
                    bullMarks == 1 -> "/"
                    else -> ""
                }
                val bullDisplayColor = if (isBullGloballyClosed || bullMarks >= 2) Color.White else textColor
                val bullFontWeight = if (isBullGloballyClosed) FontWeight.Bold else FontWeight.Normal
                val bullFontSize = if (isBullGloballyClosed || bullMarks >= 3) 24.sp else 13.sp
                Text(
                    text = bullDisplayText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = bullFontSize,
                        fontWeight = bullFontWeight,
                        color = bullDisplayColor
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center
                )
            }
            if (index < players.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = dividerColor,
                    thickness = 0.5.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun AllFivesScoreTable(
    players: List<Player>,
    currentPlayerIndex: Int,
    targetScore: Int,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val headerBgColor = if (isDarkTheme) Color(0xFF2d2d2d) else Color(0xFFf8f9fa)
    val headerTextColor = if (isDarkTheme) Color(0xFFe0e0e0) else Color(0xFF495057)
    val dividerColor = if (isDarkTheme) Color(0xFF404040) else Color(0xFFdee2e6)
    val idleTextColor = if (isDarkTheme) Color(0xFFd0d0d0) else Color(0xFF495057)

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Spieler", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = headerTextColor), modifier = Modifier.width(250.dp), textAlign = TextAlign.Center)
            Text("Punkte", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = headerTextColor), modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
            Text("Ziel", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = headerTextColor), modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
            Text("Runden", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = headerTextColor), modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
            Text("Legs", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = headerTextColor), modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = dividerColor, thickness = 1.dp)
        
        players.forEachIndexed { index, player ->
            val isCurrentPlayer = index == currentPlayerIndex
            val textColor = if (isCurrentPlayer) MaterialTheme.colorScheme.primary else idleTextColor
            val fontSize = if (isCurrentPlayer) 20.sp else 16.sp
            val fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .then(
                        if (isCurrentPlayer) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier
                    )
                    .background(if (isCurrentPlayer) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (isCurrentPlayer) "● ${player.name}" else player.name, style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize, fontWeight = fontWeight, color = textColor), modifier = Modifier.width(250.dp), textAlign = TextAlign.Center)
                Text(text = (player.currentScore / 5).toString(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize, fontWeight = fontWeight, color = textColor), modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
                Text(text = (targetScore / 5).toString(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize, fontWeight = fontWeight, color = textColor), modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
                Text(text = player.totalRounds.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize, fontWeight = fontWeight, color = textColor), modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
                Text(text = player.wonLegs.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize, fontWeight = fontWeight, color = textColor), modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
            }
            if (index < players.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = Color(0xFFdee2e6), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun PlayerStatsDialog(
    player: Player,
    isCurrentPlayer: Boolean,
    isWinner: Boolean,
    gameMode: GameMode? = null,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    var showFullscreenHeatmap by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color(0xFF2D3748),
                    textAlign = TextAlign.Center
                )
                if (isWinner) {
                    Text(
                        text = "Gewinner! 🎉",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF28a745),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                MainStatCard(
                    title = "Aktueller Score",
                    value = if (isWinner) "0" else player.currentScore.toString(),
                    subtitle = if (isWinner) "Gewonnen!" else "Punkte übrig",
                    color = if (isWinner) Color(0xFF28a745) else PrimaryOrange,
                    icon = if (isWinner) Res.drawable.ic_trophy else Res.drawable.ic_target,
                    isDarkTheme = isDarkTheme,
                    hits = player.dartThrows.map { it.position }.filter { it != Offset.Unspecified },
                    onHeatmapClick = { showFullscreenHeatmap = true }
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val avgScore = PlayerStatistics.getAverageScorePerRound(player)
                    CompactStatCard(
                        title = "Ø Punkte/Runde",
                        value = "$avgScore",
                        color = if (isDarkTheme) Color.White else Color(0xFF495057),
                        icon = Res.drawable.ic_chart_bar,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    val wonLegs = PlayerStatistics.getWonLegs(player, gameMode)
                    CompactStatCard(
                        title = "Gewonnene Legs",
                        value = "$wonLegs",
                        color = Color(0xFFffc107),
                        icon = Res.drawable.ic_trophy,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val highestRoundScore = PlayerStatistics.getHighestRoundScore(player)
                    CompactStatCard(
                        title = "Höchste Runde",
                        value = "$highestRoundScore",
                        color = Color(0xFF17a2b8),
                        icon = Res.drawable.ic_target,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    val highestSingleThrow = if (player.scores.isNotEmpty()) {
                        when {
                            highestRoundScore >= 180 -> 60
                            highestRoundScore >= 140 -> 50
                            highestRoundScore >= 100 -> 40
                            highestRoundScore >= 60 -> 20
                            else -> highestRoundScore / 3
                        }
                    } else 0
                    CompactStatCard(
                        title = "Höchster Wurf",
                        value = "$highestSingleThrow",
                        color = Color(0xFF6f42c1),
                        icon = Res.drawable.ic_lightning,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                val totalRounds = PlayerStatistics.getTotalRounds(player)
                val totalThrows = player.dartThrows.size
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactStatCard(
                        title = "Anzahl Runden",
                        value = "$totalRounds",
                        color = Color(0xFFfd7e14),
                        icon = Res.drawable.ic_refresh,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        title = "Anzahl Würfe",
                        value = "$totalThrows",
                        color = Color(0xFF20c997),
                        icon = Res.drawable.ic_target,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Schließen", fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp), color = Color.White)
            }
        },
        containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(16.dp)
    )
    
    if (showFullscreenHeatmap && player.dartThrows.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showFullscreenHeatmap = false },
            title = {
                Text(
                    text = "Heatmap: ${player.name}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDarkTheme) Color.White else Color(0xFF2D3748)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DartboardHeatmap(
                            dartPositions = player.dartThrows.map { it.position }.filter { it != Offset.Unspecified },
                            modifier = Modifier.fillMaxSize(),
                            showNumbers = true
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${player.dartThrows.size} Darts geworfen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkTheme) Color.LightGray else Color(0xFF6c757d)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFullscreenHeatmap = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Schließen", fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp), color = Color.White)
                }
            },
            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun MainStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: DrawableResource,
    isDarkTheme: Boolean,
    hits: List<Offset>? = null,
    onHeatmapClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) GlassWhite else Color(0xFFf8f9fa)
        ),
        border = if (isDarkTheme) BorderStroke(1.dp, GlassBorder) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title.uppercase(),
                        color = if (isDarkTheme) Color(0xFFAAAAAA) else Color.DarkGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        color = color,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = if (isDarkTheme) androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            ) else null
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        color = if (isDarkTheme) Color.LightGray else Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!hits.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clickable { onHeatmapClick() }
                            .padding(start = 8.dp)
                    ) {
                        HeatmapDartboard(
                            hits = hits,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).padding(start = 8.dp),
                        tint = color.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStatCard(
    title: String,
    value: String,
    color: Color,
    icon: DrawableResource,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) GlassWhite else Color(0xFFf8f9fa)
        ),
        border = if (isDarkTheme) BorderStroke(1.dp, GlassBorder) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.uppercase(),
                    color = if (isDarkTheme) Color(0xFFAAAAAA) else Color.DarkGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = if (isDarkTheme) androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        ) else null
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HighlightStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: DrawableResource,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) GlassWhite else Color(0xFFf8f9fa)
        ),
        border = if (isDarkTheme) BorderStroke(1.dp, GlassBorder) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title.uppercase(),
                        color = if (isDarkTheme) Color(0xFFAAAAAA) else Color.DarkGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            shadow = if (isDarkTheme) androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            ) else null
                        ),
                        maxLines = 1
                    )
                }
                Text(
                    text = subtitle,
                    color = color.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PlayerScoreRow(player: Player, isCurrentPlayer: Boolean, isWinner: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = when {
        isWinner -> Color(0xFFd4edda)
        isCurrentPlayer -> Color(0xFFfff8e1)
        else -> Color.White
    }
    val borderColor = when {
        isWinner -> Color(0xFF28a745)
        isCurrentPlayer -> Color(0xFFffd700)
        else -> Color(0xFFdee2e6)
    }
    val borderWidth = if (isCurrentPlayer || isWinner) 2.dp else 1.dp
    Card(
        modifier = modifier.fillMaxWidth().border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrentPlayer) Text("▶️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                else if (isWinner) Text("🏆", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                else Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Medium,
                        color = Color(0xFF495057)
                    )
                )
            }
            Text(
                text = if (isWinner) "Gewonnen" else player.currentScore.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWinner) Color(0xFF28a745) else PrimaryOrange
                )
            )
        }
    }
}

@Composable
fun RoundCompletionAnimation(
    isVisible: Boolean,
    roundSum: Int,
    playerName: String,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    if (isVisible && animationProgress > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f * animationProgress))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f * animationProgress))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10000f)
                    .graphicsLayer { alpha = animationProgress },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.8f + (0.2f * animationProgress)
                            scaleY = 0.8f + (0.2f * animationProgress)
                        }
                        .padding(horizontal = 40.dp, vertical = 40.dp)
                ) {
                    val digitCount = roundSum.toString().length
                    val fontSize = when {
                        digitCount <= 2 -> 180.sp
                        digitCount == 3 -> 140.sp
                        else -> 100.sp
                    }
                    Text(
                        text = "$roundSum",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Punkte",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
        }
    }
}

@Composable
fun AllFivesRoundAnimation(
    isVisible: Boolean,
    roundSum: Int,
    isValid: Boolean,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    if (isVisible && animationProgress > 0f) {
        val color = if (isValid) Color(0xFF28a745) else Color(0xFFdc3545)
        Box(modifier = modifier.fillMaxSize().zIndex(9999f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * animationProgress))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10000f)
                    .graphicsLayer { alpha = animationProgress },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.8f + (0.2f * animationProgress)
                            scaleY = 0.8f + (0.2f * animationProgress)
                        }
                        .padding(horizontal = 40.dp, vertical = 40.dp)
                ) {
                    Text(
                        text = "$roundSum",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 120.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreInputSection(
    game: Game,
    currentScore: Int,
    onScoreEntered: (Int) -> Unit,
    showCamera: Boolean,
    vm: DartViewModel,
    dartboardBounds: Rect?,
    onDartboardBoundsChanged: (Rect?) -> Unit,
    onDartboardBoundsInRootChanged: (Rect?) -> Unit = {},
    enabled: Boolean = true,
    isCameraConnected: Boolean = false,
    modifier: Modifier = Modifier
) {
    var magnifierCenter by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var parentCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    
    var currentRoundDartPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
    
    LaunchedEffect(game.throwHistory.size, game.currentPlayerIndex, game.throwsInRound) {
        currentRoundDartPositions = vm.getCurrentRoundDartPositions()
    }

    Box(modifier = modifier.onGloballyPositioned { parentCoords = it }) {
        Card(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                val hintText = when {
                    showCamera -> "Darts werden automatisch erkannt"
                    isCameraConnected -> "Kamera aktiv – Darts werden im Hintergrund erkannt"
                    else -> "Für Präzise Eingabe, lange drücken"
                }
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val boardSize = minOf(maxWidth, maxHeight)
                    Box(
                        modifier = Modifier
                            .size(boardSize)
                            .onGloballyPositioned { coords ->
                                parentCoords?.let { parent ->
                                    onDartboardBoundsChanged(parent.localBoundingBoxOf(coords, clipBounds = false))
                                }
                                onDartboardBoundsInRootChanged(coords.findRootCoordinates().localBoundingBoxOf(coords, clipBounds = false))
                            }
                    ) {
                        if (!showCamera) {
                            InteractiveDartboard(
                                thrownDarts = currentRoundDartPositions,
                                onDartThrown = { score, multiplier, offset ->
                                    if (!enabled) return@InteractiveDartboard
                                    val finalScore = if (score == 25 && multiplier == 2) 50 else score * multiplier
                                    val actualMultiplier = if (finalScore == 50 && score == 25) 2 else multiplier
                                    val normalizedPosition = offset
                                    vm.enterScore(finalScore, normalizedPosition, actualMultiplier)
                                },
                                onMagnifierUpdate = { center, dragging ->
                                    magnifierCenter = center
                                    isDragging = dragging
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
        
        if (!showCamera && isDragging && magnifierCenter != null && dartboardBounds != null) {
            MagnifierOverlay(
                localMagnifierCenter = magnifierCenter!!,
                dartboardBounds = dartboardBounds,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
fun ActionButtonsSection(
    onCameraClick: () -> Unit,
    onUndo: () -> Unit,
    enabled: Boolean = true,
    showCamera: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onUndo,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6c757d)),
            shape = RoundedCornerShape(8.dp),
            enabled = enabled
        ) {
            Text("Rückgängig")
        }
        Button(
            onClick = onCameraClick, 
            modifier = Modifier.weight(1f), 
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), 
            shape = RoundedCornerShape(8.dp),
            enabled = enabled
        ) {
            Text(if (showCamera) "Board" else "Kamera")
        }
    }
}

@Composable
fun InteractiveDartboard(
    thrownDarts: List<Offset>,
    onDartThrown: (score: Int, multiplier: Int, position: Offset) -> Unit,
    onMagnifierUpdate: (Offset?, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val scores = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)

    var magnifierCenter by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val startTime = currentTimeMillis()
                val startPosition = down.position
                
                magnifierCenter = startPosition
                isDragging = true
                var hasMoved = false
                var currentPos = startPosition
                val pointerId = down.id

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == pointerId }
                        if (change == null || !change.pressed) break
                        
                        currentPos = change.position
                        magnifierCenter = currentPos
                        
                        if ((currentPos - startPosition).getDistance() > 5f) {
                            hasMoved = true
                        }
                        change.consume()
                    }
                    
                    val duration = currentTimeMillis() - startTime
                    val isQuickTap = duration < 200L && !hasMoved
                    
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (min(size.width, size.height) / 2f) / 1.25f
                    
                    val finalPos = if (isQuickTap) startPosition else currentPos
                    
                    val distance = sqrt(
                        (finalPos.x - center.x).pow(2) + (finalPos.y - center.y).pow(2)
                    )
                    
                    if (distance <= radius * 1.25f) {
                        val (score, multiplier) = calculateDartScore(finalPos, center, radius, scores)
                        
                        onDartThrown(
                            score, 
                            multiplier, 
                            Offset((finalPos.x - center.x) / radius, (finalPos.y - center.y) / radius)
                        )
                    }
                } finally {
                    isDragging = false
                    magnifierCenter = null
                }
            }
        }
    ) {
        drawBoardContent(scores, textMeasurer)

        val center = this.center
        val radius = (min(size.width, size.height) / 2f) / 1.25f
        thrownDarts.forEach { normalizedPosition ->
            val dartPosition = Offset(
                center.x + normalizedPosition.x * radius,
                center.y + normalizedPosition.y * radius
            )
            drawCircle(color = Color.Yellow, radius = 8f, center = dartPosition)
            drawCircle(color = Color.Black, radius = 8f, center = dartPosition, style = Stroke(width = 3f))
        }

        if (isDragging && magnifierCenter != null) {
            onMagnifierUpdate(magnifierCenter, true)
        } else {
            onMagnifierUpdate(null, false)
        }
    }
}

private fun DrawScope.drawBoardContent(scores: List<Int>, textMeasurer: androidx.compose.ui.text.TextMeasurer) {
    val center = this.center
    val radius = (min(size.width, size.height) / 2f) / 1.25f

    val bullseyeRadius = radius * 0.037f
    val outerBullRadius = radius * 0.093f
    val tripleRingInnerRadius = radius * 0.571f
    val tripleRingOuterRadius = radius * 0.629f
    val doubleRingInnerRadius = radius * 0.941f
    val doubleRingOuterRadius = radius * 1.000f
    val outerRingRadius = radius * 1.25f

    drawCircle(color = dartboardBlack, radius = outerRingRadius)
    drawCircle(color = dartboardBlack, radius = radius)
    
    val segmentAngle = 18f
    for (i in scores.indices) {
        val startAngle = (segmentAngle * (i - 0.5f)) - 90f
        val segmentColor = if (i % 2 != 0) dartboardBeige else dartboardBlack
        drawArc(
            color = segmentColor,
            startAngle = startAngle,
            sweepAngle = segmentAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        val ringColor = if (i % 2 != 0) dartboardGreen else dartboardRed
        
        val triplePathRadius = (tripleRingInnerRadius + tripleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - triplePathRadius, center.y - triplePathRadius),
            size = Size(triplePathRadius * 2, triplePathRadius * 2),
            style = Stroke(width = tripleRingOuterRadius - tripleRingInnerRadius)
        )
        
        val doublePathRadius = (doubleRingInnerRadius + doubleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - doublePathRadius, center.y - doublePathRadius),
            size = Size(doublePathRadius * 2, doublePathRadius * 2),
            style = Stroke(width = doubleRingOuterRadius - doubleRingInnerRadius)
        )
    }
    drawCircle(color = dartboardGreen, radius = outerBullRadius, center = center)
    drawCircle(color = dartboardRed, radius = bullseyeRadius, center = center)

    val numberRadius = radius * 1.125f
    scores.forEachIndexed { index, score ->
        val angle = (segmentAngle * index) - 90f
        val angleRad = toRadians(angle.toDouble())
        val text = score.toString()
        val textLayoutResult = textMeasurer.measure(text, style = TextStyle(color = Color.White, fontSize = (radius * 0.18f).toSp()))
        val textWidth = textLayoutResult.size.width
        val textHeight = textLayoutResult.size.height

        val x = (center.x + numberRadius * cos(angleRad)).toFloat() - textWidth / 2
        val y = (center.y + numberRadius * sin(angleRad)).toFloat() - textHeight / 2
        drawText(textLayoutResult = textLayoutResult, topLeft = Offset(x, y))
    }
}

private fun DrawScope.drawBoardContentAtPosition(
    scores: List<Int>, 
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    center: Offset,
    radius: Float
) {
    val bullseyeRadius = radius * 0.037f
    val outerBullRadius = radius * 0.093f
    val tripleRingInnerRadius = radius * 0.571f
    val tripleRingOuterRadius = radius * 0.629f
    val doubleRingInnerRadius = radius * 0.941f
    val doubleRingOuterRadius = radius * 1.000f
    val outerRingRadius = radius * 1.25f

    drawCircle(color = dartboardBlack, radius = outerRingRadius, center = center)
    drawCircle(color = dartboardBlack, radius = radius, center = center)
    
    val segmentAngle = 18f
    for (i in scores.indices) {
        val startAngle = (segmentAngle * (i - 0.5f)) - 90f
        val segmentColor = if (i % 2 != 0) dartboardBeige else dartboardBlack
        drawArc(
            color = segmentColor, 
            startAngle = startAngle, 
            sweepAngle = segmentAngle, 
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        val ringColor = if (i % 2 != 0) dartboardGreen else dartboardRed
        
        val triplePathRadius = (tripleRingInnerRadius + tripleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - triplePathRadius, center.y - triplePathRadius),
            size = Size(triplePathRadius * 2, triplePathRadius * 2),
            style = Stroke(width = tripleRingOuterRadius - tripleRingInnerRadius)
        )
        
        val doublePathRadius = (doubleRingInnerRadius + doubleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - doublePathRadius, center.y - doublePathRadius),
            size = Size(doublePathRadius * 2, doublePathRadius * 2),
            style = Stroke(width = doubleRingOuterRadius - doubleRingInnerRadius)
        )
    }
    drawCircle(color = dartboardGreen, radius = outerBullRadius, center = center)
    drawCircle(color = dartboardRed, radius = bullseyeRadius, center = center)

    val numberRadius = radius * 1.125f
    scores.forEachIndexed { index, score ->
        val angle = (segmentAngle * index) - 90f
        val angleRad = toRadians(angle.toDouble())
        val text = score.toString()
        val textLayoutResult = textMeasurer.measure(text, style = TextStyle(color = Color.White, fontSize = (radius * 0.18f).toSp()))
        val textWidth = textLayoutResult.size.width
        val textHeight = textLayoutResult.size.height

        val x = (center.x + numberRadius * cos(angleRad)).toFloat() - textWidth / 2
        val y = (center.y + numberRadius * sin(angleRad)).toFloat() - textHeight / 2
        drawText(textLayoutResult = textLayoutResult, topLeft = Offset(x, y))
    }
}

@Composable
fun MagnifierOverlay(
    localMagnifierCenter: Offset,
    dartboardBounds: Rect,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val scores = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
    
    BoxWithConstraints(modifier = modifier) {
        val magnifierCenter = localMagnifierCenter + dartboardBounds.topLeft
        val density = LocalDensity.current
        val width = with(density) { maxWidth.toPx() }
        val transitionZone = width / 2f 
        val magnifierRadius = with(density) { 80.dp.toPx() }
        val horizontalOffset = with(density) { 70.dp.toPx() }
        val verticalOffset = with(density) { 80.dp.toPx() }
        
        val centerX = width / 2f
        val relativeX = (magnifierCenter.x - centerX).coerceIn(-transitionZone, transitionZone)
        val horizontalOffsetFactor = -relativeX / transitionZone
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dartboardCenter = Offset(
                x = dartboardBounds.left + dartboardBounds.width / 2f,
                y = dartboardBounds.top + dartboardBounds.height / 2f
            )
            val radius = (min(dartboardBounds.width, dartboardBounds.height) / 2f) / 1.25f
            val zoomFactor = 2.5f
            
            val targetX = magnifierCenter.x + (horizontalOffsetFactor * horizontalOffset)
            val targetY = magnifierCenter.y - verticalOffset
            val loupeCenter = Offset(x = targetX, y = targetY)
            
            val loupeRect = Rect(
                left = loupeCenter.x - magnifierRadius,
                top = loupeCenter.y - magnifierRadius,
                right = loupeCenter.x + magnifierRadius,
                bottom = loupeCenter.y + magnifierRadius
            )
            
            clipPath(Path().apply { addOval(loupeRect) }) {
                withTransform({
                    translate(loupeCenter.x - magnifierCenter.x, loupeCenter.y - magnifierCenter.y)
                    scale(zoomFactor, zoomFactor, pivot = magnifierCenter)
                }) {
                    drawBoardContentAtPosition(scores, textMeasurer, dartboardCenter, radius)
                }
            }
            
            val crossSize = 20f
            val crossColor = Color.Yellow
            val crossThickness = 3f
            
            drawLine(
                color = crossColor,
                start = Offset(loupeCenter.x - crossSize, loupeCenter.y),
                end = Offset(loupeCenter.x + crossSize, loupeCenter.y),
                strokeWidth = crossThickness
            )
            
            drawLine(
                color = crossColor,
                start = Offset(loupeCenter.x, loupeCenter.y - crossSize),
                end = Offset(loupeCenter.x, loupeCenter.y + crossSize),
                strokeWidth = crossThickness
            )
            
            val (selectedScore, selectedMultiplier) = calculateDartScore(magnifierCenter, dartboardCenter, radius, scores)
            val displayText = when {
                selectedScore == 0 && selectedMultiplier == 0 -> "Miss"
                selectedScore == 25 && selectedMultiplier == 2 -> "DBull"
                selectedScore == 25 && selectedMultiplier == 1 -> "Bull"
                selectedMultiplier == 3 -> "T$selectedScore"
                selectedMultiplier == 2 -> "D$selectedScore"
                selectedMultiplier == 1 -> "$selectedScore"
                else -> "Miss"
            }
            
            val textLayoutResult = textMeasurer.measure(
                displayText,
                style = TextStyle(color = Color.White, fontSize = 16.sp)
            )
            
            drawRect(
                color = Color.Black.copy(alpha = 0.8f),
                topLeft = Offset(loupeCenter.x - textLayoutResult.size.width / 2 - 8f, loupeCenter.y - magnifierRadius - 40f),
                size = Size(textLayoutResult.size.width + 16f, textLayoutResult.size.height + 16f)
            )
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(loupeCenter.x - textLayoutResult.size.width / 2, loupeCenter.y - magnifierRadius - 32f)
            )
        }
    }
}

fun calculateDartScore(
    tapOffset: Offset,
    center: Offset,
    radius: Float,
    scores: List<Int>
): Pair<Int, Int> {
    val bullseyeRadius = radius * 0.037f
    val outerBullRadius = radius * 0.093f
    val tripleRingInnerRadius = radius * 0.571f
    val tripleRingOuterRadius = radius * 0.629f
    val doubleRingInnerRadius = radius * 0.941f
    val doubleRingOuterRadius = radius * 1.000f

    val distance = sqrt(
        (tapOffset.x - center.x).pow(2) + (tapOffset.y - center.y).pow(2)
    )

    if (distance <= bullseyeRadius) {
        return Pair(25, 2)
    } else if (distance <= outerBullRadius) {
        return Pair(25, 1)
    }
    
    val angle = atan2(tapOffset.y - center.y, tapOffset.x - center.x)
    var degrees = (toDegrees(angle.toDouble()) + 90).toFloat()
    if (degrees < 0) degrees += 360f
    
    val segment = ((degrees + 9f) / 18f).toInt() % 20
    val baseScore = scores[segment]
    
    val multiplier = when {
        distance <= bullseyeRadius -> 2
        distance <= outerBullRadius -> 1
        distance <= tripleRingInnerRadius -> 1
        distance <= tripleRingOuterRadius -> 3
        distance <= doubleRingInnerRadius -> 1
        distance <= doubleRingOuterRadius -> 2
        else -> 0
    }
    
    if (multiplier == 0) {
        return Pair(0, 0)
    }
    
    return Pair(baseScore, multiplier)
}

@Composable
fun DartboardHeatmap(
    dartPositions: List<Offset>,
    modifier: Modifier = Modifier,
    showNumbers: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    val scores = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
    
    Canvas(modifier = modifier) {
        val center = this.center
        val radius = min(size.width, size.height) / 2f
        
        drawBoardContentForHeatmap(scores, textMeasurer, showNumbers)
        
        if (dartPositions.isNotEmpty()) {
            drawHeatmapOverlay(dartPositions, center, radius)
        }
    }
}

private fun DrawScope.drawBoardContentForHeatmap(
    scores: List<Int>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    showNumbers: Boolean
) {
    val center = this.center
    val radius = min(size.width, size.height) / 2f

    val bullseyeRadius = radius * 0.037f
    val outerBullRadius = radius * 0.093f
    val tripleRingInnerRadius = radius * 0.571f
    val tripleRingOuterRadius = radius * 0.629f
    val doubleRingInnerRadius = radius * 0.941f
    val doubleRingOuterRadius = radius * 1.000f
    val outerRingRadius = radius * 1.25f

    drawCircle(color = dartboardBlack, radius = outerRingRadius)
    drawCircle(color = dartboardBlack, radius = radius)
    
    val segmentAngle = 18f
    for (i in scores.indices) {
        val startAngle = (segmentAngle * (i - 0.5f)) - 90f
        val segmentColor = if (i % 2 != 0) dartboardBeige else dartboardBlack
        drawArc(color = segmentColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = true)

        val ringColor = if (i % 2 != 0) dartboardGreen else dartboardRed
        
        val triplePathRadius = (tripleRingInnerRadius + tripleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - triplePathRadius, center.y - triplePathRadius),
            size = Size(triplePathRadius * 2, triplePathRadius * 2),
            style = Stroke(width = tripleRingOuterRadius - tripleRingInnerRadius)
        )
        
        val doublePathRadius = (doubleRingInnerRadius + doubleRingOuterRadius) / 2f
        drawArc(
            color = ringColor, startAngle = startAngle, sweepAngle = segmentAngle, useCenter = false,
            topLeft = Offset(center.x - doublePathRadius, center.y - doublePathRadius),
            size = Size(doublePathRadius * 2, doublePathRadius * 2),
            style = Stroke(width = doubleRingOuterRadius - doubleRingInnerRadius)
        )
    }
    drawCircle(color = dartboardGreen, radius = outerBullRadius, center = center)
    drawCircle(color = dartboardRed, radius = bullseyeRadius, center = center)

    if (showNumbers && radius > 50f) {
        val numberRadius = radius * 1.125f
        scores.forEachIndexed { index, score ->
            val angle = (segmentAngle * index) - 90f
            val angleRad = toRadians(angle.toDouble())
            val text = score.toString()
            val textLayoutResult = textMeasurer.measure(text, style = TextStyle(color = Color.White, fontSize = (radius * 0.06).sp))
            val textWidth = textLayoutResult.size.width
            val textHeight = textLayoutResult.size.height

            val x = (center.x + numberRadius * cos(angleRad)).toFloat() - textWidth / 2
            val y = (center.y + numberRadius * sin(angleRad)).toFloat() - textHeight / 2
            drawText(textLayoutResult = textLayoutResult, topLeft = Offset(x, y))
        }
    }
}

private fun DrawScope.drawHeatmapOverlay(
    dartPositions: List<Offset>,
    center: Offset,
    dartboardRadius: Float
) {
    if (dartPositions.isEmpty()) return
    
    val normalizedPositions = dartPositions.map { normalizedPos ->
        Offset(center.x + normalizedPos.x * dartboardRadius, center.y + normalizedPos.y * dartboardRadius)
    }
    
    val validPositions = normalizedPositions.filter { position ->
        val distance = sqrt(
            (position.x - center.x).pow(2) + (position.y - center.y).pow(2)
        )
        distance <= dartboardRadius * 1.1f
    }
    
    if (validPositions.isEmpty()) return
    
    val scaledPositions = validPositions
    val heatmapRadius = dartboardRadius * 0.15f
    val maxIntensity = validPositions.size.toFloat()
    
    scaledPositions.forEach { position ->
        val nearbyCount = scaledPositions.count { otherPos ->
            val distance = sqrt(
                (position.x - otherPos.x).pow(2) + (position.y - otherPos.y).pow(2)
            )
            distance <= heatmapRadius * 2f
        }
        
        val intensity = (nearbyCount / maxIntensity).coerceIn(0f, 1f)
        
        val heatColor = when {
            intensity < 0.2f -> Color.Yellow.copy(alpha = intensity * 0.3f)
            intensity < 0.5f -> Color(0xFFFFA500).copy(alpha = intensity * 0.5f)
            intensity < 0.8f -> Color(0xFFFF6600).copy(alpha = intensity * 0.7f)
            else -> Color.Red.copy(alpha = intensity * 0.9f)
        }
        
        val blurLayers = 5
        for (i in blurLayers downTo 1) {
            val layerRadius = heatmapRadius * (i.toFloat() / blurLayers)
            val layerAlpha = (intensity * (1f - (i - 1) * 0.15f)).coerceIn(0f, 1f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        heatColor.copy(alpha = layerAlpha),
                        heatColor.copy(alpha = layerAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = position,
                    radius = layerRadius
                ),
                radius = layerRadius,
                center = position
            )
        }
        
        drawCircle(
            color = heatColor,
            radius = 4f,
            center = position
        )
    }
}
