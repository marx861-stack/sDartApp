package com.sdart.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdart.app.ui.LocalCompactLayout
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

/** Höchstzahl an Spielern für Spiel und Turnier; reine Obergrenze der Eingabe, die Spiellogik kennt keine. */
const val MaxPlayers = 99

/** Ein Eintrag der Spielerliste; [isLoaded] markiert einen gespeicherten Spieler. */
data class PlayerListEntry(val name: String, val isLoaded: Boolean)

/**
 * Spielerliste zum Eintragen der Teilnehmer, gleich aufgebaut in "Neues Spiel" und "Neues Turnier":
 * Aktionen und der Schalter für die zufällige Reihenfolge oben, darunter nummerierte Namensfelder.
 * Mit [scrollList] scrollt am Desktop nur die Namensliste in der verbleibenden Höhe (für lange Turnierlisten).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerListSection(
    players: List<PlayerListEntry>,
    minPlayers: Int,
    onAdd: () -> Unit,
    onLoad: () -> Unit,
    onRename: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    randomize: Boolean,
    onRandomizeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    scrollList: Boolean = false
) {
    val compact = LocalCompactLayout.current
    Column(modifier = modifier) {
        CardTitle(stringResource(Res.string.game_players_title, players.size))

        // Aktionen oberhalb der Namensfelder, damit neue Spieler nicht unter der Liste wegrutschen;
        // reicht die Breite nicht, rutscht der Zufalls-Schalter in eine zweite Zeile
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            // Bündig mit der Überschrift, damit Links und Schalter auch in der schmalen Desktop-Spalte nebeneinander passen
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            // Kurze Beschriftungen, damit Links und Schalter in eine Zeile passen
            if (players.size < MaxPlayers) {
                CardLink(
                    stringResource(Res.string.game_players_add),
                    onClick = onAdd,
                    icon = Res.drawable.ic_plus,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            CardLink(
                stringResource(Res.string.game_players_load),
                onClick = onLoad,
                primary = false,
                icon = Res.drawable.ic_folder,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            SelectChip(
                label = stringResource(Res.string.game_players_random),
                selected = randomize,
                onClick = { onRandomizeChange(!randomize) },
                icon = Res.drawable.ic_refresh
            )
        }

        // Auf dem Handy scrollt die ganze Seite, dort nie eine eigene Scrollfläche. Am Desktop wächst die Liste
        // nur so weit wie nötig (fill = false)
        Column(
            modifier = if (scrollList && !compact) {
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
            } else Modifier
        ) {
            players.forEachIndexed { index, player ->
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
                        onValueChange = { onRename(index, it) },
                        placeholder = { Text(stringResource(Res.string.game_players_placeholder, index + 1)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        trailingIcon = if (player.isLoaded) {
                            {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_folder),
                                    contentDescription = stringResource(Res.string.game_players_saved_player),
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
                    if (players.size > minPlayers) {
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_trash),
                                contentDescription = stringResource(Res.string.game_players_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
