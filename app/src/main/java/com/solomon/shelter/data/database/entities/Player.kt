package com.solomon.shelter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val name: String,
    val connectionId: String,
    val userId: String, // Связь с текущим пользователем
    val orderNumber: Int,
    val isEliminated: Boolean,
    val isHost: Boolean = false,
    val isSelected: Boolean = false
)