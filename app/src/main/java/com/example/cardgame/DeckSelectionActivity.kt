package com.example.cardgame

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.adapters.DeckSelectionAdapter
import com.example.cardgame.datamanager.deck.Deck
import com.example.cardgame.datamanager.deck.DeckManager
import kotlinx.coroutines.launch

class DeckSelectionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var deckManager: DeckManager
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_selection)

        // Initialize views
        recyclerView = findViewById(R.id.deckSelectionRecyclerView)
        titleTextView = findViewById(R.id.tvDeckSelectionTitle)
        val backButton = findViewById<Button>(R.id.backButtonDeckSelection)

        // Get user ID from intent
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Грешка: Потребителят не е намерен", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize deck manager
        deckManager = DeckManager(this)

        // Set up recycler view
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Load user's decks
        loadUserDecks()

        // Back button click listener
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadUserDecks() {
        lifecycleScope.launch {
            try {
                // Get user's purchased decks
                val userDecks = deckManager.getUserDecks(userId)

                // Get active deck
                val activeDeck = deckManager.getActiveDeck(userId)

                // Set up adapter
                val adapter = DeckSelectionAdapter(userDecks, activeDeck) { selectedDeck ->
                    setActiveDeck(selectedDeck)
                }
                recyclerView.adapter = adapter

                // Update title with count
                titleTextView.text = "Твоето тесте (${userDecks.size})"
            } catch (e: Exception) {
                Toast.makeText(
                    this@DeckSelectionActivity,
                    "Грешка при зарежданео на тесте: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setActiveDeck(deck: Deck) {
        lifecycleScope.launch {
            try {
                val success = deckManager.setActiveDeck(userId, deck.deckId)
                if (success) {
                    Toast.makeText(
                        this@DeckSelectionActivity,
                        "${deck.name} вече е твоето активно тесте!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Refresh the list to update active status
                    loadUserDecks()
                } else {
                    Toast.makeText(
                        this@DeckSelectionActivity,
                        "Грешка при смяна към активно тесте",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DeckSelectionActivity,
                    "Грешка: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}