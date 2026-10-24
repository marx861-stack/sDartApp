package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.sdart.app.ui.LocalCompactLayout
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
    // Spielregeln als kurze Absätze, angezeigt über das Info-Symbol der Karte
    val rules: List<String>
)

private val modeEntries = listOf(
    ModeEntry(
        GameMode.X501, Res.drawable.ic_standard, Res.string.game_mode_standard, listOf(
            "Alle starten mit 501 Punkten (oder 101, 301, 701 bzw. eigenen). Pro Aufnahme wirfst du drei Darts, " +
                "ihre Summe wird abgezogen.",
            "Wer zuerst genau auf 0 kommt, gewinnt das Leg. Bei Double Out muss der letzte Dart ein Double sein, " +
                "bei Double In zählt erst ab dem ersten Double.",
            "Wer unter 0 fällt (bei Double Out auch auf 1), überwirft sich: Die Aufnahme zählt nicht."
        )
    ),
    ModeEntry(
        GameMode.CRICKET, Res.drawable.ic_cricket, Res.string.game_mode_cricket, listOf(
            "Es zählen nur 15 bis 20 und das Bull. Jede Zahl ist nach drei Treffern geschlossen; Double zählt " +
                "zweifach, Triple dreifach, beim Bull Single einfach und Double zweifach.",
            "Weitere Treffer auf eine eigene geschlossene Zahl, die ein Gegner noch offen hat, bringen Punkte.",
            "Das Leg endet, wenn alle alles geschlossen haben. Standard: Die Punkte bekommst du selbst, die meisten " +
                "gewinnen. Cut Throat: Die Punkte gehen an die Gegner, die wenigsten gewinnen."
        )
    ),
    ModeEntry(
        GameMode.ALL_FIVES, Res.drawable.ic_all_fives, Res.string.game_mode_all_fives, listOf(
            "Eine Aufnahme zählt nur, wenn alle drei Darts treffen und ihre Summe durch 5 teilbar ist. " +
                "Dann wird die ganze Summe gutgeschrieben.",
            "Wer zuerst die Ziel-Punktzahl erreicht (50, 75 oder 100), gewinnt das Leg."
        )
    ),
    ModeEntry(
        GameMode.AROUND_THE_CLOCK, Res.drawable.ic_atc, Res.string.game_mode_atc, listOf(
            "Triff die Zahlen der Reihe nach von 1 bis 20, auf Wunsch rückwärts oder mit dem Bull am Ende. " +
                "Wer zuerst durch ist, gewinnt.",
            "Standard: Jedes Feld der Zahl zählt. Doubles bzw. Triples: Nur dieses Feld zählt.",
            "Quickly: Single, Double und Triple bringen dich 1, 2 oder 3 Zahlen weiter. Shanghai: Auf jeder Zahl " +
                "nacheinander Single, Double und Triple treffen."
        )
    )
)

