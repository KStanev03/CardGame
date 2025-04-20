package com.example.cardgame

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.R
import com.example.cardgame.adapters.AchievementAdapter
import com.example.cardgame.datamanager.achievement.Achievement
import com.example.cardgame.datamanager.achievement.AchievementManager
import kotlinx.coroutines.launch

class AchievementActivity : AppCompatActivity() {

    private lateinit var achievementManager: AchievementManager
    private lateinit var completedRecyclerView: RecyclerView
    private lateinit var inProgressRecyclerView: RecyclerView
    private lateinit var noCompletedText: TextView
    private lateinit var noInProgressText: TextView

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievement)

        // Get userId from intent
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish()
            return
        }

        // Initialize views
        completedRecyclerView = findViewById(R.id.completedAchievementsRecyclerView)
        inProgressRecyclerView = findViewById(R.id.inProgressAchievementsRecyclerView)
        noCompletedText = findViewById(R.id.noCompletedAchievementsText)
        noInProgressText = findViewById(R.id.noInProgressAchievementsText)

        // Set up RecyclerViews
        completedRecyclerView.layoutManager = LinearLayoutManager(this)
        inProgressRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize achievement manager
        achievementManager = AchievementManager(this)

        // Load achievements
        loadAchievements()
    }

    private fun loadAchievements() {
        lifecycleScope.launch {
            // Get completed achievements
            val completedAchievements = achievementManager.getCompletedAchievements(userId)
            if (completedAchievements.isEmpty()) {
                noCompletedText.visibility = View.VISIBLE
                completedRecyclerView.visibility = View.GONE
            } else {
                noCompletedText.visibility = View.GONE
                completedRecyclerView.visibility = View.VISIBLE
                completedRecyclerView.adapter = AchievementAdapter(completedAchievements, true)
            }

            // Get in-progress achievements
            val inProgressAchievements = achievementManager.getInProgressAchievements(userId)
            if (inProgressAchievements.isEmpty()) {
                noInProgressText.visibility = View.VISIBLE
                inProgressRecyclerView.visibility = View.GONE
            } else {
                noInProgressText.visibility = View.GONE
                inProgressRecyclerView.visibility = View.VISIBLE
                inProgressRecyclerView.adapter = AchievementAdapter(inProgressAchievements, false)
            }
        }
    }
}