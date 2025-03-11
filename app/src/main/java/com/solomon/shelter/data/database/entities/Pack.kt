package com.solomon.shelter.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packs")
data class Pack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)