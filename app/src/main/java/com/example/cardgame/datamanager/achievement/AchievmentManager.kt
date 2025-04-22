package com.example.cardgame.datamanager.achievement

import android.content.Context
import android.util.Log
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.history.GameHistoryDAO
import com.example.cardgame.datamanager.user.UserInfoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class AchievementManager(private val context: Context) {
    private val TAG = "AchievementManager"
    private val db = AppDatabase.getInstance(context)
    private val achievementDAO = db.achievementDAO()
    private val gameHistoryDAO = db.gameHistoryDAO()
    private val userInfoDAO = db.userInfoDAO()


    suspend fun initializeAchievementsForUser(userId: Int) {

        val existingAchievements = getUserAchievements(userId)
        if (existingAchievements.isNotEmpty()) {
            Log.d(TAG, "User $userId already has ${existingAchievements.size} achievements. Skipping initialization.")
            return
        }

        val winAchievements = listOf(
            Achievement(userId = userId, goalName = "Новак", targetValue = 5, currentValue = 0),
            Achievement(userId = userId, goalName = "Опитен Играч", targetValue = 10, currentValue = 0),
            Achievement(userId = userId, goalName = "Пастра Майстор", targetValue = 25, currentValue = 0),
            Achievement(userId = userId, goalName = "Пастра Шампион", targetValue = 50, currentValue = 0)
        )

        val pointsAchievements = listOf(
            Achievement(userId = userId, goalName = "Събирач на Точки", targetValue = 100, currentValue = 0),
            Achievement(userId = userId, goalName = "Трупач на Точки", targetValue = 250, currentValue = 0),
            Achievement(userId = userId, goalName = "Майстор на Точките", targetValue = 500, currentValue = 0),
            Achievement(userId = userId, goalName = "Шампион по Точки", targetValue = 1000, currentValue = 0)
        )

        val gameAchievements = listOf(
            Achievement(userId = userId, goalName = "Ентусиаст", targetValue = 5, currentValue = 0),
            Achievement(userId = userId, goalName = "Зависим", targetValue = 25, currentValue = 0),
            Achievement(userId = userId, goalName = "Ветеран", targetValue = 100, currentValue = 0)
        )

        withContext(Dispatchers.IO) {
            for (achievement in winAchievements + pointsAchievements + gameAchievements) {
                achievementDAO.insertAchievement(achievement)
            }
            Log.d(TAG, "Initialized ${winAchievements.size + pointsAchievements.size + gameAchievements.size} achievements for user $userId")
        }
    }


    suspend fun updateAchievements(userId: Int, outcome: String, score: Int) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Updating achievements for user $userId, outcome: $outcome, score: $score")

            if (outcome == "WIN") {
                updateWinAchievements(userId)
            }

            updatePointsAchievements(userId, score)

            updateGameCountAchievements(userId)

            val allAchievements = getUserAchievements(userId)
            val completedCount = allAchievements.count { it.isCompleted }
            Log.d(TAG, "After update: $completedCount/${allAchievements.size} achievements completed")
        }
    }


    private suspend fun updateWinAchievements(userId: Int) {
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")
        Log.d(TAG, "User $userId has $totalWins total wins")

        val winAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Новак") || it.goalName.contains("Опитен Играч") ||
                    it.goalName.contains("Пастра Майстор") || it.goalName.contains("Пастра Шампион")
        }

        for (achievement in winAchievements) {
            if (achievement.isCompleted) {
                Log.d(TAG, "Skipping completed achievement: ${achievement.goalName}")
                continue
            }

            if (achievement.targetValue <= totalWins) {
                val updatedAchievement = achievement.copy(
                    currentValue = totalWins,
                    isCompleted = true
                )
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement completed: ${achievement.goalName}")
            } else {
                val updatedAchievement = achievement.copy(currentValue = totalWins)
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement updated: ${achievement.goalName}, progress: ${achievement.currentValue}/${achievement.targetValue}")
            }
        }
    }


    private suspend fun updatePointsAchievements(userId: Int, gamePoints: Int) {
        val userInfo = userInfoDAO.getUserInfoByUserId(userId)
        val totalPoints = userInfo?.points ?: 0
        Log.d(TAG, "User $userId has $totalPoints total points")

        val pointsAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Точки")
        }

        for (achievement in pointsAchievements) {
            if (achievement.isCompleted) {
                Log.d(TAG, "Skipping completed achievement: ${achievement.goalName}")
                continue
            }

            if (achievement.targetValue <= totalPoints) {
                val updatedAchievement = achievement.copy(
                    currentValue = totalPoints,
                    isCompleted = true
                )
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement completed: ${achievement.goalName}")
            } else {
                val updatedAchievement = achievement.copy(currentValue = totalPoints)
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement updated: ${achievement.goalName}, progress: ${achievement.currentValue}/${achievement.targetValue}")
            }
        }
    }


    private suspend fun updateGameCountAchievements(userId: Int) {
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")
        val totalLosses = gameHistoryDAO.getGameCountForUserByOutcome(userId, "LOSS")
        val totalGames = totalWins + totalLosses
        Log.d(TAG, "User $userId has played $totalGames total games")


        val gameCountAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Ентусиаст") || it.goalName.contains("Зависим") ||
                    it.goalName.contains("Ветеран")
        }

        for (achievement in gameCountAchievements) {

            if (achievement.isCompleted) {
                Log.d(TAG, "Skipping completed achievement: ${achievement.goalName}")
                continue
            }

            if (achievement.targetValue <= totalGames) {

                val updatedAchievement = achievement.copy(
                    currentValue = totalGames,
                    isCompleted = true
                )
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement completed: ${achievement.goalName}")
            } else {

                val updatedAchievement = achievement.copy(currentValue = totalGames)
                achievementDAO.updateAchievement(updatedAchievement)
                Log.d(TAG, "Achievement updated: ${achievement.goalName}, progress: ${achievement.currentValue}/${achievement.targetValue}")
            }
        }
    }


    suspend fun getUserAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            val achievements = achievementDAO.getAchievementsForUser(userId)
            Log.d(TAG, "Retrieved ${achievements.size} achievements for user $userId")
            achievements
        }
    }


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


    suspend fun getInProgressAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            val inProgress = achievementDAO.getAchievementsForUser(userId).filter { !it.isCompleted }
            Log.d(TAG, "Retrieved ${inProgress.size} in-progress achievements for user $userId")
            inProgress
        }
    }
}