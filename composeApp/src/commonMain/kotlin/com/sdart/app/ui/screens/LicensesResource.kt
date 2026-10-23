package com.sdart.app.ui.screens

/**
 * Pfad der Lizenzliste dieser Plattform (von den Gradle-Tasks `exportLibraryDefinitions<Plattform>` erzeugt).
 * Jede Plattform liefert andere Abhängigkeiten aus, daher je Plattform eine eigene Datei.
 */
internal expect fun licensesResourcePath(): String
