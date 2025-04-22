package com.example.cardgame

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // Set up RecyclerViews with fixed height to avoid layout issues
        completedRecyclerView.layoutManager = LinearLayoutManager(this)
        inProgressRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set fixed height for RecyclerViews to ensure they're visible
        val params = completedRecyclerView.layoutParams
        params.height = 400 // Set a reasonable fixed height in pixels
        completedRecyclerView.layoutParams = params

        // Initialize achievement manager
        achievementManager = AchievementManager(this)

        // Load achievements immediately
        loadAchievements()
    }

    private fun loadAchievements() {
        lifecycleScope.launch {
            // Get completed achievements
            val completedAchievements = achievementManager.getCompletedAchievements(userId)

            // Log for debugging
            println("Debug: Found ${completedAchievements.size} completed achievements")
            completedAchievements.forEach {
                println("Debug: Completed achievement: ${it.goalName}, isCompleted=${it.isCompleted}")
            }

            // Important: Update UI visibility BEFORE setting adapter
            if (completedAchievements.isEmpty()) {
                noCompletedText.visibility = View.VISIBLE
                completedRecyclerView.visibility = View.GONE
            } else {
                noCompletedText.visibility = View.GONE
                completedRecyclerView.visibility = View.VISIBLE

                // Create and set adapter
                val adapter = AchievementAdapter(completedAchievements, true)
                completedRecyclerView.adapter = adapter

                // Force layout refresh
                completedRecyclerView.post {
                    adapter.notifyDataSetChanged()
                }
            }

            // Get in-progress achievements
            val inProgressAchievements = achievementManager.getInProgressAchievements(userId)

            // Log for debugging
            println("Debug: Found ${inProgressAchievements.size} in-progress achievements")

            // Important: Update UI visibility BEFORE setting adapter
            if (inProgressAchievements.isEmpty()) {
                noInProgressText.visibility = View.VISIBLE
                inProgressRecyclerView.visibility = View.GONE
            } else {
                noInProgressText.visibility = View.GONE
                inProgressRecyclerView.visibility = View.VISIBLE

                // Create and set adapter
                val adapter = AchievementAdapter(inProgressAchievements, false)
                inProgressRecyclerView.adapter = adapter

                // Force layout refresh
                inProgressRecyclerView.post {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload achievements whenever the activity resumes
        loadAchievements()
    }
}