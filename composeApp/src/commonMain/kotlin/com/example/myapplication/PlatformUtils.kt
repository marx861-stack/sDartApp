package com.example.myapplication

import kotlin.math.PI
import kotlin.math.round

/**
 * Liefert den aktuellen Timestamp in Millisekunden (plattformunabhängig).
 */
expect fun currentTimeMillis(): Long

/**
 * Liefert das Basisverzeichnis für persistente App-Daten (plattformunabhängig).
 */
expect fun getStorageDirectory(): String

/**
 * Persistiert einen Schlüssel-Wert-String in den App-Einstellungen.
 */
expect fun saveAppPreference(key: String, value: String)

/**
 * Liest einen Schlüssel-Wert-String aus den App-Einstellungen.
 */
expect fun loadAppPreference(key: String): String?

/**
 * Plattformunabhängige Formatierung von Gleitkommazahlen.
 */
fun Float.format(decimals: Int): String {
    return (this.toDouble()).format(decimals)
}

fun Double.format(decimals: Int): String {
    var factor = 1.0
    repeat(decimals) { factor *= 10.0 }
    val rounded = round(this * factor) / factor
    val str = rounded.toString()
    val parts = str.split(".")
    val integerPart = parts[0]
    val fractionalPart = if (parts.size > 1) {
        parts[1].padEnd(decimals, '0').take(decimals)
    } else {
        "0".repeat(decimals)
    }
    return if (decimals > 0) "$integerPart.$fractionalPart" else integerPart
}

fun toRadians(deg: Double): Double = deg * (PI / 180.0)
fun toDegrees(rad: Double): Double = rad * (180.0 / PI)
