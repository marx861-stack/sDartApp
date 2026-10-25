package com.sdart.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOrientationPolicy(resources.configuration)
        initAndroidPlatform(this)
        setContent {
            App()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Foldables: beim Auf-/Zuklappen wechselt das Gerät zwischen Handy- und Tablet-Breite
        applyOrientationPolicy(newConfig)
    }

    /**
     * Handys (kleinste Breite < 600 dp) bleiben im Hochformat, Tablets dürfen frei drehen.
     * 600 dp ist die offizielle Grenze der Android Window-Size-Classes (Compact vs. Medium).
     */
    private fun applyOrientationPolicy(config: Configuration) {
        val isTablet = config.smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP
        val desired = if (isTablet) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (requestedOrientation != desired) {
            requestedOrientation = desired
        }
    }

    private companion object {
        const val TABLET_MIN_WIDTH_DP = 600
    }
}
