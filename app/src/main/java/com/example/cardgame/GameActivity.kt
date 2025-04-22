package com.example.cardgame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.cardgame.domain.game.PastraGame
import com.example.cardgame.adapters.CardAdapter
import com.example.cardgame.datamanager.LoggedUser
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.GameCompletionHandler
import com.example.cardgame.services.GameCompletionService
import kotlinx.coroutines.launch
import com.example.cardgame.datamanager.deck.DeckManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class GameActivity : AppCompatActivity() {
    private lateinit var game: PastraGame
    private lateinit var handRecyclerView: RecyclerView
    private lateinit var tableCardsView: RecyclerView
    private lateinit var statusTextView: TextView
    private lateinit var team1ScoreTextView: TextView
    private lateinit var team2ScoreTextView: TextView
    private lateinit var gameLogTextView: TextView
    private lateinit var playCardView: FrameLayout
    private lateinit var newGameButton: Button
    private lateinit var gameCompletionService: GameCompletionService

    private var gameEndHandled = false

    private var humanPlayerIndex = 0
    private val handler = Handler(Looper.getMainLooper())


    private var currentUserId: Int = -1


    private var isAnimating = false
    private var pendingCardPlay: Int? = null


    private val cardPlayDelay = 800L
    private val aiTurnDelay = 1500L

    private enum class PlayerPosition {
        BOTTOM, LEFT, TOP, RIGHT
    }

    private lateinit var deckManager: DeckManager
    private var activeResourcePrefix = "card_"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        handRecyclerView = findViewById(R.id.playerHandRecyclerView)
        tableCardsView = findViewById(R.id.tableCardsRecyclerView)
        statusTextView = findViewById(R.id.statusTextView)
        team1ScoreTextView = findViewById(R.id.team1ScoreTextView)
        team2ScoreTextView = findViewById(R.id.team2ScoreTextView)
        gameLogTextView = findViewById(R.id.gameLogTextView)
        playCardView = findViewById(R.id.playCardView)
        newGameButton = findViewById(R.id.nextButton)


        handRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        tableCardsView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)


        playCardView.visibility = View.INVISIBLE


        gameCompletionService = GameCompletionService(this)

        deckManager = DeckManager(this)

        val username = LoggedUser.getUsername()
        if (username != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val user = db.userDAO().findByUsername(username)
                if (user != null) {
                    currentUserId = user.uid

                    loadActiveDeckPrefix()

                    setupGame()
                } else {

                    setupGame()
                }
            }
        }

        newGameButton.text = "Нова игра"
        newGameButton.visibility = View.GONE
        newGameButton.setOnClickListener {
            setupGame()
        }
    }

    private fun setupGame() {
        game = PastraGame()


        gameEndHandled = false



        game.addPlayer("Играч 1", 0, true)
        game.addPlayer("Играч 2", 1)
        game.addPlayer("Партньор", 0)
        game.addPlayer("Играч 4", 1)


        game.dealCards()


        newGameButton.visibility = View.GONE

        updateUI()

        processNextTurn()
    }

    private fun updateUI() {
        val currentPlayer = game.getCurrentPlayer()
        val isHumanTurn = currentPlayer.isHuman


        statusTextView.text = "Ред на: ${currentPlayer.name}"


        val scores = game.getTeamScores()
        team1ScoreTextView.text = "Отбор 1: ${scores[0]} точки"
        team2ScoreTextView.text = "Отбор 2: ${scores[1]} точки"


        val tableCards = game.getTableCards()

        val visibleTableCards = if (tableCards.isNotEmpty()) {
            listOf(tableCards.last())
        } else {
            emptyList()
        }

        tableCardsView.adapter = CardAdapter(
            visibleTableCards,
            { _ ->

            },
            resources.getDimensionPixelSize(R.dimen.card_width),
            activeResourcePrefix
        )


        val logMessages = game.getLogMessages().takeLast(5)
        gameLogTextView.text = logMessages.joinToString("\n")

        val humanPlayer = game.getPlayers().first { it.isHuman }
        handRecyclerView.adapter = CardAdapter(
            humanPlayer.hand,
            { cardIndex ->
                if (!isAnimating && isHumanTurn) {
                    handleCardPlay(cardIndex)
                }
            },
            resources.getDimensionPixelSize(R.dimen.card_width),
            activeResourcePrefix
        )
        handRecyclerView.visibility = View.VISIBLE

        if (game.isGameComplete() && !gameEndHandled) {
            handleGameEnd()
        }

        if (game.isRoundComplete() && !game.isGameComplete()) {
            game.dealNextRound()
            updateUI()
        }
    }

    private fun getPlayerPosition(playerIndex: Int): PlayerPosition {
        return when (playerIndex % 4) {
            0 -> PlayerPosition.BOTTOM
            1 -> PlayerPosition.RIGHT
            2 -> PlayerPosition.TOP
            3 -> PlayerPosition.LEFT
            else -> PlayerPosition.BOTTOM
        }
    }

    private fun getCardPlayInAnimation(position: PlayerPosition): Int {
        return when (position) {
            PlayerPosition.BOTTOM -> R.anim.card_play_in
            PlayerPosition.LEFT -> R.anim.card_play_from_left
            PlayerPosition.TOP -> R.anim.card_play_from_top
            PlayerPosition.RIGHT -> R.anim.card_play_from_right
        }
    }

    private fun getCardPlayOutAnimation(position: PlayerPosition, isCapture: Boolean): Int {
        return if (isCapture) {
            R.anim.card_capture_out
        } else {
            R.anim.card_play_out
        }
    }


    private fun processNextTurn() {
        if (game.isGameComplete() && !gameEndHandled) {
            handleGameEnd()
            return
        }
        if (!game.getCurrentPlayer().isHuman && !isAnimating) {
            handleAITurn()
        }
    }

    private fun handleCardPlay(cardIndex: Int) {
        isAnimating = true

        val currentPlayer = game.getCurrentPlayer()
        val position = getPlayerPosition(game.getCurrentPlayerIndex())

        val card = currentPlayer.hand[cardIndex]

        val playedCardImage = playCardView.findViewById<ImageView>(R.id.playedCardImage)
        playedCardImage.setImageResource(card.getImageResourceId(activeResourcePrefix))
        playCardView.visibility = View.VISIBLE

        val animIn = AnimationUtils.loadAnimation(this, getCardPlayInAnimation(position))
        playCardView.startAnimation(animIn)

        handler.postDelayed({
            val result = game.playTurn(cardIndex)

            if (result.success) {
                val animOut = AnimationUtils.loadAnimation(this,
                    getCardPlayOutAnimation(position, result.captured))

                playCardView.startAnimation(animOut)

                handler.postDelayed({
                    playCardView.visibility = View.INVISIBLE

                    if (result.isPastra) {
                        showPastraAnimation()
                        Toast.makeText(this, "ПАСТРА! ${result.pastraPoints} точки", Toast.LENGTH_SHORT).show()
                        handler.postDelayed({
                            updateUI()

                            handler.postDelayed({
                                isAnimating = false
                                processNextTurn()
                            }, 500)
                        }, 1500)
                    } else {
                        updateUI()

                        handler.postDelayed({
                            isAnimating = false
                            processNextTurn()
                        }, 500)
                    }
                }, animOut.duration)
            } else {
                playCardView.visibility = View.INVISIBLE
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                isAnimating = false
            }
        }, cardPlayDelay)
    }

    private fun showPastraAnimation() {
        val pastraText = findViewById<TextView>(R.id.pastraText)
        pastraText.visibility = View.VISIBLE
        val anim = AnimationUtils.loadAnimation(this, R.anim.pastra_flash)
        pastraText.startAnimation(anim)


        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            pastraText.clearAnimation()
            pastraText.visibility = View.INVISIBLE
        }, 1500)
    }

    private fun handleAITurn() {
        if (isAnimating || game.isGameComplete()) return

        isAnimating = true
        val currentPlayer = game.getCurrentPlayer()
        val position = getPlayerPosition(game.getCurrentPlayerIndex())

        if (!currentPlayer.isHuman) {
            statusTextView.text = "${currentPlayer.name} мисли..."

            handler.postDelayed({
                val result = game.playAITurn()

                if (result.success) {
                    val playedCardImage = playCardView.findViewById<ImageView>(R.id.playedCardImage)
                    playedCardImage.setImageResource(game.lastPlayedCard!!.getImageResourceId(activeResourcePrefix))
                    playCardView.visibility = View.VISIBLE

                    val animIn = AnimationUtils.loadAnimation(this, getCardPlayInAnimation(position))
                    playCardView.startAnimation(animIn)

                    handler.postDelayed({
                        val animOut = AnimationUtils.loadAnimation(this,
                            getCardPlayOutAnimation(position, result.captured))

                        playCardView.startAnimation(animOut)

                        handler.postDelayed({
                            playCardView.visibility = View.INVISIBLE

                            if (result.isPastra) {
                                showPastraAnimation()
                                Toast.makeText(this, "${currentPlayer.name} направи Пастра!", Toast.LENGTH_SHORT).show()
                                handler.postDelayed({
                                    updateUI()
                                    isAnimating = false

                                    handler.postDelayed({
                                        processNextTurn()
                                    }, 500)
                                }, 1500)
                            } else {
                                updateUI()
                                isAnimating = false

                                handler.postDelayed({
                                    processNextTurn()
                                }, 500)
                            }
                        }, animOut.duration)
                    }, cardPlayDelay)
                } else {
                    updateUI()
                    isAnimating = false
                    processNextTurn()
                }
            }, aiTurnDelay)
        } else {
            updateUI()
            isAnimating = false
        }
    }

    private fun handleGameEnd() {
        gameEndHandled = true

        game.finalizeGame()

        val scores = game.getTeamScores()
        team1ScoreTextView.text = "Отбор 1: ${scores[0]} точки"
        team2ScoreTextView.text = "Отбор 2: ${scores[1]} точки"

        val winningTeam = game.getWinningTeam()
        val message = if (winningTeam != null) {
            "${winningTeam.name} печели с ${scores[winningTeam.id]} точки!"
        } else {
            "Равно е"
        }

        Toast.makeText(this, "Край на играта! $message", Toast.LENGTH_LONG).show()

        val userId = intent.getIntExtra("USER_ID", -1).takeIf { it != -1 } ?: LoggedUser.getUserId()

        if (userId != -1) {
            val isPlayerWinner = (winningTeam?.id == 0)

            val playerScore = scores[0] ?: 0

            val aiScore = scores[1] ?: 0

            gameCompletionService.handleGameCompletion(
                userId,
                isPlayerWinner,
                playerScore,
                aiScore,
                lifecycleScope
            )

            newGameButton.visibility = View.VISIBLE
        }
    }

        private fun showGameEndDialog(resultMessage: String, rewardsMessage: String) {
            val fullMessage = if (rewardsMessage.isNotEmpty()) {
                "$resultMessage\n\n$rewardsMessage"
            } else {
                resultMessage
            }

            AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(fullMessage)
                .setPositiveButton("OK") { _, _ ->
                    newGameButton.visibility = View.VISIBLE
                }
                .setCancelable(false)
                .show()

        }

    private suspend fun loadActiveDeckPrefix() {
        if (currentUserId != -1) {
            try {
                activeResourcePrefix = deckManager.getActiveResourcePrefix(currentUserId)
                println("Зареден префикс за активно тесте: $activeResourcePrefix")
            } catch (e: Exception) {
                println("Грешка при зареждане на тестето: ${e.message}")
                activeResourcePrefix = "card_"
            }
        } else {
            activeResourcePrefix = "card_"
        }
    }
}