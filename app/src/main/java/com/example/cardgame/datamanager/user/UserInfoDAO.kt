package com.example.cardgame.datamanager.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserInfoDAO {
    @Query("SELECT * FROM UserInfo WHERE user_id = :userId")
    suspend fun getUserInfoByUserId(userId: Int): UserInfo?

    @Insert
    suspend fun insert(userInfo: UserInfo): Long

    @Update
    suspend fun update(userInfo: UserInfo)

    @Delete
    suspend fun delete(userInfo: UserInfo)

    @Query("DELETE FROM UserInfo WHERE user_id = :userId")
    suspend fun deleteUserInfoByUserId(userId: Int)

    @Query("UPDATE UserInfo SET points = :points WHERE user_id = :userId")
    suspend fun updateUserPoints(userId: Int, points: Int)

    @Query("UPDATE UserInfo SET money = :money WHERE user_id = :userId")
    suspend fun updateUserMoney(userId: Int, money: Int)

    @Query("UPDATE UserInfo SET high_score = :highScore WHERE user_id = :userId")
    suspend fun updateUserHighScore(userId: Int, highScore: Int)

    @Query("UPDATE UserInfo SET avatar = :avatar WHERE user_id = :userId")
    suspend fun updateUserAvatar(userId: Int, avatar: String)

}