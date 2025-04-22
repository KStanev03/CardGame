package com.example.cardgame.services

import android.content.Context
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.achievement.AchievementManager
import com.example.cardgame.datamanager.history.GameHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unified service that handles all game completion tasks:
 * - Recording game history
 * - Updating user stats (points, money, high score)
 * - Processing achievements
 */
class GameCompletionService(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val gameHistoryDAO = db.gameHistoryDAO()
    private val userInfoDAO = db.userInfoDAO()
    private val achievementManager = AchievementManager(context)

    /**
     * Handles all aspects of game completion in a single unified flow
     *
     * @param userId The ID of the user
     * @param isPlayerWinner Whether the user won the game
     * @param playerScore Points earned by user's team
     * @param aiScore Points earned by AI team
     * @param coroutineScope Coroutine scope for launching async operations
     */
    fun handleGameCompletion(
        userId: Int,
        isPlayerWinner: Boolean,
        playerScore: Int,
        aiScore: Int,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            // Determine outcome
            val outcome = if (isPlayerWinner) "WIN" else "LOSS"

            // Create score string
            val scoreString = "$playerScore-$aiScore"

            withContext(Dispatchers.IO) {
                // 1. Record game history - only once
                val gameHistory = GameHistory(
                    userId = userId,
                    outcome = outcome,
                    opponent = "AI",
                    score = scoreString
                )

                gameHistoryDAO.insert(gameHistory)

                // 2. Update user stats
                val userInfo = userInfoDAO.getUserInfoByUserId(userId)
                userInfo?.let {
                    // Calculate money reward
                    val moneyEarned = calculateMoneyReward(isPlayerWinner, playerScore)

                    // Update points
                    val newPoints = it.points + playerScore
                    userInfoDAO.updateUserPoints(userId, newPoints)

                    // Update money
                    val newMoney = it.money + moneyEarned
                    userInfoDAO.updateUserMoney(userId, newMoney)

                    // Update high score if needed
                    if (playerScore > it.highScore) {
                        userInfoDAO.updateUserHighScore(userId, playerScore)
                    }

                    // 3. Process achievements
                    achievementManager.updateAchievements(userId, outcome, playerScore)
                }
            }
        }
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

    /**
     * Initialize achievements for a new user
     */
    fun initializeAchievements(userId: Int, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            achievementManager.initializeAchievementsForUser(userId)
        }
    }
}