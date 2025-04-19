package com.example.cardgame.domain.model

class Team(val id: Int, val name: String) {
    val players = mutableListOf<Player>()
    private var bonusPoints = 0

    fun addPlayer(player: Player) {
        players.add(player)
    }

//    fun getScore(): Int {
//        return players.sumOf { it.getScore() }
//    }

    fun getCapturedCardCount(): Int {
        return players.sumOf { it.capturedCards.size }
    }

    fun getScore(): Int {
        var score = 0
        for (player in players) {
            score += player.getScore()
        }
        // Add bonus points for having most cards
        return score + bonusPoints
    }


    fun addBonusPoints(points: Int) {
        println("Team ${id} adding bonus points: $points. Previous bonus: $bonusPoints")
        bonusPoints += points
        println("Team ${id} new bonus points: $bonusPoints")
    }

}