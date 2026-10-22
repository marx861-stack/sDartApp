package com.sdart.app.domain.game.engine

import androidx.compose.ui.geometry.Offset
import com.sdart.app.DartThrow
import com.sdart.app.Game

/**
 * Berechnet zustandslose Rundenmetriken für Dart-Spiele (aktuelle Würfe, Runden-Summen, letzte Runden).
 */
object GameRoundHelper {

    fun calculateThrowsInRound(currentPlayerIndex: Int, throwHistory: List<DartThrow>): Int {
        if (throwHistory.isEmpty()) return 0
        var throwsInCurrentRound = 0
        for (i in throwHistory.size - 1 downTo 0) {
            val throwData = throwHistory[i]
            if (throwData.playerIndex == currentPlayerIndex) {
                throwsInCurrentRound++
            } else {
                break
            }
        }
        return throwsInCurrentRound % 3
    }

    fun getCurrentRoundDartPositions(game: Game?): List<Offset> {
        val currentGame = game ?: return emptyList()
        if (currentGame.throwHistory.isEmpty()) return emptyList()
        val currentPlayerIndex = currentGame.currentPlayerIndex
        val throwsInCurrentRound = currentGame.throwsInRound
        if (throwsInCurrentRound == 0) return emptyList()
        val currentRoundPositions = mutableListOf<Offset>()
        var throwsCollected = 0
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            if (throwsCollected >= throwsInCurrentRound) break
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == currentPlayerIndex) {
                currentRoundPositions.add(throwData.position)
                throwsCollected++
            } else {
                break
            }
        }
        return currentRoundPositions.reversed()
    }

    fun getCurrentRoundSum(game: Game?): Int {
        val currentGame = game ?: return 0
        val throwsInCurrentTurn = currentGame.throwsInRound
        if (throwsInCurrentTurn == 0) return 0
        val currentPlayerIndex = currentGame.currentPlayerIndex
        var roundSum = 0
        var throwsFound = 0
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            if (throwsFound >= throwsInCurrentTurn) break
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == currentPlayerIndex) {
                roundSum += throwData.score
                throwsFound++
            } else {
                break
            }
        }
        return roundSum
    }

    fun getLastCompletedRoundSum(game: Game?): Int {
        val currentGame = game ?: return 0
        if (currentGame.throwHistory.isEmpty()) return 0
        val lastThrow = currentGame.throwHistory.lastOrNull() ?: return 0
        val previousPlayerIndex = lastThrow.playerIndex
        var roundSum = 0
        var throwsFound = 0
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == previousPlayerIndex && throwsFound < 3) {
                roundSum += throwData.score
                throwsFound++
            } else if (throwData.playerIndex != previousPlayerIndex) {
                break
            }
        }
        return roundSum
    }

    fun isLastCompletedRoundValid(game: Game?): Boolean {
        val currentGame = game ?: return false
        if (currentGame.throwHistory.isEmpty()) return false
        val lastThrow = currentGame.throwHistory.lastOrNull() ?: return false
        val previousPlayerIndex = lastThrow.playerIndex
        val lastThreeThrows = mutableListOf<DartThrow>()
        for (i in currentGame.throwHistory.size - 1 downTo 0) {
            val throwData = currentGame.throwHistory[i]
            if (throwData.playerIndex == previousPlayerIndex && lastThreeThrows.size < 3) {
                lastThreeThrows.add(0, throwData)
            } else if (throwData.playerIndex != previousPlayerIndex) {
                break
            }
        }
        if (lastThreeThrows.size < 3) return false
        val roundSum = lastThreeThrows.sumOf { it.score }
        val hasNoMisses = lastThreeThrows.all { it.score > 0 }
        val isDivisibleBy5 = roundSum > 0 && roundSum % 5 == 0
        return isDivisibleBy5 && hasNoMisses
    }

    fun isRoundCompleted(game: Game?): Boolean {
        val currentGame = game ?: return false
        return currentGame.throwsInRound == 0 && currentGame.throwHistory.isNotEmpty()
    }
}
