package com.solomon.shelter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "player_cards",
    primaryKeys = ["playerId", "cardId"],
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CharCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("playerId"),
        Index("cardId")
    ]
)
data class PlayerCardJoin(
    val playerId: Long,
    val cardId: Long,
    val isRevealed: Boolean = false  // Раскрыта ли характеристика
)