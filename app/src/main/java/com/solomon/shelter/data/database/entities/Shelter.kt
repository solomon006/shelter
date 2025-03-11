package com.solomon.shelter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shelters",
    foreignKeys = [
        ForeignKey(
            entity = Pack::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId")]
)
data class Shelter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: Long,
    val name: String,
    val area: Int,
    val duration: Int
)