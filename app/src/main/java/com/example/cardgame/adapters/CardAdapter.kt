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
    private val cardWidth: Int = 100, // Default to the layout's 100dp
    private val resourcePrefix: String = "card_" // For skinning
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(val cardView: FrameLayout) : RecyclerView.ViewHolder(cardView) {
        val cardImage: ImageView = cardView.findViewById(R.id.cardImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item, parent, false) as FrameLayout

        // Set the card container size
        val layoutParams = ViewGroup.LayoutParams(
            cardWidth,
            (cardWidth * 1.4).toInt() // Keep the standard card ratio
        )
        cardView.layoutParams = layoutParams

        return CardViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]

        // Get card image with custom skin prefix
        val resourceId = card.getImageResourceId(resourcePrefix)
        holder.cardImage.setImageResource(resourceId)

        // Set the image to fill most of the container
        val imageParams = holder.cardImage.layoutParams
        imageParams.width = (cardWidth * 0.95).toInt() // 95% of container width
        imageParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        holder.cardImage.layoutParams = imageParams

        // Apply click animation and handler
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
//
//package com.example.cardgame.adapters;
//
//import android.os.Handler
//import android.os.Looper
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import android.view.animation.AnimationUtils
//import android.widget.FrameLayout
//import android.widget.ImageView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.cardgame.R
//import com.example.cardgame.domain.model.Card
//
//class CardAdapter(
//    private val cards: List<Card>,
//    private val onCardClick: (Int) -> Unit,
//    private val cardWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT
//) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {
//
//    class CardViewHolder(val cardView: FrameLayout) : RecyclerView.ViewHolder(cardView)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
//        val cardView = LayoutInflater.from(parent.context)
//            .inflate(R.layout.card_item, parent, false) as FrameLayout
//
//        cardView.layoutParams = ViewGroup.LayoutParams(
//            cardWidth,
//            ViewGroup.LayoutParams.MATCH_PARENT
//        )
//
//        return CardViewHolder(cardView)
//    }
//
//    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
//        val card = cards[position]
//        val imageView = holder.cardView.findViewById<ImageView>(R.id.cardImageView)
//
//        imageView.setImageResource(card.getImageResourceId())
//
//        holder.cardView.setOnClickListener {
//            // Apply a click animation
//            val clickAnim = AnimationUtils.loadAnimation(holder.cardView.context, R.anim.card_click)
//            holder.cardView.startAnimation(clickAnim)
//
//            // Delay the actual click action to let the animation play
//            Handler(Looper.getMainLooper()).postDelayed({
//                onCardClick(position)
//            }, 200)
//        }
//    }
//
//    override fun getItemCount() = cards.size
//}
//
//class CardAdapter(
//    private val cards: List<Card>,
//    private val onCardClicked: (Int) -> Unit,
//    private val cardWidth: Int,
//    private val resourcePrefix: String = "card_" // Default to classic deck
//) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {
//
//    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val cardImage: ImageView = view.findViewById(R.id.cardImageView)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.card_item, parent, false)
//        return CardViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
//        val card = cards[position]
//
//        // Use the resource prefix to load the correct card image
//        holder.cardImage.setImageResource(card.getImageResourceId(resourcePrefix))
//
//        // Set card width
//        val params = holder.cardImage.layoutParams
//        params.width = cardWidth
//        params.height = (cardWidth * 1.4).toInt() // Standard card ratio
//        holder.cardImage.layoutParams = params
//
//        // Set click listener
//        holder.itemView.setOnClickListener {
//            onCardClicked(position)
//        }
//    }
//
//    override fun getItemCount() = cards.size
//}