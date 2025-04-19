package com.example.cardgame.domain.model

enum class Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES;

    override fun toString(): String {
        return when(this) {
            CLUBS -> "♣"
            DIAMONDS -> "♦"
            HEARTS -> "♥"
            SPADES -> "♠"
        }
    }
}