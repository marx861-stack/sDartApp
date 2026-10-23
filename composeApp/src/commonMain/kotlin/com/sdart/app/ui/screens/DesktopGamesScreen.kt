package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdart.app.ui.components.*
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

private data class ModeEntry(
    val mode: GameMode,
    val icon: DrawableResource,
    val title: StringResource,
    val subtitle: String
)

private val modeEntries = listOf(
    ModeEntry(GameMode.X501, Res.drawable.ic_standard, Res.string.game_mode_standard, "101 bis 701 oder eigene"),
    ModeEntry(GameMode.CRICKET, Res.drawable.ic_cricket, Res.string.game_mode_cricket, "15 bis 20 und Bull schließen"),
    ModeEntry(GameMode.ALL_FIVES, Res.drawable.ic_all_fives, Res.string.game_mode_all_fives, "Nur Fünfer-Aufnahmen zählen"),
    ModeEntry(GameMode.AROUND_THE_CLOCK, Res.drawable.ic_atc, Res.string.game_mode_atc, "Der Reihe nach 1 bis 20")
)

/**
 * "Neues Spiel" für breite Fenster: Spielmodus links, Spieler und Einstellungen kompakt in einer Karte daneben.
 * Ersetzt auf dem Desktop die Abfolge GameModesScreen → PlayerSetupScreen; die Logik teilt es über [PlayerSetupState].
 */
