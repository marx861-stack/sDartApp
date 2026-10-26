package com.sdart.app

import androidx.compose.runtime.Composable

/**
 * Steuert die physische Bildschirmhelligkeit und den Standby-Modus der Plattform.
 *
 * @param isDimmed Wenn true, wird die tatsächliche Bildschirmhelligkeit auf das Minimum (z. B. 0.01f)
 *                 gedrosselt, um Akku zu sparen und Display-Erwärmung bei laufendem Kamera-Stream zu minimieren.
 *                 Bei false bzw. beim Verlassen des Screens wird die reguläre Systemhelligkeit wiederhergestellt.
 * @param keepScreenOn Wenn true, wird das automatische Ausschalten des Bildschirms durch das Betriebssystem verhindert,
 *                     damit der Video-Stream unterbrechungsfrei weiterläuft.
 */
@Composable
expect fun PlatformScreenBrightness(
    isDimmed: Boolean,
    keepScreenOn: Boolean = true
)
