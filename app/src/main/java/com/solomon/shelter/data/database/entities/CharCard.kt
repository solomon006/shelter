package com.solomon.shelter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Pack::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Characteristic::class,
            parentColumns = ["id"],
            childColumns = ["characteristicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("packId"),
        Index("characteristicId")
    ]
)
data class CharCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val info: String,
    val packId: Long,
    val characteristicId: Long,
    val utilityIndex: Int
)