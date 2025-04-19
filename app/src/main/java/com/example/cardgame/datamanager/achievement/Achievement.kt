package com.example.cardgame.datamanager.achievement

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val goalName: String,
    val targetValue: Int,
    val currentValue: Int = 0,
    val isCompleted: Boolean = false
)
