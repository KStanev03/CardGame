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

        // Задаване на заглавие на постижението
        holder.titleTextView.text = achievement.goalName

        // Задаване на описание на постижението според типа
        val description = when {
            achievement.goalName.contains("Новак") ||
                    achievement.goalName.contains("Опитен Играч") ||
                    achievement.goalName.contains("Пастра Майстор") ||
                    achievement.goalName.contains("Пастра Шампион") ->
                "Спечелете ${achievement.targetValue} игри"

            achievement.goalName.contains("Точки") ->
                "Съберете ${achievement.targetValue} точки"

            achievement.goalName.contains("Ентусиаст") ||
                    achievement.goalName.contains("Зависим") ||
                    achievement.goalName.contains("Ветеран") ->
                "Изиграйте ${achievement.targetValue} игри"

            else -> ""
        }
        holder.descriptionTextView.text = description

        // Задаване на прогрес
        val progress = (achievement.currentValue * 100) / achievement.targetValue
        holder.progressBar.progress = progress.coerceAtMost(100)

        // Задаване на текст за прогрес
        holder.progressTextView.text = "${achievement.currentValue}/${achievement.targetValue}"

        // Ако е завършено, задаване на прогрес бара в завършено състояние
        if (isCompleted) {
            holder.progressBar.progress = 100
            holder.progressTextView.text = "Завършено!"
        }
    }

    override fun getItemCount() = achievements.size
}