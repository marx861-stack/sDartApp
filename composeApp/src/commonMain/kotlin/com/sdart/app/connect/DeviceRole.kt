package com.sdart.app.connect

import kotlinx.serialization.Serializable

/**
 * Rolle, in der ein Gerät an einer Session teilnimmt.
 * Die Rolle wird am jeweiligen Gerät gewählt (siehe docs/companion_connection_concept.md).
 */
@Serializable
enum class DeviceRole {
    /** Hält Spielstand und Spielerdaten, nimmt Teilnehmer an. */
    HOST,

    /** Erkennt Darts lokal und sendet Treffer + Vorschau an den Host. */
    CAMERA,

    /** Zeigt das Live-Scoreboard. */
    DISPLAY,

    /** Manuelle Punkte-Eingabe und Undo. */
    INPUT;

    /** Rollen, die sich als Teilnehmer mit einem Host verbinden. */
    val isParticipant: Boolean get() = this != HOST
}

/**
 * Fähigkeiten eines Geräts. Der Host bietet nur Rollen an, die das Gerät erfüllen kann.
 */
@Serializable
enum class DeviceCapability {
    CAMERA,
    ML_DETECTION,
    DISPLAY,
    TOUCH_INPUT
}

/** Rollen, die ein Gerät mit den gegebenen Fähigkeiten übernehmen kann (ohne HOST). */
fun availableRoles(capabilities: Set<DeviceCapability>): Set<DeviceRole> {
    val roles = mutableSetOf<DeviceRole>()
    if (DeviceCapability.CAMERA in capabilities) roles += DeviceRole.CAMERA
    if (DeviceCapability.DISPLAY in capabilities) roles += DeviceRole.DISPLAY
    if (DeviceCapability.TOUCH_INPUT in capabilities) roles += DeviceRole.INPUT
    return roles
}
