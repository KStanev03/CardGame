package com.example.cardgame.domain.model

import com.example.cardgame.R

data class Card(val rank: Rank, val suit: Suit) {
    override fun toString(): String = "$rank $suit"

    fun getPoints(): Int {
        return when (rank) {
            Rank.ACE, Rank.JACK -> 1
            Rank.TEN -> if (suit == Suit.DIAMONDS) 3 else 0
            Rank.TWO -> if (suit == Suit.CLUBS) 2 else 0
            else -> 0
        }
    }

    fun matches(other: Card): Boolean {
        return rank == other.rank
    }

    fun getImageResourceId(resourcePrefix: String = "card_"): Int {
        val rankStr = when(rank) {
            Rank.ACE -> "ace"
            Rank.TWO -> "2"
            Rank.THREE -> "3"
            Rank.FOUR -> "4"
            Rank.FIVE -> "5"
            Rank.SIX -> "6"
            Rank.SEVEN -> "7"
            Rank.EIGHT -> "8"
            Rank.NINE -> "9"
            Rank.TEN -> "10"
            Rank.JACK -> "jack"
            Rank.QUEEN -> "queen"
            Rank.KING -> "king"
        }

        val suitStr = when(suit) {
            Suit.CLUBS -> "clubs"
            Suit.DIAMONDS -> "diamonds"
            Suit.HEARTS -> "hearts"
            Suit.SPADES -> "spades"
        }

        // This assumes you have card images named like "prefix_rankStr_of_suitStr"
        // For example: "card_ace_of_hearts", "neon_2_of_clubs", etc.
        val resourceName = "${resourcePrefix}${rankStr}_of_${suitStr}"

        try {
            return R.drawable::class.java.getField(resourceName).getInt(null)
        } catch (e: Exception) {
            // Fallback to the default card image if the resource doesn't exist
            return R.drawable.card_back
        }
    }
}