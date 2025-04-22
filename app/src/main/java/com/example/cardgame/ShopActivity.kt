package com.example.cardgame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.adapters.DeckShopAdapter
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.deck.Deck
import com.example.cardgame.datamanager.deck.DeckManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShopActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var coinBalanceTextView: TextView
    private lateinit var deckManager: DeckManager
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        // Initialize views
        recyclerView = findViewById(R.id.deckRecyclerView)
        coinBalanceTextView = findViewById(R.id.tvShopCoins)
        val backButton = findViewById<Button>(R.id.backButton)
        val myDecksButton = findViewById<Button>(R.id.myDecksButton)

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

        // Load user's coin balance
        loadUserCoins()

        // Load available decks
        loadDecks()

        // Back button click listener
        backButton.setOnClickListener {
            finish()
        }

        // My Decks button click listener
        myDecksButton.setOnClickListener {
            val intent = Intent(this, DeckSelectionActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    private fun loadUserCoins() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val userInfo = withContext(Dispatchers.IO) {
                    db.userInfoDAO().getUserInfoByUserId(userId)
                }
                userInfo?.let {
                    coinBalanceTextView.text = "${it.money} монети"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ShopActivity,
                    "Грешка с монетите: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadDecks() {
        lifecycleScope.launch {
            try {
                // Get all available decks
                val allDecks = withContext(Dispatchers.IO) {
                    deckManager.getAllDecks()
                }

                // Get user's purchased decks
                val userDecks = withContext(Dispatchers.IO) {
                    deckManager.getUserDecks(userId)
                }

                // Ensure default decks are added to user's collection
                ensureDefaultDecksOwned(allDecks, userDecks)

                // Set up adapter with refreshed user decks
                val refreshedUserDecks = withContext(Dispatchers.IO) {
                    deckManager.getUserDecks(userId)
                }

                val adapter = DeckShopAdapter(allDecks, refreshedUserDecks) { deck ->
                    purchaseDeck(deck)
                }
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(
                    this@ShopActivity,
                    "Грешка при зареждане на тестета: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun ensureDefaultDecksOwned(allDecks: List<Deck>, userDecks: List<Deck>) {
        // Find all free (price = 0) decks
        val freeDecks = allDecks.filter { it.price == 0 }

        // Add any free decks that user doesn't already own
        for (freeDeck in freeDecks) {
            if (userDecks.none { it.deckId == freeDeck.deckId }) {
                withContext(Dispatchers.IO) {
                    deckManager.addDeckToUser(userId, freeDeck.deckId)
                }
            }
        }

        // If user has no active deck, set the first free deck as active
        val activeDeck = withContext(Dispatchers.IO) {
            deckManager.getActiveDeck(userId)
        }

        if (activeDeck == null && freeDecks.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                deckManager.setActiveDeck(userId, freeDecks.first().deckId)
            }
        }
    }

    private fun purchaseDeck(deck: Deck) {
        lifecycleScope.launch {
            try {
                // Check if user already owns this deck
                val userDecks = withContext(Dispatchers.IO) {
                    deckManager.getUserDecks(userId)
                }

                if (userDecks.any { it.deckId == deck.deckId }) {
                    Toast.makeText(
                        this@ShopActivity,
                        "Вече притежаваш това тесте",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Get user's current coin balance
                val db = AppDatabase.getInstance(applicationContext)
                val userInfo = withContext(Dispatchers.IO) {
                    db.userInfoDAO().getUserInfoByUserId(userId)
                }

                if (userInfo == null) {
                    Toast.makeText(
                        this@ShopActivity,
                        "Грешка: Потребителското инфо не е намерено",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Check if user has enough coins
                if (userInfo.money < deck.price) {
                    Toast.makeText(
                        this@ShopActivity,
                        "не достатчно монети! Трябват ти ${deck.price} монети",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Purchase the deck
                val success = withContext(Dispatchers.IO) {
                    // Deduct coins
                    db.userInfoDAO().updateUserMoney(userId, userInfo.money - deck.price)
                    // Add deck to user's collection
                    deckManager.addDeckToUser(userId, deck.deckId)
                }

                if (success) {
                    Toast.makeText(
                        this@ShopActivity,
                        "Успешно закупен ${deck.name}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Refresh user's coin balance
                    loadUserCoins()

                    // Refresh deck list to show updated ownership status
                    loadDecks()
                } else {
                    Toast.makeText(
                        this@ShopActivity,
                        "Грешка при покупка на тесте",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ShopActivity,
                    "Грешка: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}