package com.example.cardgame

import android.content.Context
import com.example.cardgame.datamanager.LoggedUser
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.achievement.AchievementManager
import com.example.cardgame.datamanager.history.GameHistory
import com.example.cardgame.datamanager.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GameCompletionHandler(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val gameHistoryDAO = db.gameHistoryDAO()
    private val userInfoDAO = db.userInfoDAO()
    private val achievementManager = AchievementManager(context)


    fun handleGameCompletion(
        userId: Int,
        isPlayerWinner: Boolean,
        playerScore: Int,
        aiScore: Int,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            val outcome = if (isPlayerWinner) "WIN" else "LOSS"

            val scoreString = "$playerScore-$aiScore"

            val gameHistory = GameHistory(
                userId = userId,
                outcome = outcome,
                opponent = "AI",
                score = scoreString
            )

            withContext(Dispatchers.IO) {

                gameHistoryDAO.insert(gameHistory)

                val userInfo = userInfoDAO.getUserInfoByUserId(userId)
                if (userInfo != null) {
                    val newPoints = userInfo.points + playerScore

                    val newHighScore = if(playerScore > userInfo.highScore) playerScore else userInfo.highScore

                    userInfoDAO.updateUserPoints(userId, newPoints)
                    userInfoDAO.updateUserHighScore(userId, newHighScore)

                    achievementManager.updateAchievements(userId, outcome, playerScore)
                }
            }
        }
    }


    fun updateAchievementsOnly(
        userId: Int,
        isPlayerWinner: Boolean,
        playerScore: Int
    ) {
        val outcome = if (isPlayerWinner) "WIN" else "LOSS"

    }


    fun initializeAchievements(userId: Int, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            achievementManager.initializeAchievementsForUser(userId)
        }
    }
}