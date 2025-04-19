package com.example.cardgame.domain.model

data class PlayResult(
    val success: Boolean,
    val message: String,
    val captured: Boolean = false,
    val isPastra: Boolean = false,
    val pastraPoints: Int = 0,
    val capturedCards: List<Card> = emptyList()
)
