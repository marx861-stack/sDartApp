package com.sdart.app

import com.sdart.app.domain.player.PlayerStatistics

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdart.app.core.camera.CameraSettingsController
import com.sdart.app.domain.game.engine.AllFivesGameEngine
import com.sdart.app.domain.game.engine.ATCGameEngine
import com.sdart.app.domain.game.engine.CricketGameEngine
import com.sdart.app.domain.game.engine.GameRoundHelper
import com.sdart.app.domain.game.engine.GameUndoEngine
import com.sdart.app.domain.game.engine.X01GameEngine
import com.sdart.app.tournament.MatchPlayerResult
import com.sdart.app.tournament.TournamentManager
import com.sdart.app.tournament.TournamentMatch
import com.sdart.app.tournament.web.TournamentWebServer
import com.sdart.app.tournament.web.createTournamentWebServer
import kotlinx.coroutines.launch

class DartViewModel : ViewModel() {
    // Decoupled callback for haptic feedback, initialized by the UI layer to avoid memory leaks
    var onVibrate: (() -> Unit)? = null
    var onPlaySound: ((String, Int) -> Unit)? = null

    private fun triggerVibration() {
        if (isVibrationEnabled) {
            onVibrate?.invoke()
        }
    }

    private fun triggerSound(text: String, excitement: Int = 0) {
        val isSoundEffect = text.lowercase() == "bing" || text.lowercase() == "buzzer"
        if (isSoundEffect) {
            if (isSoundEnabled) {
                onPlaySound?.invoke(text, excitement)
            }
        } else {
            if (isVoiceCallerEnabled) {
                onPlaySound?.invoke(text, excitement)
            }
        }
    }

    private var _game by mutableStateOf<Game?>(null)
    var game: Game?
        get() = _game
        private set(value) {
            val oldGame = _game
            _game = value
            if (value != null && oldGame != null) {
                val throwAdded = value.throwHistory.size > oldGame.throwHistory.size ||
                    value.currentLeg > oldGame.currentLeg
                if (throwAdded) {
                    triggerVibration()
                }
            }
        }

    // Flag um Eingaben während der Runden-Animation zu blockieren
    var isInputBlocked by mutableStateOf(false)

    // Flag um mehrfaches Speichern von Statistiken für das gleiche Spiel zu verhindern
    var statisticsSavedForCurrentGame = false

    // Globale Einstellungen (persistent via AppPreferences)
    private var _isSoundEnabled by mutableStateOf(AppPreferences.isSoundEnabled)
    var isSoundEnabled: Boolean
        get() = _isSoundEnabled
        set(value) {
            _isSoundEnabled = value
            AppPreferences.isSoundEnabled = value
        }

    private var _isVibrationEnabled by mutableStateOf(AppPreferences.isVibrationEnabled)
    var isVibrationEnabled: Boolean
        get() = _isVibrationEnabled
        set(value) {
            _isVibrationEnabled = value
            AppPreferences.isVibrationEnabled = value
        }

    private var _isVoiceCallerEnabled by mutableStateOf(AppPreferences.isVoiceCallerEnabled)
    var isVoiceCallerEnabled: Boolean
        get() = _isVoiceCallerEnabled
        set(value) {
            _isVoiceCallerEnabled = value
            AppPreferences.isVoiceCallerEnabled = value
        }

    private var _audioAnnounceEachDart by mutableStateOf(AppPreferences.audioAnnounceEachDart)
    var audioAnnounceEachDart: Boolean
        get() = _audioAnnounceEachDart
        set(value) {
            _audioAnnounceEachDart = value
            AppPreferences.audioAnnounceEachDart = value
        }

    private var _audioAnnounceTurnTotal by mutableStateOf(AppPreferences.audioAnnounceTurnTotal)
    var audioAnnounceTurnTotal: Boolean
        get() = _audioAnnounceTurnTotal
        set(value) {
            _audioAnnounceTurnTotal = value
            AppPreferences.audioAnnounceTurnTotal = value
        }

    // --- Entwickler-Einstellungen (versteckt, Standard: deaktiviert) ---
    // Wird durch 10x Tippen auf die Versionsnummer in den Einstellungen freigeschaltet.
    // Bewusst nicht gespeichert: nach jedem App-Start sind die Optionen wieder ausgeblendet.
    // Die Overlay-Schalter wirken nur bei freigeschaltetem Modus; ihre Werte bleiben gespeichert.
    var isDevModeUnlocked by mutableStateOf(false)

