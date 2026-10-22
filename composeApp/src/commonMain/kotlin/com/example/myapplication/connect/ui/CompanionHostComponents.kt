package com.example.myapplication.connect.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.CameraSourceType
import com.example.myapplication.DartViewModel
import com.example.myapplication.connect.*
import com.example.myapplication.ui.theme.GlassBorder
import com.example.myapplication.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

private val HostDialogBackground = Color(0xFF1E1E1E)
private val HostCardBackground = Color(0xFF262626)

enum class HostDialogPage {
    MAIN,
    TRUSTED_DEVICES
}

/**
 * Schlanke Übersicht und Steuerung des Host-Modus:
 * - Host-Status (aktiv / inaktiv) mit Ein/Aus-Schalter
 * - Kamera-Quellenauswahl (Lokale Kamera, Companion-Kamera, Netzwerk-Stream)
 * - Inline-Freigabe für neue Geräte (ohne Pop-up-Fenster)
 * - Verbundene Geräte mit Rolle & Trennen-Option
 * - Gemerkte Geräte: Öffnet eine eigene Seite im Popup zur Verwaltung
 */
@Composable
fun HostOverviewDialog(
    sessionManager: SessionManager,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DartViewModel? = null
) {
    val hostState by sessionManager.hostState.collectAsState()
    val pendingAuth by sessionManager.pendingAuthorization.collectAsState()
    val trustedDevices by sessionManager.trustedDevices.collectAsState()
    var currentPage by remember { mutableStateOf(HostDialogPage.MAIN) }

    LaunchedEffect(Unit) {
        vm?.refreshWebcams()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = HostDialogBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (currentPage) {
                HostDialogPage.MAIN -> {
                    HostMainPage(
                        sessionManager = sessionManager,
                        hostState = hostState,
                        pendingAuth = pendingAuth,
                        trustedDevicesCount = trustedDevices.size,
                        vm = vm,
                        onNavigateToTrustedDevices = { currentPage = HostDialogPage.TRUSTED_DEVICES },
                        onDismissRequest = onDismissRequest
                    )
                }
                HostDialogPage.TRUSTED_DEVICES -> {
                    TrustedDevicesPage(
                        trustedDevices = trustedDevices,
                        onRemove = { id -> sessionManager.revokeTrust(id) },
                        onClearAll = { sessionManager.clearAllTrustedDevices() },
                        onBack = { currentPage = HostDialogPage.MAIN }
                    )
                }
            }
        }
    }
}

