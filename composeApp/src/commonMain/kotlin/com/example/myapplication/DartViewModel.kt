package com.example.myapplication

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.tournament.MatchPlayerResult
import com.example.myapplication.tournament.TournamentManager
import com.example.myapplication.tournament.TournamentMatch
import com.example.myapplication.tournament.web.TournamentWebServer
import com.example.myapplication.tournament.web.createTournamentWebServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ZOOM_PERSIST_DELAY_MS = 400L

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
                if (value.throwHistory.size > oldGame.throwHistory.size) {
                    triggerVibration()
                }
            }
        }
    
    // Flag um Eingaben wÃ¤hrend der Runden-Animation zu blockieren
    var isInputBlocked by mutableStateOf(false)
    
    // Flag um mehrfaches Speichern von Statistiken fÃ¼r das gleiche Spiel zu verhindern
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
    private var _isDevModeUnlocked by mutableStateOf(AppPreferences.isDevModeUnlocked)
    var isDevModeUnlocked: Boolean
        get() = _isDevModeUnlocked
        set(value) {
            _isDevModeUnlocked = value
            AppPreferences.isDevModeUnlocked = value
        }

    // Zeigt YOLO-Bounding-Boxes und Klassen-Labels im Kamerabild an
    private var _showCameraDetectionOverlay by mutableStateOf(AppPreferences.showCameraDetectionOverlay)
    var showCameraDetectionOverlay: Boolean
        get() = _showCameraDetectionOverlay
        set(value) {
            _showCameraDetectionOverlay = value
            AppPreferences.showCameraDetectionOverlay = value
        }

    // Zeigt Inferenzzeit, FPS und GPU/CPU-Status im Kamerabild an
    private var _showCameraDebugHud by mutableStateOf(AppPreferences.showCameraDebugHud)
    var showCameraDebugHud: Boolean
        get() = _showCameraDebugHud
        set(value) {
            _showCameraDebugHud = value
            AppPreferences.showCameraDebugHud = value
        }

    // Zeigt das kalibrierte Dartboard-Overlay (Grüne Klammern, Ringe, Heatmap) im Kamerabild an
    private var _showDartboardOverlay by mutableStateOf(AppPreferences.showDartboardOverlay)
    var showDartboardOverlay: Boolean
        get() = _showDartboardOverlay
        set(value) {
            _showDartboardOverlay = value
            AppPreferences.showDartboardOverlay = value
        }
    
    // Zoom-Level der Kamera (wird über alle Spiele hinweg gemerkt, bis die App geschlossen wird)
    // Zoom ändert sich während einer Pinch-Geste viele Male pro Sekunde; gespeichert wird daher
    // erst, wenn sich der Wert ZOOM_PERSIST_DELAY_MS lang nicht mehr geändert hat.
    private var _cameraZoomLevel by mutableFloatStateOf(AppPreferences.cameraZoomLevel)
    var cameraZoomLevel: Float
        get() = _cameraZoomLevel
        set(value) {
            _cameraZoomLevel = value
            scheduleZoomPersist()
        }

    private var _cameraZoomRatio by mutableFloatStateOf(AppPreferences.cameraZoomRatio)
    var cameraZoomRatio: Float
        get() = _cameraZoomRatio
        set(value) {
            _cameraZoomRatio = value
            scheduleZoomPersist()
        }

    private var zoomPersistJob: Job? = null

    private fun scheduleZoomPersist() {
        zoomPersistJob?.cancel()
        zoomPersistJob = viewModelScope.launch {
            delay(ZOOM_PERSIST_DELAY_MS)
            persistZoom()
        }
    }

    private fun persistZoom() {
        AppPreferences.cameraZoomLevel = _cameraZoomLevel
        AppPreferences.cameraZoomRatio = _cameraZoomRatio
    }

    // Direkte Zoom-Steuerung durch die Plattform (verhindert Ruckeln & Feedback-Loops)
    var onPinchZoom: ((Float) -> Unit)? = null
    var onResetZoom: (() -> Unit)? = null

    fun resetZoom() {
        cameraZoomLevel = 0f
        cameraZoomRatio = 1.0f
        onResetZoom?.invoke()
    }
    
    fun resetSettingsToDefaults() {
        isSoundEnabled = true
        isVibrationEnabled = true
        isVoiceCallerEnabled = true
        audioAnnounceEachDart = true
        audioAnnounceTurnTotal = true
        showCameraDetectionOverlay = false
        showCameraDebugHud = false
        showDartboardOverlay = false
        cameraZoomLevel = 0f
        cameraZoomRatio = 1f
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



    fun startGame(playerNames: List<String>, mode: GameMode, customScore: Int = 301, doubleIn: Boolean = false, doubleOut: Boolean = true, legs: Int = 1, randomizeOrder: Boolean = false, atcConfig: ATCGameState? = null, cricketConfig: CricketConfig? = null) {
        try {
            println("=== STARTING GAME ===")
            
            // All Fives: customScore ist die Ziel-Punktzahl (50, 75 oder 100)
            val targetScore = if (mode == GameMode.ALL_FIVES) customScore * 5 else 0

            val startScore = when (mode) {
                GameMode.CUSTOM -> customScore.coerceAtLeast(1)
                GameMode.CRICKET -> 0
                GameMode.ALL_FIVES -> 0
                GameMode.AROUND_THE_CLOCK -> 0
                else -> {
                    if (customScore > 0) customScore else mode.startScore
                }
            }
            
            // Spieler erstellen
            var players = playerNames.map { name ->
                val atcStartTarget = if (mode == GameMode.AROUND_THE_CLOCK && atcConfig?.reverse == true) {
                    if (atcConfig.withBulls) 25 else 20
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
            
            // ... (Randomize order logic)
            if (randomizeOrder) {
                players = players.shuffled()
            }
            
            // Game-Objekt erstellen
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
            
            println("Created game object successfully")
            
            // State aktualisieren
            game = newGame
            statisticsSavedForCurrentGame = false
            
            println("Game state updated successfully")
            println("=== GAME STARTED SUCCESSFULLY ===")
            
            // ZusÃ¤tzliche Validierung nach dem Setzen
            if (game == null) {
                println("CRITICAL ERROR: Game is still null after assignment!")
                throw IllegalStateException("Game assignment failed")
            }
            
            println("Final validation: Game is not null, players: ${game?.players?.size}")
            
        } catch (e: Exception) {
            println("=== CRITICAL ERROR STARTING GAME ===")
            println("Error type: ${e.javaClass.simpleName}")
            println("Error message: ${e.message}")
            e.printStackTrace()
            
            // Fallback: Einfaches Standard-Spiel starten
            try {
                println("Attempting fallback game start...")
                val fallbackPlayers = listOf("Spieler 1", "Spieler 2").map { name ->
                    Player(name = name, currentScore = 501, scores = mutableListOf(), dartThrows = mutableListOf())
                }
                
                val fallbackGame = Game(
                    players = fallbackPlayers,
                    mode = GameMode.X501,
                    currentPlayerIndex = 0,
                    isFinished = false,
                    throwsInRound = 0,
                    doubleIn = false,
                    doubleOut = true,
                    targetScore = 0,
                    totalLegs = 1,
                    currentLeg = 1,
                    randomizeOrder = false,
                    startScore = 501
                )
                
                game = fallbackGame
                println("Fallback game started successfully")
                
            } catch (fallbackError: Exception) {
                println("Fallback also failed: ${fallbackError.message}")
                fallbackError.printStackTrace()
                // Letzter Ausweg: Game auf null setzen
                game = null
            }
            if (game != null) {
                triggerSound("Game on")
            }
        }
    }

    fun enterScore(score: Int, position: androidx.compose.ui.geometry.Offset? = null, multiplier: Int = 1) {
        if (isInputBlocked) return // Blockiere Eingabe wÃ¤hrend Animation
        val currentGame = game ?: return
        if (currentGame.isFinished) return
        
        // CRICKET-MODUS: Andere Logik
        if (currentGame.mode == GameMode.CRICKET) {
            enterCricketScore(score, position, multiplier, currentGame)
            return
        }
        
        // ALL_FIVES-MODUS: Andere Logik
        if (currentGame.mode == GameMode.ALL_FIVES) {
            enterAllFivesScore(score, position, multiplier, currentGame)
            return
        }

        // ATC-MODUS: Andere Logik
        if (currentGame.mode == GameMode.AROUND_THE_CLOCK) {
            enterATCScore(score, position, multiplier, currentGame)
            return
        }
        
        // Validierung: Score muss im gÃ¼ltigen Bereich sein
        if (score !in 0..180) {
            println("DEBUG: UngÃ¼ltiger Score $score (muss zwischen 0 und 180 sein)")
            return
        }
        
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        val currentScore = currentPlayer.currentScore
        
        // Double-In Validierung: Wenn Double-In aktiv ist und Spieler noch nicht Double-In hat
        val isDouble = multiplier == 2 || score == 50 // Double Ring oder Double Bull
        val needsDoubleIn = currentGame.doubleIn && !currentPlayer.hasDoubleIn
        
        if (needsDoubleIn) {
            // Nur Doubles werden akzeptiert, bis Double-In erreicht ist
            if (!isDouble) {
                println("DEBUG: Double-In Regel: Wurf wird ignoriert (kein Double). Score bleibt: $currentScore")
                
                // Wurf wird ignoriert, aber Runde lÃ¤uft weiter
                var throwsInRound = currentGame.throwsInRound + 1
                var currentPlayerIndex = currentGame.currentPlayerIndex
                var isFinished = false
                
                if (throwsInRound >= 3) {
                    // Finde den nÃ¤chsten nicht-fertigen Spieler
                    val nextActivePlayerIndex = findNextActivePlayer(currentPlayerIndex, currentGame.players)
                    if (nextActivePlayerIndex != -1) {
                        currentPlayerIndex = nextActivePlayerIndex
                        throwsInRound = 0
                    } else {
                        // Alle Spieler sind fertig
                        isFinished = areAllPlayersFinished(currentGame.players)
                    }
                }
                
                // Neues Game-Objekt mit aktualisiertem Spielerwechsel (Score bleibt unverÃ¤ndert)
                game = Game(
                    players = currentGame.players,
                    mode = currentGame.mode,
                    currentPlayerIndex = currentPlayerIndex,
                    isFinished = isFinished,
                    throwsInRound = throwsInRound,
                    throwHistory = currentGame.throwHistory,
                    doubleIn = currentGame.doubleIn,
                    doubleOut = currentGame.doubleOut,
                    targetScore = currentGame.targetScore,
                    totalLegs = currentGame.totalLegs,
                    currentLeg = currentGame.currentLeg,
                    randomizeOrder = currentGame.randomizeOrder,
                    startScore = currentGame.startScore
                )
                return
            } else {
                // Erster Double-Wurf: Double-In erreicht!
                println("DEBUG: Double-In erreicht! Spieler ${currentGame.currentPlayerIndex + 1} hat mit Double gestartet.")
            }
        }
        
        val newScore = currentScore - score
        
        // BUST: Wenn Score negativ wird oder bei Double-Out 1 Ã¼brig bleibt (da kein Double-Finish mÃ¶glich ist)
        val isBust = newScore < 0 || (newScore == 1 && currentGame.doubleOut)
        
        // Double-Out Validierung: Wenn Score auf 0 geht, muss der letzte Wurf ein Double sein (auÃŸer Double Bull = 50)
        val isValidFinish = if (newScore == 0) {
            if (currentGame.doubleOut) {
                // Double-Out aktiv: Muss mit Double enden (Multiplikator 2 oder Double Bull 50)
                val isDoubleOut = multiplier == 2 || score == 50
                if (!isDoubleOut) {
                    println("DEBUG: Double-Out Regel: Spiel kann nicht mit Single beendet werden")
                }
                isDoubleOut
            } else {
                // Double-Out nicht aktiv: Jeder Wurf kann beenden
                true
            }
        } else {
            true
        }
        
        // Wenn Bust oder ungÃ¼ltiges Finish, wird der Wurf nicht akzeptiert
        // Score wird zurÃ¼ckgesetzt, aber WÃ¼rfe bleiben fÃ¼r Statistik erhalten
        if (isBust || !isValidFinish) {
            println("DEBUG: Bust oder ungÃ¼ltiges Finish - Score wird zurÃ¼ckgesetzt, WÃ¼rfe bleiben fÃ¼r Statistik erhalten")
            triggerSound("buzzer ... Bust")
            
            val bustingPlayerIndex = currentGame.currentPlayerIndex
            val bustingPlayer = currentGame.players[bustingPlayerIndex]
            
            // Erstelle DartThrow-Objekt fÃ¼r den Bust-Wurf (wird fÃ¼r Statistik benÃ¶tigt, auch bei position == null)
            val dartThrow = DartThrow(
                score = score,
                position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
                playerIndex = bustingPlayerIndex,
                multiplier = multiplier,
                isBust = true
            )
            
            // Berechne die Summe aller WÃ¼rfe der aktuellen Runde (OHNE dem aktuellen Bust-Wurf)
            // Der Bust-Wurf wird NICHT zur roundScoreSum gezÃ¤hlt, da er nicht akzeptiert wird
            var roundScoreSum = 0 // Starte bei 0, da Bust-Wurf nicht zÃ¤hlt
            val throwsInCurrentRound = currentGame.throwsInRound
            
            // Gehe rÃ¼ckwÃ¤rts durch die Throw-History und berechne die Summe der aktuellen Runde
            // (nur die gÃ¼ltigen WÃ¼rfe vor dem Bust-Wurf)
            var throwsFound = 0
            for (i in currentGame.throwHistory.size - 1 downTo 0) {
                val throwData = currentGame.throwHistory[i]
                
                if (throwData.playerIndex != bustingPlayerIndex) {
                    // Anderer Spieler gefunden - Runde ist abgeschlossen, stoppe sofort
                    break
                }
                
                // Gleicher Spieler: PrÃ¼fe ob dies noch zur aktuellen Runde gehÃ¶rt
                if (throwsFound < throwsInCurrentRound) {
                    // Dies ist ein Wurf aus der aktuellen Runde (vor dem Bust-Wurf)
                    roundScoreSum += throwData.score
                    throwsFound++
                } else {
                    // Wir haben bereits alle WÃ¼rfe der aktuellen Runde gefunden
                    // Alle weiteren WÃ¼rfe gehÃ¶ren zu frÃ¼heren Runden - stoppe
                    break
                }
            }
            
            // Berechne den Score vom Anfang der Runde
            // bustingPlayer.currentScore ist der Score NACH allen gÃ¼ltigen WÃ¼rfen der Runde (vor dem Bust)
            // roundScoreSum ist die Summe aller gÃ¼ltigen WÃ¼rfe der Runde
            // Also: scoreAtRoundStart = aktueller Score + alle gÃ¼ltigen WÃ¼rfe der Runde
            val scoreAtRoundStart = bustingPlayer.currentScore + roundScoreSum
            
            // PrÃ¼fe ob Double-In in dieser Runde erreicht wurde (muss zurÃ¼ckgesetzt werden)
            // Double-In sollte nur zurÃ¼ckgesetzt werden, wenn es in dieser Runde ERREICHT wurde,
            // nicht wenn der Spieler bereits Double-In hatte und dann bustet
            val hadDoubleInBeforeRound = bustingPlayer.hasDoubleIn
            
            // PrÃ¼fe ob in dieser Runde ein Double geworfen wurde (das Double-In erreicht haben kÃ¶nnte)
            var hasDoubleInCurrentRound = false
            throwsFound = 0
            for (i in currentGame.throwHistory.size - 1 downTo 0) {
                val throwData = currentGame.throwHistory[i]
                
                if (throwData.playerIndex != bustingPlayerIndex) {
                    break
                }
                
                if (throwsFound < throwsInCurrentRound) {
                    if (throwData.multiplier == 2 || throwData.score == 50) {
                        hasDoubleInCurrentRound = true
                        break // Ein Double gefunden reicht
                    }
                    throwsFound++
                } else {
                    break
                }
            }
            
            // PrÃ¼fe auch den aktuellen Bust-Wurf (falls er ein Double ist)
            if (!hasDoubleInCurrentRound && (multiplier == 2 || score == 50)) {
                hasDoubleInCurrentRound = true
            }
            
            // Double-In wurde in dieser Runde erreicht, wenn:
            // 1. Double-In ist aktiv
            // 2. Spieler hatte KEIN Double-In vor der Runde
            // 3. Ein Double wurde in dieser Runde geworfen
            // Nur dann sollte Double-In zurÃ¼ckgesetzt werden (da es in dieser Runde erreicht wurde)
            val doubleInAchievedThisRound = currentGame.doubleIn && 
                !hadDoubleInBeforeRound && 
                hasDoubleInCurrentRound
            
            // WICHTIG: WÃ¼rfe werden NICHT entfernt - sie bleiben fÃ¼r die Statistik erhalten
            // Der Bust-Wurf wird zu den Listen hinzugefÃ¼gt, aber der Score wird zurÃ¼ckgesetzt
            val updatedThrowHistory = currentGame.throwHistory.toMutableList().apply {
                dartThrow?.let { add(it) }
            }
            
            val playersCopy = currentGame.players.mapIndexed { idx, player ->
                if (idx == bustingPlayerIndex) {
                    // Wenn Double-In in dieser Runde erreicht wurde, zurÃ¼cksetzen
                    val updatedHasDoubleIn = if (doubleInAchievedThisRound) {
                        false
                    } else {
                        hadDoubleInBeforeRound
                    }
                    
                    player.copy(
                        currentScore = scoreAtRoundStart,
                        hasDoubleIn = updatedHasDoubleIn,
                        scores = player.scores.toMutableList().apply { add(score) }, // Bust-Wurf fÃ¼r Statistik hinzufÃ¼gen
                        dartThrows = player.dartThrows.toMutableList().apply { 
                            dartThrow?.let { add(it) } // Bust-Wurf fÃ¼r Statistik hinzufÃ¼gen
                        }
                    )
                } else {
                    player.copy()
                }
            }
            
            // Runde sofort beenden: Spieler wechseln (unabhÃ¤ngig von der Anzahl der WÃ¼rfe)
            // Finde den nÃ¤chsten nicht-fertigen Spieler
            val nextPlayerIndex = findNextActivePlayer(bustingPlayerIndex, playersCopy)
            val throwsInRound = 0 // Runde ist beendet, nÃ¤chster Spieler startet mit 0 WÃ¼rfen
            
            // PrÃ¼fe ob alle Spieler fertig sind
            val isFinished = if (nextPlayerIndex == -1) {
                areAllPlayersFinished(playersCopy)
            } else {
                false
            }
            
            println("DEBUG: Score zurÃ¼ckgesetzt von $currentScore auf $scoreAtRoundStart (Anfang der Runde)")
            println("DEBUG: ${throwsInCurrentRound + 1} WÃ¼rfe der Runde (inkl. Bust-Wurf) bleiben fÃ¼r Statistik erhalten")
            
            // Neues Game-Objekt mit aktualisiertem Spielerwechsel und zurÃ¼ckgesetztem Score
            // throwHistory enthÃ¤lt jetzt auch den Bust-Wurf
            // Wenn kein aktiver Spieler gefunden wurde, verwende den nÃ¤chsten Index (fÃ¼r Fallback)
            val finalPlayerIndex = if (nextPlayerIndex == -1) {
                (bustingPlayerIndex + 1) % playersCopy.size
            } else {
                nextPlayerIndex
            }
            
            game = Game(
                players = playersCopy,
                mode = currentGame.mode,
                currentPlayerIndex = finalPlayerIndex,
                isFinished = isFinished,
                throwsInRound = throwsInRound,
                throwHistory = updatedThrowHistory, // EnthÃ¤lt jetzt auch den Bust-Wurf
                doubleIn = currentGame.doubleIn,
                doubleOut = currentGame.doubleOut,
                targetScore = currentGame.targetScore,
                totalLegs = currentGame.totalLegs,
                currentLeg = currentGame.currentLeg,
                randomizeOrder = currentGame.randomizeOrder,
                startScore = currentGame.startScore
            )
            return
        }
        
        // GÃ¼ltiger Wurf: Score wird akzeptiert
        println("DEBUG: GÃ¼ltiger Wurf - Score: $score, Multiplikator: $multiplier, Neuer Score: $newScore")
        
        // Play the score sound for single dart
        if (audioAnnounceEachDart && newScore != 0) {
            val voiceKey = when {
                score == 50 -> "Bullseye"
                score == 25 -> "Twenty Five"
                multiplier == 3 -> "Triple ${score / multiplier}"
                multiplier == 2 -> "Double ${score / multiplier}"
                score == 0 -> "Miss"
                else -> score.toString()
            }
            triggerSound(voiceKey, 0)
        }
        
        // DartThrow-Objekt erstellen (auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        
        // Deep copy der Spieler mit aktualisiertem Score
        // Wenn Double-In erreicht wurde, setze hasDoubleIn = true
        val justAchievedDoubleIn = needsDoubleIn && isDouble
        var playersCopy = currentGame.players.mapIndexed { idx, player ->
            if (idx == currentGame.currentPlayerIndex) {
                player.copy(
                    currentScore = newScore,
                    hasDoubleIn = player.hasDoubleIn || justAchievedDoubleIn, // Double-In Status aktualisieren
                    scores = player.scores.toMutableList().apply { add(score) },
                    dartThrows = player.dartThrows.toMutableList().apply { 
                        dartThrow?.let { add(it) }
                    }
                )
            } else {
                player.copy(
                    scores = player.scores.toMutableList(),
                    dartThrows = player.dartThrows.toMutableList()
                )
            }
        }
        
        // Runden-Logik
        var isFinished = false
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        
        var currentLeg = currentGame.currentLeg
        var legWon = false
        if (newScore == 0) {
            // Aktueller Spieler hat 0 erreicht
            println("DEBUG: Spieler ${currentPlayerIndex + 1} (${playersCopy[currentPlayerIndex].name}) hat 0 erreicht!")
            
            // Gewonnenes Leg verbuchen
            val winningPlayer = playersCopy[currentPlayerIndex]
            val newWonLegs = winningPlayer.wonLegs + 1
            playersCopy = playersCopy.mapIndexed { idx, player ->
                if (idx == currentPlayerIndex) {
                    player.copy(wonLegs = newWonLegs)
                } else {
                    player.copy()
                }
            }

            val requiredLegs = if (currentGame.players.size == 1) currentGame.totalLegs else (currentGame.totalLegs / 2) + 1
            
            // Hat dieser Leg-Gewinn das Spiel entschieden?
            // Oder sind wir bereits in der VerlÃ¤ngerung ("Weiterspielen" nach Spielende)?
            val alreadyHasWinner = currentGame.players.any { it.wonLegs >= requiredLegs }
            val nowHasWinner = playersCopy.any { it.wonLegs >= requiredLegs }
            
            if (nowHasWinner) {
                if (alreadyHasWinner) {
                    // Im Weiterspielen-Modus: PrÃ¼fen ob alle fertig sind
                    if (areAllPlayersFinished(playersCopy)) {
                        isFinished = true
                        println("DEBUG: Alle Spieler sind fertig - Spiel beendet!")
                    } else {
                        // NÃ¤chsten aktiven Spieler suchen
                        val nextActivePlayerIndex = findNextActivePlayer(currentPlayerIndex, playersCopy)
                        if (nextActivePlayerIndex != -1) {
                            currentPlayerIndex = nextActivePlayerIndex
                            throwsInRound = 0
                        } else {
                            isFinished = true
                        }
                    }
                } else {
                    // Das Spiel wurde gerade eben erst entschieden!
                    isFinished = true
                    triggerSound("Game Shot and the Win")
                    println("DEBUG: Spiel beendet! Gewinner: ${winningPlayer.name} mit ${newWonLegs} Legs")
                }
            } else {
                // Normaler Leg-Gewinn (noch kein Spielgewinner)
                legWon = true
                triggerSound("Game Shot")
                
                currentLeg++
                println("DEBUG: Leg gewonnen! Neues Leg $currentLeg gestartet.")
                
                // Spieler-Scores zurÃ¼cksetzen
                playersCopy = playersCopy.map { player ->
                    player.copy(
                        currentScore = currentGame.startScore,
                        hasDoubleIn = false
                    )
                }
                
                // Rotation fÃ¼r das nÃ¤chste Leg
                currentPlayerIndex = (currentGame.currentLeg) % playersCopy.size
                throwsInRound = 0
            }
        } else {
            // Play round total announcement if enabled
            if (throwsInRound >= 3 && audioAnnounceTurnTotal) {
                var roundScoreSum = score
                var foundThrows = 0
                for (i in currentGame.throwHistory.size - 1 downTo 0) {
                    val throwData = currentGame.throwHistory[i]
                    if (throwData.playerIndex == currentGame.currentPlayerIndex && foundThrows < 2) {
                        roundScoreSum += throwData.score
                        foundThrows++
                    } else if (throwData.playerIndex != currentGame.currentPlayerIndex) {
                        break
                    }
                }
                if (roundScoreSum == 180) {
                    triggerSound("... 180", 2)
                } else if (roundScoreSum == 0) {
                    triggerSound("... No score", 0)
                } else {
                    triggerSound("... $roundScoreSum", 0)
                }
            }

            // Normale Runden-Logik
            if (throwsInRound >= 3) {
                // Finde den nÃ¤chsten nicht-fertigen Spieler
                val nextActivePlayerIndex = findNextActivePlayer(currentPlayerIndex, playersCopy)
                if (nextActivePlayerIndex != -1) {
                    currentPlayerIndex = nextActivePlayerIndex
                    throwsInRound = 0
                } else {
                    // Alle Spieler sind fertig - Spiel beendet
                    isFinished = true
                    println("DEBUG: Alle Spieler sind fertig - Spiel beendet!")
                }
            }
        }
        
        // Neues Game-Objekt zuweisen mit aktualisierter Throw-History
        val updatedThrowHistory = if (legWon && !isFinished) {
            // Neues Leg: Throw-History zurÃ¼cksetzen
            mutableListOf<DartThrow>()
        } else {
            currentGame.throwHistory.toMutableList().apply {
                dartThrow?.let { add(it) }
            }
        }
        
        game = currentGame.copy(
            players = playersCopy,
            currentPlayerIndex = currentPlayerIndex,
            isFinished = isFinished,
            throwsInRound = throwsInRound,
            throwHistory = updatedThrowHistory,
            currentLeg = currentLeg
        )
    }
    
    // CRICKET-MODUS: Spezielle Logik fÃ¼r Cricket
    private fun enterCricketScore(score: Int, position: androidx.compose.ui.geometry.Offset?, multiplier: Int, currentGame: Game) {
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        
        // Bestimme welches Segment getroffen wurde
        val baseScore = if (multiplier > 0) score / multiplier else score
        val cricketSegment: Int? = when {
            baseScore == 25 -> 25 // Bull (25 = Single Bull, 50 = Double Bull)
            baseScore in 15..20 -> baseScore // GÃ¼ltige Cricket-Segmente
            else -> null // Nicht gÃ¼ltig fÃ¼r Cricket
        }
        
        // DartThrow-Objekt erstellen (IMMER fÃ¼r alle Segmente, auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        println("DEBUG Cricket: DartThrow erstellt - Score: $score, Multiplier: $multiplier, Position: ${position ?: "Unspecified"}, PlayerIndex: ${currentGame.currentPlayerIndex}")
        
        if (position == null) {
            println("DEBUG Cricket: WARNUNG - Keine Position fÃ¼r DartThrow! Der Wurf wird nicht auf dem Dartboard angezeigt.")
        }
        
        // Deep copy der Spieler mit aktualisierten Statistiken
        val playersCopy = currentGame.players.toMutableList()
        val player = playersCopy[currentGame.currentPlayerIndex]
        
        // Basis-Update: Wurf und Score immer zur Statistik hinzufÃ¼gen
        var updatedPlayer = player.copy(
            scores = player.scores.toMutableList().apply { add(score) },
            dartThrows = player.dartThrows.toMutableList().apply { 
                dartThrow?.let { add(it) }
            }
        )
        playersCopy[currentGame.currentPlayerIndex] = updatedPlayer
        
        var isFinished = false
        var currentLeg = currentGame.currentLeg
        var legWon = false
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        var winners = emptyList<Player>()
        var pointsScoredThisDart = 0
        var announceAction: String? = null

        // Wenn Segment gÃ¼ltig fÃ¼r Cricket, Marken und Punkte berechnen
        if (cricketSegment != null) {
            // Marken hinzufÃ¼gen (Single=1, Double=2, Triple=3)
            // WICHTIG: FÃ¼r Bull: 25 = Single Bull (1 Mark), 50 = Double Bull (2 Marks)
            val marksToAdd = when {
                cricketSegment == 25 && score == 50 -> 2 // Double Bull = 2 Marks
                cricketSegment == 25 && score == 25 -> 1 // Single Bull = 1 Mark
                multiplier == 3 -> 3 // Triple = 3 Marken
                multiplier == 2 -> 2 // Double = 2 Marken
                else -> 1 // Single = 1 Marke
            }
            
            val currentMarks = updatedPlayer.cricketMarks.getOrDefault(cricketSegment, 0)
            val newMarks = currentMarks + marksToAdd // KEINE Begrenzung auf 3 - Marks Ã¼ber 3 hinaus zÃ¤hlen als Scoring-Marks
            
            // Update Marks fÃ¼r aktuellen Spieler
            val updatedMarks = updatedPlayer.cricketMarks.toMutableMap()
            updatedMarks[cricketSegment] = newMarks
            updatedPlayer = updatedPlayer.copy(cricketMarks = updatedMarks)
            playersCopy[currentGame.currentPlayerIndex] = updatedPlayer

            // Punkteberechnung
            val wasAlreadyClosed = currentMarks >= 3
            val isNowClosed = newMarks >= 3
            
            if (isNowClosed && !wasAlreadyClosed) {
                val allPlayersHaveItClosedNow = playersCopy.all { it.isSegmentClosed(cricketSegment) }
                announceAction = if (allPlayersHaveItClosedNow) "closed" else "open"
            }
            
            if (isNowClosed) {
                 val variant = currentGame.cricketConfig?.variant ?: CricketVariant.STANDARD
                 
                 // Berechne Scoring-Marks (Marks Ã¼ber 3 hinaus)
                 val scoringMarks = if (wasAlreadyClosed) {
                     marksToAdd
                 } else {
                     newMarks - 3
                 }
                 
                 if (scoringMarks > 0) {
                     val pointsPerMark = if (cricketSegment == 25) 25 else cricketSegment
                     val pointsTotal = scoringMarks * pointsPerMark
                     
                     if (variant == CricketVariant.STANDARD) {
                         // STANDARD: Punkte fÃ¼r mich, wenn andere noch offen haben
                          val hasOpponentWithOpenSegment = playersCopy.any { opponent ->
                            opponent.name != updatedPlayer.name && !opponent.isSegmentClosed(cricketSegment)
                        }
                        
                        if (hasOpponentWithOpenSegment) {
                            updatedPlayer = updatedPlayer.copy(currentScore = updatedPlayer.currentScore + pointsTotal)
                            playersCopy[currentGame.currentPlayerIndex] = updatedPlayer
                            pointsScoredThisDart = pointsTotal
                            println("DEBUG Cricket: Punkte fÃ¼r ${updatedPlayer.name}: +$pointsTotal")
                        }
                     } else {
                         // CUT THROAT: Punkte fÃ¼r Gegner, die das Segment noch offen haben
                         var pointsAwarded = false
                         playersCopy.forEachIndexed { paramsIndex, opponent ->
                             if (paramsIndex != currentGame.currentPlayerIndex && !opponent.isSegmentClosed(cricketSegment)) {
                                 // Gegner hat Segment noch offen -> bekommt Punkte (Strafe)
                                 val newOpponentScore = opponent.currentScore + pointsTotal
                                 playersCopy[paramsIndex] = opponent.copy(currentScore = newOpponentScore)
                                 pointsAwarded = true
                                 println("DEBUG Cricket Cut Throat: Punkte fÃ¼r Gegner ${opponent.name}: +$pointsTotal")
                             }
                         }
                         if (pointsAwarded) {
                             pointsScoredThisDart = pointsTotal
                         }
                     }
                 }
            }
        } else {
            println("DEBUG: Cricket - Segment $score ist nicht gÃ¼ltig fÃ¼r Cricket-Punkte (nur 15-20 und Bull)")
        }
        
        // Spielende prÃ¼fen: Hat der aktuelle Spieler alle Segmente geschlossen?
        val currentPlayerUpdated = playersCopy[currentGame.currentPlayerIndex]
        val currentPlayerFinished = currentPlayerUpdated.hasAllSegmentsClosed()
        
        // PrÃ¼fe ob alle Spieler fertig sind (alle Segmente geschlossen)
        val allPlayersFinished = playersCopy.all { it.hasAllSegmentsClosed() }
        
        if (allPlayersFinished) {
            // Alle Spieler haben alle Segmente geschlossen
            
             val variant = currentGame.cricketConfig?.variant ?: CricketVariant.STANDARD
             
            // Gewinnerbestimmung je nach Variante
            winners = if (variant == CricketVariant.CUT_THROAT) {
                // Cut Throat: Gewinner ist wer die NIEDRIGSTE Punktzahl hat
                val minScore = playersCopy.minOfOrNull { it.currentScore } ?: 0
                playersCopy.filter { it.currentScore == minScore }
            } else {
                // Standard: Gewinner ist wer die HÃ–CHSTE Punktzahl hat
                val maxScore = playersCopy.maxOfOrNull { it.currentScore } ?: 0
                playersCopy.filter { it.currentScore == maxScore }
            }
            
            if (winners.isNotEmpty()) {
                // Leg gewonnen! (kann mehrere Gewinner bei Unentschieden geben)
                legWon = true
                
                // ErhÃ¶he wonLegs fÃ¼r alle Gewinner
                winners.forEach { winner ->
                    val winnerIndex = playersCopy.indexOfFirst { it.name == winner.name }
                    if (winnerIndex != -1) {
                        val winnerPlayer = playersCopy[winnerIndex]
                        val newWonLegs = winnerPlayer.wonLegs + 1
                        playersCopy[winnerIndex] = winnerPlayer.copy(wonLegs = newWonLegs)
                        println("DEBUG Cricket: Spieler ${winnerPlayer.name} hat Leg ${currentGame.currentLeg} gewonnen (Legs: $newWonLegs/${currentGame.totalLegs})")
                    }
                }
                
                // PrÃ¼fe ob ein Spieler genug Legs gewonnen hat (totalLegs / 2 + 1)
                val requiredLegs = if (currentGame.players.size == 1) currentGame.totalLegs else (currentGame.totalLegs / 2) + 1
                val hasGameWinner = playersCopy.any { it.wonLegs >= requiredLegs }
                
                if (hasGameWinner) {
                    // Spiel gewonnen!
                    isFinished = true
                    val gameWinner = playersCopy.first { it.wonLegs >= requiredLegs }
                    println("DEBUG Cricket: Spiel beendet! Gewinner: ${gameWinner.name} mit ${gameWinner.wonLegs} Legs")
                } else {
                    // Neues Leg starten
                    currentLeg++
                    println("DEBUG Cricket: Neues Leg $currentLeg gestartet")
                    
                    // Setze Marks und Punkte zurÃ¼ck, aber behalte wonLegs
                    playersCopy.forEachIndexed { idx, p ->
                        val resetMarks = mutableMapOf<Int, Int>(
                            15 to 0, 16 to 0, 17 to 0, 18 to 0, 19 to 0, 20 to 0, 25 to 0
                        )
                        playersCopy[idx] = p.copy(
                            currentScore = 0,
                            cricketMarks = resetMarks
                        )
                    }
                    
                    // Gewinner des vorherigen Legs startet das nÃ¤chste Leg
                    val winnerIndex = playersCopy.indexOfFirst { it.name == winners[0].name }
                    if (winnerIndex != -1) {
                        currentPlayerIndex = winnerIndex
                    }
                    throwsInRound = 0
                }
            }
        } else if (currentPlayerFinished) {
            // Aktueller Spieler hat alle Segmente geschlossen, aber andere noch nicht
            // (Logik zur Information, keine Ã„nderung am Status nÃ¶tig)
        }
        
        // 1. Single Dart Sound
        if (audioAnnounceEachDart && !legWon) {
            val voiceKey = when {
                score == 50 -> "Bullseye"
                score == 25 -> "Twenty Five"
                multiplier == 3 -> "Triple ${score / multiplier}"
                multiplier == 2 -> "Double ${score / multiplier}"
                score == 0 -> "Miss"
                else -> score.toString()
            }
            
            var announcement = voiceKey
            
            if (announceAction != null) {
                announcement += " ... $announceAction"
            }
            
            if (pointsScoredThisDart > 0) {
                announcement += " ... Score"
            }
            
            triggerSound(announcement, 0)
        }

        // 2. Leg / Match Win
        if (legWon) {
            if (isFinished) {
                triggerSound("Game Shot and the Win")
            } else {
                triggerSound("Game Shot")
            }
        }

        if (!legWon) { // Nur fortfahren wenn kein neues Leg gestartet wurde
            if (currentPlayerFinished && !allPlayersFinished) {
                // Aktueller Spieler ist fertig, aber es gibt noch andere
                println("DEBUG Cricket: Spieler ${currentPlayerIndex + 1} (${playersCopy[currentPlayerIndex].name}) hat alle Segmente geschlossen, aber andere Spieler spielen noch weiter")
                val nextActivePlayerIndex = findNextActivePlayerCricket(currentPlayerIndex, playersCopy)
                if (nextActivePlayerIndex != -1) {
                    currentPlayerIndex = nextActivePlayerIndex
                    throwsInRound = 0
                    println("DEBUG Cricket: NÃ¤chster aktiver Spieler: ${playersCopy[nextActivePlayerIndex].name} (Index: $nextActivePlayerIndex)")
                }
            } else if (!isFinished && throwsInRound >= 3) {
                // Normale Runden-Logik
                val nextActivePlayerIndex = findNextActivePlayerCricket(currentPlayerIndex, playersCopy)
                if (nextActivePlayerIndex != -1) {
                    currentPlayerIndex = nextActivePlayerIndex
                    throwsInRound = 0
                }
            }
        }
        
        // Neues Game-Objekt zuweisen
        val updatedThrowHistory = if (legWon && !isFinished) {
            // Neues Leg: Throw-History zurÃ¼cksetzen
            mutableListOf<DartThrow>()
        } else {
            currentGame.throwHistory.toMutableList().apply {
                dartThrow?.let { 
                    add(it)
                    println("DEBUG Cricket: DartThrow zur throwHistory hinzugefÃ¼gt. Neue GrÃ¶ÃŸe: ${size}")
                }
            }
        }
        
        game = currentGame.copy(
            players = playersCopy,
            currentPlayerIndex = currentPlayerIndex,
            isFinished = isFinished,
            throwsInRound = throwsInRound,
            throwHistory = updatedThrowHistory,
            currentLeg = currentLeg
        )
        
        if (isFinished) {
            println("DEBUG: Cricket - Spiel beendet! Alle Spieler haben alle Segmente geschlossen!")
        } else if (legWon) {
            println("DEBUG: Cricket - Leg $currentLeg gestartet (vorheriges Leg wurde gewonnen)")
        } else if (currentPlayerUpdated.hasAllSegmentsClosed()) {
            println("DEBUG: Cricket - Spieler ${currentPlayerUpdated.name} hat alle Segmente geschlossen, andere Spieler spielen weiter")
        }
    }
    
    private fun enterAllFivesScore(score: Int, position: androidx.compose.ui.geometry.Offset?, multiplier: Int, currentGame: Game) {
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        val isMiss = score == 0
        
        // Play the score sound for single dart
        if (audioAnnounceEachDart) {
            val voiceKey = when {
                score == 50 -> "Bullseye"
                score == 25 -> "Twenty Five"
                multiplier == 3 -> "Triple ${score / multiplier}"
                multiplier == 2 -> "Double ${score / multiplier}"
                score == 0 -> "Miss"
                else -> score.toString()
            }
            triggerSound(voiceKey, 0)
        }
        
        // Berechne aktuelles Runden-Total (Summe der letzten WÃ¼rfe des aktuellen Spielers in dieser Runde)
        var roundTotal = 0
        var throwsInCurrentRound = 0
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == currentGame.currentPlayerIndex) {
                // ZÃ¤hle nur WÃ¼rfe der aktuellen Runde (bis zum letzten Spielerwechsel)
                if (throwsInCurrentRound < currentGame.throwsInRound) {
                    roundTotal += throwData.score
                    throwsInCurrentRound++
                } else {
                    break
                }
            } else {
                break
            }
        }
        
        // FÃ¼ge aktuellen Wurf zum Runden-Total hinzu (wenn kein Miss)
        val newRoundTotal = if (isMiss) roundTotal else roundTotal + score
        
        // Spezielle Regel: Wenn Runden-Total bereits durch 5 teilbar ist UND ein Miss geworfen wird
        // â†’ Runde wird sofort als ungÃ¼ltig markiert
        val roundWasDivisibleBy5 = roundTotal > 0 && roundTotal % 5 == 0
        val roundInvalidated = roundWasDivisibleBy5 && isMiss
        
        // DartThrow-Objekt erstellen (auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        
        // Deep copy der Spieler
        val playersCopy = currentGame.players.mapIndexed { idx, player ->
            if (idx == currentGame.currentPlayerIndex) {
                val newScores = player.scores.toMutableList().apply { add(score) }
                val newDartThrows = player.dartThrows.toMutableList().apply { 
                    dartThrow?.let { add(it) }
                }
                player.copy(
                    scores = newScores,
                    dartThrows = newDartThrows
                )
            } else {
                player.copy(
                    scores = player.scores.toMutableList(),
                    dartThrows = player.dartThrows.toMutableList()
                )
            }
        }.toMutableList()
        
        // Runden-Logik
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        var isFinished = false
        var currentLeg = currentGame.currentLeg
        
        // Wenn Runde ungÃ¼ltig gemacht wurde, setze Runden-Total auf 0
        val finalRoundTotal = if (roundInvalidated) 0 else newRoundTotal
        
        // Nach 3 WÃ¼rfen: Runde auswerten
        if (throwsInRound >= 3) {
            val currentPlayerUpdated = playersCopy[currentGame.currentPlayerIndex]
            
            // PrÃ¼fe ob Runde gÃ¼ltig ist:
            // 1. Runden-Total muss durch 5 teilbar sein
            // 2. Alle 3 Pfeile mÃ¼ssen getroffen werden (keine Misses)
            // Sammle die letzten 3 WÃ¼rfe des aktuellen Spielers (inkl. aktueller Wurf)
            val lastThreeThrows = mutableListOf<DartThrow>()
            var foundThrows = 0
            // FÃ¼ge zuerst aktuellen Wurf hinzu
            dartThrow?.let { lastThreeThrows.add(it) }
            // Dann die letzten 2 WÃ¼rfe aus der History
            for (i in currentGame.throwHistory.size - 1 downTo 0) {
                val throwData = currentGame.throwHistory[i]
                if (throwData.playerIndex == currentGame.currentPlayerIndex && foundThrows < 2) {
                    lastThreeThrows.add(0, throwData)
                    foundThrows++
                } else if (throwData.playerIndex != currentGame.currentPlayerIndex) {
                    break
                }
            }
            
            // PrÃ¼fe ob alle 3 WÃ¼rfe getroffen wurden (kein Miss)
            val hasNoMisses = lastThreeThrows.size == 3 && lastThreeThrows.all { it.score > 0 }
            val isDivisibleBy5 = finalRoundTotal > 0 && finalRoundTotal % 5 == 0
            val isValidRound = isDivisibleBy5 && hasNoMisses && !roundInvalidated
            
            if (isValidRound) {
                // Runde ist gÃ¼ltig: Punkte vergeben (jetzt direkt Zahlenwert summiert)
                val pointsEarned = finalRoundTotal
                val newScore = currentPlayerUpdated.currentScore + pointsEarned
                
                println("DEBUG All Fives: GÃ¼ltige Runde! Runden-Total: $finalRoundTotal, Punkte: $pointsEarned")
                
                // Aktualisiere Spieler-Score
                playersCopy[currentGame.currentPlayerIndex] = currentPlayerUpdated.copy(
                    currentScore = newScore,
                    totalRounds = currentPlayerUpdated.totalRounds + 1
                )
                
                var soundToPlay = "bing"
                
                // PrÃ¼fe ob Spieler Ziel-Punktzahl erreicht hat (Leg gewonnen)
                if (newScore >= currentGame.targetScore) {
                    // Leg gewonnen!
                    val updatedPlayer = playersCopy[currentGame.currentPlayerIndex]
                    val newWonLegs = updatedPlayer.wonLegs + 1
                    playersCopy[currentGame.currentPlayerIndex] = updatedPlayer.copy(
                        wonLegs = newWonLegs
                    )
                    
                    // PrÃ¼fe ob Spieler genug Legs gewonnen hat (Anzahl Legs Ã· 2 + 1)
                    val requiredLegs = if (currentGame.players.size == 1) currentGame.totalLegs else (currentGame.totalLegs / 2) + 1
                    if (newWonLegs >= requiredLegs) {
                        // Spiel gewonnen!
                        isFinished = true
                        soundToPlay = "bing ... Game Shot and the Win"
                    } else {
                        // Neues Leg starten
                        currentLeg++
                        soundToPlay = "bing ... Leg to ... Game on"
                        // Setze alle Punkte auf 0 zurÃ¼ck
                        for (i in playersCopy.indices) {
                            playersCopy[i] = playersCopy[i].copy(currentScore = 0)
                        }
                        
                        // Spieler-Reihenfolge mischen wenn aktiviert
                        if (currentGame.randomizeOrder) {
                            val shuffledPlayers = playersCopy.shuffled()
                            playersCopy.clear()
                            playersCopy.addAll(shuffledPlayers)
                            currentPlayerIndex = 0
                            println("DEBUG All Fives: Spieler-Reihenfolge fÃ¼r Leg $currentLeg gemischt")
                        } else {
                            currentPlayerIndex = 0
                        }
                        
                        println("DEBUG All Fives: Neues Leg $currentLeg gestartet")
                    }
                }
                triggerSound(soundToPlay)
            } else {
                // Runde ist ungÃ¼ltig: Keine Punkte
                playersCopy[currentGame.currentPlayerIndex] = currentPlayerUpdated.copy(
                    totalRounds = currentPlayerUpdated.totalRounds + 1
                )
                triggerSound("buzzer")
                println("DEBUG All Fives: UngÃ¼ltige Runde! Runden-Total: $finalRoundTotal (nicht durch 5 teilbar oder Miss)")
            }
            
            // Zum nÃ¤chsten Spieler wechseln
            if (!isFinished) {
                currentPlayerIndex = (currentPlayerIndex + 1) % playersCopy.size
                throwsInRound = 0
            }
        }
        
        // Neues Game-Objekt zuweisen
        val updatedThrowHistory = currentGame.throwHistory.toMutableList().apply {
            dartThrow?.let { add(it) }
        }
        
        game = currentGame.copy(
            players = playersCopy,
            currentPlayerIndex = currentPlayerIndex,
            isFinished = isFinished,
            throwsInRound = throwsInRound,
            throwHistory = updatedThrowHistory,
            currentLeg = currentLeg
        )
    }
    
    // setMultiplier wurde entfernt - der Multiplikator wird direkt aus dem Dartboard berechnet
    
    // ATC-MODUS: Spezielle Logik fÃ¼r Around the Clock
    private fun enterATCScore(score: Int, position: androidx.compose.ui.geometry.Offset?, multiplier: Int, currentGame: Game) {
        val currentPlayer = currentGame.players[currentGame.currentPlayerIndex]
        val atcConfig = currentGame.atcConfig ?: ATCGameState() // Fallback zu Standard
        
        // PrÃ¼fe ob Treffer relevant ist
        val baseScore = if (multiplier > 0) score / multiplier else score
        val currentTarget = currentPlayer.atcCurrentTarget
        val isTargetHit = baseScore == currentTarget
        
        // Spezieller Check fÃ¼r Bull (25 oder 50)
        val isBullHit = (score == 25 || score == 50) && currentTarget == 25
        
        // Spezieller Check fÃ¼r Shanghai und Quickly
        var advanceBy = 0
        var shanghaiHit: Int? = null
        
        if (isTargetHit || isBullHit) {
            when (atcConfig.variant) {
                ATCVariant.STANDARD -> {
                     // Standard: Beliebiges Segment der Zahl (S, D, T) zÃ¤hlt
                     advanceBy = 1
                }
                ATCVariant.DOUBLE -> {
                     // Nur Doubles zÃ¤hlen (oder Double Bull fÃ¼r Target 25)
                     if (multiplier == 2 || (score == 50 && currentTarget == 25)) {
                         advanceBy = 1
                     }
                }
                ATCVariant.TRIPLE -> {
                     // Nur Triples zÃ¤hlen (Bull hat keine Triples, daher erlauben wir beliebige Bull-Treffer fÃ¼r Target 25)
                     if (multiplier == 3 || (currentTarget == 25 && (score == 25 || score == 50))) {
                         advanceBy = 1
                     }
                }
                ATCVariant.QUICKLY -> {
                    // S=1, D=2, T=3 SprÃ¼nge
                    if (currentTarget == 25) {
                        // Bei Bull zÃ¤hlt Bull simple als +1 (oder Spielende)
                        advanceBy = 1
                    } else {
                        advanceBy = multiplier
                         // Bei Double Bull (50) als Ziel 25 -> Spielende
                        if (score == 50 && currentTarget == 25) advanceBy = 1
                    }
                }
                ATCVariant.SHANGHAI -> {
                    // Muss S, D und T in Reihenfolge (1 -> 2 -> 3) treffen
                    val currentHits = currentPlayer.atcShanghaiHits
                    val nextRequired = when {
                        currentHits.isEmpty() -> 1
                        currentHits.contains(1) && !currentHits.contains(2) -> 2
                        currentHits.contains(1) && currentHits.contains(2) -> 3
                        else -> 1
                    }
                    
                    if (currentTarget == 25) {
                        val hitType = if (score == 25) 1 else 2 // 1=Single Bull, 2=Double Bull
                        if (hitType == nextRequired) {
                            shanghaiHit = hitType
                        }
                    } else {
                        if (multiplier == nextRequired) {
                            shanghaiHit = multiplier
                        }
                    }
                }
            }
        }
        
        // Handling Logic
        var newTarget = currentTarget
        var newShanghaiHits = currentPlayer.atcShanghaiHits
        
        if (atcConfig.variant == ATCVariant.SHANGHAI) {
            shanghaiHit?.let { hit ->
                // FÃ¼ge Hit hinzu
                val updatedHits = newShanghaiHits.toMutableSet().apply { add(hit) }
                newShanghaiHits = updatedHits
                
                // PrÃ¼fe ob weiter
                val neededHits = if (currentTarget == 25) setOf(1, 2) else setOf(1, 2, 3) 
                if (updatedHits.containsAll(neededHits)) {
                    advanceBy = 1
                    newShanghaiHits = emptySet()
                }
            }
        }
        
        // Wenn weitergeschaltet werden soll
        var isFinished = false
        if (advanceBy > 0) {
            if (atcConfig.reverse) {
                if (currentTarget == 25) {
                    newTarget = 20
                } else {
                    newTarget = currentTarget - advanceBy
                    if (newTarget < 1) {
                        isFinished = true
                    }
                }
            } else {
                if (currentTarget == 25) {
                    isFinished = true
                } else {
                    newTarget = currentTarget + advanceBy
                    if (newTarget > 20) {
                        if (atcConfig.withBulls) {
                            newTarget = 25
                        } else {
                            isFinished = true
                        }
                    }
                }
            }
        }
        
        val wasHit = advanceBy > 0 || (atcConfig.variant == ATCVariant.SHANGHAI && shanghaiHit != null)
        triggerSound(if (wasHit) "bing" else "buzzer")

        // DartThrow erstellen (auch bei position == null fÃ¼r vollstÃ¤ndige Historie)
        val dartThrow = DartThrow(
            score = score,
            position = position ?: androidx.compose.ui.geometry.Offset.Unspecified,
            playerIndex = currentGame.currentPlayerIndex,
            multiplier = multiplier
        )
        
        // Spieler updaten
        val playersCopy = currentGame.players.mapIndexed { idx, player ->
            if (idx == currentGame.currentPlayerIndex) {
                player.copy(
                    currentScore = 0, // Score ist im ATC irrelevant fÃ¼r Gewinn, nutzen wir fÃ¼r Anzeige?
                    atcCurrentTarget = if (isFinished) 0 else newTarget, // 0 = Finished
                    atcShanghaiHits = newShanghaiHits,
                    scores = player.scores.toMutableList().apply { add(score) },
                    dartThrows = player.dartThrows.toMutableList().apply { 
                         dartThrow?.let { add(it) }
                    }
                )
            } else {
                player.copy(
                    scores = player.scores.toMutableList(),
                    dartThrows = player.dartThrows.toMutableList()
                )
            }
        }.toMutableList()
        
        // PrÃ¼fen ob finished
        if (isFinished) {
             println("DEBUG ATC: Player finished!")
        }
        
        // Runden-Logik (Standard 3 WÃ¼rfe)
        var throwsInRound = currentGame.throwsInRound + 1
        var currentPlayerIndex = currentGame.currentPlayerIndex
        var gameFinished = isFinished // Wenn aktueller Spieler fertig ist, ist das Spiel vorbei? 
        // Standard: Wer zuerst fertig ist gewinnt Leg.
        // Aber Runde zu Ende spielen?
        // User Docs: "Ein Leg oder das Spiel endet erst, wenn der aktuelle Spieler seine 3 Darts geworfen hat."
        
        if (isFinished) {
            // Markiere Spieler als fertig (Target 0 oder Ã¤hnliches Flag wÃ¤re gut, hier nutzen wir isFinished im Game status spÃ¤ter)
            // PrÃ¼fe am Ende der Runde wer gewonnen hat.
            // ABER: "Wer zuerst das Ziel erreicht, gewinnt das Leg."
            // UND: "Ein Leg oder das Spiel endet erst, wenn der aktuelle Spieler seine 3 Darts geworfen hat."
            // Also: Spiel lÃ¤uft weiter bis throwsInRound == 3.
            gameFinished = false 
        }

        // Spielerwechsel nach 3 WÃ¼rfen
        if (throwsInRound >= 3) {
            // Check win condition
            // Hat jemand target erreicht (Target = 0 oder Target > max)?
            // Im ATC: target 0 oder so?
            // Wir schauen auf isFinished Flag vom Schritt oben.
            // Wir mÃ¼ssen speichern dass er fertig ist. `atcCurrentTarget` setzen wir auf 0/Finished?
            
            // Check if ANY player is finished
            val anyFinished = playersCopy.any { 
                   // Logic for finished player: 
                   // Reverse: target < 1 (or 0)
                   // Forward: target > 20 (or > 25)
                   // We set it to 0 if finished above.
                   // Wait, initialization is 1. 0 is a good "finished" marker? 
                   // Let's assume 0 means "Done".
                   it.atcCurrentTarget == 0
            }
            
            if (anyFinished) {
                gameFinished = true
                val winner = playersCopy.first { it.atcCurrentTarget == 0 }
                triggerSound("Game Shot and the Win")
            } else {
                 currentPlayerIndex = (currentPlayerIndex + 1) % playersCopy.size
                 throwsInRound = 0
            }
        }
        
        // Update Game
        val updatedThrowHistory = currentGame.throwHistory.toMutableList().apply {
            dartThrow?.let { add(it) }
        }
        
        game = Game(
            players = playersCopy,
            mode = currentGame.mode,
            currentPlayerIndex = currentPlayerIndex,
            isFinished = gameFinished,
            throwsInRound = throwsInRound,
            throwHistory = updatedThrowHistory,
            doubleIn = currentGame.doubleIn,
            doubleOut = currentGame.doubleOut,
            targetScore = currentGame.targetScore,
            totalLegs = currentGame.totalLegs,
            currentLeg = currentGame.currentLeg,
            randomizeOrder = currentGame.randomizeOrder,
            atcConfig = currentGame.atcConfig,
            startScore = currentGame.startScore
        )
    }

    // ML-optimierte Punkteeingabe mit Konfidenz
    fun enterScoreML(score: Int, confidence: Double, position: androidx.compose.ui.geometry.Offset? = null, multiplier: Int = 1) {
        // BerÃ¼cksichtige ML-Konfidenz fÃ¼r bessere Entscheidungen
        val adjustedScore = when {
            confidence > 0.8 -> score // Hohe Konfidenz
            confidence > 0.6 -> score // Mittlere Konfidenz
            confidence > 0.4 -> score // Niedrige Konfidenz, aber akzeptabel
            else -> return // Zu niedrige Konfidenz, ignoriere
        }
        
        enterScore(adjustedScore, position, multiplier)
    }

    fun undo() {
        val currentGame = game ?: return
        
        // PrÃ¼fe ob Ã¼berhaupt WÃ¼rfe vorhanden sind
        if (currentGame.throwHistory.isEmpty()) {
            println("DEBUG: Keine WÃ¼rfe zum RÃ¼ckgÃ¤ngig machen verfÃ¼gbar")
            return
        }
        
        val currentPlayerIndex = currentGame.currentPlayerIndex
        val lastThrowInHistory = currentGame.throwHistory.lastOrNull()
        

        
        // Normales Undo: Entferne den letzten Wurf aus der Throw-History (LIFO-Prinzip)
        // Erstelle eine Kopie der Throw-History fÃ¼r die Manipulation
        val updatedThrowHistory = currentGame.throwHistory.toMutableList()
        val lastThrow = updatedThrowHistory.removeAt(updatedThrowHistory.size - 1)
        val playerIndex = lastThrow.playerIndex
        
        println("DEBUG: Undo - Entferne letzten Wurf von Spieler ${playerIndex + 1}: Score ${lastThrow.score}")
        
        // Erstelle eine Kopie des aktuellen Spiels mit korrekter Deep Copy
        val playersCopy = currentGame.players.map { player ->
            player.copy(
                cricketMarks = player.cricketMarks.toMutableMap(),
                scores = player.scores.toMutableList(),
                dartThrows = player.dartThrows.toMutableList()
            )
        }.toMutableList()
        
        // Entferne den Wurf vom entsprechenden Spieler
        val targetPlayer = playersCopy[playerIndex]
        if (targetPlayer.scores.isNotEmpty()) {
            val removedScore = targetPlayer.scores.removeAt(targetPlayer.scores.size - 1)
            
            // Spielmodus-spezifisches Undo der Punkte und Stati
            when (currentGame.mode) {
                GameMode.CRICKET -> {
                    // Cricket-Undo: Marks und Scoring-Punkte zurÃ¼cksetzen
                    val baseScore = if (lastThrow.multiplier > 0) lastThrow.score / lastThrow.multiplier else lastThrow.score
                    val segment = when {
                        baseScore == 25 -> 25
                        baseScore in 15..20 -> baseScore
                        else -> null
                    }
                    
                    if (segment != null) {
                        // Marks berechnen die entfernt werden mÃ¼ssen
                        val marksToRemove = when {
                            segment == 25 && lastThrow.score == 50 -> 2
                            segment == 25 && lastThrow.score == 25 -> 1
                            else -> lastThrow.multiplier
                        }
                        
                        val currentMarks = targetPlayer.cricketMarks.getOrDefault(segment, 0)
                        val oldMarks = maxOf(0, currentMarks - marksToRemove)
                        
                        // Scoring-Punkte berechnen die zurÃ¼ckgenommen werden mÃ¼ssen
                        // Scoring-Marks sind Marks, die den Schwellenwert von 3 bereits Ã¼berschritten hatten
                        val scoringMarksCalculated = maxOf(0, currentMarks - maxOf(3, oldMarks))
                        
                        if (scoringMarksCalculated > 0) {
                            val pointsPerMark = if (segment == 25) 25 else segment
                            val pointsToUndo = scoringMarksCalculated * pointsPerMark
                            val variant = currentGame.cricketConfig?.variant ?: CricketVariant.STANDARD
                            
                            if (variant == CricketVariant.STANDARD) {
                                // Standard: Punkte vom eigenen Score abziehen
                                targetPlayer.currentScore -= pointsToUndo
                                println("DEBUG Cricket Undo: Score reduziert um $pointsToUndo fÃ¼r ${targetPlayer.name}")
                            } else {
                                // Cut-Throat: Punkte von allen Gegnern abziehen, die das Segment offen haben
                                playersCopy.forEachIndexed { idx, player ->
                                    if (idx != playerIndex && !player.isSegmentClosed(segment)) {
                                        player.currentScore -= pointsToUndo
                                        println("DEBUG Cricket Cut Throat Undo: Score reduziert um $pointsToUndo fÃ¼r Gegner ${player.name}")
                                    }
                                }
                            }
                        }
                        
                        // Marks in der Map aktualisieren
                        targetPlayer.cricketMarks[segment] = oldMarks
                        println("DEBUG Cricket Undo: Marks fÃ¼r Segment $segment reduziert von $currentMarks auf $oldMarks")
                    }
                }
                GameMode.ALL_FIVES -> {
                    // Wenn wir das Ende einer Runde undo-en (throwsInRound == 0),
                    // mÃ¼ssen wir prÃ¼fen ob in dieser abgeschlossenen Runde Punkte vergeben wurden.
                    if (currentGame.throwsInRound == 0) {
                        val lastThreeThrows = mutableListOf<DartThrow>()
                        for (i in currentGame.throwHistory.size - 1 downTo 0) {
                            val throwData = currentGame.throwHistory[i]
                            if (throwData.playerIndex == playerIndex && lastThreeThrows.size < 3) {
                                lastThreeThrows.add(0, throwData)
                            } else if (throwData.playerIndex != playerIndex) {
                                break
                            }
                        }
                        
                        if (lastThreeThrows.size == 3) {
                            val roundSum = lastThreeThrows.sumOf { it.score }
                            val hasNoMisses = lastThreeThrows.all { it.score > 0 }
                            val isDivisibleBy5 = roundSum > 0 && roundSum % 5 == 0
                            val isValidRound = isDivisibleBy5 && hasNoMisses
                            
                            if (isValidRound) {
                                targetPlayer.currentScore -= roundSum
                                println("DEBUG All Fives Undo: Runden-Punkte $roundSum von ${targetPlayer.name} abgezogen")
                            }
                        }
                    }
                }
                GameMode.AROUND_THE_CLOCK -> {
                    // ATC: Punkte sind immer 0, aber Target muss ggf. dekrementiert werden
                    // (Dies erfordert eine PrÃ¼fung ob der entfernte Wurf ein gÃ¼ltiger Hit war)
                    // TODO: Target-Reversion implementieren
                    println("DEBUG ATC Undo: Target-Reversion noch nicht implementiert")
                }
                else -> {
                    if (lastThrow.isBust) {
                        // Bust-Undo: Der Wurf war ein Bust. Der Score wurde am Ende der Runde (beim Bust-Wurf)
                        // auf den Score zu Rundenbeginn zurÃ¼ckgesetzt. Um auf den Score vor dem Bust-Wurf zurÃ¼ckzukehren,
                        // mÃ¼ssen wir die Summe der anderen (gÃ¼ltigen) WÃ¼rfe der Runde abziehen.
                        val newThrowsInRound = calculateThrowsInRound(playerIndex, updatedThrowHistory)
                        val otherThrowsSum = targetPlayer.scores.takeLast(newThrowsInRound).sum()
                        targetPlayer.currentScore -= otherThrowsSum
                        println("DEBUG X01 Undo (Bust): Score um $otherThrowsSum auf ${targetPlayer.currentScore} reduziert")
                    } else {
                        // X01: Punkte zum Score hinzufÃ¼gen (da sie beim Wurf abgezogen wurden)
                        targetPlayer.currentScore += removedScore
                        println("DEBUG X01 Undo: Score $removedScore zu Spieler ${playerIndex + 1} hinzugefÃ¼gt, neuer Score: ${targetPlayer.currentScore}")
                    }
                }
            }
        }
        
        // Entferne die entsprechende Dart-Position
        val removedThrow: DartThrow? = if (targetPlayer.dartThrows.isNotEmpty()) {
            targetPlayer.dartThrows.removeAt(targetPlayer.dartThrows.size - 1)
            println("DEBUG: Dart-Position von Spieler ${playerIndex + 1} entfernt")
            // Speichere den entfernten Wurf fÃ¼r Double-In PrÃ¼fung
            lastThrow
        } else {
            null
        }
        
        // Double-In Status prÃ¼fen und ggf. zurÃ¼cksetzen
        // Wenn der entfernte Wurf ein Double war (der Double-In erreicht hat) und keine weiteren Doubles in der History sind
        if (currentGame.doubleIn && removedThrow != null) {
            val wasDoubleInWurf = removedThrow.multiplier == 2 || removedThrow.score == 50
            if (wasDoubleInWurf) {
                // PrÃ¼fe ob noch andere Double-WÃ¼rfe in der verbleibenden History sind
                val hasOtherDoubles = updatedThrowHistory.any { throwData ->
                    throwData.playerIndex == playerIndex && (throwData.multiplier == 2 || throwData.score == 50)
                }
                if (!hasOtherDoubles) {
                    // Keine weiteren Doubles: Double-In Status zurÃ¼cksetzen
                    targetPlayer.hasDoubleIn = false
                    println("DEBUG: Double-In Status zurÃ¼ckgesetzt fÃ¼r Spieler ${playerIndex + 1}")
                }
            }
        }
        
        // Berechne den neuen Spieler und die Wurf-Anzahl
        val newCurrentPlayerIndex = playerIndex
        val newThrowsInRound = calculateThrowsInRound(newCurrentPlayerIndex, updatedThrowHistory)
        
        // Neues Game-Objekt erstellen mit aktualisierter Throw-History
        game = currentGame.copy(
            players = playersCopy,
            currentPlayerIndex = newCurrentPlayerIndex,
            isFinished = false,
            throwsInRound = newThrowsInRound,
            throwHistory = updatedThrowHistory
        )
        
        println("DEBUG: Undo abgeschlossen - Spieler ${newCurrentPlayerIndex + 1} ist jetzt dran mit $newThrowsInRound WÃ¼rfen")
        println("DEBUG: Throw-History hat jetzt ${updatedThrowHistory.size} EintrÃ¤ge (vorher: ${currentGame.throwHistory.size})")
    }
    
    // Hilfsfunktion zur Berechnung der WÃ¼rfe in der aktuellen Runde
    private fun calculateThrowsInRound(currentPlayerIndex: Int, throwHistory: List<DartThrow>): Int {
        if (throwHistory.isEmpty()) return 0
        
        // ZÃ¤hle WÃ¼rfe des aktuellen Spielers von hinten bis zum ersten Wurf eines anderen Spielers.
        // Wichtig: Wir brechen ab, sobald ein anderer Spieler gefunden wurde, damit frÃ¼here
        // Runden desselben Spielers nicht mitgezÃ¤hlt werden.
        var throwsInCurrentRound = 0
        
        for (i in throwHistory.size - 1 downTo 0) {
            val throwData = throwHistory[i]
            if (throwData.playerIndex == currentPlayerIndex) {
                throwsInCurrentRound++
            } else {
                // Anderer Spieler â†’ aktuelle Runde ist vollstÃ¤ndig erfasst
                break
            }
        }
        
        return throwsInCurrentRound % 3
    }
    
    // Gibt die Dart-Positionen der AKTUELLEN Runde des aktuellen Spielers zurÃ¼ck.
    // Verwendet throwsInRound als exakten ZÃ¤hler, damit bei Einzel-Spieler-Szenarien
    // (kein anderer Spieler in der History) die WÃ¼rfe frÃ¼herer Runden nicht
    // fÃ¤lschlicherweise mit angezeigt werden.
    fun getCurrentRoundDartPositions(): List<androidx.compose.ui.geometry.Offset> {
        val currentGame = game ?: return emptyList()
        if (currentGame.throwHistory.isEmpty()) return emptyList()
        
        val currentPlayerIndex = currentGame.currentPlayerIndex
        val throwsInCurrentRound = currentGame.throwsInRound // exakte Anzahl WÃ¼rfe dieser Runde
        
        if (throwsInCurrentRound == 0) return emptyList()
        
        val currentRoundPositions = mutableListOf<androidx.compose.ui.geometry.Offset>()
        var throwsCollected = 0
        
        // Gehe rÃ¼ckwÃ¤rts und nehme genau throwsInRound WÃ¼rfe des aktuellen Spielers
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            if (throwsCollected >= throwsInCurrentRound) break
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == currentPlayerIndex) {
                currentRoundPositions.add(throwData.position)
                throwsCollected++
            } else {
                // Anderer Spieler â†’ Runde des aktuellen Spielers beginnt nicht weiter hinten
                break
            }
        }
        
        // Liste umkehren: Ã¤lteste WÃ¼rfe zuerst
        return currentRoundPositions.reversed()
    }
    
    // Berechnet die Summe der bisherigen WÃ¼rfe der aktuellen Runde.
    // Verwendet throwsInRound als exakten ZÃ¤hler (gleiche Logik wie getCurrentRoundDartPositions).
    fun getCurrentRoundSum(): Int {
        val currentGame = game ?: return 0
        
        val throwsInCurrentTurn = currentGame.throwsInRound
        if (throwsInCurrentTurn == 0) return 0
        
        var roundSum = 0
        val currentPlayerIndex = currentGame.currentPlayerIndex
        var throwsFound = 0
        
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            if (throwsFound >= throwsInCurrentTurn) break
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == currentPlayerIndex) {
                roundSum += throwData.score
                throwsFound++
            } else {
                // Anderer Spieler â†’ aktuelle Runde beginnt nicht weiter hinten
                break
            }
        }
        
        return roundSum
    }
    
    /**
     * Berechnet die Summe der zuletzt abgeschlossenen Runde.
     * Sucht rÃ¼ckwÃ¤rts in der throwHistory nach dem Block von WÃ¼rfen des Spielers,
     * der unmittelbar VOR dem aktuellen Spieler gespielt hat (anhand der History,
     * nicht per naiver Index-Arithmetik). Das ist korrekt auch wenn einzelne Spieler
     * bereits fertig sind und Ã¼bersprungen werden.
     */
    fun getLastCompletedRoundSum(): Int {
        val currentGame = game ?: return 0
        if (currentGame.throwHistory.isEmpty()) return 0
        
        // Finde den Spieler der als letztes geworfen hat (nicht den aktuellen Spieler)
        val lastThrow = currentGame.throwHistory.lastOrNull() ?: return 0
        val previousPlayerIndex = lastThrow.playerIndex
        
        // Aktueller Spieler hat noch nicht geworfen â†’ letzter Block gehÃ¶rt dem VorgÃ¤nger
        var roundSum = 0
        var throwsFound = 0
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == previousPlayerIndex && throwsFound < 3) {
                roundSum += throwData.score
                throwsFound++
            } else if (throwData.playerIndex != previousPlayerIndex) {
                break
            }
        }
        return roundSum
    }

    /**
     * PrÃ¼ft ob die zuletzt abgeschlossene Runde (des Spielers der gerade dran war)
     * fÃ¼r All Fives gÃ¼ltig war (durch 5 teilbar, kein Miss).
     */
    fun isLastCompletedRoundValid(): Boolean {
        val currentGame = game ?: return false
        if (currentGame.throwHistory.isEmpty()) return false
        
        // Finde den Spieler, der zuletzt geworfen hat (direkt aus History)
        val lastThrow = currentGame.throwHistory.lastOrNull() ?: return false
        val previousPlayerIndex = lastThrow.playerIndex
        
        val lastThreeThrows = mutableListOf<DartThrow>()
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == previousPlayerIndex && lastThreeThrows.size < 3) {
                lastThreeThrows.add(0, throwData)
            } else if (throwData.playerIndex != previousPlayerIndex) {
                break
            }
        }
        
        if (lastThreeThrows.size < 3) return false
        
        val roundSum = lastThreeThrows.sumOf { it.score }
        val hasNoMisses = lastThreeThrows.all { it.score > 0 }
        val isDivisibleBy5 = roundSum > 0 && roundSum % 5 == 0
        
        return isDivisibleBy5 && hasNoMisses
    }

    // NEU: Funktion um zu prÃ¼fen ob eine Runde gerade beendet wurde
    fun isRoundCompleted(): Boolean {
        val currentGame = game ?: return false
        return currentGame.throwsInRound == 0 && currentGame.throwHistory.isNotEmpty()
    }

    fun reset() {
        game = null
    }
    
    /**
     * Aktualisiert die Statistiken aller Spieler nach Spielende
     * Wird vom GameScreen aufgerufen wenn das Spiel beendet ist
     * Aktualisiert nur Spieler, die bereits in der Verwaltung existieren
     */
    fun updatePlayerStatisticsAfterGame(repository: PlayerRepository, forceSave: Boolean = false) {
        if (statisticsSavedForCurrentGame) return
        val finishedGame = game ?: return
        if (!finishedGame.isFinished && !forceSave) return
        
        statisticsSavedForCurrentGame = true
        
        viewModelScope.launch {
            var updatedCount = 0
            var skippedCount = 0
            
            // FÃ¼r jeden Spieler die Statistiken aktualisieren (nur wenn er in der Verwaltung existiert)
            finishedGame.players.forEach { player ->
                val stats = PlayerStatistics.getAllStatistics(player, finishedGame)
                val wasUpdated = repository.updatePlayerStatistics(player.name, stats)
                if (wasUpdated) {
                    updatedCount++
                    println("DEBUG: Statistiken fÃ¼r '${player.name}' aktualisiert")
                } else {
                    skippedCount++
                    println("DEBUG: Spieler '${player.name}' nicht in Verwaltung gefunden - Statistiken nicht aktualisiert")
                }
            }
            
            println("DEBUG: Statistiken aktualisiert: $updatedCount Spieler, Ã¼bersprungen: $skippedCount Spieler")
        }
    }
    
    // resetMultiplier wurde entfernt - der Multiplikator wird direkt aus dem Dartboard berechnet
    
    /**
     * Findet den nÃ¤chsten Spieler, der noch nicht fertig ist (currentScore > 0).
     * SchlieÃŸt den Startindex selbst ein â€” das ist korrekt wenn nur noch ein Spieler
     * Ã¼brig ist, denn dann soll er sich selbst die nÃ¤chste Runde spielen.
     *
     * @param startIndex Der Index des gerade spielenden Spielers
     * @param players Die Liste der Spieler
     * @return Der Index des nÃ¤chsten nicht-fertigen Spielers, oder -1 wenn alle fertig sind
     */
    private fun findNextActivePlayer(startIndex: Int, players: List<Player>): Int {
        // PrÃ¼fe zuerst alle *anderen* Spieler (nach startIndex umlaufend)
        for (offset in 1..players.size) {
            val idx = (startIndex + offset) % players.size
            if (players[idx].currentScore > 0) {
                return idx
            }
        }
        // Kein anderer aktiver Spieler â†’ alle fertig
        return -1
    }
    
    /**
     * PrÃ¼ft ob alle Spieler fertig sind (currentScore == 0)
     */
    private fun areAllPlayersFinished(players: List<Player>): Boolean {
        return players.all { it.currentScore == 0 }
    }
    
    /**
     * Findet den nÃ¤chsten Spieler im Cricket-Modus, der noch nicht fertig ist
     * (hat noch nicht alle Segmente geschlossen).
     * Wenn nur noch ein Spieler aktiv ist, gibt dieser sich selbst zurÃ¼ck.
     *
     * @param startIndex Der Index des gerade spielenden Spielers
     * @param players Die Liste der Spieler
     * @return Der Index des nÃ¤chsten nicht-fertigen Spielers, oder -1 wenn alle fertig sind
     */
    private fun findNextActivePlayerCricket(startIndex: Int, players: List<Player>): Int {
        // PrÃ¼fe alle anderen Spieler (nach startIndex umlaufend)
        for (offset in 1..players.size) {
            val idx = (startIndex + offset) % players.size
            if (!players[idx].hasAllSegmentsClosed()) {
                return idx
            }
        }
        // Kein anderer aktiver Spieler â†’ alle fertig
        return -1
    }
    
    /**
     * Setzt das Spiel fort, damit andere Spieler weiterspielen kÃ¶nnen.
     * Setzt isFinished auf false zurÃ¼ck und wechselt zum nÃ¤chsten nicht-fertigen Spieler.
     * throwsInRound wird korrekt aus der throwHistory ermittelt.
     */
    fun continueGame() {
        val currentGame = game ?: return
        if (!currentGame.isFinished) return
        
        // Finde den nÃ¤chsten nicht-fertigen Spieler
        val nextActivePlayerIndex = findNextActivePlayer(
            currentGame.currentPlayerIndex,
            currentGame.players
        )
        
        if (nextActivePlayerIndex == -1) {
            // Alle Spieler sind fertig - Spiel sollte beendet bleiben
            println("DEBUG: Alle Spieler sind fertig - Spiel bleibt beendet")
            return
        }
        
        // Berechne die korrekte Anzahl bereits getÃ¤tigter WÃ¼rfe des nÃ¤chsten Spielers
        // in seiner laufenden Runde (kann > 0 sein wenn er mitten in seiner Runde fertig wurde)
        val throwsInRound = calculateThrowsInRound(nextActivePlayerIndex, currentGame.throwHistory)
        
        // Setze isFinished auf false und wechsle zum nÃ¤chsten aktiven Spieler
        game = Game(
            players = currentGame.players,
            mode = currentGame.mode,
            currentPlayerIndex = nextActivePlayerIndex,
            isFinished = false,
            throwsInRound = throwsInRound,
            throwHistory = currentGame.throwHistory,
            doubleIn = currentGame.doubleIn,
            doubleOut = currentGame.doubleOut,
            targetScore = currentGame.targetScore,
            totalLegs = currentGame.totalLegs,
            currentLeg = currentGame.currentLeg,
            randomizeOrder = currentGame.randomizeOrder,
            startScore = currentGame.startScore
        )
        println("DEBUG: Spiel fortgesetzt - nÃ¤chster aktiver Spieler: ${currentGame.players[nextActivePlayerIndex].name} (Index: $nextActivePlayerIndex), throwsInRound: $throwsInRound")
    }

    // Test-Funktion um zu prÃ¼fen ob der ViewModel korrekt funktioniert
    fun testViewModel() {
        println("=== TESTING VIEWMODEL ===")
        println("Current game: $game")
        println("No pendingMultiplier needed - scores come directly from dartboard")
        
        try {
            // Einfachen Test durchfÃ¼hren
            val testPlayers = listOf("Test1", "Test2").map { name ->
                Player(name = name, currentScore = 501, scores = mutableListOf(), dartThrows = mutableListOf())
            }
            
            val testGame = Game(
                players = testPlayers,
                mode = GameMode.X501,
                currentPlayerIndex = 0,
                isFinished = false,
                throwsInRound = 0,
                doubleIn = false,
                doubleOut = true,
                targetScore = 0,
                totalLegs = 1,
                currentLeg = 1,
                randomizeOrder = false
            )
            
            println("Test game created successfully: $testGame")
            
            // Test-Spiel zuweisen
            game = testGame
            println("Test game assigned successfully")
            
            // Sofort wieder zurÃ¼cksetzen
            game = null
            println("Test game reset successfully")
            
        } catch (e: Exception) {
            println("Test failed: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (zoomPersistJob?.isActive == true) {
            zoomPersistJob?.cancel()
            persistZoom()
        }
        tournamentWebServer.stop()
    }
} 
