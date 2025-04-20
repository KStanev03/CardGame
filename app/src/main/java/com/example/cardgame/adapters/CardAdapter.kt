package com.example.cardgame.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.R
import com.example.cardgame.domain.model.Card

class CardAdapter(
    private val cards: List<Card>,
    private val onCardClicked: (Int) -> Unit,
    private val cardWidth: Int,
    private val resourcePrefix: String = "card_" // Default to classic deck
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardImage: ImageView = view.findViewById(R.id.cardImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]

        // Use the resource prefix to load the correct card image
        holder.cardImage.setImageResource(card.getImageResourceId(resourcePrefix))

        // Set card width
        val params = holder.cardImage.layoutParams
        params.width = cardWidth
        params.height = (cardWidth * 1.4).toInt() // Standard card ratio
        holder.cardImage.layoutParams = params

        // Set click listener
        holder.itemView.setOnClickListener {
            onCardClicked(position)
        }
    }

    override fun getItemCount() = cards.size
}