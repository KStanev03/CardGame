package com.example.cardgame.domain.model


class Player(val name: String, val teamId: Int, val isHuman: Boolean = false) {
    val hand = mutableListOf<Card>()
    val capturedCards = mutableListOf<Card>()
    var pastraPoints = 0

    fun playCard(index: Int): Card {
        return hand.removeAt(index)
    }

    fun addToHand(card: Card) {
        hand.add(card)
    }

    fun captureCards(cards: List<Card>) {
        capturedCards.addAll(cards)
    }

    fun addPastraPoints(points: Int) {
        pastraPoints += points
    }

    fun getScore(): Int {
        var score = 0
        for (card in capturedCards) {
            score += card.getPoints()
        }
        score += pastraPoints
        return score
    }
}