@Composable
fun DesktopGamesScreen(
    initialMode: GameMode?,
    repository: PlayerRepository,
    onModeSelected: (GameMode) -> Unit,
    onStartGame: (List<String>, GameMode, Int, Boolean, Boolean, Int, Boolean, ATCGameState?, CricketConfig?) -> Unit,
    onNavigate: (DesktopDestination) -> Unit
) {
    // X01-Varianten laufen alle über die Standard-Karte; die Startpunktzahl wählt man im Setup
    var selectedMode by remember {
        mutableStateOf(modeEntries.firstOrNull { it.mode == initialMode }?.mode ?: GameMode.X501)
    }
    val setup = rememberPlayerSetupState()
    var showLoadDialog by remember { mutableStateOf(false) }

    DesktopShell(
        selected = DesktopDestination.GAMES,
        title = "Neues Spiel",
        onNavigate = onNavigate
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.width(270.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                modeEntries.forEach { entry ->
                    ModeCard(
                        entry = entry,
                        selected = entry.mode == selectedMode,
                        onClick = {
                            selectedMode = entry.mode
                            onModeSelected(entry.mode)
                        }
                    )
                }
            }

            DashboardCard(title = null, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    PlayersSection(
                        setup = setup,
                        onLoadClick = { showLoadDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSection(
                        setup = setup,
                        mode = selectedMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.weight(1f).heightIn(min = 20.dp))
                HorizontalDivider(color = GlassCardBorder)
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (setup.showError) setup.errorMessage(selectedMode) else summary(setup, selectedMode),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (setup.showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { setup.startGame(selectedMode, onStartGame) },
                        enabled = setup.canStart,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Spiel starten", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showLoadDialog) {
        PlayerLoadDialog(
            repository = repository,
            onDismiss = { showLoadDialog = false },
            onPlayersSelected = { names -> setup.addLoadedPlayers(names) }
        )
    }
}

@Composable
private fun ModeCard(entry: ModeEntry, selected: Boolean, onClick: () -> Unit) {
    DashboardCard(
        title = null,
        highlighted = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(entry.icon),
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(entry.title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlayersSection(
    setup: PlayerSetupState,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        CardTitle("Spieler · ${setup.playerNames.size} von 8")

        setup.playerNames.forEachIndexed { index, player ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.width(22.dp)
                )
                OutlinedTextField(
                    value = player.name,
                    onValueChange = { setup.renamePlayer(index, it) },
                    placeholder = { Text("Spieler ${index + 1}") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    trailingIcon = if (player.isLoaded) {
                        {
                            Icon(
                                painter = painterResource(Res.drawable.ic_folder),
                                contentDescription = "Gespeicherter Spieler",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = GlassControlBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (setup.playerNames.size > 1) {
                    IconButton(onClick = { setup.removePlayer(index) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_trash),
                            contentDescription = "Spieler entfernen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(start = 22.dp, top = 6.dp, bottom = 16.dp)
        ) {
            if (setup.playerNames.size < 8) {
                CardLink("Spieler hinzufügen", onClick = { setup.addPlayer() })
            }
            CardLink("Gespeicherte laden", onClick = onLoadClick, primary = false)
        }

        SelectChip(
            label = "Zufällige Reihenfolge",
            selected = setup.randomizeOrder,
            onClick = { setup.randomizeOrder = !setup.randomizeOrder },
            icon = Res.drawable.ic_refresh
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSection(
    setup: PlayerSetupState,
    mode: GameMode,
    modifier: Modifier = Modifier
) {
    val chipRow: @Composable (@Composable FlowRowScope.() -> Unit) -> Unit = { chips ->
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 20.dp),
            content = chips
        )
    }

    Column(modifier = modifier) {
        when (mode) {
            GameMode.CRICKET -> {
                CardTitle("Variante")
                chipRow {
                    SelectChip("Standard", setup.cricketVariant == CricketVariant.STANDARD, { setup.cricketVariant = CricketVariant.STANDARD })
                    SelectChip("Cut Throat", setup.cricketVariant == CricketVariant.CUT_THROAT, { setup.cricketVariant = CricketVariant.CUT_THROAT })
                }
                LegsSetting(setup)
            }
            GameMode.ALL_FIVES -> {
                CardTitle("Ziel-Punktzahl")
                chipRow {
                    listOf(50 to "Kurz", 75 to "Standard", 100 to "Lang").forEach { (score, label) ->
                        SelectChip("$score · $label", setup.targetScore == score, { setup.targetScore = score })
                    }
                }
                LegsSetting(setup)
            }
            GameMode.AROUND_THE_CLOCK -> {
                CardTitle("Variante")
                chipRow {
                    listOf(
                        ATCVariant.STANDARD to "Standard",
                        ATCVariant.DOUBLE to "Nur Doubles",
                        ATCVariant.TRIPLE to "Nur Triples",
                        ATCVariant.QUICKLY to "Quickly",
                        ATCVariant.SHANGHAI to "Shanghai"
                    ).forEach { (variant, label) ->
                        SelectChip(label, setup.atcVariant == variant, { setup.atcVariant = variant })
                    }
                }
                CardTitle("Optionen")
                chipRow {
                    SelectChip("Rückwärts 20 → 1", setup.atcReverse, { setup.atcReverse = !setup.atcReverse })
                    SelectChip("Mit Bull", setup.atcWithBulls, { setup.atcWithBulls = !setup.atcWithBulls })
                }
            }
            else -> {
                CardTitle("Startpunkte")
                chipRow {
                    listOf(101, 301, 501, 701).forEach { score ->
                        SelectChip("$score", setup.startScore == score, {
                            setup.startScore = score
                            setup.customScore = ""
                        })
                    }
                    SelectChip("Eigene", setup.startScore == 0, { setup.startScore = 0 })
                }
                if (setup.startScore == 0) {
                    OutlinedTextField(
                        value = setup.customScore,
                        onValueChange = { setup.customScore = it.filter(Char::isDigit) },
                        placeholder = { Text("z. B. 1001") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = GlassControlBorder
                        ),
                        modifier = Modifier.width(180.dp).padding(bottom = 20.dp)
                    )
                }
                CardTitle("Regeln")
                chipRow {
                    SelectChip("Double In", setup.doubleIn, { setup.doubleIn = !setup.doubleIn })
                    SelectChip("Double Out", setup.doubleOut, { setup.doubleOut = !setup.doubleOut })
                }
                LegsSetting(setup)
            }
        }
    }
}

@Composable
private fun LegsSetting(setup: PlayerSetupState) {
    CardTitle(if (setup.playerNames.size <= 1) "Legs" else "Legs · Best of")
    Stepper(value = setup.legs, onValueChange = { setup.legs = it }, range = 1..21)
}

private fun summary(setup: PlayerSetupState, mode: GameMode): String {
    val players = setup.playerNames.count { it.name.isNotBlank() }
    val playerText = if (players == 1) "1 Spieler" else "$players Spieler"
    val modeText = when (mode) {
        GameMode.CRICKET -> "Cricket"
        GameMode.ALL_FIVES -> "Durch 5 teilbar · Ziel ${setup.targetScore ?: 50}"
        GameMode.AROUND_THE_CLOCK -> "Around the Clock"
        else -> {
            val score = if (setup.startScore > 0) setup.startScore.toString() else setup.customScore.ifBlank { "501" }
            "$score · ${if (setup.doubleOut) "Double Out" else "Single Out"}"
        }
    }
    val legsText = if (setup.legs > 1) " · ${setup.legs} Legs" else ""
    return "$modeText$legsText · $playerText"
}
