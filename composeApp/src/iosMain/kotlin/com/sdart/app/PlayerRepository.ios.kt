package com.sdart.app

/**
 * iOS Fallback-Implementierung von PlayerRepository (In-Memory bis iOS File-IO angebunden wird).
 */
actual fun createPersistentPlayerRepository(): PlayerRepository = InMemoryPlayerRepository()
