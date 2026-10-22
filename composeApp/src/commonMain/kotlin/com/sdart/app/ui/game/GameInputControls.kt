package com.sdart.app.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdart.app.DartViewModel
import com.sdart.app.Game

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
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
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
    showCamera: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
