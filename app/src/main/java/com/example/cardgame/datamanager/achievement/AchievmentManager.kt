package com.example.cardgame.datamanager.achievement

import android.content.Context
import android.util.Log
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.history.GameHistoryDAO
import com.example.cardgame.datamanager.user.UserInfoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Управлява постиженията за потребителите в играта на карти.
 */
class AchievementManager(private val context: Context) {
    private val TAG = "AchievementManager"
    private val db = AppDatabase.getInstance(context)
    private val achievementDAO = db.achievementDAO()
    private val gameHistoryDAO = db.gameHistoryDAO()
    private val userInfoDAO = db.userInfoDAO()

    /**
     * Инициализира постиженията за нов потребител.
     */
    suspend fun initializeAchievementsForUser(userId: Int) {
        // Check if user already has achievements
        val existingAchievements = getUserAchievements(userId)
        if (existingAchievements.isNotEmpty()) {
            Log.d(TAG, "User $userId already has ${existingAchievements.size} achievements. Skipping initialization.")
            return
        }

        // Постижения за брой победи
        val winAchievements = listOf(
            Achievement(userId = userId, goalName = "Новак", targetValue = 5, currentValue = 0),
            Achievement(userId = userId, goalName = "Опитен Играч", targetValue = 10, currentValue = 0),
            Achievement(userId = userId, goalName = "Пастра Майстор", targetValue = 25, currentValue = 0),
            Achievement(userId = userId, goalName = "Пастра Шампион", targetValue = 50, currentValue = 0)
        )

        // Постижения за точки
        val pointsAchievements = listOf(
            Achievement(userId = userId, goalName = "Събирач на Точки", targetValue = 100, currentValue = 0),
            Achievement(userId = userId, goalName = "Трупач на Точки", targetValue = 250, currentValue = 0),
            Achievement(userId = userId, goalName = "Майстор на Точките", targetValue = 500, currentValue = 0),
            Achievement(userId = userId, goalName = "Шампион по Точки", targetValue = 1000, currentValue = 0)
        )

        // Постижения за брой изиграни игри
        val gameAchievements = listOf(
            Achievement(userId = userId, goalName = "Ентусиаст", targetValue = 5, currentValue = 0),
            Achievement(userId = userId, goalName = "Зависим", targetValue = 25, currentValue = 0),
            Achievement(userId = userId, goalName = "Ветеран", targetValue = 100, currentValue = 0)
        )

        // Вмъкване на всички постижения
        withContext(Dispatchers.IO) {
            for (achievement in winAchievements + pointsAchievements + gameAchievements) {
                achievementDAO.insertAchievement(achievement)
            }
            Log.d(TAG, "Initialized ${winAchievements.size + pointsAchievements.size + gameAchievements.size} achievements for user $userId")
        }
    }

    /**
     * Актуализира постиженията след завършване на игра.
     */
    suspend fun updateAchievements(userId: Int, outcome: String, score: Int) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Updating achievements for user $userId, outcome: $outcome, score: $score")

            // Актуализиране на постиженията за победи, ако потребителят е спечелил
            if (outcome == "WIN") {
                updateWinAchievements(userId)
            }

            // Актуализиране на постиженията за точки
            updatePointsAchievements(userId, score)

            // Актуализиране на постиженията за брой изиграни игри (независимо от резултата)
            updateGameCountAchievements(userId)

