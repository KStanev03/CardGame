package com.example.cardgame.datamanager

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cardgame.datamanager.user.User
import com.example.cardgame.datamanager.user.UserDAO
import com.example.cardgame.datamanager.user.UserInfo
import com.example.cardgame.datamanager.user.UserInfoDAO
import com.example.cardgame.datamanager.achievement.Achievement
import com.example.cardgame.datamanager.achievement.AchievementDAO
import com.example.cardgame.datamanager.history.GameHistory
import com.example.cardgame.datamanager.history.GameHistoryDAO
import com.example.cardgame.datamanager.deck.Deck
import com.example.cardgame.datamanager.deck.DeckDAO
import com.example.cardgame.datamanager.deck.UserDeck
import com.example.cardgame.datamanager.deck.UserDeckDAO

@Database(
    entities = [
        User::class, UserInfo::class, Achievement::class, GameHistory::class,
        Deck::class, UserDeck::class
    ],
    version = 8
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDAO(): UserDAO
    abstract fun userInfoDAO(): UserInfoDAO
    abstract fun achievementDAO(): AchievementDAO
    abstract fun gameHistoryDAO(): GameHistoryDAO
    abstract fun deckDAO(): DeckDAO
    abstract fun userDeckDAO(): UserDeckDAO

    companion object {
        private const val DATABASE_NAME = "cardgame.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    ).fallbackToDestructiveMigration() // This will recreate the database if version changes
                        .allowMainThreadQueries()
                        .build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
/*synchronized(this) {
                context.deleteDatabase(DATABASE_NAME)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()

                INSTANCE = instance
                return instance
            }




            synchronized(this) {
                var instance = INSTANCE

                if (instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    ).allowMainThreadQueries().build()
                }
                return instance
            }*/