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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdart.app.ui.LocalCompactLayout
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
 * Hülle der Hauptbereiche auf schmalen Fenstern (Handy): Inhalt oben, darunter eine feste Menüleiste.
 * Einstellungen haben keinen eigenen Eintrag, sie liegen als Zahnrad auf der Übersicht.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobileScaffold(
    selected: DesktopDestination,
    onNavigate: (DesktopDestination) -> Unit,
    content: @Composable () -> Unit
) {
    val bottomInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Den unteren Systemrand übernimmt die Menüleiste, sonst bliebe darüber eine Lücke
                .consumeWindowInsets(bottomInsets)
        ) {
            content()
        }
        MobileNavBar(selected = selected, onNavigate = onNavigate, bottomInsets = bottomInsets)
    }
}

@Composable
private fun MobileNavBar(
    selected: DesktopDestination,
    onNavigate: (DesktopDestination) -> Unit,
    bottomInsets: WindowInsets
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlassCardBorder))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(bottomInsets)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val item: @Composable RowScope.(DesktopDestination, DrawableResource, String) -> Unit = { destination, icon, label ->
                MobileNavItem(
                    icon = icon,
                    label = label,
                    selected = selected == destination,
                    onClick = { if (selected != destination) onNavigate(destination) },
                    modifier = Modifier.weight(1f)
                )
            }
            item(DesktopDestination.OVERVIEW, Res.drawable.ic_chart_bar, stringResource(Res.string.nav_overview))
            item(DesktopDestination.GAMES, Res.drawable.ic_target, stringResource(Res.string.main_title_games))
            item(DesktopDestination.TOURNAMENT, Res.drawable.ic_trophy, stringResource(Res.string.main_title_tournament))
            item(DesktopDestination.PLAYERS, Res.drawable.ic_player_management, stringResource(Res.string.main_title_players))
            item(DesktopDestination.DEVICES, Res.drawable.ic_mobile_connect, stringResource(Res.string.nav_devices))
        }
    }
}

@Composable
private fun MobileNavItem(
    icon: DrawableResource,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        // Aktiver Eintrag mit oranger Pille hinter dem Symbol, wie die Auswahl in der Desktop-Leiste
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) ActiveGlassBg else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 3.dp)
        ) {
            Icon(painterResource(icon), null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Seite im Inhaltsbereich von [DesktopScaffold]: Titel und Inhalt über die ganze Fläche.
 * Der Inhalt bekommt eine feste Höhe (Fensterhöhe, mindestens [minHeight]), damit Kacheln
 * per `weight` mitwachsen und auch große Monitore füllen.
 */
@Composable
fun DesktopPage(
    title: String,
    fillHeight: Boolean = true,
    minHeight: Dp = MinContentHeight,
    onBack: (() -> Unit)? = null,
    compactScroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalCompactLayout.current) {
        CompactPage(title = title, onBack = onBack, scroll = compactScroll, content = content)
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentHeight = maxOf(maxHeight, minHeight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .then(if (fillHeight) Modifier.height(contentHeight) else Modifier)
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

/**
 * [DesktopPage] auf schmalen Fenstern: eine scrollbare Spalte ohne feste Höhe, die Karten stehen untereinander.
 * Inhalte dürfen hier daher keine Gewichte oder Lazy-Listen über die ganze Höhe verwenden.
 * Ohne [scroll] ist die Spalte genau fensterhoch, für Inhalte, die selbst scrollen oder per `weight` die Höhe füllen.
 * [onBack] zeigt einen Zurück-Pfeil vor dem Titel; am Desktop führt die Navigationsleiste zurück.
 */
@Composable
private fun CompactPage(
    title: String,
    onBack: (() -> Unit)?,
    scroll: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    // Systemleisten-Abstand außen, sonst scrollt der Inhalt unter die Statusleiste
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier.fillMaxHeight())
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.offset(x = (-8).dp)) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Zurück",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        item(DesktopDestination.OVERVIEW, Res.drawable.ic_chart_bar, stringResource(Res.string.nav_overview))
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
