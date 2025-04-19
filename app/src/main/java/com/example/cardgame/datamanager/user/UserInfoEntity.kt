package com.example.cardgame.datamanager.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "UserInfo",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = arrayOf("uid"),
            childColumns = arrayOf("user_id"),
            onDelete = ForeignKey.CASCADE
        )
    ])
data class UserInfo(
    @PrimaryKey(autoGenerate = true) val profileId: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    @ColumnInfo(name = "avatar", defaultValue = "panda") val avatar: String = "panda",
    @ColumnInfo(name = "points", defaultValue = "0") val points: Int = 0,
    @ColumnInfo(name = "money", defaultValue = "0") val money: Int = 0,
    @ColumnInfo(name = "high_score", defaultValue = "0") val highScore: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserInfo

        if (profileId != other.profileId) return false
        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (avatar != other.avatar) return false
        if (points != other.points) return false
        if (money != other.money) return false
        if (highScore != other.highScore) return false

        return true
    }

    override fun hashCode(): Int {
        var result = profileId
        result = 31 * result + userId
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + avatar.hashCode()
        result = 31 * result + points
        result = 31 * result + money
        result = 31 * result + highScore
        return result
    }
}