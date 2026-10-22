package com.sdart.app

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun getStorageDirectory(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    return (paths.firstOrNull() as? String) ?: "."
}

actual fun saveAppPreference(key: String, value: String) {
    platform.Foundation.NSUserDefaults.standardUserDefaults.setObject(value, key)
}

actual fun loadAppPreference(key: String): String? {
    return platform.Foundation.NSUserDefaults.standardUserDefaults.stringForKey(key)
}
