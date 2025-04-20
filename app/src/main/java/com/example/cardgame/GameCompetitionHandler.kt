package com.example.cardgame

import android.content.Context
import com.example.cardgame.datamanager.LoggedUser
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.achievement.AchievementManager
import com.example.cardgame.datamanager.history.GameHistory
import com.example.cardgame.datamanager.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles game completion tasks like updating history, points, achievements
 */
class GameCompletionHandler(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val gameHistoryDAO = db.gameHistoryDAO()
    private val userInfoDAO = db.userInfoDAO()
    private val achievementManager = AchievementManager(context)

    /**
     * Process game completion
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

            // Record game history
            val gameHistory = GameHistory(
                userId = userId,
                outcome = outcome,
                opponent = "AI",
                score = scoreString
            )

            withContext(Dispatchers.IO) {
                // Insert game history
                gameHistoryDAO.insert(gameHistory)

                // Update user points
                val userInfo = userInfoDAO.getUserInfoByUserId(userId)
                if (userInfo != null) {
                    // Add player score to total points
                    val newPoints = userInfo.points + playerScore

                    // Update high score if needed
                    val newHighScore = if(playerScore > userInfo.highScore) playerScore else userInfo.highScore

                    // Update user info
                    userInfoDAO.updateUserPoints(userId, newPoints)
                    userInfoDAO.updateUserHighScore(userId, newHighScore)

                    // Update achievements
                    achievementManager.updateAchievements(userId, outcome, playerScore)
                }
            }
        }
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