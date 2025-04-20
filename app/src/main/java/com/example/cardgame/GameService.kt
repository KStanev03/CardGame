package com.example.cardgame.services

import android.content.Context
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.history.GameHistory
import com.example.cardgame.datamanager.user.UserInfoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles game-related database operations including recording history and updating user stats
 */
class GameService(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val userInfoDAO: UserInfoDAO = db.userInfoDAO()
    private val gameHistoryDAO = db.gameHistoryDAO()

    /**
     * Records game outcome and updates player stats
     *
     * @param userId The ID of the user
     * @param isWin Whether the user won the game
     * @param score The final score (team vs team)
     * @param pointsEarned Points earned by user's team
     * @param moneyEarned Money earned (calculated based on points and win status)
     */
    suspend fun saveGameResults(
        userId: Int,
        isWin: Boolean,
        score: String,
        pointsEarned: Int,
        moneyEarned: Int = calculateMoneyReward(isWin, pointsEarned)
    ) = withContext(Dispatchers.IO) {
        // 1. Update the user's points and money
        val userInfo = userInfoDAO.getUserInfoByUserId(userId)

        userInfo?.let {
            // Update points
            val newPoints = it.points + pointsEarned
            userInfoDAO.updateUserPoints(userId, newPoints)

            // Update money
            val newMoney = it.money + moneyEarned
            userInfoDAO.updateUserMoney(userId, newMoney)

            // Update high score if current score is higher
            if (pointsEarned > it.highScore) {
                userInfoDAO.updateUserHighScore(userId, pointsEarned)
            }
        }

        // 2. Record the game in history
        val gameHistory = GameHistory(
            userId = userId,
            outcome = if (isWin) "WIN" else "LOSS",
            opponent = "AI Team",
            score = score
        )

        gameHistoryDAO.insert(gameHistory)
    }

    /**
     * Calculate money reward based on win status and points earned
     */
    private fun calculateMoneyReward(isWin: Boolean, points: Int): Int {
        // Base reward
        val baseReward = if (isWin) 10 else 5

        // Bonus for points (1 coin per 2 points)
        val pointsBonus = points / 2

        return baseReward + pointsBonus
    }
}