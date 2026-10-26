package com.sdart.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Android-Implementierung: Passt die Fenster-Helligkeit (WindowManager.LayoutParams.screenBrightness)
 * und das FLAG_KEEP_SCREEN_ON an.
 *
 * Im abgedunkelten Zustand ([isDimmed] == true) wird [WindowManager.LayoutParams.screenBrightness]
 * auf 0.01f (1% minimale Hardware-Beleuchtung) gesetzt. Bei Reaktivierung oder beim Verlassen
 * des Screens wird der Wert auf [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE] (-1.0f)
 * zurückgestellt, wodurch das System wieder die Standard-Nutzerhelligkeit übernimmt.
 */
@Composable
actual fun PlatformScreenBrightness(
    isDimmed: Boolean,
    keepScreenOn: Boolean
) {
    val context = LocalContext.current
    val activity = remember(context) { findActivity(context) }

    DisposableEffect(activity, keepScreenOn) {
        val window = activity?.window
        if (window != null && keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (window != null && keepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    DisposableEffect(activity, isDimmed) {
        val window = activity?.window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = if (isDimmed) {
                0.01f
            } else {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = layoutParams
        }
        onDispose {
            val windowDispose = activity?.window
            if (windowDispose != null) {
                val layoutParams = windowDispose.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                windowDispose.attributes = layoutParams
            }
        }
    }
}
