package com.sdart.app.ui.screens

// Gradle kann die iOS-Abhängigkeiten nur auf macOS auflösen. Bis dort eine eigene iOS-Liste erzeugt wird,
// dient die Gesamtliste als Fallback (enthält zusätzlich Komponenten der anderen Plattformen).
internal actual fun licensesResourcePath(): String = "files/aboutlibraries.json"
