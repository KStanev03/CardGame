package com.example.cardgame.datamanager.deck

import android.content.Context
import android.content.SharedPreferences
import com.example.cardgame.datamanager.AppDatabase

class DeckManager(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val deckDAO = db.deckDAO()
    private val userDeckDAO = db.userDeckDAO()
    private val prefs: SharedPreferences = context.getSharedPreferences("deck_prefs", Context.MODE_PRIVATE)

    // Initialize decks if needed
    suspend fun initializeDecks() {
        val existingDecks = deckDAO.getAllDecks()
        if (existingDecks.isEmpty()) {
            deckDAO.insertDecks(Deck.PREDEFINED_DECKS)
        }
    }

    // Get all available decks
    suspend fun getAllDecks(): List<Deck> {
        initializeDecks() // Make sure decks are initialized
        return deckDAO.getAllDecks()
    }

    // Get decks owned by a user
    suspend fun getUserDecks(userId: Int): List<Deck> {
        return userDeckDAO.getUserDecksWithDetails(userId)
    }

    // Add a deck to a user's collection
    suspend fun addDeckToUser(userId: Int, deckId: Int): Boolean {
        return try {
            // First check if this is the user's first deck
            val existingDecks = userDeckDAO.getUserDecks(userId)
            val isFirstDeck = existingDecks.isEmpty()

            // Add the deck to the user's collection
            userDeckDAO.insertUserDeck(
                UserDeck(
                    userId = userId,
                    deckId = deckId,
                    isActive = isFirstDeck // Make active if it's the first deck
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // Set a user's active deck
    suspend fun setActiveDeck(userId: Int, deckId: Int): Boolean {
        return try {
            userDeckDAO.changeActiveDeck(userId, deckId)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get a user's active deck
    suspend fun getActiveDeck(userId: Int): Deck? {
        val activeDeckRelation = userDeckDAO.getActiveUserDeck(userId) ?: return null
        return deckDAO.getDeckById(activeDeckRelation.deckId)
    }

    // Get the resource prefix for the active deck
    suspend fun getActiveResourcePrefix(userId: Int): String {
        val activeDeck = getActiveDeck(userId)
        return activeDeck?.prefix ?: "card_" // Default to "card_" if no active deck
    }
}