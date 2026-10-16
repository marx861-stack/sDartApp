@file:JvmName("PlayerRepositoryAndroidKt")
package com.example.myapplication

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android Implementierung von PlayerRepository basierend auf saved_players_v2.json.
 */
class AndroidJsonPlayerRepository(
    private val storageDir: String = getStorageDirectory()
) : BasePlayerRepository() {
    private val gson: Gson = Gson()
    private val fileName = "saved_players_v2.json"

    private fun getFile(): File {
        val dir = File(storageDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }

    override suspend fun loadAllPlayers(): List<SavedPlayer> = withContext(Dispatchers.IO) {
        try {
            val file = getFile()
            if (!file.exists()) {
                return@withContext emptyList()
            }
            val json = file.readText()
            if (json.isBlank()) {
                return@withContext emptyList()
            }
            val type = object : TypeToken<List<SavedPlayer>>() {}.type
            gson.fromJson<List<SavedPlayer>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            println("Error loading players (android): ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun savePlayer(player: SavedPlayer): Boolean = withContext(Dispatchers.IO) {
        try {
            val players = loadAllPlayers().toMutableList()
            val existingIndex = players.indexOfFirst { it.name == player.name }
            if (existingIndex >= 0) {
                players[existingIndex] = player
            } else {
                players.add(player)
            }
            val json = gson.toJson(players)
            getFile().writeText(json)
            true
        } catch (e: Exception) {
            println("Error saving player (android): ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun deletePlayer(name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val players = loadAllPlayers().toMutableList()
            val removed = players.removeAll { it.name == name }
            if (removed) {
                val json = gson.toJson(players)
                getFile().writeText(json)
            }
            removed
        } catch (e: Exception) {
            println("Error deleting player (android): ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun findPlayer(name: String): SavedPlayer? = withContext(Dispatchers.IO) {
        try {
            val players = loadAllPlayers()
            players.find { it.name == name }
        } catch (e: Exception) {
            println("Error finding player (android): ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

actual fun createPersistentPlayerRepository(): PlayerRepository = AndroidJsonPlayerRepository()
