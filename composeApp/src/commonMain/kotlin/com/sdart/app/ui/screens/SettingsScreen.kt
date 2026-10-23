package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.ScreenHeader
import com.sdart.app.ui.theme.DarkTextSecondary
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: DartViewModel,
    onBack: () -> Unit
) {
    val isDark = true
    val scrollState = rememberScrollState()

    var versionClickCount by remember { mutableStateOf(0) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
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
                title = "Einstellungen",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Audio & Feedback
                SettingsCard(title = "Audio & Feedback", isDark = isDark) {
                    SettingsSwitchItem(
                        iconRes = Res.drawable.ic_sound,
                        title = "Soundeffekte",
                        description = "Töne bei Treffern und Events abspielen",
                        currentValue = vm.isSoundEnabled,
                        onValueChanged = { vm.isSoundEnabled = it },
                        isDark = isDark
                    )
                    SettingsSwitchItem(
                        iconRes = Res.drawable.ic_vibration,
                        title = "Vibration",
                        description = "Haptisches Feedback bei Treffern",
                        currentValue = vm.isVibrationEnabled,
                        onValueChanged = { vm.isVibrationEnabled = it },
                        isDark = isDark
                    )
                    SettingsSwitchItem(
                        iconRes = Res.drawable.ic_voice,
                        title = "Voice Caller",
                        description = "Sprachausgabe für Punkte und Ansagen",
                        currentValue = vm.isVoiceCallerEnabled,
                        onValueChanged = { vm.isVoiceCallerEnabled = it },
                        isDark = isDark
                    )
                }

                // Info & Version
                SettingsCard(title = "Über", isDark = isDark) {
                    SettingsInfoItem(
                        iconRes = Res.drawable.ic_info,
                        title = "App",
                        value = "s'Dart App Pro",
                        isDark = isDark
                    )
                    VersionInfoItem(
                        iconRes = Res.drawable.ic_info,
                        versionName = "1.0.0 (KMP Multiplatform)",
                        clickCount = versionClickCount,
                        onVersionClick = {
                            versionClickCount++
                            if (versionClickCount >= 10) {
                                vm.isDevModeUnlocked = true
                            }
                        },
                        isDark = isDark
                    )
                    SettingsActionItem(
                        iconRes = Res.drawable.ic_standard,
                        title = "Lizenzen",
                        value = "Anzeigen",
                        onClick = { showLicensesDialog = true },
                        isDark = isDark
                    )
                }

                // Entwickler-Einstellungen
                AnimatedVisibility(
                    visible = vm.isDevModeUnlocked,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingsCard(title = "Entwickler-Optionen", isDark = isDark) {
                        SettingsSwitchItem(
                            iconRes = Res.drawable.ic_developer,
                            title = "Detection Overlay",
                            description = "Bounding Boxes der Erkennung anzeigen",
                            currentValue = vm.showCameraDetectionOverlay,
                            onValueChanged = { vm.showCameraDetectionOverlay = it },
                            isDark = isDark
                        )
                        SettingsSwitchItem(
                            iconRes = Res.drawable.ic_developer,
                            title = "Dartboard Overlay",
                            description = "Kalibriertes Dartboard-Gitter anzeigen",
                            currentValue = vm.showDartboardOverlay,
                            onValueChanged = { vm.showDartboardOverlay = it },
                            isDark = isDark
                        )
                        SettingsSwitchItem(
                            iconRes = Res.drawable.ic_developer,
                            title = "Debug HUD",
                            description = "Inferenzzeit, FPS und Hardware-Status anzeigen",
                            currentValue = vm.showCameraDebugHud,
                            onValueChanged = { vm.showCameraDebugHud = it },
                            isDark = isDark
                        )
                    }
                }
            }
        }

        if (showLicensesDialog) {
            LicensesDialog(onDismiss = { showLicensesDialog = false })
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    // Ohne Schatten: die halbtransparente Glass-Fläche würde ihn als zweites Rechteck durchscheinen lassen
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsActionItem(
    iconRes: DrawableResource,
    title: String,
    value: String,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsSwitchItem(
    iconRes: DrawableResource,
    title: String,
    description: String,
    currentValue: Boolean,
    onValueChanged: (Boolean) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onValueChanged(!currentValue) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = currentValue,
            onCheckedChange = onValueChanged
        )
    }
}

