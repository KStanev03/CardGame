package com.example.cardgame.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.R
import com.example.cardgame.datamanager.deck.Deck

class DeckSelectionAdapter(
    private val decks: List<Deck>,
    private val activeDeck: Deck?,
    private val onDeckSelected: (Deck) -> Unit
) : RecyclerView.Adapter<DeckSelectionAdapter.DeckViewHolder>() {

    class DeckViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deckImage: ImageView = view.findViewById(R.id.deckImage)
        val deckName: TextView = view.findViewById(R.id.deckName)
        val selectButton: Button = view.findViewById(R.id.buyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_deck, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val deck = decks[position]

        holder.deckImage.setImageResource(deck.previewImageResId)

        holder.deckName.text = deck.name

        val isActive = activeDeck?.deckId == deck.deckId


            holder.selectButton.text = if (isActive) "Активен" else "Избери"
        holder.selectButton.isEnabled = !isActive

        holder.selectButton.setOnClickListener {
            if (!isActive) {
                onDeckSelected(deck)
            }
        }
    }

    override fun getItemCount() = decks.size
}