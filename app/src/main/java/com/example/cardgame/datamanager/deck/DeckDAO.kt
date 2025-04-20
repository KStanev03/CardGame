package com.example.cardgame.datamanager.deck

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DeckDAO {
    @Query("SELECT * FROM Deck")
    suspend fun getAllDecks(): List<Deck>

    @Query("SELECT * FROM Deck WHERE deckId = :deckId")
    suspend fun getDeckById(deckId: Int): Deck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<Deck>)

    @Query("DELETE FROM Deck")
    suspend fun deleteAllDecks()

    @Transaction
    suspend fun resetAndPopulateDecks(decks: List<Deck>) {
        deleteAllDecks()
        insertDecks(decks)
    }
}