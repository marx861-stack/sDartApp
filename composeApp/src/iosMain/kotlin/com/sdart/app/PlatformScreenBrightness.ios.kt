package com.sdart.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen

/**
 * iOS-Implementierung: Regelt die Bildschirmhelligkeit via UIScreen.mainScreen.brightness
 * und verhindert den Standby über UIApplication.sharedApplication.idleTimerDisabled.
 */
@Composable
actual fun PlatformScreenBrightness(
    isDimmed: Boolean,
    keepScreenOn: Boolean
) {
    DisposableEffect(keepScreenOn) {
        val app = UIApplication.sharedApplication
        val prevIdleTimer = app.idleTimerDisabled
        if (keepScreenOn) {
            app.idleTimerDisabled = true
        }
        onDispose {
            if (keepScreenOn) {
                app.idleTimerDisabled = prevIdleTimer
            }
        }
    }

    DisposableEffect(isDimmed) {
        val screen = UIScreen.mainScreen
        val originalBrightness = screen.brightness
        if (isDimmed) {
            screen.brightness = 0.01
        }
        onDispose {
            if (isDimmed) {
                screen.brightness = originalBrightness
            }
        }
    }
}
