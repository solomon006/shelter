package com.solomon.shelter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.solomon.shelter.data.database.entities.Catastrophe

@Dao
interface CatastropheDao {
    @Query("SELECT * FROM catastrophes WHERE packId = :packId")
    suspend fun getCatastrophesByPack(packId: Long): List<Catastrophe>

    @Query("SELECT * FROM catastrophes WHERE packId = :packId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomCatastrophe(packId: Long): Catastrophe?

    @Insert
    suspend fun insert(catastrophe: Catastrophe): Long

    @Update
    suspend fun update(catastrophe: Catastrophe)

    @Delete
    suspend fun delete(catastrophe: Catastrophe)
}