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

        recyclerView = findViewById(R.id.deckRecyclerView)
        coinBalanceTextView = findViewById(R.id.tvShopCoins)
        val backButton = findViewById<Button>(R.id.backButton)
        val myDecksButton = findViewById<Button>(R.id.myDecksButton)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Грешка: Потребителят не е намерен", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
       deckManager = DeckManager(this)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        loadUserCoins()

        loadDecks()

        backButton.setOnClickListener {
            finish()
        }

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
                val allDecks = withContext(Dispatchers.IO) {
                    deckManager.getAllDecks()
                }

                val userDecks = withContext(Dispatchers.IO) {
                    deckManager.getUserDecks(userId)
                }

                ensureDefaultDecksOwned(allDecks, userDecks)

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
        val freeDecks = allDecks.filter { it.price == 0 }

        for (freeDeck in freeDecks) {
            if (userDecks.none { it.deckId == freeDeck.deckId }) {
                withContext(Dispatchers.IO) {
                    deckManager.addDeckToUser(userId, freeDeck.deckId)
                }
            }
        }

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

                if (userInfo.money < deck.price) {
                    Toast.makeText(
                        this@ShopActivity,
                        "не достатчно монети! Трябват ти ${deck.price} монети",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val success = withContext(Dispatchers.IO) {
                    db.userInfoDAO().updateUserMoney(userId, userInfo.money - deck.price)
                    deckManager.addDeckToUser(userId, deck.deckId)
                }

                if (success) {
                    Toast.makeText(
                        this@ShopActivity,
                        "Успешно закупен ${deck.name}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadUserCoins()

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