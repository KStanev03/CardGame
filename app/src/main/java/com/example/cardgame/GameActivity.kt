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

    // User ID for database operations
    private var currentUserId: Int = -1

    // Animation control flags
    private var isAnimating = false
    private var pendingCardPlay: Int? = null

    // Animation delays
    private val cardPlayDelay = 800L // ms
    private val aiTurnDelay = 1500L // ms

    // Define player positions
    private enum class PlayerPosition {
        BOTTOM, LEFT, TOP, RIGHT
    }

    private lateinit var deckManager: DeckManager
    private var activeResourcePrefix = "card_"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialize UI components
        handRecyclerView = findViewById(R.id.playerHandRecyclerView)
        tableCardsView = findViewById(R.id.tableCardsRecyclerView)
        statusTextView = findViewById(R.id.statusTextView)
        team1ScoreTextView = findViewById(R.id.team1ScoreTextView)
        team2ScoreTextView = findViewById(R.id.team2ScoreTextView)
        gameLogTextView = findViewById(R.id.gameLogTextView)
        playCardView = findViewById(R.id.playCardView)
        newGameButton = findViewById(R.id.nextButton) // Repurpose next button as new game button

        // Set up RecyclerViews
        handRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        tableCardsView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Set up play area for animations
        playCardView.visibility = View.INVISIBLE

        // Initialize the Game Service
        gameCompletionService = GameCompletionService(this)

        deckManager = DeckManager(this)
        // Get current user ID from logged in user
        // This assumes there's a way to get the current user ID - using LoggedUser singleton
        val username = LoggedUser.getUsername()
        if (username != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val user = db.userDAO().findByUsername(username)
                if (user != null) {
                    currentUserId = user.uid
                    // First load the deck prefix
                    loadActiveDeckPrefix()
                    // THEN setup the game after the prefix has been loaded
                    setupGame()
                } else {
                    // Fallback for when no user is found
                    setupGame()
                }
            }
        }

        // Set up new game button (previously next button)
        newGameButton.text = "Нова игра"
        newGameButton.visibility = View.GONE
        newGameButton.setOnClickListener {
            setupGame()
        }
    }

    private fun setupGame() {
        game = PastraGame()

        // Reset game end flag
        gameEndHandled = false


        // Add players (0 is human, others are AI)
        game.addPlayer("Играч 1", 0, true)
        game.addPlayer("Играч 2", 1)
        game.addPlayer("Партньор", 0)
        game.addPlayer("Играч 4", 1)

        // Deal initial cards
        game.dealCards()

        // Reset new game button
        newGameButton.visibility = View.GONE

        // Update UI
        updateUI()

        // Automatically start AI turns if human isn't first
        processNextTurn()
    }

    private fun updateUI() {
        val currentPlayer = game.getCurrentPlayer()
        val isHumanTurn = currentPlayer.isHuman

        // Update status
        statusTextView.text = "Ред на: ${currentPlayer.name}"

        // Update scores
        val scores = game.getTeamScores()
        team1ScoreTextView.text = "Отбор 1: ${scores[0]} точки"
        team2ScoreTextView.text = "Отбор 2: ${scores[1]} точки"

        // Update table cards
        val tableCards = game.getTableCards()
        // Show only the last card if there are cards on the table
        val visibleTableCards = if (tableCards.isNotEmpty()) {
            listOf(tableCards.last())
        } else {
            emptyList()
        }

        tableCardsView.adapter = CardAdapter(
            visibleTableCards,
            { _ ->
                // Nothing happens when clicking table cards
            },
            resources.getDimensionPixelSize(R.dimen.card_width),
            activeResourcePrefix
        )

        // Update game log
        val logMessages = game.getLogMessages().takeLast(5)
        gameLogTextView.text = logMessages.joinToString("\n")

        // Always show the human player's hand regardless of whose turn it is
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

        // Check if game is complete
        if (game.isGameComplete() && !gameEndHandled) {
            handleGameEnd()
        }

        // Check if round is complete
        if (game.isRoundComplete() && !game.isGameComplete()) {
            game.dealNextRound()
            updateUI()
        }
    }

    // Helper method to determine player position based on player index
    private fun getPlayerPosition(playerIndex: Int): PlayerPosition {
        return when (playerIndex % 4) {
            0 -> PlayerPosition.BOTTOM // Human is always at bottom
            1 -> PlayerPosition.RIGHT  // First AI to the right
            2 -> PlayerPosition.TOP    // Partner across the table
            3 -> PlayerPosition.LEFT   // Last AI to the left
            else -> PlayerPosition.BOTTOM // Fallback
        }
    }

    // Get animation resource based on player position
    private fun getCardPlayInAnimation(position: PlayerPosition): Int {
        return when (position) {
            PlayerPosition.BOTTOM -> R.anim.card_play_in
            PlayerPosition.LEFT -> R.anim.card_play_from_left
            PlayerPosition.TOP -> R.anim.card_play_from_top
            PlayerPosition.RIGHT -> R.anim.card_play_from_right
        }
    }

    // Get out animation resource based on player position
    private fun getCardPlayOutAnimation(position: PlayerPosition, isCapture: Boolean): Int {
        // If it's a capture, always go to center-out, otherwise direction-specific
        return if (isCapture) {
            R.anim.card_capture_out
        } else {
            // You can also create position-specific out animations if desired
            R.anim.card_play_out
        }
    }

    // Process the next turn automatically
    private fun processNextTurn() {
        // Check if game is over before processing next turn
        if (game.isGameComplete() && !gameEndHandled) {
            handleGameEnd()
            return
        }

        // If it's the AI's turn, handle it automatically
        if (!game.getCurrentPlayer().isHuman && !isAnimating) {
            handleAITurn()
        }
    }

    // Improved card play with position-based animations
    private fun handleCardPlay(cardIndex: Int) {
        isAnimating = true

        val currentPlayer = game.getCurrentPlayer()
        val position = getPlayerPosition(game.getCurrentPlayerIndex())

        // Create a copy of the card for animation before removing it from hand
        val card = currentPlayer.hand[cardIndex]

        // Show the played card in center
        val playedCardImage = playCardView.findViewById<ImageView>(R.id.playedCardImage)
        playedCardImage.setImageResource(card.getImageResourceId(activeResourcePrefix))
        playCardView.visibility = View.VISIBLE

        // Apply card play animation based on player position
        val animIn = AnimationUtils.loadAnimation(this, getCardPlayInAnimation(position))
        playCardView.startAnimation(animIn)

        // Play sound effect
        // playCardSound()

        // After animation, process the move
        handler.postDelayed({
            val result = game.playTurn(cardIndex)

            if (result.success) {
                // Animate card movement to table or captured pile
                val animOut = AnimationUtils.loadAnimation(this,
                    getCardPlayOutAnimation(position, result.captured))

                playCardView.startAnimation(animOut)

                handler.postDelayed({
                    playCardView.visibility = View.INVISIBLE

                    if (result.isPastra) {
                        // Show Pastra animation and play sound
                        showPastraAnimation()
                        Toast.makeText(this, "ПАСТРА! ${result.pastraPoints} точки", Toast.LENGTH_SHORT).show()
                        handler.postDelayed({
                            updateUI()

                            // Process next turn after a delay
                            handler.postDelayed({
                                isAnimating = false
                                processNextTurn()
                            }, 500)
                        }, 1500) // Extra delay for Pastra celebration
                    } else {
                        updateUI()

                        // Process next turn after a delay
                        handler.postDelayed({
                            isAnimating = false
                            processNextTurn()
                        }, 500)
                    }
                }, animOut.duration)
            } else {
                // If there was an error, just hide the card
                playCardView.visibility = View.INVISIBLE
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                isAnimating = false
            }
        }, cardPlayDelay)
    }

    // Helper method to show Pastra celebration animation
    private fun showPastraAnimation() {
        val pastraText = findViewById<TextView>(R.id.pastraText)
        pastraText.visibility = View.VISIBLE
        val anim = AnimationUtils.loadAnimation(this, R.anim.pastra_flash)
        pastraText.startAnimation(anim)

        // Ensure animation cleanup with a firm handler timeout
        handler.removeCallbacksAndMessages(null) // Remove any pending callbacks
        handler.postDelayed({
            pastraText.clearAnimation() // Clear any ongoing animations
            pastraText.visibility = View.INVISIBLE
        }, 1500)
    }

    // Handle a single AI turn with animation
    private fun handleAITurn() {
        if (isAnimating || game.isGameComplete()) return

        isAnimating = true
        val currentPlayer = game.getCurrentPlayer()
        val position = getPlayerPosition(game.getCurrentPlayerIndex())

        if (!currentPlayer.isHuman) {
            // Show thinking animation
            statusTextView.text = "${currentPlayer.name} мисли..."

            // Delay to make it feel more natural
            handler.postDelayed({
                // Apply the AI move
                val result = game.playAITurn()

                if (result.success) {
                    // Show the played card in center
                    val playedCardImage = playCardView.findViewById<ImageView>(R.id.playedCardImage)
                    playedCardImage.setImageResource(game.lastPlayedCard!!.getImageResourceId(activeResourcePrefix))
                    playCardView.visibility = View.VISIBLE

                    // Apply card play animation based on AI player position
                    val animIn = AnimationUtils.loadAnimation(this, getCardPlayInAnimation(position))
                    playCardView.startAnimation(animIn)

                    // After animation, update the table
                    handler.postDelayed({
                        // Animate card movement to table or captured pile
                        val animOut = AnimationUtils.loadAnimation(this,
                            getCardPlayOutAnimation(position, result.captured))

                        playCardView.startAnimation(animOut)

                        handler.postDelayed({
                            playCardView.visibility = View.INVISIBLE

                            if (result.isPastra) {
                                // Show Pastra animation
                                showPastraAnimation()
                                Toast.makeText(this, "${currentPlayer.name} направи Пастра!", Toast.LENGTH_SHORT).show()
                                handler.postDelayed({
                                    updateUI()
                                    isAnimating = false

                                    // Process next turn after a delay
                                    handler.postDelayed({
                                        processNextTurn()
                                    }, 500)
                                }, 1500) // Extra delay for Pastra
                            } else {
                                updateUI()
                                isAnimating = false

                                // Process next turn after a delay
                                handler.postDelayed({
                                    processNextTurn()
                                }, 500)
                            }
                        }, animOut.duration)
                    }, cardPlayDelay)
                } else {
                    // If AI play failed, just update UI
                    updateUI()
                    isAnimating = false
                    processNextTurn()
                }
            }, aiTurnDelay)
        } else {
            // If it's human turn, just update UI
            updateUI()
            isAnimating = false
        }
    }

    private fun handleGameEnd() {
        // Set flag to indicate game end has been handled
        gameEndHandled = true

        game.finalizeGame()

        // Update scores one last time
        val scores = game.getTeamScores()
        team1ScoreTextView.text = "Отбор 1: ${scores[0]} точки"
        team2ScoreTextView.text = "Отбор 2: ${scores[1]} точки"

        // Determine winner
        val winningTeam = game.getWinningTeam()
        val message = if (winningTeam != null) {
            "${winningTeam.name} печели с ${scores[winningTeam.id]} точки!"
        } else {
            "Равно е"
        }

        // Show game over dialog
        Toast.makeText(this, "Край на играта! $message", Toast.LENGTH_LONG).show()

        // Get the logged-in user ID - use LoggedUser if userId from intent is invalid
        val userId = intent.getIntExtra("USER_ID", -1).takeIf { it != -1 } ?: LoggedUser.getUserId()

        // If user is logged in, update their achievements and history
        if (userId != -1) {
            // Check if player's team won
            val isPlayerWinner = (winningTeam?.id == 0) // Team 0 is player's team

            // Get player's score (Team 1 score)
            val playerScore = scores[0] ?: 0

            // Get AI's score (Team 2 score)
            val aiScore = scores[1] ?: 0

            // Use GameService to save results and handle rewards
            gameCompletionService.handleGameCompletion(
                userId,
                isPlayerWinner,
                playerScore,
                aiScore,
                lifecycleScope
            )

            // Show new game button
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
                    // Show new game button
                    newGameButton.visibility = View.VISIBLE
                }
                .setCancelable(false)
                .show()

        }

    private suspend fun loadActiveDeckPrefix() {
        if (currentUserId != -1) {
            try {
                // Зареждане на префикс за активното тесте
                activeResourcePrefix = deckManager.getActiveResourcePrefix(currentUserId)
                // Отпечатване на заредения префикс за дебъг
                println("Зареден префикс за активно тесте: $activeResourcePrefix")
            } catch (e: Exception) {
                // При грешка – използване на префикс по подразбиране
                println("Грешка при зареждане на тестето: ${e.message}")
                activeResourcePrefix = "card_"
            }
        } else {
            // По подразбиране за разработка/тестване
            activeResourcePrefix = "card_"
        }
    }
}