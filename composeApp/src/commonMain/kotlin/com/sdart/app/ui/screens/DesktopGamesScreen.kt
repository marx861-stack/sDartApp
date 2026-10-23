package com.sdart.app.ui.screens

import com.sdart.app.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdart.app.ui.components.DashboardCard
import com.sdart.app.ui.components.DesktopDestination
import com.sdart.app.ui.components.DesktopShell
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
    val description: StringResource
)

private val modeEntries = listOf(
    ModeEntry(GameMode.X501, Res.drawable.ic_standard, Res.string.game_mode_standard, Res.string.game_mode_standard_desc),
    ModeEntry(GameMode.CRICKET, Res.drawable.ic_cricket, Res.string.game_mode_cricket, Res.string.game_mode_cricket_desc),
    ModeEntry(GameMode.ALL_FIVES, Res.drawable.ic_all_fives, Res.string.game_mode_all_fives, Res.string.game_mode_all_fives_desc),
    ModeEntry(GameMode.AROUND_THE_CLOCK, Res.drawable.ic_atc, Res.string.game_mode_atc, Res.string.game_mode_atc_desc)
)

/**
 * "Neues Spiel" für breite Fenster: Spielmodus links, Spieler und Einstellungen daneben auf einer Seite.
 * Ersetzt auf dem Desktop die Abfolge GameModesScreen → PlayerSetupScreen.
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

    DesktopShell(
        selected = DesktopDestination.GAMES,
        title = "Neues Spiel",
        onNavigate = onNavigate
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.width(260.dp).padding(top = 8.dp),
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

            // Setup pro Modus neu aufbauen, damit modusabhängige Vorgaben (z. B. All-Fives-Ziel) greifen
            key(selectedMode) {
                PlayerSetupContent(
                    selectedGameMode = selectedMode,
                    repository = repository,
                    onStartGame = onStartGame,
                    isWidescreen = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
                modifier = Modifier.size(32.dp)
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
                    text = stringResource(entry.description),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
