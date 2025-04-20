package com.example.cardgame.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.R
import com.example.cardgame.datamanager.achievement.Achievement

class AchievementAdapter(
    private val achievements: List<Achievement>,
    private val isCompleted: Boolean
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.achievementTitleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.achievementDescriptionTextView)
        val progressBar: ProgressBar = view.findViewById(R.id.achievementProgressBar)
        val progressTextView: TextView = view.findViewById(R.id.achievementProgressTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]

        // Set achievement title
        holder.titleTextView.text = achievement.goalName

        // Set achievement description based on type
        val description = when {
            achievement.goalName.contains("Win") ||
                    achievement.goalName.contains("Champion") ||
                    achievement.goalName.contains("Master") ||
                    achievement.goalName.contains("Player") ->
                "Win ${achievement.targetValue} games"

            achievement.goalName.contains("Point") ->
                "Accumulate ${achievement.targetValue} points"

            achievement.goalName.contains("Enthusiast") ||
                    achievement.goalName.contains("Addict") ||
                    achievement.goalName.contains("Veteran") ->
                "Play ${achievement.targetValue} games"

            else -> ""
        }
        holder.descriptionTextView.text = description

        // Set progress
        val progress = (achievement.currentValue * 100) / achievement.targetValue
        holder.progressBar.progress = progress.coerceAtMost(100)

        // Set progress text
        holder.progressTextView.text = "${achievement.currentValue}/${achievement.targetValue}"

        // If completed, set progress bar to completed state
        if (isCompleted) {
            holder.progressBar.progress = 100
            holder.progressTextView.text = "Completed!"
        }
    }

    override fun getItemCount() = achievements.size
}