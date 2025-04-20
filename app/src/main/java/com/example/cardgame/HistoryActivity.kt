package com.example.cardgame

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.R
import com.example.cardgame.adapter.GameHistoryAdapter
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.LoggedUser
import com.example.cardgame.datamanager.history.GameHistory
import com.example.cardgame.datamanager.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyHistoryText: TextView
    private lateinit var winCountText: TextView
    private lateinit var lossCountText: TextView
    private lateinit var historyAdapter: GameHistoryAdapter
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize UI components
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyHistoryText = findViewById(R.id.emptyHistoryText)
        winCountText = findViewById(R.id.winCountText)
        lossCountText = findViewById(R.id.lossCountText)

        // Get userId from intent or from LoggedUser
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            // If not provided in intent, try to get from current logged in user
            val username = LoggedUser.getUsername()
            if (username != null) {
                lifecycleScope.launch {
                    val user = getUserByUsername(username)
                    if (user != null) {
                        userId = user.uid
                        loadGameHistory()
                    }
                }
            }
        } else {
            // If userId is provided, load history directly
            loadGameHistory()
        }

        // Set up RecyclerView
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyAdapter = GameHistoryAdapter(emptyList())
        historyRecyclerView.adapter = historyAdapter

        // Set up back button in toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.game_history)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadGameHistory() {
        lifecycleScope.launch {
            // Get game history from database
            val history = withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(applicationContext)
                val historyDao = db.gameHistoryDAO()

                // Get win/loss counts
                val winCount = historyDao.getGameCountForUserByOutcome(userId, "WIN")
                val lossCount = historyDao.getGameCountForUserByOutcome(userId, "LOSS")

                // Update UI with counts
                withContext(Dispatchers.Main) {
                    winCountText.text = getString(R.string.wins_count, winCount)
                    lossCountText.text = getString(R.string.losses_count, lossCount)
                }

                // Return game history list
                historyDao.getGameHistoryForUser(userId)
            }

            // Update UI with history data
            withContext(Dispatchers.Main) {
                if (history.isEmpty()) {
                    emptyHistoryText.visibility = View.VISIBLE
                    historyRecyclerView.visibility = View.GONE
                } else {
                    emptyHistoryText.visibility = View.GONE
                    historyRecyclerView.visibility = View.VISIBLE
                    historyAdapter.updateHistory(history)
                }
            }
        }
    }

    private suspend fun getUserByUsername(username: String): User? {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val userDao = db.userDAO()
            userDao.findByUsername(username)
        }
    }
}