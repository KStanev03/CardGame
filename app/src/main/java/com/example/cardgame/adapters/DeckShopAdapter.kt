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

class DeckShopAdapter(
    private val decks: List<Deck>,
    private val userDecks: List<Deck>,
    private val onBuyClicked: (Deck) -> Unit
) : RecyclerView.Adapter<DeckShopAdapter.DeckViewHolder>() {

    class DeckViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deckImage: ImageView = view.findViewById(R.id.deckImage)
        val deckName: TextView = view.findViewById(R.id.deckName)
        val deckPrice: TextView = view.findViewById(R.id.deckPrice)
        val buyButton: Button = view.findViewById(R.id.buyButton)
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

        // Check if the user already owns this deck
        val isOwned = userDecks.any { it.deckId == deck.deckId }

        if (isOwned) {
            holder.buyButton.text = "Owned"
            holder.buyButton.isEnabled = false
            holder.deckPrice.visibility = View.GONE
        } else {
            holder.buyButton.text = "Buy"
            holder.buyButton.isEnabled = true
            holder.deckPrice.text = "${deck.price} coins"
            holder.deckPrice.visibility = View.VISIBLE

            // Set click listener for buy button
            holder.buyButton.setOnClickListener {
                onBuyClicked(deck)
            }
        }
    }

    override fun getItemCount() = decks.size
}