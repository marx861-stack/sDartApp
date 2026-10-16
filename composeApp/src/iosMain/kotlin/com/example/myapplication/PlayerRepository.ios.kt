package com.example.myapplication

/**
 * iOS Fallback-Implementierung von PlayerRepository (In-Memory bis iOS File-IO angebunden wird).
 */
actual fun createPersistentPlayerRepository(): PlayerRepository = InMemoryPlayerRepository()