@Composable
private fun HostMainPage(
    sessionManager: SessionManager,
    hostState: HostState,
    pendingAuth: PendingAuthorization?,
    trustedDevicesCount: Int,
    vm: DartViewModel?,
    onNavigateToTrustedDevices: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: Icon, Titel, Status & Host-Schalter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryOrange.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_role_host),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = PrimaryOrange
                    )
                }

                Column {
                    Text(
                        text = "Geräte-Verbindung (Host)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = when (val state = hostState) {
                            is HostState.Active -> "● Aktiv (Port ${state.port})"
                            is HostState.Inactive -> "Inaktiv"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (hostState is HostState.Active) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (hostState is HostState.Active) FontWeight.Medium else FontWeight.Normal
                        )
                    )
                }
            }

            // Schalter: Host an / aus
            val isHostActive = hostState is HostState.Active
            Switch(
                checked = isHostActive,
                onCheckedChange = { active ->
                    if (active) {
                        sessionManager.startHost("sDart Host", preferredPort = 0)
                    } else {
                        sessionManager.stopHost()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryOrange
                )
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        // Kamera-Quellen-Auswahl (plattformübergreifend)
        if (vm != null) {
            CameraSourceSection(
                vm = vm,
                sessionManager = sessionManager,
                hostState = hostState
            )
        }

        // Inline-Verbindungsanfrage (falls ein unbekanntes Gerät anklopft - KEIN Pop-up-Fenster!)
        pendingAuth?.let { pending ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PrimaryOrange.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryOrange.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(roleIconRes(pending.hello.role)),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = PrimaryOrange
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Neue Verbindungsanfrage",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange
                                )
                            )
                            Text(
                                text = "\"${pending.hello.deviceName}\" (${pending.hello.platform.uppercase()}) möchte sich als ${roleLabel(pending.hello.role)} verbinden.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sessionManager.rejectPendingAuthorization() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text("Ablehnen")
                        }

                        Button(
                            onClick = { sessionManager.approvePendingAuthorization(remember = true) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryOrange,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Zulassen & Merken", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Liste verbundener Teilnehmer (wird nur angezeigt, wenn Host aktiv ist)
        if (hostState is HostState.Active) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Verbundene Geräte (${hostState.participants.size})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )

                if (hostState.participants.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HostCardBackground, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Warte auf Geräte...\nÖffne die s'Dart App auf deinem Handy und wähle 'Geräte verbinden'.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.65f),
                                lineHeight = 18.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hostState.participants, key = { it.connectionId }) { participant ->
                            ParticipantRow(
                                participant = participant,
                                onDisconnect = { sessionManager.disconnectParticipant(participant.connectionId) }
                            )
                        }
                    }
                }
            }
        }

        // Gemerkte Geräte Bereich (öffnet neue Seite im Popup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable { onNavigateToTrustedDevices() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check_circle),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PrimaryOrange
                )
                Text(
                    text = "Gemerkte Geräte: $trustedDevicesCount",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Text(
                text = "Verwalten ›",
                color = PrimaryOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Fertig / Schließen-Button
        Button(
            onClick = onDismissRequest,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White
            )
        ) {
            Text("Fertig")
        }
    }
}

private data class CameraSourceEntry(
    val id: String,
    val title: String,
    val icon: DrawableResource,
    val type: CameraSourceType,
    val webcamIndex: Int = 0,
    val participantId: String? = null
)

/**
 * Kompakte, zentrale Kamera-Quellenauswahl als Dropdown-Menü im Host-Fenster.
 * Beinhaltet alle verfügbaren lokalen Kameras sowie alle über den Host verbundenen Begleit-Kameras.
 */