@Composable
fun SettingsInfoItem(
    iconRes: DrawableResource,
    title: String,
    value: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun VersionInfoItem(
    iconRes: DrawableResource,
    versionName: String,
    clickCount: Int,
    onVersionClick: () -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVersionClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "Version",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
        Text(
            text = versionName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}


/**
 * Lizenzen aller ausgelieferten Bibliotheken. Die Liste wird beim Build vom AboutLibraries-Plugin
 * aus den tatsächlichen Abhängigkeiten der jeweiligen Plattform erzeugt ([licensesResourcePath]) und enthält die vollen
 * Lizenztexte. Manuelle Einträge (Modell, native Libs) liegen in composeApp/aboutlibraries/.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LicensesDialog(
    onDismiss: () -> Unit
) {
    val libraries by produceState<List<Library>?>(initialValue = null) {
        value = runCatching {
            val json = Res.readBytes(licensesResourcePath()).decodeToString()
            Libs.Builder().withJson(json).build().libraries.sortedBy { it.name.lowercase() }
        }.getOrElse {
            println("Error loading licenses: ${it.message}")
            emptyList()
        }
    }
    var expandedId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Lizenzen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                libraries?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = "${it.size} Open-Source-Komponenten · Eintrag antippen für den Lizenztext",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextSecondary
                    )
                }
            }
        },
        text = {
            val list = libraries
            when {
                list == null -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = APP_LICENSE_ID) {
                        AppLicenseCard(
                            expanded = expandedId == APP_LICENSE_ID,
                            onToggle = {
                                expandedId = if (expandedId == APP_LICENSE_ID) null else APP_LICENSE_ID
                            }
                        )
                    }
                    if (list.isEmpty()) {
                        item {
                            Text(
                                text = "Die Lizenzliste der Bibliotheken konnte nicht geladen werden.",
                                color = DarkTextSecondary
                            )
                        }
                    }
                    items(list, key = { it.uniqueId }) { library ->
                        LicenseEntry(
                            library = library,
                            expanded = expandedId == library.uniqueId,
                            onToggle = {
                                expandedId = if (expandedId == library.uniqueId) null else library.uniqueId
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Schließen",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        // Deckender Hintergrund: colorScheme.surface ist hier nur 5 % weiß und damit fast durchsichtig
        containerColor = Color(0xFF262626)
    )
}

@Composable
private fun LicenseEntry(
    library: Library,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val author = library.organization?.name
        ?: library.developers.mapNotNull { it.name }.joinToString(", ").takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2F2F2F))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                val meta = listOfNotNull(author, library.artifactVersion).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkTextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                library.licenses.forEach { license ->
                    Text(
                        text = license.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        library.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = DarkTextSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                library.licenses.forEach { license ->
                    Text(
                        text = license.licenseContent?.takeIf { it.isNotBlank() }
                            ?: license.url
                            ?: license.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        color = DarkTextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

private const val APP_LICENSE_ID = "app-license"
private const val APP_SOURCE_URL = "https://github.com/marx861-stack/sDartApp"

/**
 * Rechtshinweise der App selbst (GPL-3.0, Abschnitt 5d): Copyright, Gewährleistungsausschluss,
 * Quellcode-Angebot und voller Lizenztext. Der Text wird beim Build aus der LICENSE des Projekts kopiert.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun AppLicenseCard(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val licenseText by produceState<String?>(initialValue = null, expanded) {
        if (expanded && value == null) {
            value = runCatching { Res.readBytes("files/app-license.txt").decodeToString() }
                .getOrElse { "Lizenztext nicht verfügbar. Er steht unter $APP_SOURCE_URL" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2F2F2F))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "s'Dart App",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Copyright (C) 2026 marx861-stack",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkTextSecondary.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "GPL v3",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Dieses Programm kommt ohne jegliche Gewährleistung. Es ist freie Software und darf " +
                "unter den Bedingungen der GNU General Public License v3 weitergegeben werden.",
            style = MaterialTheme.typography.bodySmall,
            color = DarkTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Quellcode: $APP_SOURCE_URL",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable { uriHandler.openUri(APP_SOURCE_URL) }
        )
        AnimatedVisibility(visible = expanded) {
            Text(
                text = licenseText ?: "…",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                ),
                color = DarkTextSecondary,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )
        }
    }
}
