package com.sdart.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.sdart.app.ui.components.GlassCardBg
import com.sdart.app.ui.components.GlassControlBorder
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.Res
import sdartapp.composeapp.generated.resources.ic_camera_placeholder
import sdartapp.composeapp.generated.resources.ic_target
import sdartapp.composeapp.generated.resources.ic_undo
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
import com.sdart.app.GameMode

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

    // Cricket: die Zahlen hervorheben, die der Spieler am Zug noch nicht geschlossen hat
    val openCricketSegments: Set<Int> = if (game.mode == GameMode.CRICKET) {
        game.players.getOrNull(game.currentPlayerIndex)
            ?.cricketMarks?.filterValues { it < 3 }?.keys?.toSet()
            ?: emptySet()
    } else emptySet()
    
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
                                modifier = Modifier.fillMaxSize(),
                                highlightedSegments = openCricketSegments
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionTile(
            label = "Rückgängig",
            icon = Res.drawable.ic_undo,
            onClick = onUndo,
            enabled = enabled,
            background = GlassCardBg,
            borderColor = GlassControlBorder,
            contentColor = Color.White,
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            label = if (showCamera) "Board" else "Kamera",
            icon = if (showCamera) Res.drawable.ic_target else Res.drawable.ic_camera_placeholder,
            onClick = onCameraClick,
            enabled = enabled,
            background = PrimaryOrange.copy(alpha = 0.14f),
            borderColor = PrimaryOrange.copy(alpha = 0.6f),
            contentColor = PrimaryOrange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    enabled: Boolean,
    background: Color,
    borderColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(Color(0xFF262626))
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = contentColor
        )
    }
}
