@file:JvmName("PlatformUtilsAndroidKt")
package com.sdart.app

import android.content.Context

private var appContext: Context? = null

fun initAndroidPlatform(context: Context) {
    appContext = context.applicationContext
}

/** Liefert den Application-Context; schlägt fehl, wenn [initAndroidPlatform] noch nicht aufgerufen wurde. */
fun requireAppContext(): Context =
    appContext ?: error("initAndroidPlatform() wurde noch nicht aufgerufen")

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun getStorageDirectory(): String {
    val ctx = appContext
    return ctx?.filesDir?.absolutePath ?: System.getProperty("java.io.tmpdir") ?: "."
}

private const val PREFS_NAME = "sdart_app_prefs"

actual fun saveAppPreference(key: String, value: String) {
    try {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    } catch (e: Exception) {
        println("Error saving android preference $key: ${e.message}")
    }
}

actual fun loadAppPreference(key: String): String? {
    return try {
        val ctx = appContext ?: return null
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(key, null)
    } catch (e: Exception) {
        println("Error loading android preference $key: ${e.message}")
        null
    }
}
