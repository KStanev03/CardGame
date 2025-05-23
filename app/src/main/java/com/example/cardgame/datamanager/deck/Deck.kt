package com.example.cardgame.datamanager.deck

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cardgame.R

@Entity(tableName = "Deck")
data class Deck(
    @PrimaryKey(autoGenerate = true) val deckId: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "price") val price: Int,
    @ColumnInfo(name = "prefix") val prefix: String,  // Used for resource naming
    @ColumnInfo(name = "preview_image_res_id") val previewImageResId: Int
) {
    companion object {
        // Predefined deck options
        val PREDEFINED_DECKS = listOf(
            Deck(
                deckId = 1,
                name = "Класическо тесте",
                description = "Класически дизайн на карти за игра",
                price = 0,
                prefix = "card_",
                previewImageResId = R.drawable.card_king_of_hearts
            ),
            Deck(
                deckId = 2,
                name = "Unicorn тесте",
                description = "Елегантен дизайн за вашите карти",
                price = 10,
                prefix = "unicorn_",
                previewImageResId = R.drawable.unicorn_king_of_hearts
            )
        )
    }
}