package com.example.cardgame.services

import android.content.Context
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.achievement.AchievementManager
import com.example.cardgame.datamanager.history.GameHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GameCompletionService(private val context: Context) {

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

            withContext(Dispatchers.IO) {
                val gameHistory = GameHistory(
                    userId = userId,
                    outcome = outcome,
                    opponent = "AI",
                    score = scoreString
                )

                gameHistoryDAO.insert(gameHistory)

                val userInfo = userInfoDAO.getUserInfoByUserId(userId)
                userInfo?.let {
                    val moneyEarned = calculateMoneyReward(isPlayerWinner, playerScore)

                    val newPoints = it.points + playerScore
                    userInfoDAO.updateUserPoints(userId, newPoints)

                    val newMoney = it.money + moneyEarned
                    userInfoDAO.updateUserMoney(userId, newMoney)

                    if (playerScore > it.highScore) {
                        userInfoDAO.updateUserHighScore(userId, playerScore)
                    }

                    achievementManager.updateAchievements(userId, outcome, playerScore)
                }
            }
        }
    }


    private fun calculateMoneyReward(isWin: Boolean, points: Int): Int {
        val baseReward = if (isWin) 10 else 5
        val pointsBonus = points / 2
        return baseReward + pointsBonus
    }


    fun initializeAchievements(userId: Int, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            achievementManager.initializeAchievementsForUser(userId)
        }
    }
}