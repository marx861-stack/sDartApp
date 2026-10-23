package com.sdart.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Gemeinsame Bausteine im Dashboard-Stil für die Desktop-Screens:
 * Orange nur für das, was gerade aktiv ist; alle Karten mit gleichem Rahmen und kleinem Titel.
 */
val DashboardCardShape = RoundedCornerShape(15.dp)
val ActiveOrangeBg = Color(0xFF2E2218)
val OnlineGreen = Color(0xFF4CAF50)

// Fast durchsichtige Kacheln wie im Spiel-Screen
val GlassCardBg = Color.White.copy(alpha = 0.04f)
val GlassCardBorder = Color.White.copy(alpha = 0.08f)
val GlassControlBorder = Color.White.copy(alpha = 0.16f)
val ActiveGlassBg = PrimaryOrange.copy(alpha = 0.10f)

@Composable
fun DashboardCard(
    title: String?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = verticalArrangement,
        modifier = modifier
            .clip(DashboardCardShape)
            .background(if (highlighted) ActiveGlassBg else GlassCardBg)
            .border(
                width = if (highlighted) 1.5.dp else 1.dp,
                color = if (highlighted) PrimaryOrange else GlassCardBorder,
                shape = DashboardCardShape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(24.dp)
    ) {
        if (title != null) {
            CardTitle(title)
        }
        content()
    }
}

@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        ),
        modifier = modifier.padding(bottom = 12.dp)
    )
}

/**
 * Textlink in Karten. Ohne [icon] mit Pfeil für "weiter", mit [icon] steht stattdessen das Symbol vor dem Text.
 */
@Composable
fun CardLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    icon: DrawableResource? = null
) {
    val color = if (primary) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = if (icon == null) "$text →" else text,
            style = MaterialTheme.typography.labelLarge.copy(
                color = color,
                fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun ModeChip(icon: DrawableResource, label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, GlassControlBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(painterResource(icon), null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassCardBg)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun StatusLine(online: Boolean, label: String, detail: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (online) OnlineGreen else GlassControlBorder)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val range = (values.max() - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val point = Offset(i * stepX, size.height - (v - min) / range * size.height)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Auswahl-Chip: aktiv orange hinterlegt, sonst nur grau umrandet.
 */
@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(if (selected) ActiveGlassBg else Color.Transparent)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) PrimaryOrange else GlassControlBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        if (icon != null) {
            Icon(
                painterResource(icon),
                null,
                tint = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            maxLines = 1
        )
    }
}

/**
 * Kompakter Zähler mit Minus/Plus, z. B. für die Anzahl der Legs.
 */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .border(1.dp, GlassControlBorder, shape)
    ) {
        StepperButton("−", enabled = value > range.first) { onValueChange(value - 1) }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        StepperButton("+", enabled = value < range.last) { onValueChange(value + 1) }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                color = if (enabled) PrimaryOrange else GlassControlBorder
            )
        )
    }
}

/**
 * Große Auswahl-Kachel für Formulare (gleich hoch wie Eingabefelder), teilt sich per weight die Breite.
 */
@Composable
fun SelectTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (selected) ActiveGlassBg else GlassCardBg)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) PrimaryOrange else GlassControlBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Breiter Zähler im Kachel-Format: Minus links, Wert mittig, Plus rechts.
 */
@Composable
fun StepperTile(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(GlassCardBg)
            .border(1.dp, GlassControlBorder, shape)
    ) {
        StepperTileButton("−", enabled = value > range.first) { onValueChange(value - 1) }
        Text(
            text = value.toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
        StepperTileButton("+", enabled = value < range.last) { onValueChange(value + 1) }
    }
}

@Composable
private fun StepperTileButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .width(72.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = if (enabled) PrimaryOrange else GlassControlBorder
            )
        )
    }
}