/**
 * "Neues Spiel": Spielmodus, Spieler und Einstellungen auf einer Seite. Breite Fenster zeigen die Modi links
 * und Spieler und Einstellungen kompakt in einer Karte daneben, schmale Fenster stapeln alles untereinander.
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
    val compact = LocalCompactLayout.current

    val modeCard: @Composable (ModeEntry, Modifier) -> Unit = { entry, modifier ->
        ModeCard(
            entry = entry,
            selected = entry.mode == selectedMode,
            onClick = {
                selectedMode = entry.mode
                onModeSelected(entry.mode)
            },
            modifier = modifier
        )
    }
    val summaryText: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = if (setup.showError) setup.errorMessage(selectedMode) else summary(setup, selectedMode),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (setup.showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = modifier
        )
    }
    val startButton: @Composable (Modifier) -> Unit = { modifier ->
        Button(
            onClick = { setup.startGame(selectedMode, onStartGame) },
            enabled = setup.canStart,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            modifier = modifier
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Spiel starten", fontWeight = FontWeight.SemiBold)
        }
    }

    DesktopPage(title = "Neues Spiel") {
        if (compact) {
            // Karten einer Reihe gleich hoch, auch wenn ein Untertitel zweizeilig wird
            modeEntries.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    row.forEach { entry -> modeCard(entry, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
            DashboardCard(title = null, modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                PlayersSection(setup = setup, onLoadClick = { showLoadDialog = true }, modifier = Modifier.fillMaxWidth())
            }
            DashboardCard(title = null, modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                SettingsSection(setup = setup, mode = selectedMode, modifier = Modifier.fillMaxWidth())
            }
            summaryText(Modifier.padding(horizontal = 4.dp))
            startButton(Modifier.fillMaxWidth())
            return@DesktopPage
        }

        // Modus-Karten und Setup-Panel füllen die ganze verfügbare Höhe
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                modeEntries.forEach { entry -> modeCard(entry, Modifier.weight(1f)) }
            }

            DashboardCard(title = null, modifier = Modifier.weight(2.6f).fillMaxHeight()) {
                // Spieler und Einstellungen teilen sich die Kartenhöhe und scrollen bei Bedarf jeweils für sich,
                // damit auch viele Spieler erreichbar bleiben und der Start-Button unten stehen bleibt
                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    PlayersSection(
                        setup = setup,
                        onLoadClick = { showLoadDialog = true },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        scrollList = true
                    )
                    SettingsSection(
                        setup = setup,
                        mode = selectedMode,
                        modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = GlassCardBorder)
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    summaryText(Modifier.weight(1f))
                    startButton(Modifier)
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
    val compact = LocalCompactLayout.current
    val title = stringResource(entry.title)
    Box(modifier = modifier.fillMaxWidth()) {
        DashboardCard(
            title = null,
            highlighted = selected,
            onClick = onClick,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentPadding = if (compact) 14.dp else 24.dp
        ) {
            // Auf dem Handy sind die Karten flach: rechts Platz lassen für das Info-Symbol in der Ecke
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = if (compact) 18.dp else 0.dp)
            ) {
                Icon(
                    painter = painterResource(entry.icon),
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(if (compact) 26.dp else 40.dp)
                )
                Spacer(Modifier.width(if (compact) 10.dp else 18.dp))
                Text(
                    text = title,
                    style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge).copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    ),
                    // Lange Namen wie "Around the Clock" dürfen umbrechen (neben dem Info-Symbol ist wenig Platz)
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        RulesInfoButton(title = title, rules = entry.rules, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
    }
}

@Composable
private fun PlayersSection(
    setup: PlayerSetupState,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollList: Boolean = false
) {
    PlayerListSection(
        players = setup.playerNames.map { PlayerListEntry(it.name, it.isLoaded) },
        minPlayers = 1,
        onAdd = { setup.addPlayer() },
        onLoad = onLoadClick,
        onRename = { index, name -> setup.renamePlayer(index, name) },
        onRemove = { index -> setup.removePlayer(index) },
        randomize = setup.randomizeOrder,
        onRandomizeChange = { setup.randomizeOrder = it },
        modifier = modifier,
        scrollList = scrollList
    )
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
                val compact = LocalCompactLayout.current
                val customTile: @Composable (Modifier) -> Unit = { m ->
                    SelectTile(if (compact) "Eigene Startpunkte" else "Eigene", setup.startScore == 0, { setup.startScore = 0 }, m)
                }
                tileRow(5) {
                    listOf(101, 301, 501, 701).forEach { score ->
                        SelectTile("$score", setup.startScore == score, {
                            setup.startScore = score
                            setup.customScore = ""
                        }, Modifier.weight(1f))
                    }
                    if (!compact) customTile(Modifier.weight(1f))
                }
                // Auf dem Handy reicht die Breite nicht für fünf Kacheln, "Eigene" bekommt eine eigene Reihe
                if (compact) {
                    tileRow(1) { customTile(Modifier.weight(1f)) }
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
