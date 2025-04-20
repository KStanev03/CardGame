package com.example.cardgame.datamanager.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update


@Dao
interface UserDAO {
    @Query("SELECT * FROM User")
    fun getAll(): List<User>

    @Query("SELECT * FROM User WHERE uid = :userId LIMIT 1")
    fun findById(userId: Int): User

    @Query("SELECT * FROM User WHERE username LIKE :username LIMIT 1")
    fun findByUsername(username: String): User?

    @Query("SELECT * FROM User WHERE email LIKE :email LIMIT 1")
    fun findByEmail(email: String): User?

    @Query("UPDATE User SET username = :newUsername WHERE username LIKE :username")
    fun updateUserUsername(username: String, newUsername: String)

    @Query("UPDATE User SET email = :newEmail WHERE username LIKE :username")
    fun updateUserEmail(username: String, newEmail: String)

    @Query("UPDATE User SET password = :password WHERE email LIKE :email")
    fun updateUserPasswordByEmail(email: String, password: String)

    @Query("UPDATE User SET password = :password WHERE username LIKE :username")
    fun updateUserPassword(username: String, password: String)

    @Query("DELETE FROM User WHERE username = :username")
    fun deleteUser(username: String)

    @Query("DELETE FROM User WHERE email = :email")
    fun deleteUserByEmail(email: String)

    @Query("SELECT * FROM User WHERE uid = :userId")
    suspend fun getUserById(userId: Int): User?

    @Update
    fun update(user: User)

    @Insert
    fun insert(user: User)

    @Delete
    fun delete(user: User)

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