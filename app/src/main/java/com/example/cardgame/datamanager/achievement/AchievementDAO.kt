package com.example.cardgame.datamanager.achievement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface AchievementDAO {
    @Insert
    suspend fun insertAchievement(achievement: Achievement)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAchievementsForUser(userId: Int): List<Achievement>

    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievementById(achievementId: Int): Achievement?

    @Delete
    suspend fun deleteAchievement(achievement: Achievement)

    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun deleteAllAchievementsForUser(userId: Int)
}