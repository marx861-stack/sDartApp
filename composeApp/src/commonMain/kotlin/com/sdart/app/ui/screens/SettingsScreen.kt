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
import com.sdart.app.ui.LocalCompactLayout
import com.sdart.app.ui.components.ActiveGlassBg
import com.sdart.app.ui.components.CardLink
import com.sdart.app.ui.components.DashboardCard
import com.sdart.app.ui.components.DesktopPage
import com.sdart.app.ui.components.GlassCardBg
import com.sdart.app.ui.components.GlassCardBorder
import com.sdart.app.ui.components.GlassControlBorder
import com.sdart.app.ui.theme.PrimaryOrange
import androidx.compose.foundation.shape.CircleShape
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
    var versionClickCount by remember { mutableStateOf(0) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    val onVersionClick: () -> Unit = {
        versionClickCount++
        if (versionClickCount >= 10) {
            vm.isDevModeUnlocked = true
        }
    }

    SettingsContent(
        vm = vm,
        onBack = onBack,
        onVersionClick = onVersionClick,
        onShowLicenses = { showLicensesDialog = true }
    )

    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

/**
 * Einstellungen: große Schalter (Audio & Feedback, ggf. Entwickler-Optionen) und "Über S'Dart" mit Version,
 * Lizenzhinweis, Lizenzen und Quellcode. Breite Fenster zeigen beides nebeneinander, schmale untereinander.
 */
@Composable
private fun SettingsContent(
    vm: DartViewModel,
    onBack: () -> Unit,
    onVersionClick: () -> Unit,
    onShowLicenses: () -> Unit
) {
    val compact = LocalCompactLayout.current
    val cardPadding = if (compact) 16.dp else 24.dp
    val toggleCards: @Composable ColumnScope.() -> Unit = {
        DashboardCard(title = "Audio & Feedback", modifier = Modifier.fillMaxWidth(), contentPadding = cardPadding) {
            DesktopToggleRow(
                icon = Res.drawable.ic_sound,
                title = "Soundeffekte",
                description = "Töne bei Treffern und Events abspielen",
                checked = vm.isSoundEnabled,
                onCheckedChange = { vm.isSoundEnabled = it }
            )
            HorizontalDivider(color = GlassCardBorder)
            DesktopToggleRow(
                icon = Res.drawable.ic_voice,
                title = "Voice Caller",
                description = "Sprachausgabe für Punkte und Ansagen",
                checked = vm.isVoiceCallerEnabled,
                onCheckedChange = { vm.isVoiceCallerEnabled = it }
            )
            HorizontalDivider(color = GlassCardBorder)
            DesktopToggleRow(
                icon = Res.drawable.ic_vibration,
                title = "Vibration",
                description = "Haptisches Feedback bei Treffern (Tablets und Handys)",
                checked = vm.isVibrationEnabled,
                onCheckedChange = { vm.isVibrationEnabled = it }
            )
        }

        AnimatedVisibility(
            visible = vm.isDevModeUnlocked,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            DashboardCard(title = "Entwickler-Optionen", modifier = Modifier.fillMaxWidth(), contentPadding = cardPadding) {
                DesktopToggleRow(
                    icon = Res.drawable.ic_developer,
                    title = "Detection Overlay",
                    description = "Bounding Boxes der Erkennung anzeigen",
                    checked = vm.showCameraDetectionOverlay,
                    onCheckedChange = { vm.showCameraDetectionOverlay = it }
                )
                HorizontalDivider(color = GlassCardBorder)
                DesktopToggleRow(
                    icon = Res.drawable.ic_developer,
                    title = "Dartboard Overlay",
                    description = "Kalibriertes Dartboard-Gitter anzeigen",
                    checked = vm.showDartboardOverlay,
                    onCheckedChange = { vm.showDartboardOverlay = it }
                )
                HorizontalDivider(color = GlassCardBorder)
                DesktopToggleRow(
                    icon = Res.drawable.ic_developer,
                    title = "Debug HUD",
                    description = "Inferenzzeit, FPS und Hardware-Status anzeigen",
                    checked = vm.showCameraDebugHud,
                    onCheckedChange = { vm.showCameraDebugHud = it }
                )
            }
        }
    }

    // Wenig Inhalt: am Desktop genau fensterhoch, damit die Buttons unten in "Über S'Dart" ohne Scrollen sichtbar sind
    DesktopPage(title = "Einstellungen", minHeight = 0.dp, onBack = onBack) {
        if (compact) {
            toggleCards()
            AboutCard(
                devModeUnlocked = vm.isDevModeUnlocked,
                onVersionClick = onVersionClick,
                onShowLicenses = onShowLicenses,
                modifier = Modifier.fillMaxWidth()
            )
            return@DesktopPage
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.weight(1.5f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = toggleCards
            )

            AboutCard(
                devModeUnlocked = vm.isDevModeUnlocked,
                onVersionClick = onVersionClick,
                onShowLicenses = onShowLicenses,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun DesktopToggleRow(
    icon: DrawableResource,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val compact = LocalCompactLayout.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = if (compact) 10.dp else 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 38.dp else 44.dp)
                .background(if (checked) ActiveGlassBg else GlassCardBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (checked) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = if (compact) 12.dp else 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryOrange,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = GlassControlBorder
            )
        )
    }
}

/** App-Infos: Logo, Version (zehnmal klicken schaltet die Entwickler-Optionen frei), Lizenz und Quellcode. */
@Composable
private fun AboutCard(
    devModeUnlocked: Boolean,
    onVersionClick: () -> Unit,
    onShowLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val compact = LocalCompactLayout.current
    DashboardCard(title = "Über S'Dart", modifier = modifier, contentPadding = if (compact) 16.dp else 24.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.ic_main_logo),
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(if (compact) 44.dp else 56.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "S'Dart App",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Version 1.0.0 (KMP Multiplatform)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onVersionClick
                    )
                )
            }
        }

        if (devModeUnlocked) {
            Text(
                text = "Entwickler-Modus aktiv",
                style = MaterialTheme.typography.labelMedium.copy(color = PrimaryOrange, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(ActiveGlassBg, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider(color = GlassCardBorder, modifier = Modifier.padding(vertical = 20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Copyright (C) 2026 marx861-stack",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "GPL v3",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = PrimaryOrange),
                modifier = Modifier
                    .background(ActiveGlassBg, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            text = "Dieses Programm kommt ohne jegliche Gewährleistung. Es ist freie Software und darf " +
                "unter den Bedingungen der GNU General Public License v3 weitergegeben werden.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(top = 8.dp)
        )

        // Am Desktop rutschen die Buttons ans Kartenende; in der scrollbaren Handy-Spalte gibt es keine Resthöhe
        Spacer(if (compact) Modifier.height(16.dp) else Modifier.weight(1f).heightIn(min = 20.dp))

        val licensesButton: @Composable () -> Unit = {
            OutlinedButton(
                onClick = onShowLicenses,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GlassControlBorder),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text("Lizenzen anzeigen", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        val sourceLink: @Composable () -> Unit = {
            CardLink(
                text = "Quellcode auf GitHub",
                onClick = { uriHandler.openUri(APP_SOURCE_URL) }
            )
        }
        // Auf dem Handy reicht die Breite nicht für beide nebeneinander
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                licensesButton()
                sourceLink()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                licensesButton()
                sourceLink()
            }
        }
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
                    text = "S'Dart App",
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
