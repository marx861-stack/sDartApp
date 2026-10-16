package com.example.myapplication

/**
 * Multiplatform-Speicher für Einstellungen und UI-Zustände.
 */
object AppPreferences {
    private const val KEY_LAST_PLAYERS = "last_player_names"

    fun getLastPlayerNames(): String? = loadAppPreference(KEY_LAST_PLAYERS)

    fun saveLastPlayerNames(names: String) {
        saveAppPreference(KEY_LAST_PLAYERS, names)
    }
}
