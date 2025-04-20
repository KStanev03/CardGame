package com.example.cardgame.datamanager.achievement

import android.content.Context
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.history.GameHistoryDAO
import com.example.cardgame.datamanager.user.UserInfoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages achievements for users in the card game
 */
class AchievementManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val achievementDAO = db.achievementDAO()
    private val gameHistoryDAO = db.gameHistoryDAO()
    private val userInfoDAO = db.userInfoDAO()

    /**
     * Initialize achievements for a new user
     */
    suspend fun initializeAchievementsForUser(userId: Int) {
        // Win count achievements
        val winAchievements = listOf(
            Achievement(userId = userId, goalName = "Rookie Winner", targetValue = 5, currentValue = 0),
            Achievement(userId = userId, goalName = "Experienced Player", targetValue = 10, currentValue = 0),
            Achievement(userId = userId, goalName = "Pastra Master", targetValue = 25, currentValue = 0),
            Achievement(userId = userId, goalName = "Card Game Champion", targetValue = 50, currentValue = 0)
        )

        // Points achievements
        val pointsAchievements = listOf(
            Achievement(userId = userId, goalName = "Point Collector", targetValue = 100, currentValue = 0),
            Achievement(userId = userId, goalName = "Point Accumulator", targetValue = 250, currentValue = 0),
            Achievement(userId = userId, goalName = "Point Master", targetValue = 500, currentValue = 0),
            Achievement(userId = userId, goalName = "Point Champion", targetValue = 1000, currentValue = 0)
        )

        // Game count achievements
        val gameAchievements = listOf(
            Achievement(userId = userId, goalName = "Card Game Enthusiast", targetValue = 5, currentValue = 0),
            Achievement(userId = userId, goalName = "Card Game Addict", targetValue = 25, currentValue = 0),
            Achievement(userId = userId, goalName = "Card Game Veteran", targetValue = 100, currentValue = 0)
        )

        // Insert all achievements
        withContext(Dispatchers.IO) {
            for (achievement in winAchievements + pointsAchievements + gameAchievements) {
                achievementDAO.insertAchievement(achievement)
            }
        }
    }

    /**
     * Update achievements after a game completes
     */
    suspend fun updateAchievements(userId: Int, outcome: String, score: Int) {
        withContext(Dispatchers.IO) {
            // Update win achievements if the user won
            if (outcome == "WIN") {
                updateWinAchievements(userId)
            }

            // Update points achievements
            updatePointsAchievements(userId, score)

            // Update game count achievements (regardless of outcome)
            updateGameCountAchievements(userId)
        }
    }

    /**
     * Update win-related achievements
     */
    private suspend fun updateWinAchievements(userId: Int) {
        // Get total wins
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")

        // Get all win achievements
        val winAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Winner") || it.goalName.contains("Master") ||
                    it.goalName.contains("Champion") || it.goalName.contains("Player")
        }

        // Update each achievement's progress
        for (achievement in winAchievements) {
            if (!achievement.isCompleted && achievement.targetValue <= totalWins) {
                achievementDAO.updateAchievement(
                    achievement.copy(
                        currentValue = totalWins,
                        isCompleted = true
                    )
                )
            } else if (!achievement.isCompleted) {
                achievementDAO.updateAchievement(
                    achievement.copy(currentValue = totalWins)
                )
            }
        }
    }

    /**
     * Update point-related achievements
     */
    private suspend fun updatePointsAchievements(userId: Int, gamePoints: Int) {
        // Get user info to get current total points
        val userInfo = userInfoDAO.getUserInfoByUserId(userId)
        val totalPoints = userInfo?.points ?: 0

        // Get all points achievements
        val pointsAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Point")
        }

        // Update each achievement's progress
        for (achievement in pointsAchievements) {
            if (!achievement.isCompleted && achievement.targetValue <= totalPoints) {
                achievementDAO.updateAchievement(
                    achievement.copy(
                        currentValue = totalPoints,
                        isCompleted = true
                    )
                )
            } else if (!achievement.isCompleted) {
                achievementDAO.updateAchievement(
                    achievement.copy(currentValue = totalPoints)
                )
            }
        }
    }

    /**
     * Update game count achievements
     */
    private suspend fun updateGameCountAchievements(userId: Int) {
        // Get total games played (WIN + LOSS)
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")
        val totalLosses = gameHistoryDAO.getGameCountForUserByOutcome(userId, "LOSS")
        val totalGames = totalWins + totalLosses

        // Get all game count achievements
        val gameCountAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Enthusiast") || it.goalName.contains("Addict") ||
                    it.goalName.contains("Veteran")
        }

        // Update each achievement's progress
        for (achievement in gameCountAchievements) {
            if (!achievement.isCompleted && achievement.targetValue <= totalGames) {
                achievementDAO.updateAchievement(
                    achievement.copy(
                        currentValue = totalGames,
                        isCompleted = true
                    )
                )
            } else if (!achievement.isCompleted) {
                achievementDAO.updateAchievement(
                    achievement.copy(currentValue = totalGames)
                )
            }
        }
    }

    /**
     * Get all achievements for a user
     */
    suspend fun getUserAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            achievementDAO.getAchievementsForUser(userId)
        }
    }

    /**
     * Get only completed achievements for a user
     */
    suspend fun getCompletedAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            achievementDAO.getAchievementsForUser(userId).filter { it.isCompleted }
        }
    }

    /**
     * Get in-progress achievements for a user
     */
    suspend fun getInProgressAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            achievementDAO.getAchievementsForUser(userId).filter { !it.isCompleted }
        }
    }
}