package com.example.cardgame.domain.game

import com.example.cardgame.domain.model.Card
import com.example.cardgame.domain.model.PlayResult
import com.example.cardgame.domain.model.Player
import com.example.cardgame.domain.model.Rank
import com.example.cardgame.domain.model.Suit
import com.example.cardgame.domain.model.Team
import kotlin.random.Random

class PastraGame {
    private val deck = mutableListOf<Card>()
    private val tableCards = mutableListOf<Card>()
    private val players = mutableListOf<Player>()
    private val teams = mutableListOf<Team>()
    private var currentPlayerIndex = 0
    private var lastCaptor: Player? = null
    private var gameLog = mutableListOf<String>()

    // Последно изиграна карта за анимация
    var lastPlayedCard: Card? = null

    init {
        // Създаване на тесте (без жокери)
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                deck.add(Card(rank, suit))
            }
        }

        // Създаване на отбори
        teams.add(Team(0, "Отбор 1"))
        teams.add(Team(1, "Отбор 2"))
    }

    fun addPlayer(name: String, teamId: Int, isHuman: Boolean = false): Player {
        val player = Player(name, teamId, isHuman)
        players.add(player)
        teams[teamId].addPlayer(player)
        return player
    }

    fun dealCards() {
        deck.shuffle()

        for (player in players) {
            repeat(4) {
                if (deck.isNotEmpty()) {
                    player.addToHand(deck.removeAt(0))
                }
            }
        }

        repeat(4) {
            if (deck.isNotEmpty()) {
                tableCards.add(deck.removeAt(0))
            }
        }

        addToLog("Играта започна. 4 карти са раздадени на всеки играч и 4 карти са поставени на масата.")
    }

    fun dealNextRound() {
        if (deck.isEmpty()) return

        for (player in players) {
            repeat(4) {
                if (deck.isNotEmpty()) {
                    player.addToHand(deck.removeAt(0))
                }
            }
        }

        addToLog("Следващ рунд: 4 нови карти са раздадени на всеки играч.")
    }

    fun playTurn(cardIndex: Int): PlayResult {
        val currentPlayer = players[currentPlayerIndex]
        if (cardIndex >= currentPlayer.hand.size) {
            return PlayResult(false, "Невалиден индекс на карта")
        }

        val playedCard = currentPlayer.playCard(cardIndex)
        lastPlayedCard = playedCard

        addToLog("${currentPlayer.name} играе $playedCard")

        var captured = false
        var isPastra = false
        var pastraPoints = 0
        val capturedCards = mutableListOf<Card>()

        if (playedCard.rank == Rank.JACK) {
            if (tableCards.isNotEmpty()) {
                capturedCards.addAll(tableCards)
                capturedCards.add(playedCard)

                if (tableCards.size == 1 && tableCards[0].rank == Rank.JACK) {
                    isPastra = true
                    pastraPoints = 20
                    currentPlayer.addPastraPoints(pastraPoints)
                    addToLog("ПАСТРА с вале! ${currentPlayer.name} получава 20 допълнителни точки!")
                }

                currentPlayer.captureCards(capturedCards)
                tableCards.clear()
                lastCaptor = currentPlayer
                captured = true
                addToLog("${currentPlayer.name} взе всички карти с вале!")
            } else {
                tableCards.add(playedCard)
            }
        } else if (tableCards.isNotEmpty() && playedCard.matches(tableCards.last())) {
            capturedCards.addAll(tableCards)
            capturedCards.add(playedCard)

            if (tableCards.size == 1) {
                isPastra = true
                pastraPoints = 10
                currentPlayer.addPastraPoints(pastraPoints)
                addToLog("ПАСТРА! ${currentPlayer.name} получава 10 допълнителни точки!")
            }

            currentPlayer.captureCards(capturedCards)
            addToLog("${currentPlayer.name} взе ${tableCards.size + 1} карти")
            tableCards.clear()
            lastCaptor = currentPlayer
            captured = true

        } else {
            tableCards.add(playedCard)
        }

        if (!captured) {
            addToLog("${currentPlayer.name} постави $playedCard на масата")
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % players.size

        return PlayResult(
            true,
            if (captured) {
                if (isPastra) "Пастра! Взе карти с $playedCard"
                else "Взе карти с $playedCard"
            } else {
                "Постави $playedCard на масата"
            },
            captured,
            isPastra,
            pastraPoints,
            capturedCards
        )
    }

    fun playAITurn(): PlayResult {
        val aiPlayer = players[currentPlayerIndex]
        if (aiPlayer.isHuman) {
            return PlayResult(false, "Текущият играч не е AI")
        }

        var cardToPlay = -1

        for (i in aiPlayer.hand.indices) {
            val card = aiPlayer.hand[i]

            for (tableCard in tableCards) {
                if (card.matches(tableCard)) {
                    cardToPlay = i
                    break
                }
            }

            if (cardToPlay != -1) break

            if (card.rank == Rank.JACK && tableCards.isNotEmpty()) {
                cardToPlay = i
                break
            }
        }

        if (cardToPlay == -1) {
            cardToPlay = Random.nextInt(aiPlayer.hand.size)
        }

        return playTurn(cardToPlay)
    }

    fun isRoundComplete(): Boolean {
        return players.all { it.hand.isEmpty() }
    }

    fun isGameComplete(): Boolean {
        return deck.isEmpty() && isRoundComplete()
    }

    fun finalizeGame() {
        if (tableCards.isNotEmpty() && lastCaptor != null) {
            lastCaptor!!.captureCards(tableCards)
            addToLog("Оставащите ${tableCards.size} карти на масата бяха дадени на ${lastCaptor!!.name}")
            tableCards.clear()
        }

        val team1CardCount = teams[0].getCapturedCardCount()
        val team2CardCount = teams[1].getCapturedCardCount()

        println("Резултат на отбор 1 преди бонус: ${teams[0].getScore()}")
        println("Резултат на отбор 2 преди бонус: ${teams[1].getScore()}")

        if (team1CardCount > team2CardCount) {
            addToLog("${teams[0].name} има най-много карти и получава 3 допълнителни точки")
            teams[0].addBonusPoints(3)
            println("Бонус точки за отбор 1. Нов резултат: ${teams[0].getScore()}")
        } else if (team2CardCount > team1CardCount) {
            addToLog("${teams[1].name} има най-много карти и получава 3 допълнителни точки")
            teams[1].addBonusPoints(3)
            println("Бонус точки за отбор 2. Нов резултат: ${teams[1].getScore()}")
        } else {
            addToLog("Двата отбора имат еднакъв брой карти, не се присъждат допълнителни точки")
        }

        println("Краен резултат на отбор 1: ${getTeamScores()[0]}")
        println("Краен резултат на отбор 2: ${getTeamScores()[1]}")
    }

    private fun addToLog(message: String) {
        gameLog.add(message)
    }

    fun getLogMessages(): List<String> = gameLog

    fun getTeamScores(): Map<Int, Int> {
        return teams.associateBy({ it.id }, { it.getScore() })
    }

    fun getCurrentPlayer(): Player {
        return players[currentPlayerIndex]
    }

    fun getTableCards(): List<Card> {
        return tableCards.toList()
    }

    fun getRemainingCards(): Int {
        return deck.size
    }

    fun getWinningTeam(): Team? {
        val scores = getTeamScores()
        return if (scores[0]!! > scores[1]!!) teams[0]
        else if (scores[0]!! < scores[1]!!) teams[1]
        else null
    }

    fun getCurrentPlayerIndex(): Int {
        return currentPlayerIndex
    }

    fun getPlayers(): List<Player> {
        return players.toList()
    }

    fun getPastrasCountForTeam(teamId: Int): Int {
        return teams[teamId].players.sumOf { player ->
            player.pastraPoints / 10
        }
    }
}
