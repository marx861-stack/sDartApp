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
    onStartGame: (List<String>, GameMode, Int, Boolean, Boolean, Int, Boolean, ATCGameState?, CricketConfig?) -> Unit
) {
    // X01-Varianten laufen alle über die Standard-Karte; die Startpunktzahl wählt man im Setup
    var selectedMode by remember {
        mutableStateOf(modeEntries.firstOrNull { it.mode == initialMode }?.mode ?: GameMode.X501)
    }
    val setup = rememberPlayerSetupState()
    var showLoadDialog by remember { mutableStateOf(false) }

    DesktopPage(title = "Neues Spiel") {
        // Modus-Karten und Setup-Panel füllen die ganze verfügbare Höhe
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                modeEntries.forEach { entry ->
                    ModeCard(
                        entry = entry,
                        selected = entry.mode == selectedMode,
                        onClick = {
                            selectedMode = entry.mode
                            onModeSelected(entry.mode)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            DashboardCard(title = null, modifier = Modifier.weight(2.6f).fillMaxHeight()) {
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
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
private fun ModeCard(entry: ModeEntry, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DashboardCard(
        title = null,
        highlighted = selected,
        onClick = onClick,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(entry.icon),
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    text = stringResource(entry.title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
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

        // Aktionen oberhalb der Namensfelder, damit neue Spieler nicht unter der Liste wegrutschen
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(start = 22.dp, bottom = 12.dp)
        ) {
            if (setup.playerNames.size < 8) {
                CardLink("Spieler hinzufügen", onClick = { setup.addPlayer() }, icon = Res.drawable.ic_plus)
            }
            CardLink("Gespeicherte laden", onClick = onLoadClick, primary = false, icon = Res.drawable.ic_folder)
        }

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

        Spacer(Modifier.height(8.dp))
        SelectChip(
            label = "Zufällige Reihenfolge",
            selected = setup.randomizeOrder,
            onClick = { setup.randomizeOrder = !setup.randomizeOrder },
            icon = Res.drawable.ic_refresh
        )
    }
}

@Composable
private fun SettingsSection(
    setup: PlayerSetupState,
    mode: GameMode,
    modifier: Modifier = Modifier
) {
    // Eine Reihe gleich breiter Kacheln; Lücken am Ende füllt ein Platzhalter, damit Kacheln gleich groß bleiben
    val tileRow: @Composable (Int, @Composable RowScope.() -> Unit) -> Unit = { _, tiles ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            content = tiles
        )
    }
    // Erster Abschnitt bündig mit "Spieler" links, weitere mit Abstand
    val section: @Composable (String, Boolean) -> Unit = { title, first ->
        CardTitle(title, modifier = Modifier.padding(top = if (first) 0.dp else 14.dp))
    }

    Column(modifier = modifier) {
        when (mode) {
            GameMode.CRICKET -> {
                section("Variante", true)
                tileRow(2) {
                    SelectTile("Standard", setup.cricketVariant == CricketVariant.STANDARD, { setup.cricketVariant = CricketVariant.STANDARD }, Modifier.weight(1f), "Punkte für dich")
                    SelectTile("Cut Throat", setup.cricketVariant == CricketVariant.CUT_THROAT, { setup.cricketVariant = CricketVariant.CUT_THROAT }, Modifier.weight(1f), "Punkte an Gegner")
                }
                LegsSetting(setup)
            }
            GameMode.ALL_FIVES -> {
                section("Ziel-Punktzahl", true)
                tileRow(3) {
                    listOf(50 to "Kurz", 75 to "Standard", 100 to "Lang").forEach { (score, label) ->
                        SelectTile("$score", setup.targetScore == score, { setup.targetScore = score }, Modifier.weight(1f), label)
                    }
                }
                LegsSetting(setup)
            }
            GameMode.AROUND_THE_CLOCK -> {
                section("Variante", true)
                tileRow(3) {
                    listOf(
                        Triple(ATCVariant.STANDARD, "Standard", "Einfach treffen"),
                        Triple(ATCVariant.DOUBLE, "Doubles", "Nur Doubles"),
                        Triple(ATCVariant.TRIPLE, "Triples", "Nur Triples")
                    ).forEach { (variant, label, sub) ->
                        SelectTile(label, setup.atcVariant == variant, { setup.atcVariant = variant }, Modifier.weight(1f), sub)
                    }
                }
                tileRow(3) {
                    listOf(
                        Triple(ATCVariant.QUICKLY, "Quickly", "S=1, D=2, T=3"),
                        Triple(ATCVariant.SHANGHAI, "Shanghai", "S, D, T nötig")
                    ).forEach { (variant, label, sub) ->
                        SelectTile(label, setup.atcVariant == variant, { setup.atcVariant = variant }, Modifier.weight(1f), sub)
                    }
                    Spacer(Modifier.weight(1f))
                }
                section("Optionen", false)
                tileRow(2) {
                    SelectTile("Rückwärts", setup.atcReverse, { setup.atcReverse = !setup.atcReverse }, Modifier.weight(1f), "20 → 1")
                    SelectTile("Mit Bull", setup.atcWithBulls, { setup.atcWithBulls = !setup.atcWithBulls }, Modifier.weight(1f), "Bull am Ende")
                }
            }
            else -> {
                section("Startpunkte", true)
                tileRow(5) {
                    listOf(101, 301, 501, 701).forEach { score ->
                        SelectTile("$score", setup.startScore == score, {
                            setup.startScore = score
                            setup.customScore = ""
                        }, Modifier.weight(1f))
                    }
                    SelectTile("Eigene", setup.startScore == 0, { setup.startScore = 0 }, Modifier.weight(1f))
                }
                if (setup.startScore == 0) {
                    OutlinedTextField(
                        value = setup.customScore,
                        onValueChange = { setup.customScore = it.filter(Char::isDigit) },
                        placeholder = { Text("Eigene Startpunkte, z. B. 1001") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = GlassControlBorder
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    )
                }
                section("Regeln", false)
                tileRow(2) {
                    SelectTile("Double In", setup.doubleIn, { setup.doubleIn = !setup.doubleIn }, Modifier.weight(1f), "Start mit Double")
                    SelectTile("Double Out", setup.doubleOut, { setup.doubleOut = !setup.doubleOut }, Modifier.weight(1f), "Sieg mit Double")
                }
                LegsSetting(setup)
            }
        }
    }
}

@Composable
private fun LegsSetting(setup: PlayerSetupState) {
    CardTitle(if (setup.playerNames.size <= 1) "Legs" else "Legs · Best of", modifier = Modifier.padding(top = 14.dp))
    StepperTile(
        value = setup.legs,
        onValueChange = { setup.legs = it },
        range = 1..21,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun summary(setup: PlayerSetupState, mode: GameMode): String {
    // Jede Zeile spielt mit; leere Namen werden beim Start zu "Spieler N"
    val players = setup.playerNames.size
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
