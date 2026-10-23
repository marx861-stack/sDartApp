package com.sdart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

/**
 * Hauptbereiche der Desktop-Navigationsleiste.
 */
enum class DesktopDestination {
    OVERVIEW, GAMES, TOURNAMENT, PLAYERS, DEVICES, SETTINGS
}

// Unterhalb dieser Höhe wird gescrollt statt weiter gestaucht
private val MinContentHeight = 820.dp

/**
 * Feste Hülle der Desktop-Screens: Navigationsleiste links, rechts der Seiteninhalt.
 * Wird einmal oberhalb der Seitenübergänge aufgebaut, damit die Leiste beim Wechsel der Bereiche
 * stehen bleibt und nur der Inhalt ([DesktopPage]) wechselt.
 */
@Composable
fun DesktopScaffold(
    selected: DesktopDestination,
    onNavigate: (DesktopDestination) -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .safeDrawingPadding()
    ) {
        DesktopNavRail(selected = selected, onNavigate = onNavigate)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

/**
 * Seite im Inhaltsbereich von [DesktopScaffold]: Titel und Inhalt über die ganze Fläche.
 * Der Inhalt bekommt eine feste Höhe (Fensterhöhe, mindestens [MinContentHeight]), damit Kacheln
 * per `weight` mitwachsen und auch große Monitore füllen.
 */
@Composable
fun DesktopPage(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentHeight = maxOf(maxHeight, MinContentHeight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .height(contentHeight)
                .padding(horizontal = 36.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            content()
        }
    }
}

@Composable
private fun DesktopNavRail(
    selected: DesktopDestination,
    onNavigate: (DesktopDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .width(244.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, bottom = 20.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_main_logo),
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "S'Dart App",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        val item: @Composable (DesktopDestination, DrawableResource, String) -> Unit = { destination, icon, label ->
            NavRailItem(
                icon = icon,
                label = label,
                selected = selected == destination,
                onClick = { if (selected != destination) onNavigate(destination) }
            )
        }
        item(DesktopDestination.OVERVIEW, Res.drawable.ic_chart_bar, "Übersicht")
        item(DesktopDestination.GAMES, Res.drawable.ic_target, stringResource(Res.string.main_title_games))
        item(DesktopDestination.TOURNAMENT, Res.drawable.ic_trophy, stringResource(Res.string.main_title_tournament))
        item(DesktopDestination.PLAYERS, Res.drawable.ic_player_management, stringResource(Res.string.main_title_players))
        item(DesktopDestination.DEVICES, Res.drawable.ic_mobile_connect, stringResource(Res.string.main_title_connect))

        Spacer(Modifier.weight(1f))

        NavRailItem(
            vector = Icons.Default.Settings,
            label = stringResource(Res.string.settings),
            selected = selected == DesktopDestination.SETTINGS,
            onClick = { if (selected != DesktopDestination.SETTINGS) onNavigate(DesktopDestination.SETTINGS) }
        )
    }
}

@Composable
private fun NavRailItem(
    label: String,
    onClick: () -> Unit,
    icon: DrawableResource? = null,
    vector: ImageVector? = null,
    selected: Boolean = false
) {
    val contentColor = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ActiveGlassBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        when {
            icon != null -> Icon(painterResource(icon), null, tint = contentColor, modifier = Modifier.size(22.dp))
            vector != null -> Icon(vector, null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