            // Log status after update
            val allAchievements = getUserAchievements(userId)
            val completedCount = allAchievements.count { it.isCompleted }
            Log.d(TAG, "After update: $completedCount/${allAchievements.size} achievements completed")
        }
    }

    /**
     * Актуализира постиженията, свързани с победи.
     */
    private suspend fun updateWinAchievements(userId: Int) {
        // Get total wins
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")
        Log.d(TAG, "User $userId has $totalWins total wins")

        // Get all win achievements
        val winAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Новак") || it.goalName.contains("Опитен Играч") ||
                    it.goalName.contains("Пастра Майстор") || it.goalName.contains("Пастра Шампион")
        }

        // Update each achievement
        for (achievement in winAchievements) {
            // Important: NEVER modify completed achievements
            if (achievement.isCompleted) {
                Log.d(TAG, "Skipping completed achievement: ${achievement.goalName}")
                continue
            }

            if (achievement.targetValue <= totalWins) {
                // Mark as completed
                val updatedAchievement = achievement.copy(
                    currentValue = totalWins,
                    isCompleted = true
                )
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement completed: ${achievement.goalName}")
            } else {
                // Just update progress
                val updatedAchievement = achievement.copy(currentValue = totalWins)
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement updated: ${achievement.goalName}, progress: ${achievement.currentValue}/${achievement.targetValue}")
            }
        }
    }

    /**
     * Актуализира постиженията, свързани с точки.
     */
    private suspend fun updatePointsAchievements(userId: Int, gamePoints: Int) {
        // Get user info for total points
        val userInfo = userInfoDAO.getUserInfoByUserId(userId)
        val totalPoints = userInfo?.points ?: 0
        Log.d(TAG, "User $userId has $totalPoints total points")

        // Get all points achievements
        val pointsAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Точки")
        }

        // Update each achievement
        for (achievement in pointsAchievements) {
            // Important: NEVER modify completed achievements
            if (achievement.isCompleted) {
                Log.d(TAG, "Skipping completed achievement: ${achievement.goalName}")
                continue
            }

            if (achievement.targetValue <= totalPoints) {
                // Mark as completed
                val updatedAchievement = achievement.copy(
                    currentValue = totalPoints,
                    isCompleted = true
                )
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement completed: ${achievement.goalName}")
            } else {
                // Just update progress
                val updatedAchievement = achievement.copy(currentValue = totalPoints)
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement updated: ${achievement.goalName}, progress: ${achievement.currentValue}/${achievement.targetValue}")
            }
        }
    }

    /**
     * Актуализира постиженията за брой изиграни игри.
     */
    private suspend fun updateGameCountAchievements(userId: Int) {
        // Get total games (wins + losses)
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")
        val totalLosses = gameHistoryDAO.getGameCountForUserByOutcome(userId, "LOSS")
        val totalGames = totalWins + totalLosses
        Log.d(TAG, "User $userId has played $totalGames total games")

        // Get all game count achievements
        val gameCountAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Ентусиаст") || it.goalName.contains("Зависим") ||
                    it.goalName.contains("Ветеран")
        }

        // Update each achievement
        for (achievement in gameCountAchievements) {
            // Important: NEVER modify completed achievements
            if (achievement.isCompleted) {
                Log.d(TAG, "Skipping completed achievement: ${achievement.goalName}")
                continue
            }

            if (achievement.targetValue <= totalGames) {
                // Mark as completed
                val updatedAchievement = achievement.copy(
                    currentValue = totalGames,
                    isCompleted = true
                )
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement completed: ${achievement.goalName}")
            } else {
                // Just update progress
                val updatedAchievement = achievement.copy(currentValue = totalGames)
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement updated: ${achievement.goalName}, progress: ${achievement.currentValue}/${achievement.targetValue}")
            }
        }
    }

    /**
     * Вземане на всички постижения за потребител.
     */
    suspend fun getUserAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            val achievements = achievementDAO.getAchievementsForUser(userId)
            Log.d(TAG, "Retrieved ${achievements.size} achievements for user $userId")
            achievements
        }
    }

    /**
     * Вземане само на завършени постижения за потребител.
     */
    suspend fun getCompletedAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            val completed = achievementDAO.getAchievementsForUser(userId).filter { it.isCompleted }
            Log.d(TAG, "Retrieved ${completed.size} completed achievements for user $userId")
            completed.forEach {
                Log.d(TAG, "Completed achievement: ${it.goalName}")
            }
            completed
        }
    }

    /**
     * Вземане на незавършени постижения за потребител.
     */
    suspend fun getInProgressAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            val inProgress = achievementDAO.getAchievementsForUser(userId).filter { !it.isCompleted }
            Log.d(TAG, "Retrieved ${inProgress.size} in-progress achievements for user $userId")
            inProgress
        }
    }
}