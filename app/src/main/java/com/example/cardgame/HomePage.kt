package com.example.cardgame

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import  com.example.cardgame.datamanager.AppDatabase

class HomePage : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_home_page)
            val userId = intent.getIntExtra("USER_ID", -1)

            // Profile button
            findViewById<CardView>(R.id.cardProfile).setOnClickListener {
                val intent = Intent(this, Profile::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }

            // Play button
            findViewById<CardView>(R.id.cardPlay).setOnClickListener {
                val intent = Intent(this, GameActivity::class.java)
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
//            findViewById<CardView>(R.id.cardShop).setOnClickListener {
//                val intent = Intent(this, ShopActivity::class.java)
//                startActivity(intent)
//            }
//
//            // Tutorial button
//            findViewById<CardView>(R.id.cardTutorial).setOnClickListener {
//                val intent = Intent(this, TutorialActivity::class.java)
//                startActivity(intent)
//            }
//
//            // Tournaments button
//            findViewById<CardView>(R.id.cardTournaments).setOnClickListener {
//                val intent = Intent(this, TournamentsActivity::class.java)
//                startActivity(intent)
//            }
        }


    private fun loadUserAvatar(userId: Int, imageView: CircleImageView) {
        if (userId == -1) return

        val db = AppDatabase.getInstance(applicationContext)
        val userInfoDAO = db.userInfoDAO()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userInfo = userInfoDAO.getUserInfoByUserId(userId)

                withContext(Dispatchers.Main) {
                    userInfo?.let {
                        try {
                            val resourceId = resources.getIdentifier(it.avatar, "drawable", packageName)
                            if (resourceId != 0) {
                                imageView.setImageResource(resourceId)
                            } else {
                                // Fallback to default avatar
                                imageView.setImageResource(R.drawable.panda)
                            }
                        } catch (e: Exception) {
                            // If there's an error, use default avatar
                            imageView.setImageResource(R.drawable.panda)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HomePage,
                        "Error loading avatar: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}