    // Zeigt YOLO-Bounding-Boxes und Klassen-Labels im Kamerabild an
    private var _showCameraDetectionOverlay by mutableStateOf(AppPreferences.showCameraDetectionOverlay)
    var showCameraDetectionOverlay: Boolean
        get() = _showCameraDetectionOverlay && isDevModeUnlocked
        set(value) {
            _showCameraDetectionOverlay = value
            AppPreferences.showCameraDetectionOverlay = value
        }

    // Zeigt Inferenzzeit, FPS und GPU/CPU-Status im Kamerabild an
    private var _showCameraDebugHud by mutableStateOf(AppPreferences.showCameraDebugHud)
    var showCameraDebugHud: Boolean
        get() = _showCameraDebugHud && isDevModeUnlocked
        set(value) {
            _showCameraDebugHud = value
            AppPreferences.showCameraDebugHud = value
        }

    // Zeigt das kalibrierte Dartboard-Overlay (Grüne Klammern, Ringe, Heatmap) im Kamerabild an
    private var _showDartboardOverlay by mutableStateOf(AppPreferences.showDartboardOverlay)
    var showDartboardOverlay: Boolean
        get() = _showDartboardOverlay && isDevModeUnlocked
        set(value) {
            _showDartboardOverlay = value
            AppPreferences.showDartboardOverlay = value
        }

    // --- Kamera-Controller & Delegation ---
    val cameraSettings = CameraSettingsController(viewModelScope)

    var cameraZoomLevel: Float
        get() = cameraSettings.cameraZoomLevel
        set(value) { cameraSettings.cameraZoomLevel = value }

    var cameraZoomRatio: Float
        get() = cameraSettings.cameraZoomRatio
        set(value) { cameraSettings.cameraZoomRatio = value }

    var onPinchZoom: ((Float) -> Unit)?
        get() = cameraSettings.onPinchZoom
        set(value) { cameraSettings.onPinchZoom = value }

    var onResetZoom: (() -> Unit)?
        get() = cameraSettings.onResetZoom
        set(value) { cameraSettings.onResetZoom = value }

    val cameraSourceType: CameraSourceType
        get() = cameraSettings.cameraSourceType

    val cameraStreamUrl: String
        get() = cameraSettings.cameraStreamUrl

    val selectedWebcamIndex: Int
        get() = cameraSettings.selectedWebcamIndex

    val selectedCompanionConnectionId: String?
        get() = cameraSettings.selectedCompanionConnectionId

    var availableWebcams: List<String>
        get() = cameraSettings.availableWebcams
        set(value) { cameraSettings.availableWebcams = value }

    var onRefreshWebcams: (() -> Unit)?
        get() = cameraSettings.onRefreshWebcams
        set(value) { cameraSettings.onRefreshWebcams = value }

    var onCameraSourceChanged: ((CameraSourceType) -> Unit)?
        get() = cameraSettings.onCameraSourceChanged
        set(value) { cameraSettings.onCameraSourceChanged = value }

    var isCameraSourceDialogOpen: Boolean
        get() = cameraSettings.isCameraSourceDialogOpen
        set(value) { cameraSettings.isCameraSourceDialogOpen = value }

    var onRequestOpenSourceDialog: (() -> Unit)?
        get() = cameraSettings.onRequestOpenSourceDialog
        set(value) { cameraSettings.onRequestOpenSourceDialog = value }

    fun setCameraSource(type: CameraSourceType) = cameraSettings.setCameraSource(type)
    fun selectCompanionCamera(connectionId: String?) = cameraSettings.selectCompanionCamera(connectionId)
    fun setStreamUrl(url: String) = cameraSettings.setStreamUrl(url)
    fun setWebcamIndex(index: Int) = cameraSettings.setWebcamIndex(index)
    fun refreshWebcams() = cameraSettings.refreshWebcams()
    fun resetZoom() = cameraSettings.resetZoom()

    fun resetSettingsToDefaults() {
        isSoundEnabled = true
        isVibrationEnabled = true
        isVoiceCallerEnabled = true
        audioAnnounceEachDart = true
        audioAnnounceTurnTotal = true
        showCameraDetectionOverlay = false
        showCameraDebugHud = false
        showDartboardOverlay = false
        cameraSettings.resetZoom()
    }

    // --- Turnier-Integration ---
    val tournamentManager = TournamentManager()
    val tournamentWebServer: TournamentWebServer = createTournamentWebServer(tournamentManager)
    var activeTournamentMatchId by mutableStateOf<String?>(null)

