package com.example.myapplication

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Plattformspezifischer Handler für System-Zurück-Events (z. B. Android Hardware-/Software-Back-Button
 * und System-Gesten-Navigation / Wischgesten vom Bildschirmrand).
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * Erkennt eine Wischgeste (Touch) vom linken Bildschirmrand nach rechts zum Zurücknavigieren.
 * Unterstützt Gesten auf mobilen Geräten (iOS und Android) ergänzend zur System-Navigation.
 */
fun Modifier.edgeSwipeBack(
    enabled: Boolean = true,
    edgeThreshold: Dp = 28.dp,
    minSwipeDistance: Dp = 60.dp,
    onBack: () -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled, onBack) {
    val edgeThresholdPx = edgeThreshold.toPx()
    val minSwipeDistancePx = minSwipeDistance.toPx()

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // Nur Touch-Gesten (kein Maus-Klick auf Desktop) und nur nahe am linken Bildschirmrand
        if (down.type != PointerType.Touch || down.position.x > edgeThresholdPx) {
            return@awaitEachGesture
        }

        var totalDx = 0f
        var totalDy = 0f
        var triggered = false

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                break
            }

            val delta = change.position - change.previousPosition
            totalDx += delta.x
            totalDy += delta.y

            // Auslösen, wenn deutlich nach rechts gewischt wurde und die horizontale Bewegung dominiert
            if (totalDx > minSwipeDistancePx && abs(totalDx) > abs(totalDy) * 1.5f) {
                triggered = true
                change.consume()
                break
            }

            // Abbruch, wenn die Bewegung primär vertikal verläuft (z. B. Scrollen)
            if (abs(totalDy) > minSwipeDistancePx && abs(totalDy) > abs(totalDx)) {
                break
            }
        }

        if (triggered) {
            onBack()
        }
    }
}
