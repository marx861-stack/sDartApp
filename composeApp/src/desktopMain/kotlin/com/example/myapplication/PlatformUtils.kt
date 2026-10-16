@file:JvmName("PlatformUtilsDesktopKt")
package com.example.myapplication

import java.io.File

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getStorageDirectory(): String {
    val appData = System.getenv("APPDATA")
    val baseDir = if (!appData.isNullOrBlank()) {
        File(appData, "sDartApp")
    } else {
        File(System.getProperty("user.home"), ".sDartApp")
    }
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }
    return baseDir.absolutePath
}

private fun getPreferencesFile(): File {
    val dir = File(getStorageDirectory())
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return File(dir, "preferences.properties")
}

actual fun saveAppPreference(key: String, value: String) {
    try {
        val file = getPreferencesFile()
        val props = java.util.Properties()
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
        props.setProperty(key, value)
        file.outputStream().use { props.store(it, "sDartApp Preferences") }
    } catch (e: Exception) {
        println("Error saving desktop preference $key: ${e.message}")
    }
}

actual fun loadAppPreference(key: String): String? {
    return try {
        val file = getPreferencesFile()
        if (!file.exists()) return null
        val props = java.util.Properties()
        file.inputStream().use { props.load(it) }
        props.getProperty(key)
    } catch (e: Exception) {
        println("Error loading desktop preference $key: ${e.message}")
        null
    }
}
