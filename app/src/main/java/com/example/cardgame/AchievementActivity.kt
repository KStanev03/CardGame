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

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            finish()
            return
        }

        completedRecyclerView = findViewById(R.id.completedAchievementsRecyclerView)
        inProgressRecyclerView = findViewById(R.id.inProgressAchievementsRecyclerView)
        noCompletedText = findViewById(R.id.noCompletedAchievementsText)
        noInProgressText = findViewById(R.id.noInProgressAchievementsText)

        completedRecyclerView.layoutManager = LinearLayoutManager(this)
        inProgressRecyclerView.layoutManager = LinearLayoutManager(this)

        val params = completedRecyclerView.layoutParams
        params.height = 400
        completedRecyclerView.layoutParams = params

        achievementManager = AchievementManager(this)

        loadAchievements()
    }

    private fun loadAchievements() {
        lifecycleScope.launch {
            val completedAchievements = achievementManager.getCompletedAchievements(userId)

            println("Debug: Found ${completedAchievements.size} completed achievements")
            completedAchievements.forEach {
                println("Debug: Completed achievement: ${it.goalName}, isCompleted=${it.isCompleted}")
            }

            if (completedAchievements.isEmpty()) {
                noCompletedText.visibility = View.VISIBLE
                completedRecyclerView.visibility = View.GONE
            } else {
                noCompletedText.visibility = View.GONE
                completedRecyclerView.visibility = View.VISIBLE

                val adapter = AchievementAdapter(completedAchievements, true)
                completedRecyclerView.adapter = adapter

                completedRecyclerView.post {
                    adapter.notifyDataSetChanged()
                }
            }

            val inProgressAchievements = achievementManager.getInProgressAchievements(userId)

            println("Debug: Found ${inProgressAchievements.size} in-progress achievements")

            if (inProgressAchievements.isEmpty()) {
                noInProgressText.visibility = View.VISIBLE
                inProgressRecyclerView.visibility = View.GONE
            } else {
                noInProgressText.visibility = View.GONE
                inProgressRecyclerView.visibility = View.VISIBLE

                val adapter = AchievementAdapter(inProgressAchievements, false)
                inProgressRecyclerView.adapter = adapter

                inProgressRecyclerView.post {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAchievements()
    }
}