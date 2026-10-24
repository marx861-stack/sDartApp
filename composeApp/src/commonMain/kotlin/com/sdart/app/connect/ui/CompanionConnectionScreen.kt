package com.sdart.app.connect.ui

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
import com.sdart.app.ui.AdaptiveContainer
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
    onBack: () -> Unit,
    onOpenHostOverview: () -> Unit,
    vm: DartViewModel = remember { DartViewModel() },
    // Für die Desktop-Ansicht, die die Host-Verwaltung direkt einbettet
    sessionManager: SessionManager? = null
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
        is ParticipantState.Idle -> "Geräte verbinden"
        is ParticipantState.Searching -> "Gerätesuche"
        is ParticipantState.Connecting -> "Verbindung"
        is ParticipantState.Connected -> when (s.role) {
            DeviceRole.CAMERA -> "Kamera-Stream"
            DeviceRole.INPUT -> "Touch-Eingabe"
            DeviceRole.DISPLAY -> "Zweiter Bildschirm"
            DeviceRole.HOST -> "Host-Steuerung"
        }
        is ParticipantState.Rejected -> "Verbindung abgelehnt"
        is ParticipantState.Disconnected -> "Verbindung getrennt"
    }

    val handleHeaderBack: () -> Unit = {
        if (state !is ParticipantState.Idle) {
            participantManager.disconnect()
        } else {
            onBack()
        }
    }

    AdaptiveContainer(modifier = Modifier.fillMaxSize()) { windowInfo ->
        if (windowInfo.isExpanded && sessionManager != null) {
            DesktopDeviceConnection(
                state = state,
                discoveredHosts = discoveredHosts,
                participantManager = participantManager,
                sessionManager = sessionManager,
                vm = vm
            )
            return@AdaptiveContainer
        }

        Box(
            modifier = Modifier.fillMaxSize().ditheredBackground(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Einheitlicher Screen-Header
                ScreenHeader(
                    title = headerTitle,
                    onBack = handleHeaderBack
                )

                // Hauptinhalt je nach Teilnehmer-Zustand
                Box(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "ConnectionStateTransition"
                    ) { currentState ->
                        when (currentState) {
                            is ParticipantState.Idle -> {
                                IdleRoleSelection(
                                    onSelectRole = { role -> participantManager.selectRole(role) },
                                    onOpenHostOverview = onOpenHostOverview,
                                    isWidescreen = windowInfo.isWidescreen
                                )
                            }

                            is ParticipantState.Searching -> {
                                SearchingView(
                                    role = currentState.role,
                                    hosts = discoveredHosts,
                                    onConnectToHost = { host -> participantManager.connectToHost(host, currentState.role) },
                                    onCancel = { participantManager.disconnect() }
                                )
                            }

                            is ParticipantState.Connecting -> {
                                ConnectingView(
                                    host = currentState.host,
                                    role = currentState.role,
                                    onCancel = { participantManager.disconnect() }
                                )
                            }

                            is ParticipantState.Connected -> {
                                ConnectedView(
                                    state = currentState,
                                    participantManager = participantManager,
                                    onDisconnect = { participantManager.disconnect() },
                                    vm = vm
                                )
                            }

                            is ParticipantState.Rejected -> {
                                RejectedView(
                                    state = currentState,
                                    onRetry = { participantManager.selectRole(currentState.role) },
                                    onCancel = { participantManager.disconnect() }
                                )
                            }

                            is ParticipantState.Disconnected -> {
                                DisconnectedView(
                                    state = currentState,
                                    onRetry = { participantManager.selectRole(currentState.role) },
                                    onCancel = { participantManager.disconnect() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rollenauswahl, wenn noch keine Rolle aktiv ist (übersichtlich & kompakt).
 */
@Composable
private fun IdleRoleSelection(
    onSelectRole: (DeviceRole) -> Unit,
    onOpenHostOverview: () -> Unit,
    isWidescreen: Boolean
) {
    CenteredScrollColumn(
        modifier = Modifier.fillMaxSize(),
        spacing = 10.dp,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Geräterolle wählen:",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp
            ),
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // 1. Kamera (Hervorgehoben für Smart-Companion Nutzung)
        CompactRoleCard(
            iconRes = Res.drawable.ic_role_camera,
            title = "Kamera & Erkennung",
            badge = null,
            description = "Automatische Treffer-Erkennung per Kamera",
            isPrimary = true,
            onClick = { onSelectRole(DeviceRole.CAMERA) }
        )

        // 2. Anzeige (Tablet/Zweitbildschirm)
        CompactRoleCard(
            iconRes = Res.drawable.ic_role_display,
            title = "Live Scoreboard",
            badge = null,
            description = "Großes Display für Spielstände & Checkout",
            isPrimary = false,
            onClick = { onSelectRole(DeviceRole.DISPLAY) }
        )

        // 3. Eingabe (Controller)
        CompactRoleCard(
            iconRes = Res.drawable.ic_role_input,
            title = "Touch-Eingabe",
            badge = null,
            description = "Drahtlose Punkteingabe & Spielsteuerung",
            isPrimary = false,
            onClick = { onSelectRole(DeviceRole.INPUT) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Host-Verwaltung (PC als Host steuern)
        CompactRoleCard(
            iconRes = Res.drawable.ic_role_host,
            title = "Host-Verwaltung",
            badge = null,
            description = "Diesen PC als Host im Netzwerk bereitstellen",
            isPrimary = false,
            actionLabel = "Öffnen",
            onClick = onOpenHostOverview
        )
    }
}

@Composable
private fun CompactRoleCard(
    iconRes: DrawableResource,
    title: String,
    badge: String? = null,
    description: String,
    isPrimary: Boolean = false,
    actionLabel: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) Color(0xFF2C2520) else Color(0xFF242424)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isPrimary) 1.5.dp else 1.dp,
            if (isPrimary) PrimaryOrange.copy(alpha = 0.65f) else GlassBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isPrimary) PrimaryOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isPrimary) PrimaryOrange.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (isPrimary) PrimaryOrange else Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )

                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryOrange.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (actionLabel != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryOrange.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = actionLabel,
                        color = PrimaryOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                Text(
                    text = "→",
                    color = if (isPrimary) PrimaryOrange else Color.White.copy(alpha = 0.4f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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
            text = "Suche Host im WLAN...",
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
                text = "Rolle: ${roleLabel(role)}",
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
                text = "Gefundene Hosts (${hosts.size}):",
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
                            Text("Verbinden →", color = PrimaryOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            Text("Suche abbrechen", color = Color.White)
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
            text = "Verbinde mit \"${host.name}\"...",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Verbindung wird hergestellt...",
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
            Text("Abbrechen", color = Color.White)
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
            text = "Erfolgreich verbunden!",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Host: ${state.hostName} • Rolle: ${roleLabel(state.role)}",
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
                            DeviceRole.CAMERA -> "Kamera streamt live an Host"
                            DeviceRole.DISPLAY -> "Anzeige empfängt Live-Scores"
                            DeviceRole.INPUT -> "Eingabepad ist aktiv"
                            DeviceRole.HOST -> "Host aktiv"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when (state.role) {
                            DeviceRole.CAMERA -> "Richte die Kamera stabil auf die Dartscheibe aus. Die Erkennung rechnet in Echtzeit am Host."
                            DeviceRole.DISPLAY -> "Dieses Gerät spiegelt den Punktestand und Checkout-Empfehlungen in Echtzeit wider."
                            DeviceRole.INPUT -> "Du kannst Würfe und Korrekturen direkt über dieses Gerät steuern."
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
            Text("Verbindung trennen", color = Color.White, fontWeight = FontWeight.SemiBold)
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
                        contentDescription = "Screen Dimmed",
                        tint = Color.Gray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Energiesparend...",
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
                    title = "Kamera-Stream",
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                    .border(
                        1.5.dp,
                        if (isZoomActive) PrimaryOrange.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f),
                        CircleShape
                    )
                    .clickable {
                        vm.resetZoom()
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "${currentDisplayZoom.format(1)}x",
                    color = if (isZoomActive) PrimaryOrange else Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

/**
 * Drahtloses Touch-Dartboard für das Companion-Gerät (Rolle INPUT).
 */
@Composable
private fun CompanionInputView(
    hostName: String,
    participantManager: ParticipantSessionManager,
    onDisconnect: () -> Unit
) {
    var thrownDarts by remember { mutableStateOf(listOf<Offset>()) }
    var throwLabels by remember { mutableStateOf(listOf<String>()) }
    var magnifierCenter by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dartboardBounds by remember { mutableStateOf<Rect?>(null) }
    var parentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Obere kompakte Statusleiste
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
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
                        text = "Eingabe aktiv",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Verbunden mit: $hostName",
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
                Text("Trennen", color = Color.White, fontSize = 12.sp)
            }
        }

        // Anzeige der aktuellen 3 Würfe
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until 3) {
                val label = throwLabels.getOrNull(i)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (label != null) PrimaryOrange.copy(alpha = 0.2f) else Color(0xFF242424)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (label != null) PrimaryOrange else GlassBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pfeil ${i + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label ?: "-",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (label != null) Color.White else Color.Gray
                            )
                        )
                    }
                }
            }
        }

        // Dartboard Bereich mit Lupe
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
                        thrownDarts = thrownDarts,
                        onDartThrown = { score, multiplier, position ->
                            val finalScore = if (score == 25 && multiplier == 2) 50 else score * multiplier
                            val actualMultiplier = if (finalScore == 50 && score == 25) 2 else multiplier

                            val throwName = when {
                                finalScore == 50 -> "Bull (50)"
                                finalScore == 25 -> "Bull (25)"
                                finalScore == 0 -> "Miss (0)"
                                actualMultiplier == 3 -> "T$score ($finalScore)"
                                actualMultiplier == 2 -> "D$score ($finalScore)"
                                else -> "S$score ($finalScore)"
                            }

                            if (thrownDarts.size >= 3) {
                                thrownDarts = listOf(position)
                                throwLabels = listOf(throwName)
                            } else {
                                thrownDarts = thrownDarts + position
                                throwLabels = throwLabels + throwName
                            }

                            participantManager.sendScoreInput(finalScore, actualMultiplier, position.x, position.y)
                        },
                        onMagnifierUpdate = { center, dragging ->
                            magnifierCenter = center
                            isDragging = dragging
                        },
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

        // Undo & Steuerung unten
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    participantManager.sendUndo()
                    if (thrownDarts.isNotEmpty()) {
                        thrownDarts = thrownDarts.dropLast(1)
                    }
                    if (throwLabels.isNotEmpty()) {
                        throwLabels = throwLabels.dropLast(1)
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF383838))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_undo),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "Wurf Rückgängig (Undo)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (thrownDarts.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        thrownDarts = emptyList()
                        throwLabels = emptyList()
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text("Neu", color = Color.White)
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
            text = "Verbindung abgelehnt",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = when (state.reason) {
                RejectReason.DECLINED_BY_USER -> "Der Host hat die Verbindungsanfrage abgelehnt."
                RejectReason.PROTOCOL_MISMATCH -> "App-Versionen stimmen nicht überein."
                RejectReason.ROLE_NOT_SUPPORTED -> "Diese Rolle wird vom Host nicht unterstützt."
                RejectReason.ROLE_ALREADY_TAKEN -> "Diese Rolle ist bereits von einem anderen Gerät belegt."
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
                Text("Abbrechen", color = Color.White)
            }
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text("Erneut versuchen", color = Color.White, fontWeight = FontWeight.Bold)
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
            text = "Verbindung unterbrochen",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = state.message,
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
                Text("Abbrechen", color = Color.White)
            }
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text("Erneut verbinden", color = Color.White, fontWeight = FontWeight.Bold)
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

internal fun roleLabel(role: DeviceRole): String = when (role) {
    DeviceRole.HOST -> "Host"
    DeviceRole.CAMERA -> "Kamera"
    DeviceRole.DISPLAY -> "Anzeige"
    DeviceRole.INPUT -> "Eingabe"
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
 * "Geräte verbinden" für breite Fenster: Oben gleichwertig nebeneinander "selbst Host sein" und
 * "mit einem anderen Host verbinden", darunter die verbundenen Geräte, Kamera-Quelle und gemerkte Geräte.
 */
@Composable
private fun DesktopDeviceConnection(
    state: ParticipantState,
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

    // Läuft eine Suche oder Verbindung als Teilnehmer, gehört die ganze Seite diesem Ablauf
    val participantRole = state.activeRole
    if (participantRole != null) {
        DesktopPage(title = "Geräte verbinden") {
            DashboardCard(
                title = "Dieser PC als ${roleLabel(participantRole)}",
                modifier = Modifier.fillMaxWidth().weight(1f)
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

    DesktopPage(title = "Geräte verbinden") {
        // Die beiden Wege gleichwertig nebeneinander: selbst Host sein oder sich mit einem Host verbinden
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HostStatusCard(
                hostState = hostState,
                onStart = { sessionManager.startHost("sDart Host", preferredPort = 0) },
                onStop = { sessionManager.stopHost() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            JoinOtherHostCard(
                onSelectRole = { role -> participantManager.selectRole(role) },
                modifier = Modifier.weight(1.4f).fillMaxHeight()
            )
        }

        pendingAuth?.let { pending ->
            PendingAuthorizationCard(pending = pending, sessionManager = sessionManager)
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1.3f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DashboardCard(
                title = "Verbundene Geräte" + (activeHost?.let { " · ${it.participants.size}" } ?: ""),
                modifier = Modifier.weight(2f).fillMaxHeight()
            ) {
                when {
                    activeHost == null -> EmptyDevicesHint(
                        title = "Host ist aus",
                        text = "Starte den Host, damit sich Handys und Tablets mit diesem PC verbinden können."
                    )
                    activeHost.participants.isEmpty() -> EmptyDevicesHint(
                        title = "Warte auf Geräte",
                        text = "1. S'Dart auf dem Handy oder Tablet öffnen\n" +
                            "2. \"Geräte verbinden\" wählen\n" +
                            "3. Rolle antippen – dieser PC wird im WLAN automatisch gefunden"
                    )
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

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                DashboardCard(title = null, modifier = Modifier.fillMaxWidth()) {
                    CameraSourceSection(vm = vm, sessionManager = sessionManager, hostState = hostState)
                }
                DashboardCard(
                    title = "Gemerkte Geräte · ${trustedDevices.size}",
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    if (trustedDevices.isEmpty()) {
                        Text(
                            text = "Geräte, die du zulässt, verbinden sich beim nächsten Mal ohne Nachfrage.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    } else {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trustedDevices.forEach { device ->
                                TrustedDeviceRow(device = device, onRemove = { sessionManager.revokeTrust(device.deviceId) })
                            }
                        }
                        CardLink(
                            text = "Alle entfernen",
                            onClick = { sessionManager.clearAllTrustedDevices() },
                            primary = false,
                            icon = Res.drawable.ic_trash,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Host-Status: aktiv mit Port und Gerätezahl, sonst mit Start-Button. */
@Composable
private fun HostStatusCard(
    hostState: HostState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = hostState as? HostState.Active
    DashboardCard(title = null, highlighted = active != null, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                    text = "Dieser PC als Host",
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
                            "Aktiv · Port ${active.port} · $count ${if (count == 1) "Gerät" else "Geräte"}"
                        } else {
                            "Aus"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        Text(
            text = "Handys und Tablets verbinden sich mit diesem PC als Kamera, Scoreboard oder Eingabe.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(top = 14.dp)
        )

        Spacer(Modifier.weight(1f).heightIn(min = 16.dp))

        if (active != null) {
            OutlinedButton(
                onClick = onStop,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassControlBorder),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Host beenden", color = MaterialTheme.colorScheme.onSurface)
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
                Text("Host starten", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyDevicesHint(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f),
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
    DashboardCard(title = "Mit einem anderen Host verbinden", modifier = modifier) {
        Text(
            text = "Läuft S'Dart schon auf einem anderen Gerät? Wähle, was dieser PC dort übernimmt – " +
                "der Host wird im WLAN automatisch gesucht.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                Triple(DeviceRole.CAMERA, "Kamera", "Treffer erkennen"),
                Triple(DeviceRole.DISPLAY, "Scoreboard", "Spielstand zeigen"),
                Triple(DeviceRole.INPUT, "Eingabe", "Punkte eingeben")
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 120.dp)
            .clip(shape)
            .background(GlassCardBg)
            .border(1.dp, GlassControlBorder, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            painter = painterResource(roleIconRes(role)),
            contentDescription = null,
            tint = PrimaryOrange,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = "Host suchen →",
            style = MaterialTheme.typography.labelLarge.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(top = 10.dp)
        )
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
