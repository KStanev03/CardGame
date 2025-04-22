package com.example.cardgame.adapters

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardgame.R
import com.example.cardgame.domain.model.Card
class CardAdapter(
    private val cards: List<Card>,
    private val onCardClicked: (Int) -> Unit,
    private val cardWidth: Int = 100,
    private val resourcePrefix: String = "card_"
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(val cardView: FrameLayout) : RecyclerView.ViewHolder(cardView) {
        val cardImage: ImageView = cardView.findViewById(R.id.cardImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item, parent, false) as FrameLayout


        val layoutParams = ViewGroup.LayoutParams(
            cardWidth,
            (cardWidth * 1.4).toInt()
        )
        cardView.layoutParams = layoutParams

        return CardViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]

        val resourceId = card.getImageResourceId(resourcePrefix)
        holder.cardImage.setImageResource(resourceId)

        val imageParams = holder.cardImage.layoutParams
        imageParams.width = (cardWidth * 0.95).toInt()
        imageParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        holder.cardImage.layoutParams = imageParams

        holder.cardView.setOnClickListener {
            val clickAnim = AnimationUtils.loadAnimation(holder.cardView.context, R.anim.card_click)
            holder.cardView.startAnimation(clickAnim)

            Handler(Looper.getMainLooper()).postDelayed({
                onCardClicked(position)
            }, 200)
        }
    }

    override fun getItemCount() = cards.size
}