    fun startTournamentMatch(match: TournamentMatch, onGameReady: () -> Unit) {
        val p1 = match.player1 ?: return
        val p2 = match.player2 ?: return
        val config = tournamentManager.currentTournament?.config ?: return

        activeTournamentMatchId = match.id

        val playerNames = listOf(p1.name, p2.name)
        val startScore = when (config.mode) {
            GameMode.X301 -> 301
            GameMode.X501 -> 501
            GameMode.X701 -> 701
            GameMode.CUSTOM -> config.customStartScore
            else -> 0
        }

        val atcState = if (config.mode == GameMode.AROUND_THE_CLOCK) {
            ATCGameState(variant = config.atcVariant)
        } else null

        val cricketCfg = if (config.mode == GameMode.CRICKET) {
            CricketConfig(variant = config.cricketVariant)
        } else null

        startGame(
            playerNames = playerNames,
            mode = config.mode,
            customScore = startScore,
            doubleIn = config.doubleIn,
            doubleOut = config.doubleOut,
            legs = config.legs,
            randomizeOrder = false,
            atcConfig = atcState,
            cricketConfig = cricketCfg
        )
        onGameReady()
    }

    fun finishTournamentMatch() {
        val matchId = activeTournamentMatchId ?: return
        val currentGame = game ?: return
        val tState = tournamentManager.currentTournament ?: return
        val match = tState.matches.find { it.id == matchId } ?: return

        val p1 = match.player1 ?: return
        val p2 = match.player2 ?: return

        val p1Game = currentGame.players.find { it.name == p1.name }
        val p2Game = currentGame.players.find { it.name == p2.name }

        val p1WonLegs = p1Game?.wonLegs ?: 0
        val p2WonLegs = p2Game?.wonLegs ?: 0

        val winnerPlayerId = if (p1WonLegs > p2WonLegs) {
            p1.id
        } else if (p2WonLegs > p1WonLegs) {
            p2.id
        } else {
            if (p1Game?.currentScore == 0) p1.id else p2.id
        }

        val p1Avg = if (p1Game != null && p1Game.dartThrows.isNotEmpty()) {
            val totalScore = p1Game.dartThrows.sumOf { it.score * it.multiplier }
            (totalScore.toDouble() / p1Game.dartThrows.size) * 3.0
        } else 0.0

        val p2Avg = if (p2Game != null && p2Game.dartThrows.isNotEmpty()) {
            val totalScore = p2Game.dartThrows.sumOf { it.score * it.multiplier }
            (totalScore.toDouble() / p2Game.dartThrows.size) * 3.0
        } else 0.0

        val p1Result = MatchPlayerResult(
            legsWon = p1WonLegs,
            average = (p1Avg * 10).toInt() / 10.0,
            dartsThrown = p1Game?.dartThrows?.size ?: 0
        )
        val p2Result = MatchPlayerResult(
            legsWon = p2WonLegs,
            average = (p2Avg * 10).toInt() / 10.0,
            dartsThrown = p2Game?.dartThrows?.size ?: 0
        )

        tournamentManager.resolveMatch(matchId, winnerPlayerId, p1Result, p2Result)
        activeTournamentMatchId = null
    }

    fun cancelTournamentMatch() {
        activeTournamentMatchId = null
    }

    // --- Spielablauf & Steuerung ---
    fun startGame(
        playerNames: List<String>,
        mode: GameMode,
        customScore: Int = 301,
        doubleIn: Boolean = false,
        doubleOut: Boolean = true,
        legs: Int = 1,
        randomizeOrder: Boolean = false,
        atcConfig: ATCGameState? = null,
        cricketConfig: CricketConfig? = null
    ) {
        try {
            println("=== STARTING GAME ===")
            val targetScore = if (mode == GameMode.ALL_FIVES) customScore * 5 else 0

            val startScore = when (mode) {
                GameMode.CUSTOM -> customScore.coerceAtLeast(1)
                GameMode.CRICKET -> 0
                GameMode.ALL_FIVES -> 0
                GameMode.AROUND_THE_CLOCK -> 0
                else -> if (customScore > 0) customScore else mode.startScore
            }

            var players = playerNames.map { name ->
                val atcStartTarget = if (mode == GameMode.AROUND_THE_CLOCK && atcConfig != null) {
                    ATCGameEngine.startTarget(atcConfig)
                } else 1

                Player(
                    name = name.trim(),
                    currentScore = startScore,
                    scores = mutableListOf(),
                    dartThrows = mutableListOf(),
                    totalRounds = 0,
                    wonLegs = 0,
                    atcCurrentTarget = atcStartTarget
                )
            }

            if (randomizeOrder) {
                players = players.shuffled()
            }

            val newGame = Game(
                players = players,
                mode = mode,
                currentPlayerIndex = 0,
                isFinished = false,
                throwsInRound = 0,
                doubleIn = doubleIn,
                doubleOut = doubleOut,
                targetScore = targetScore,
                totalLegs = legs,
                currentLeg = 1,
                randomizeOrder = randomizeOrder,
                atcConfig = atcConfig,
                cricketConfig = cricketConfig,
                startScore = startScore
            )

            game = newGame
            statisticsSavedForCurrentGame = false
            triggerSound("Game on")
        } catch (e: Exception) {
            println("=== CRITICAL ERROR STARTING GAME ===")
            e.printStackTrace()
            game = null
        }
    }

