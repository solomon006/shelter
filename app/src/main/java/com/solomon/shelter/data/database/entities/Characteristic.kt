package com.solomon.shelter.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characteristics")
data class Characteristic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val characteristicName: String
)