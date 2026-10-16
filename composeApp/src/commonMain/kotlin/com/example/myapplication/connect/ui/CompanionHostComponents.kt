package com.example.myapplication.connect.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.connect.*
import com.example.myapplication.ui.theme.GlassBorder
import com.example.myapplication.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

private val HostDialogBackground = Color(0xFF1E1E1E)
private val HostCardBackground = Color(0xFF262626)

/**
 * Schlanke Übersicht und Steuerung des Host-Modus:
 * - Host-Status (aktiv / inaktiv) mit Ein/Aus-Schalter
 * - Inline-Freigabe für neue Geräte (ohne Pop-up-Fenster)
 * - Verbundene Geräte mit Rolle & Trennen-Option
 * - Ausklappbare Trust-Liste zum Verwalten und Entfernen einzelner Geräte
 */
@Composable
fun HostOverviewDialog(
    sessionManager: SessionManager,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hostState by sessionManager.hostState.collectAsState()
    val pendingAuth by sessionManager.pendingAuthorization.collectAsState()
    val trustedDevices by sessionManager.trustedDevices.collectAsState()
    var showTrustedList by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = HostDialogBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
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

                // Liste verbundener Teilnehmer
                when (val state = hostState) {
                    is HostState.Active -> {
                        Text(
                            text = "Verbundene Geräte (${state.participants.size})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )

                        if (state.participants.isEmpty()) {
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
                                items(state.participants, key = { it.connectionId }) { participant ->
                                    ParticipantRow(
                                        participant = participant,
                                        onDisconnect = { sessionManager.disconnectParticipant(participant.connectionId) }
                                    )
                                }
                            }
                        }
                    }
                    is HostState.Inactive -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HostCardBackground, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Schalter aktivieren, um diesen PC als Host bereitzustellen.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.6f)
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Trust-Liste Bereich (ausklappbar zum Verwalten einzelner Geräte)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { showTrustedList = !showTrustedList }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                text = "Gemerkt: ${trustedDevices.size} vertrauenswürdige(s) Gerät(e)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Text(
                            text = if (showTrustedList) "Ausblenden ▲" else "Verwalten ▼",
                            color = PrimaryOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(
                        visible = showTrustedList,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HostCardBackground, RoundedCornerShape(10.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (trustedDevices.isEmpty()) {
                                Text(
                                    text = "Bisher keine gemerkten Geräte.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(trustedDevices, key = { it.deviceId }) { device ->
                                        TrustedDeviceRow(
                                            device = device,
                                            onRemove = { sessionManager.revokeTrust(device.deviceId) }
                                        )
                                    }
                                }

                                if (trustedDevices.size > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { sessionManager.clearAllTrustedDevices() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Alle entfernen",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Schließen-Button
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
