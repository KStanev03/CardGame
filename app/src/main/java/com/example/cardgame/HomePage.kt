package com.example.cardgame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import  com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.achievement.AchievementManager
import com.google.android.material.button.MaterialButton

class HomePage : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_home_page)
            val userId = intent.getIntExtra("USER_ID", -1)

            if (userId != -1) {
                checkAndInitializeAchievements(userId)
                loadUserCoins(userId)
            }

            // Profile button
            findViewById<CardView>(R.id.cardProfile).setOnClickListener {
                val intent = Intent(this, Profile::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }

            // Play button
            findViewById<CardView>(R.id.cardPlay).setOnClickListener {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }

//            // History button
            findViewById<CardView>(R.id.cardHistory).setOnClickListener {
                val intent = Intent(this, HistoryActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
           }
//
//            // Shop button
            findViewById<CardView>(R.id.cardShop).setOnClickListener {
                val intent = Intent(this, ShopActivity::class.java)
                intent.putExtra("USER_ID", userId)
               startActivity(intent)
            }
//
            // Tutorial button
            findViewById<CardView>(R.id.cardTutorial).setOnClickListener {
                val intent = Intent(this, TutorialActivity::class.java)
               startActivity(intent)
            }

            findViewById<CardView>(R.id.cardAchievements).setOnClickListener {
                val intent = Intent(this, AchievementActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }

        }
    override fun onResume() {
        super.onResume()

        val userId = intent.getIntExtra("USER_ID", -1)
        if (userId != -1) {
            loadUserCoins(userId)
        }
    }
    private fun loadUserCoins(userId: Int) {
        if (userId == -1) return

        val db = AppDatabase.getInstance(applicationContext)
        val userInfoDAO = db.userInfoDAO()
        val tvCoins = findViewById<TextView>(R.id.tvCoins)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userInfo = userInfoDAO.getUserInfoByUserId(userId)
                withContext(Dispatchers.Main) {
                    userInfo?.let {
                        tvCoins.text = it.money.toString()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HomePage,
                        "Error loading coins: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkAndInitializeAchievements(userId: Int) {
        lifecycleScope.launch {
            val achievementManager = AchievementManager(this@HomePage)
            val achievements = achievementManager.getUserAchievements(userId)


            if (achievements.isEmpty()) {
                achievementManager.initializeAchievementsForUser(userId)
            }
        }
    }
}