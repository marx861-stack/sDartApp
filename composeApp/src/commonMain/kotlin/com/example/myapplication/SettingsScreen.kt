package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.ScreenHeader
import org.jetbrains.compose.resources.DrawableResource
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
    var showModelDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(16.dp)
        ) {
            // Einheitlicher Screen-Header
            ScreenHeader(
                title = "Einstellungen",
                onBack = onBack
            )

            Column(
                modifier = Modifier
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
                }

                // Entwickler-Einstellungen
                AnimatedVisibility(
                    visible = vm.isDevModeUnlocked,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingsCard(title = "Entwickler-Optionen", isDark = isDark) {
                        SettingsActionItem(
                            iconRes = Res.drawable.ic_developer,
                            title = "Modell",
                            value = vm.selectedModelType.name,
                            onClick = { showModelDialog = true },
                            isDark = isDark
                        )
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
                    }
                }
            }
        }
    }

    if (showModelDialog) {
        ModelSelectionDialog(
            currentMode = vm.selectedModelType,
            onModeSelected = {
                vm.selectedModelType = it
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false },
            isDark = isDark
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

@Composable
fun ModelSelectionDialog(
    currentMode: ModelType,
    onModeSelected: (ModelType) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ML Modell wählen") },
        text = {
            Column {
                ModelOption(
                    title = "RTMDet Tiny",
                    selected = currentMode == ModelType.RTMDET_TINY,
                    onClick = { onModeSelected(ModelType.RTMDET_TINY) }
                )
                ModelOption(
                    title = "YOLOX Tiny",
                    selected = currentMode == ModelType.YOLOX_TINY,
                    onClick = { onModeSelected(ModelType.YOLOX_TINY) }
                )
                ModelOption(
                    title = "YOLOX Nano",
                    selected = currentMode == ModelType.YOLOX_NANO,
                    onClick = { onModeSelected(ModelType.YOLOX_NANO) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ModelOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
