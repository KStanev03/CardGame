package com.example.cardgame.datamanager.achievement

import android.content.Context
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.history.GameHistoryDAO
import com.example.cardgame.datamanager.user.UserInfoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Управлява постиженията за потребителите в играта на карти.
 */
class AchievementManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val achievementDAO = db.achievementDAO()
    private val gameHistoryDAO = db.gameHistoryDAO()
    private val userInfoDAO = db.userInfoDAO()

    /**
     * Инициализира постиженията за нов потребител.
     */
    suspend fun initializeAchievementsForUser(userId: Int) {
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
        }
    }

    /**
     * Актуализира постиженията след завършване на игра.
     */
    suspend fun updateAchievements(userId: Int, outcome: String, score: Int) {
        withContext(Dispatchers.IO) {
            // Актуализиране на постиженията за победи, ако потребителят е спечелил
            if (outcome == "WIN") {
                updateWinAchievements(userId)
            }

            // Актуализиране на постиженията за точки
            updatePointsAchievements(userId, score)

            // Актуализиране на постиженията за брой изиграни игри (независимо от резултата)
            updateGameCountAchievements(userId)
        }
    }

    /**
     * Актуализира постиженията, свързани с победи.
     */
    private suspend fun updateWinAchievements(userId: Int) {
        // Вземане на общия брой победи
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")

        // Вземане на всички постижения за победи
        val winAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Победител") || it.goalName.contains("Майстор") ||
                    it.goalName.contains("Шампион") || it.goalName.contains("Играч")
        }

        // Актуализиране на напредъка на всяко постижение
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
     * Актуализира постиженията, свързани с точки.
     */
    private suspend fun updatePointsAchievements(userId: Int, gamePoints: Int) {
        // Вземане на информация за потребителя, за да се вземат текущите общи точки
        val userInfo = userInfoDAO.getUserInfoByUserId(userId)
        val totalPoints = userInfo?.points ?: 0

        // Вземане на всички постижения за точки
        val pointsAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Точки")
        }

        // Актуализиране на напредъка на всяко постижение
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
     * Актуализира постиженията за брой изиграни игри.
     */
    private suspend fun updateGameCountAchievements(userId: Int) {
        // Вземане на общия брой изиграни игри (ПОБЕДИ + ЗАГУБИ)
        val totalWins = gameHistoryDAO.getGameCountForUserByOutcome(userId, "WIN")
        val totalLosses = gameHistoryDAO.getGameCountForUserByOutcome(userId, "LOSS")
        val totalGames = totalWins + totalLosses

        // Вземане на всички постижения за брой изиграни игри
        val gameCountAchievements = achievementDAO.getAchievementsForUser(userId).filter {
            it.goalName.contains("Ентусиаст") || it.goalName.contains("Зависим") ||
                    it.goalName.contains("Ветеран")
        }

        // Актуализиране на напредъка на всяко постижение
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
     * Вземане на всички постижения за потребител.
     */
    suspend fun getUserAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            achievementDAO.getAchievementsForUser(userId)
        }
    }

    /**
     * Вземане само на завършени постижения за потребител.
     */
    suspend fun getCompletedAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            achievementDAO.getAchievementsForUser(userId).filter { it.isCompleted }
        }
    }

    /**
     * Вземане на незавършени постижения за потребител.
     */
    suspend fun getInProgressAchievements(userId: Int): List<Achievement> {
        return withContext(Dispatchers.IO) {
            achievementDAO.getAchievementsForUser(userId).filter { !it.isCompleted }
        }
    }
}