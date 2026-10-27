package com.sdart.app.connect.ui

import org.jetbrains.compose.resources.stringResource
import com.sdart.app.ui.theme.ditheredBackground
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sdart.app.DartViewModel
import com.sdart.app.format
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.CenteredScrollColumn
import com.sdart.app.ui.components.ActiveGlassBg
import com.sdart.app.ui.components.GlassCardBg
import com.sdart.app.ui.components.CardLink
import com.sdart.app.ui.components.GlassControlBorder
import com.sdart.app.ui.components.OnlineGreen
import com.sdart.app.ui.components.DashboardCard
import com.sdart.app.ui.components.DesktopPage
import com.sdart.app.ui.game.InteractiveDartboard
import com.sdart.app.ui.game.MagnifierOverlay
import com.sdart.app.connect.*
import com.sdart.app.ui.LocalCompactLayout
import com.sdart.app.ui.ScreenHeader
import com.sdart.app.ui.theme.DarkBgGradient
import com.sdart.app.ui.theme.GlassBorder
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

/**
 * Screen zur Geräte-Verbindung:
 * - Moderne Kacheln mit konsistenten Vektor-Icons
 * - Apple-Like animierter Such- und Verbindungsscreen
 * - Rollenübersicht und Host-Verwaltung
 */
@Composable
fun CompanionConnectionScreen(
    participantManager: ParticipantSessionManager,
    sessionManager: SessionManager,
    vm: DartViewModel = remember { DartViewModel() }
) {
    val state by participantManager.state.collectAsState()
    val discoveredHosts by participantManager.discovery.hosts.collectAsState()

    val currentState = state
    if (currentState is ParticipantState.Connected && currentState.role == DeviceRole.CAMERA) {
        CompanionCameraStreamingView(
            hostName = currentState.hostName,
            participantManager = participantManager,
            onDisconnect = { participantManager.disconnect() },
            onBack = { participantManager.disconnect() },
            vm = vm
        )
        return
    }

    val headerTitle = when (val s = state) {
        is ParticipantState.Idle -> stringResource(Res.string.connect_title_connect_devices)
        is ParticipantState.Searching -> stringResource(Res.string.connect_title_device_search)
        is ParticipantState.Connecting -> stringResource(Res.string.connect_title_connection)
        is ParticipantState.Connected -> when (s.role) {
            DeviceRole.CAMERA -> stringResource(Res.string.connect_title_camera_stream)
            DeviceRole.INPUT -> stringResource(Res.string.connect_title_touch_input)
            DeviceRole.DISPLAY -> stringResource(Res.string.connect_title_second_screen)
            DeviceRole.HOST -> stringResource(Res.string.connect_title_host_control)
        }
        is ParticipantState.Rejected -> stringResource(Res.string.connect_title_rejected)
        is ParticipantState.Disconnected -> stringResource(Res.string.connect_title_disconnected)
    }

    DeviceConnection(
        state = state,
        headerTitle = headerTitle,
        discoveredHosts = discoveredHosts,
        participantManager = participantManager,
        sessionManager = sessionManager,
        vm = vm
    )
}

/**
 * Moderner Such- und Erkennungs-Bildschirm mit Apple-Like Radar/Pulse-Animation.
 */
@Composable
private fun SearchingView(
    role: DeviceRole,
    hosts: List<DiscoveredHost>,
    onConnectToHost: (DiscoveredHost) -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animierte Radar-Puls-Komponente (kompakt)
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .background(PrimaryOrange.copy(alpha = pulseAlpha), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(PrimaryOrange.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF262626), CircleShape)
                    .border(2.dp, PrimaryOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_wifi_search),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = PrimaryOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.connect_searching_host),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Text(
                text = stringResource(Res.string.connect_role_line, roleLabel(role)),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Falls mehrere Hosts gefunden wurden: Liste anzeigen
        if (hosts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.connect_found_hosts, hosts.size.toString()),
                style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hosts) { host ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onConnectToHost(host) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_role_host),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = PrimaryOrange
                                )
                                Text(host.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Text(stringResource(Res.string.connect_connect_arrow), color = PrimaryOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(stringResource(Res.string.connect_cancel_search), color = Color.White)
        }
    }
}