@Composable
private fun CameraSourceSection(
    vm: DartViewModel,
    sessionManager: SessionManager,
    hostState: HostState
) {
    val connectedCameraParticipants = (hostState as? HostState.Active)?.participants?.filter { it.role == DeviceRole.CAMERA } ?: emptyList()

    val cameraSources = remember(vm.availableWebcams, connectedCameraParticipants) {
        val list = mutableListOf<CameraSourceEntry>()

        // 1. Alle verfügbaren lokalen Kameras (USB-Webcams am PC / Gerätekamera mobil)
        vm.availableWebcams.forEachIndexed { index, name ->
            list.add(
                CameraSourceEntry(
                    id = "local_$index",
                    title = name,
                    icon = Res.drawable.ic_role_camera,
                    type = CameraSourceType.LOCAL,
                    webcamIndex = index
                )
            )
        }

        // 2. Alle über den Host verbundenen Kameras
        connectedCameraParticipants.forEach { participant ->
            list.add(
                CameraSourceEntry(
                    id = "companion_${participant.connectionId}",
                    title = "${participant.name} (Host-Kamera)",
                    icon = Res.drawable.ic_mobile_connect,
                    type = CameraSourceType.COMPANION_STREAM,
                    participantId = participant.connectionId
                )
            )
        }

        list
    }

    val selectedEntry = cameraSources.firstOrNull { entry ->
        when (entry.type) {
            CameraSourceType.LOCAL -> vm.cameraSourceType == CameraSourceType.LOCAL && entry.webcamIndex == vm.selectedWebcamIndex
            CameraSourceType.COMPANION_STREAM -> {
                vm.cameraSourceType == CameraSourceType.COMPANION_STREAM &&
                    (vm.selectedCompanionConnectionId == null || entry.participantId == vm.selectedCompanionConnectionId)
            }
        }
    } ?: cameraSources.firstOrNull()

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera_streaming),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PrimaryOrange
                )
                Text(
                    text = "Kamera-Quelle",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                )
            }

            IconButton(
                onClick = { vm.refreshWebcams() },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_refresh),
                    contentDescription = "Kamera-Quellen aktualisieren",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    anchorWidthPx = coordinates.size.width
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HostCardBackground)
                    .border(
                        1.dp,
                        if (isDropdownExpanded) PrimaryOrange else GlassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = cameraSources.isNotEmpty()) {
                        isDropdownExpanded = !isDropdownExpanded
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(selectedEntry?.icon ?: Res.drawable.ic_role_camera),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryOrange
                    )
                    Text(
                        text = selectedEntry?.title ?: "Keine Kamera gefunden",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (selectedEntry != null) Color.White else Color.White.copy(alpha = 0.5f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (isDropdownExpanded) "▲" else "▼",
                    color = PrimaryOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier
                    .then(
                        if (anchorWidthPx > 0) {
                            Modifier.width(with(density) { anchorWidthPx.toDp() })
                        } else {
                            Modifier.widthIn(min = 200.dp, max = 320.dp)
                        }
                    )
                    .background(Color(0xFF262626))
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            ) {
                cameraSources.forEach { source ->
                    val isCurrent = source.id == selectedEntry?.id
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(source.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isCurrent) PrimaryOrange else Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = source.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) PrimaryOrange else Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_check_circle),
                                        contentDescription = "Ausgewählt",
                                        modifier = Modifier.size(14.dp),
                                        tint = PrimaryOrange
                                    )
                                }
                            }
                        },
                        onClick = {
                            isDropdownExpanded = false
                            when (source.type) {
                                CameraSourceType.LOCAL -> {
                                    sessionManager.activeCameraConnectionId = null
                                    vm.setWebcamIndex(source.webcamIndex)
                                    vm.setCameraSource(CameraSourceType.LOCAL)
                                }
                                CameraSourceType.COMPANION_STREAM -> {
                                    sessionManager.activeCameraConnectionId = source.participantId
                                    vm.selectCompanionCamera(source.participantId)
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) PrimaryOrange.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

/**
 * Eigene Seite im Popup zur Verwaltung der gemerkten (vertrauenswürdigen) Geräte.
 */
@Composable
private fun TrustedDevicesPage(
    trustedDevices: List<TrustedDevice>,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header mit Zurück-Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = "Zurück",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gemerkte Geräte",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${trustedDevices.size} vertrauenswürdige(s) Gerät(e)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        if (trustedDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HostCardBackground, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bisher keine gemerkten Geräte.\n\nSobald sich ein Companion-Gerät verbindet und du 'Zulassen & Merken' wählst, wird es hier dauerhaft hinterlegt.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.65f),
                        lineHeight = 18.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trustedDevices, key = { it.deviceId }) { device ->
                    TrustedDeviceRow(
                        device = device,
                        onRemove = { onRemove(device.deviceId) }
                    )
                }
            }

            if (trustedDevices.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onClearAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_trash),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Alle entfernen",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White
            )
        ) {
            Text("Zurück")
        }
    }
}

@Composable
private fun TrustedDeviceRow(
    device: TrustedDevice,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = device.platform.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            )
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_cross_circle),
                contentDescription = "Entfernen",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: ConnectedParticipant,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HostCardBackground, RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(PrimaryOrange.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(roleIconRes(participant.role)),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryOrange
                )
            }

            Column {
                Text(
                    text = participant.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${roleLabel(participant.role)} • ${participant.remoteAddress}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                )
            }
        }

        OutlinedButton(
            onClick = onDisconnect,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Text("Trennen", fontSize = 11.sp)
        }
    }
}
