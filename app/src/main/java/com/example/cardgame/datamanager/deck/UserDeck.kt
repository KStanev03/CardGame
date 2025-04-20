package com.example.cardgame.datamanager.deck

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.cardgame.datamanager.user.User

@Entity(
    tableName = "UserDeck",
    primaryKeys = ["user_id", "deck_id"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = arrayOf("uid"),
            childColumns = arrayOf("user_id"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id"), Index("deck_id")]
)
data class UserDeck(
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "deck_id") val deckId: Int,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false
)