package com.example.cardgame.datamanager.deck

import android.content.Context
import android.content.SharedPreferences
import com.example.cardgame.datamanager.AppDatabase

class DeckManager(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val deckDAO = db.deckDAO()
    private val userDeckDAO = db.userDeckDAO()
    private val prefs: SharedPreferences = context.getSharedPreferences("deck_prefs", Context.MODE_PRIVATE)

    suspend fun initializeDecks() {
        val existingDecks = deckDAO.getAllDecks()
        if (existingDecks.isEmpty()) {
            deckDAO.insertDecks(Deck.PREDEFINED_DECKS)
        }
    }

    suspend fun getAllDecks(): List<Deck> {
        initializeDecks()
        return deckDAO.getAllDecks()
    }

    suspend fun getUserDecks(userId: Int): List<Deck> {
        return userDeckDAO.getUserDecksWithDetails(userId)
    }

    suspend fun addDeckToUser(userId: Int, deckId: Int): Boolean {
        return try {
            val existingDecks = userDeckDAO.getUserDecks(userId)
            val isFirstDeck = existingDecks.isEmpty()

            userDeckDAO.insertUserDeck(
                UserDeck(
                    userId = userId,
                    deckId = deckId,
                    isActive = isFirstDeck
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setActiveDeck(userId: Int, deckId: Int): Boolean {
        return try {
            userDeckDAO.changeActiveDeck(userId, deckId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getActiveDeck(userId: Int): Deck? {
        val activeDeckRelation = userDeckDAO.getActiveUserDeck(userId) ?: return null
        return deckDAO.getDeckById(activeDeckRelation.deckId)
    }

    suspend fun getActiveResourcePrefix(userId: Int): String {
        val activeDeck = getActiveDeck(userId)
        return activeDeck?.prefix ?: "card_"
    }
}