/**
 * Verbindungsaufbau / Warten auf Erlaubnis am Host.
 */
@Composable
private fun ConnectingView(
    host: DiscoveredHost,
    role: DeviceRole,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryOrange,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 3.5.dp
            )
            Icon(
                painter = painterResource(roleIconRes(role)),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(Res.string.connect_connecting_to, host.name),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(Res.string.connect_connecting),
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.72f)),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(stringResource(Res.string.connect_cancel), color = Color.White)
        }
    }
}

/**
 * Erfolgreich verbundener Zustand!
 */
@Composable
private fun ConnectedView(
    state: ParticipantState.Connected,
    participantManager: ParticipantSessionManager,
    onDisconnect: () -> Unit,
    vm: DartViewModel
) {
    if (state.role == DeviceRole.INPUT) {
        CompanionInputView(
            hostName = state.hostName,
            participantManager = participantManager,
            onDisconnect = onDisconnect
        )
        return
    }

    if (state.role == DeviceRole.DISPLAY) {
        CompanionScoreboardView(
            hostName = state.hostName,
            participantManager = participantManager,
            onDisconnect = onDisconnect
        )
        return
    }

    if (state.role == DeviceRole.CAMERA) {
        CompanionCameraStreamingView(
            hostName = state.hostName,
            participantManager = participantManager,
            onDisconnect = onDisconnect,
            onBack = onDisconnect,
            vm = vm
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFF4CAF50).copy(alpha = 0.18f), CircleShape)
                .border(2.dp, Color(0xFF4CAF50), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_check_circle),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFF4CAF50)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.connect_connected_success),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.connect_host_role_line, state.hostName, roleLabel(state.role)),
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryOrange.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(roleIconRes(state.role)),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = PrimaryOrange
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (state.role) {
                            DeviceRole.CAMERA -> stringResource(Res.string.connect_status_camera)
                            DeviceRole.DISPLAY -> stringResource(Res.string.connect_status_display)
                            DeviceRole.INPUT -> stringResource(Res.string.connect_status_input)
                            DeviceRole.HOST -> stringResource(Res.string.connect_status_host)
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when (state.role) {
                            DeviceRole.CAMERA -> stringResource(Res.string.connect_desc_camera)
                            DeviceRole.DISPLAY -> stringResource(Res.string.connect_desc_display)
                            DeviceRole.INPUT -> stringResource(Res.string.connect_desc_input)
                            DeviceRole.HOST -> ""
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.72f),
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onDisconnect,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(stringResource(Res.string.connect_disconnect), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Original StreamingView aus PcIntegrationScreen:
 * - Reines Vollbild-Kamerabild (Live-Feed an Host)
 * - Touch-Interceptor für Reset des 10-Sekunden-Inaktivitäts-Timers
 * - Energiesparmodus nach 10s Inaktivität (schwarzer Bildschirm mit ic_camera_placeholder & "Energiesparend...", Tap-to-Wake)
 * - Schließen/Stream-Stoppen-Button oben rechts (wenn nicht abgedunkelt)
 */
@Composable
private fun CompanionCameraStreamingView(
    hostName: String,
    participantManager: ParticipantSessionManager,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
    vm: DartViewModel
) {
    var interactionTrigger by remember { mutableIntStateOf(0) }
    var isDimmed by remember { mutableStateOf(false) }

    // Inaktivitäts-Timer (10 Sekunden wie im Original)
    LaunchedEffect(interactionTrigger) {
        isDimmed = false
        delay(10000L)
        isDimmed = true
    }

    // Hardware-Helligkeit steuern: bei isDimmed auf Minimalstufe (0.01f) runterregeln und Display wachhalten
    com.sdart.app.PlatformScreenBrightness(isDimmed = isDimmed, keepScreenOn = true)

    // Hardware-/System-Zuruecktaste beendet das Streaming sauber
    com.sdart.app.PlatformBackHandler {
        onDisconnect()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Kamera-Vorschau (Live-Feed im Vollbild)
        com.sdart.app.CameraBackground(
            modifier = Modifier.fillMaxSize(),
            vm = vm,
            showCamera = true,
            showOverlay = true,
            participantManager = participantManager
        )

        // 2. Touch Interceptor Overlay (fängt Berührungen für Inaktivitäts-Timer und Zoom ab)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        interactionTrigger++
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            interactionTrigger++
                            if (vm.onPinchZoom != null) {
                                vm.onPinchZoom?.invoke(zoom)
                            } else {
                                val currentZoom = vm.cameraZoomLevel
                                val currentScale = 1f + currentZoom * 3f
                                val newScale = (currentScale * zoom).coerceIn(1f, 4f)
                                val newZoom = (newScale - 1f) / 3f
                                vm.cameraZoomLevel = newZoom
                                vm.cameraZoomRatio = newScale
                            }
                        }
                    }
                }
        )

        // 3. Energy Saving Overlay (Original-Design aus PcIntegrationScreen)
        if (isDimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures {
                        interactionTrigger++
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_camera_placeholder),
                        contentDescription = stringResource(Res.string.connect_screen_dimmed),
                        tint = Color.Gray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.connect_power_saving),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // 4. Controls wenn nicht abgedunkelt (Einheitlicher Screen-Header oben)
        if (!isDimmed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.TopCenter
            ) {
                ScreenHeader(
                    title = stringResource(Res.string.connect_title_camera_stream),
                    onBack = onDisconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .safeDrawingPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            // 5. Minimalist Zoom Indicator Pill (zeigt aktuellen Zoom z.B. 1.0x; Klick setzt auf 1.0x zurück)
            val isZoomActive = kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f || vm.cameraZoomLevel > 0.01f
            val currentDisplayZoom = if (kotlin.math.abs(vm.cameraZoomRatio - 1.0f) > 0.05f) {
                vm.cameraZoomRatio
            } else if (vm.cameraZoomLevel > 0.01f) {
                1.0f + vm.cameraZoomLevel * 3.0f
            } else {
                1.0f
            }
            if (isZoomActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                        .padding(bottom = 40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            vm.resetZoom()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${currentDisplayZoom.format(1)}x",
                        color = PrimaryOrange,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Drahtloses Touch-Dartboard für das Companion-Gerät (Rolle INPUT).
 * Voll synchronisiert mit dem Host über GameSnapshot.
 */
@Composable
private fun CompanionInputView(
    hostName: String,
    participantManager: ParticipantSessionManager,
    onDisconnect: () -> Unit
) {
    val gameSnapshot by participantManager.gameSnapshot.collectAsState()
    val snapshot = gameSnapshot

    if (snapshot == null || !snapshot.isGameActive) {
        CompanionInputWaitingView(
            hostName = hostName,
            onDisconnect = onDisconnect
        )
        return
    }

    var magnifierCenter by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dartboardBounds by remember { mutableStateOf<Rect?>(null) }
    var parentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val dartPositions = remember(snapshot.currentRoundDarts) {
        snapshot.currentRoundDarts.mapNotNull { dart ->
            if (dart.x != null && dart.y != null) Offset(dart.x, dart.y) else null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Obere Statusleiste: Host-Verbindung & Spielmodus
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Column {
                    Text(
                        text = snapshot.gameMode.ifBlank { stringResource(Res.string.connect_input_active) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = stringResource(Res.string.connect_connected_to, hostName) + if (snapshot.totalLegs > 1) " • Leg ${snapshot.currentLeg}/${snapshot.totalLegs}" else "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(stringResource(Res.string.connect_disconnect_short), color = Color.White, fontSize = 12.sp)
            }
        }

        // 2. Aktiver Spieler & Punktestand
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF222222)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (snapshot.isFinished) Color(0xFF4CAF50) else if (snapshot.isBust) Color(0xFFFF5252) else GlassBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (snapshot.isFinished) stringResource(Res.string.connect_winner) else stringResource(Res.string.connect_on_turn),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = snapshot.currentPlayerName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val statusText = when {
                        snapshot.isFinished -> stringResource(Res.string.connect_game_over)
                        snapshot.isBust -> stringResource(Res.string.connect_bust)
                        snapshot.checkoutSuggestion != null -> stringResource(Res.string.connect_checkout_line, snapshot.checkoutSuggestion.toString())
                        else -> stringResource(Res.string.connect_round_pts, snapshot.currentRoundTotal.toString())
                    }
                    val statusColor = when {
                        snapshot.isFinished -> Color(0xFF4CAF50)
                        snapshot.isBust -> Color(0xFFFF5252)
                        snapshot.checkoutSuggestion != null -> Color(0xFF448AFF)
                        else -> Color.White.copy(alpha = 0.7f)
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }

                // Großer Rest-Score
                Text(
                    text = "${snapshot.currentPlayerScore}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = if (snapshot.isFinished) Color(0xFF4CAF50) else PrimaryOrange
                    )
                )
            }
        }

        // 3. Anzeige der aktuellen 3 Würfe (automatische Runden-Aktualisierung)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until 3) {
                val dart = snapshot.currentRoundDarts.getOrNull(i)
                val isNext = i == snapshot.throwsInRound && !snapshot.isFinished && !snapshot.isBust

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            dart?.isBust == true -> Color(0xFFFF5252).copy(alpha = 0.25f)
                            dart != null -> PrimaryOrange.copy(alpha = 0.2f)
                            isNext -> Color(0xFF2E2E2E)
                            else -> Color(0xFF1E1E1E)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            dart?.isBust == true -> Color(0xFFFF5252)
                            dart != null -> PrimaryOrange
                            isNext -> Color.White.copy(alpha = 0.45f)
                            else -> GlassBorder
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.connect_dart_n, (i + 1).toString()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dart?.label ?: if (isNext) stringResource(Res.string.connect_dart_ready) else "-",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    dart != null -> Color.White
                                    isNext -> PrimaryOrange
                                    else -> Color.Gray
                                },
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 4. Dartboard Bereich mit Lupe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { parentCoords = it },
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val boardSize = minOf(maxWidth, maxHeight)

                Box(
                    modifier = Modifier
                        .size(boardSize)
                        .onGloballyPositioned { coords ->
                            parentCoords?.let { parent ->
                                dartboardBounds = parent.localBoundingBoxOf(coords, clipBounds = false)
                            }
                        }
                ) {
                    InteractiveDartboard(
                        thrownDarts = dartPositions,
                        onDartThrown = { score, multiplier, position ->
                            if (snapshot.isFinished) return@InteractiveDartboard

                            val finalScore = if (score == 25 && multiplier == 2) 50 else score * multiplier
                            val actualMultiplier = if (finalScore == 50 && score == 25) 2 else multiplier

                            // Sendet Treffer an Host. Bei 3 Darts beendet der Host die Runde automatisch.
                            participantManager.sendScoreInput(finalScore, actualMultiplier, position.x, position.y)
                        },
                        onMagnifierUpdate = { center, dragging ->
                            magnifierCenter = center
                            isDragging = dragging
                        },
                        highlightedSegments = snapshot.highlightedSegments,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isDragging && magnifierCenter != null && dartboardBounds != null) {
                MagnifierOverlay(
                    localMagnifierCenter = magnifierCenter!!,
                    dartboardBounds = dartboardBounds!!,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        // 5. Aktionsleiste unten: Undo, Fehlwurf (Miss), Optional: Weiter
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo-Button
            Button(
                onClick = { participantManager.sendUndo() },
                modifier = Modifier.weight(1.2f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF383838))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_undo),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(Res.string.connect_undo),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Fehlwurf (Miss / 0 Punkte) - zählt für genau 1 Dart
            Button(
                onClick = {
                    if (!snapshot.isFinished) {
                        participantManager.sendScoreInput(0, 0, null, null)
                    }
                },
                enabled = !snapshot.isFinished,
                modifier = Modifier.weight(1.2f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF383838),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Miss (0)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Ansprechender Wartezustand für das Companion-Gerät, wenn am Host noch kein Spiel aktiv ist.
 */
@Composable
private fun CompanionInputWaitingView(
    hostName: String,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Obere Statusleiste
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Column {
                    Text(
                        text = stringResource(Res.string.connect_input_connected),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = stringResource(Res.string.connect_connected_with, hostName),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(stringResource(Res.string.connect_disconnect_short), color = Color.White, fontSize = 12.sp)
            }
        }

        // Warte-Karte zentriert
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(PrimaryOrange.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, PrimaryOrange.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_target),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = PrimaryOrange
                        )
                    }

                    Text(
                        text = stringResource(Res.string.connect_waiting_game_start),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(Res.string.connect_waiting_game_start_desc, hostName),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Abgewiesen durch den Host.
 */
@Composable
private fun RejectedView(
    state: ParticipantState.Rejected,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.error, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_cross_circle),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.connect_title_rejected),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = when (state.reason) {
                RejectReason.DECLINED_BY_USER -> stringResource(Res.string.connect_reject_declined)
                RejectReason.PROTOCOL_MISMATCH -> stringResource(Res.string.connect_reject_protocol)
                RejectReason.ROLE_NOT_SUPPORTED -> stringResource(Res.string.connect_reject_role_unsupported)
                RejectReason.ROLE_ALREADY_TAKEN -> stringResource(Res.string.connect_reject_role_taken)
            },
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.72f)),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Text(stringResource(Res.string.connect_cancel), color = Color.White)
            }
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text(stringResource(Res.string.connect_retry), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Verbindung getrennt.
 */
@Composable
private fun DisconnectedView(
    state: ParticipantState.Disconnected,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFFFFA000).copy(alpha = 0.18f), CircleShape)
                .border(2.dp, Color(0xFFFFA000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_alert_triangle),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFFFFA000)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.connect_connection_interrupted),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = when (state.kind) {
                DisconnectKind.HOST_CLOSED -> stringResource(Res.string.connect_disc_host_closed)
                DisconnectKind.SESSION_ENDED -> stringResource(Res.string.connect_disc_session_ended)
                DisconnectKind.ERROR -> stringResource(Res.string.connect_disc_error, state.detail ?: stringResource(Res.string.connect_disc_server_unreachable))
                DisconnectKind.CLOSED -> state.detail ?: stringResource(Res.string.connect_disc_disconnected)
            },
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.72f)),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Text(stringResource(Res.string.connect_cancel), color = Color.White)
            }
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text(stringResource(Res.string.connect_reconnect), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal fun roleIconRes(role: DeviceRole): DrawableResource = when (role) {
    DeviceRole.HOST -> Res.drawable.ic_role_host
    DeviceRole.CAMERA -> Res.drawable.ic_role_camera
    DeviceRole.DISPLAY -> Res.drawable.ic_role_display
    DeviceRole.INPUT -> Res.drawable.ic_role_input
}

@Composable
internal fun roleLabel(role: DeviceRole): String = when (role) {
    DeviceRole.HOST -> stringResource(Res.string.connect_role_host)
    DeviceRole.CAMERA -> stringResource(Res.string.connect_role_camera)
    DeviceRole.DISPLAY -> stringResource(Res.string.connect_role_display)
    DeviceRole.INPUT -> stringResource(Res.string.connect_role_input)
}

private val ParticipantState.activeRole: DeviceRole?
    get() = when (this) {
        is ParticipantState.Idle -> null
        is ParticipantState.Searching -> role
        is ParticipantState.Connecting -> role
        is ParticipantState.Connected -> role
        is ParticipantState.Rejected -> role
        is ParticipantState.Disconnected -> role
    }

/**
 * "Geräte verbinden": "selbst Host sein" und "mit einem anderen Host verbinden" als gleichwertige Wege,
 * dazu die verbundenen Geräte, Kamera-Quelle und gemerkte Geräte. Breite Fenster legen die Karten
 * nebeneinander, schmale stapeln sie (dort zuerst "verbinden", die häufigste Aufgabe eines Handys).
 */
@Composable
private fun DeviceConnection(
    state: ParticipantState,
    headerTitle: String,
    discoveredHosts: List<DiscoveredHost>,
    participantManager: ParticipantSessionManager,
    sessionManager: SessionManager,
    vm: DartViewModel
) {
    val hostState by sessionManager.hostState.collectAsState()
    val pendingAuth by sessionManager.pendingAuthorization.collectAsState()
    val trustedDevices by sessionManager.trustedDevices.collectAsState()

    LaunchedEffect(Unit) {
        vm.refreshWebcams()
    }

    val compact = LocalCompactLayout.current
    val cardPadding = if (compact) 16.dp else 24.dp

    // Läuft eine Suche oder Verbindung als Teilnehmer, gehört die ganze Seite diesem Ablauf
    val participantRole = state.activeRole
    if (participantRole != null) {
        // Auf dem Handy führt der Zurück-Pfeil aus dem Ablauf heraus; die Ansichten füllen die Höhe selbst
        DesktopPage(
            title = if (compact) headerTitle else stringResource(Res.string.connect_title_connect_devices),
            onBack = { participantManager.disconnect() },
            compactScroll = false
        ) {
            DashboardCard(
                title = stringResource(Res.string.connect_device_as_role, thisDeviceLabel(compact), roleLabel(participantRole)),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = cardPadding
            ) {
                ParticipantStateContent(
                    state = state,
                    discoveredHosts = discoveredHosts,
                    participantManager = participantManager,
                    vm = vm
                )
            }
        }
        return
    }

    val activeHost = hostState as? HostState.Active

    val hostCard: @Composable (Modifier) -> Unit = { m ->
        HostStatusCard(
            hostState = hostState,
            onStart = { sessionManager.startHost("sDart Host", preferredPort = 0) },
            onStop = { sessionManager.stopHost() },
            modifier = m
        )
    }
    val joinCard: @Composable (Modifier) -> Unit = { m ->
        JoinOtherHostCard(onSelectRole = { role -> participantManager.selectRole(role) }, modifier = m)
    }
    val connectedCard: @Composable (Modifier) -> Unit = { m ->
        DashboardCard(
            title = stringResource(Res.string.connect_connected_devices) + (activeHost?.let { " · ${it.participants.size}" } ?: ""),
            modifier = m,
            contentPadding = cardPadding
        ) {
            when {
                activeHost == null -> EmptyDevicesHint(
                    title = stringResource(Res.string.connect_host_off),
                    text = stringResource(Res.string.connect_host_off_desc)
                )
                activeHost.participants.isEmpty() -> EmptyDevicesHint(
                    title = stringResource(Res.string.connect_waiting_devices),
                    text = if (compact) {
                        stringResource(Res.string.connect_waiting_hint_compact)
                    } else {
                        stringResource(Res.string.connect_waiting_hint_steps)
                    }
                )
                // Auf dem Handy scrollt die ganze Seite, dort reicht eine einfache Spalte
                compact -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeHost.participants.forEach { participant ->
                        ParticipantRow(
                            participant = participant,
                            onDisconnect = { sessionManager.disconnectParticipant(participant.connectionId) }
                        )
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(activeHost.participants, key = { it.connectionId }) { participant ->
                        ParticipantRow(
                            participant = participant,
                            onDisconnect = { sessionManager.disconnectParticipant(participant.connectionId) }
                        )
                    }
                }
            }
        }
    }
    val cameraCard: @Composable () -> Unit = {
        DashboardCard(title = null, modifier = Modifier.fillMaxWidth(), contentPadding = cardPadding) {
            CameraSourceSection(vm = vm, sessionManager = sessionManager, hostState = hostState)
        }
    }
    val trustedCard: @Composable (Modifier) -> Unit = { m ->
        DashboardCard(
            title = stringResource(Res.string.connect_trusted_title, trustedDevices.size.toString()),
            modifier = m,
            contentPadding = cardPadding
        ) {
            if (trustedDevices.isEmpty()) {
                Text(
                    text = stringResource(Res.string.connect_trusted_hint),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else {
                Column(
                    modifier = if (compact) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    trustedDevices.forEach { device ->
                        TrustedDeviceRow(device = device, onRemove = { sessionManager.revokeTrust(device.deviceId) })
                    }
                }
                CardLink(
                    text = stringResource(Res.string.connect_remove_all),
                    onClick = { sessionManager.clearAllTrustedDevices() },
                    primary = false,
                    icon = Res.drawable.ic_trash,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    DesktopPage(title = stringResource(Res.string.connect_title_connect_devices)) {
        // Auf dem Handy nur das, was gerade zählt: verbundene Geräte erst bei laufendem Host,
        // gemerkte Geräte erst, wenn es welche gibt
        if (compact) {
            joinCard(Modifier.fillMaxWidth())
            hostCard(Modifier.fillMaxWidth())
            pendingAuth?.let { pending ->
                PendingAuthorizationCard(pending = pending, sessionManager = sessionManager)
            }
            if (activeHost != null) connectedCard(Modifier.fillMaxWidth())
            cameraCard()
            if (trustedDevices.isNotEmpty()) trustedCard(Modifier.fillMaxWidth())
            return@DesktopPage
        }

        // Die beiden Wege gleichwertig nebeneinander: selbst Host sein oder sich mit einem Host verbinden
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            hostCard(Modifier.weight(1f).fillMaxHeight())
            joinCard(Modifier.weight(1.4f).fillMaxHeight())
        }

        pendingAuth?.let { pending ->
            PendingAuthorizationCard(pending = pending, sessionManager = sessionManager)
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1.3f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            connectedCard(Modifier.weight(2f).fillMaxHeight())
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                cameraCard()
                trustedCard(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/** "Dieser PC" am Desktop, "Dieses Gerät" auf Handy und Tablet. */
@Composable
private fun thisDeviceLabel(compact: Boolean): String =
    if (compact) stringResource(Res.string.connect_this_device) else stringResource(Res.string.connect_this_pc)

/** Host-Status: aktiv mit Port und Gerätezahl, sonst mit Start-Button. */
@Composable
private fun HostStatusCard(
    hostState: HostState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = hostState as? HostState.Active
    val compact = LocalCompactLayout.current
    DashboardCard(
        title = null,
        highlighted = active != null,
        modifier = modifier,
        contentPadding = if (compact) 16.dp else 24.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compact) 44.dp else 52.dp)
                    .background(ActiveGlassBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_role_host),
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(Res.string.connect_device_as_host, thisDeviceLabel(compact)),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(if (active != null) OnlineGreen else GlassControlBorder, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (active != null) {
                            val count = active.participants.size
                            stringResource(Res.string.connect_host_active_line, active.port.toString(), stringResource(if (count == 1) Res.string.connect_devices_one else Res.string.connect_devices_many, count.toString()))
                        } else {
                            stringResource(Res.string.connect_off)
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        if (!compact) {
            Text(
                text = stringResource(Res.string.connect_host_desc),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        // In der scrollbaren Handy-Spalte gibt es keine Resthöhe zu verteilen
        Spacer(if (compact) Modifier.height(16.dp) else Modifier.weight(1f).heightIn(min = 16.dp))

        if (active != null) {
            OutlinedButton(
                onClick = onStop,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassControlBorder),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(stringResource(Res.string.connect_stop_host), color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Icon(painterResource(Res.drawable.ic_wifi_search), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.connect_start_host), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyDevicesHint(title: String, text: String) {
    val compact = LocalCompactLayout.current
    Column(
        modifier = Modifier.fillMaxWidth().then(if (compact) Modifier.padding(vertical = 8.dp) else Modifier.weight(1f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_mobile_connect),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Diesen PC als Teilnehmer mit einem anderen Host verbinden: je Rolle eine große Kachel,
 * ein Klick startet die Suche im WLAN.
 */
@Composable
private fun JoinOtherHostCard(onSelectRole: (DeviceRole) -> Unit, modifier: Modifier = Modifier) {
    val compact = LocalCompactLayout.current
    DashboardCard(
        title = if (compact) stringResource(Res.string.connect_join_title_compact) else stringResource(Res.string.connect_join_title),
        modifier = modifier,
        contentPadding = if (compact) 16.dp else 24.dp
    ) {
        // Auf dem Handy erklären die Kacheln sich selbst
        if (!compact) {
            Text(
                text = stringResource(Res.string.connect_join_desc),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().then(if (compact) Modifier else Modifier.weight(1f)),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            listOf(
                Triple(DeviceRole.CAMERA, stringResource(Res.string.connect_role_camera), stringResource(Res.string.connect_tile_camera_sub)),
                Triple(DeviceRole.DISPLAY, stringResource(Res.string.connect_tile_scoreboard), stringResource(Res.string.connect_tile_scoreboard_sub)),
                Triple(DeviceRole.INPUT, stringResource(Res.string.connect_role_input), stringResource(Res.string.connect_tile_input_sub))
            ).forEach { (role, title, subtitle) ->
                JoinRoleTile(
                    role = role,
                    title = title,
                    subtitle = subtitle,
                    onClick = { onSelectRole(role) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun JoinRoleTile(
    role: DeviceRole,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val compact = LocalCompactLayout.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = if (compact) 84.dp else 120.dp)
            .clip(shape)
            .background(GlassCardBg)
            .border(1.dp, GlassControlBorder, shape)
            .clickable(onClick = onClick)
            .padding(if (compact) 8.dp else 12.dp)
    ) {
        Icon(
            painter = painterResource(roleIconRes(role)),
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(if (compact) 28.dp else 34.dp)
        )
        Text(
            text = title,
            style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium).copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (!compact) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        // Auf dem Handy ist die ganze Kachel der Knopf, der Link würde die schmalen Kacheln sprengen
        if (!compact) {
            Text(
                text = stringResource(Res.string.connect_search_host),
                style = MaterialTheme.typography.labelLarge.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/** Suche, Verbindung und Fehlerzustände einer Rolle mit den bestehenden Ansichten. */
@Composable
private fun ParticipantStateContent(
    state: ParticipantState,
    discoveredHosts: List<DiscoveredHost>,
    participantManager: ParticipantSessionManager,
    vm: DartViewModel
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            when (state) {
                is ParticipantState.Idle -> Unit
                is ParticipantState.Searching -> SearchingView(
                    role = state.role,
                    hosts = discoveredHosts,
                    onConnectToHost = { host -> participantManager.connectToHost(host, state.role) },
                    onCancel = { participantManager.disconnect() }
                )
                is ParticipantState.Connecting -> ConnectingView(
                    host = state.host,
                    role = state.role,
                    onCancel = { participantManager.disconnect() }
                )
                is ParticipantState.Connected -> ConnectedView(
                    state = state,
                    participantManager = participantManager,
                    onDisconnect = { participantManager.disconnect() },
                    vm = vm
                )
                is ParticipantState.Rejected -> RejectedView(
                    state = state,
                    onRetry = { participantManager.selectRole(state.role) },
                    onCancel = { participantManager.disconnect() }
                )
                is ParticipantState.Disconnected -> DisconnectedView(
                    state = state,
                    onRetry = { participantManager.selectRole(state.role) },
                    onCancel = { participantManager.disconnect() }
                )
            }
        }
    }
}
