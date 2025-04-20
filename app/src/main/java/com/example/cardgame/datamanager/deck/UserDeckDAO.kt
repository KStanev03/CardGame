package com.example.cardgame.datamanager.deck

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UserDeckDAO {
    @Query("SELECT * FROM UserDeck WHERE user_id = :userId")
    suspend fun getUserDecks(userId: Int): List<UserDeck>

    @Query("SELECT d.* FROM Deck d INNER JOIN UserDeck ud ON d.deckId = ud.deck_id WHERE ud.user_id = :userId")
    suspend fun getUserDecksWithDetails(userId: Int): List<Deck>

    @Query("SELECT * FROM UserDeck WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    suspend fun getActiveUserDeck(userId: Int): UserDeck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserDeck(userDeck: UserDeck)

    @Query("UPDATE UserDeck SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateAllUserDecks(userId: Int)

    @Query("UPDATE UserDeck SET is_active = 1 WHERE user_id = :userId AND deck_id = :deckId")
    suspend fun setActiveDeck(userId: Int, deckId: Int)

    @Transaction
    suspend fun changeActiveDeck(userId: Int, deckId: Int) {
        deactivateAllUserDecks(userId)
        setActiveDeck(userId, deckId)
    }
}