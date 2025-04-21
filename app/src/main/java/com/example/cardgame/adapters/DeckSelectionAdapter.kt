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
        val selectButton: Button = view.findViewById(R.id.buyButton) // Reusing shop layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_deck, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val deck = decks[position]

        // Set deck preview image
        holder.deckImage.setImageResource(deck.previewImageResId)

        // Set deck name
        holder.deckName.text = deck.name

        // Check if this is the active deck
        val isActive = activeDeck?.deckId == deck.deckId

        // Update button text and state
        holder.selectButton.text = if (isActive) "Active" else "Select"
        holder.selectButton.isEnabled = !isActive

        // Set click listener for select button
        holder.selectButton.setOnClickListener {
            if (!isActive) {
                onDeckSelected(deck)
            }
        }
    }

    override fun getItemCount() = decks.size
}