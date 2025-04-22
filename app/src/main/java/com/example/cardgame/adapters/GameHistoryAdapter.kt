package com.example.cardgame.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.R
import com.example.cardgame.datamanager.history.GameHistory

class GameHistoryAdapter(private var history: List<GameHistory>) :
    RecyclerView.Adapter<GameHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outcomeText: TextView = itemView.findViewById(R.id.gameOutcomeText)
        val opponentText: TextView = itemView.findViewById(R.id.opponentText)
        val scoreText: TextView = itemView.findViewById(R.id.scoreText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = history[position]

        // Set outcome text with appropriate color
        holder.outcomeText.text = when (historyItem.outcome.lowercase()) {
            "win" -> "Победа"
            "loss" -> "Загуба"
            else -> historyItem.outcome.capitalize()
        }
        holder.outcomeText.setTextColor(
            if (historyItem.outcome.equals("win", ignoreCase = true)) {
                holder.itemView.context.getColor(R.color.win_color)
            } else {
                holder.itemView.context.getColor(R.color.loss_color)
            }
        )

        // Set other details
        holder.opponentText.text = historyItem.opponent ?: "Неизвестен противник"
        holder.scoreText.text = historyItem.score ?: "Резултатът не е записан"

        // Format and set date
        holder.dateText.text = formatDate(historyItem.timestamp)
    }

    override fun getItemCount(): Int = history.size

    fun updateHistory(newHistory: List<GameHistory>) {
        history = newHistory
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: String): String {
        // Could enhance this to format the date more nicely
        return timestamp
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { it.uppercase() }
    }
}