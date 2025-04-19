package com.example.cardgame.datamanager.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameHistoryDAO {
    @Insert
    suspend fun insert(gameHistory: GameHistory)

    @Query("SELECT * FROM game_history WHERE user_id = :userId ORDER BY timestamp DESC")
    suspend fun getGameHistoryForUser(userId: Int): List<GameHistory>

    @Query("SELECT * FROM game_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestGameHistory(limit: Int): List<GameHistory>

    @Query("SELECT COUNT(*) FROM game_history WHERE user_id = :userId AND outcome = :outcome")
    suspend fun getGameCountForUserByOutcome(userId: Int, outcome: String): Int

}