    fun enterScore(score: Int, position: Offset? = null, multiplier: Int = 1) {
        if (isInputBlocked) return
        val currentGame = game ?: return
        if (currentGame.isFinished) return

        val updatedGame = when (currentGame.mode) {
            GameMode.CRICKET -> CricketGameEngine.processThrow(
                score, position, multiplier, currentGame, audioAnnounceEachDart, ::triggerSound
            )
            GameMode.ALL_FIVES -> AllFivesGameEngine.processThrow(
                score, position, multiplier, currentGame, audioAnnounceEachDart, audioAnnounceTurnTotal, ::triggerSound
            )
            GameMode.AROUND_THE_CLOCK -> ATCGameEngine.processThrow(
                score, position, multiplier, currentGame, audioAnnounceEachDart, ::triggerSound
            )
            else -> X01GameEngine.processThrow(
                score, position, multiplier, currentGame, audioAnnounceEachDart, audioAnnounceTurnTotal, ::triggerSound
            )
        }

        if (updatedGame != null) {
            game = updatedGame
        }
    }

    fun enterScoreML(score: Int, confidence: Double, position: Offset? = null, multiplier: Int = 1) {
        val adjustedScore = when {
            confidence > 0.8 -> score
            confidence > 0.6 -> score
            confidence > 0.4 -> score
            else -> return
        }
        enterScore(adjustedScore, position, multiplier)
    }

    fun undo() {
        val currentGame = game ?: return
        val previousGame = GameUndoEngine.undo(currentGame)
        if (previousGame != null) {
            game = previousGame
        }
    }

    // --- Rundenabfragen delegiert an GameRoundHelper ---
    fun getCurrentRoundDartPositions(): List<Offset> = GameRoundHelper.getCurrentRoundDartPositions(game)

    fun getCurrentRoundSum(): Int = GameRoundHelper.getCurrentRoundSum(game)

    fun getLastCompletedRoundSum(): Int = GameRoundHelper.getLastCompletedRoundSum(game)

    fun isLastCompletedRoundValid(): Boolean = GameRoundHelper.isLastCompletedRoundValid(game)

    fun isRoundCompleted(): Boolean {
        val currentGame = game ?: return false
        return currentGame.throwsInRound == 0 && currentGame.throwHistory.isNotEmpty()
    }

    fun reset() {
        game = null
    }

    fun updatePlayerStatisticsAfterGame(repository: PlayerRepository, forceSave: Boolean = false) {
        if (statisticsSavedForCurrentGame) return
        val finishedGame = game ?: return
        if (!finishedGame.isFinished && !forceSave) return

        statisticsSavedForCurrentGame = true

        viewModelScope.launch {
            finishedGame.players.forEach { player ->
                val stats = PlayerStatistics.getAllStatistics(player, finishedGame)
                repository.updatePlayerStatistics(player.name, stats)
            }
        }
    }

    fun continueGame() {
        val currentGame = game ?: return
        if (!currentGame.isFinished) return

        val nextActivePlayerIndex = X01GameEngine.findNextActivePlayer(
            currentGame.currentPlayerIndex,
            currentGame.players
        )

        if (nextActivePlayerIndex == -1) return

        val throwsInRound = GameRoundHelper.calculateThrowsInRound(nextActivePlayerIndex, currentGame.throwHistory)

        game = currentGame.copy(
            currentPlayerIndex = nextActivePlayerIndex,
            isFinished = false,
            throwsInRound = throwsInRound
        )
    }

    override fun onCleared() {
        super.onCleared()
        tournamentWebServer.stop()
    }
}
