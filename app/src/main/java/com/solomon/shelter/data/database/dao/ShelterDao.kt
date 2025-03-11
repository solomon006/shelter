package com.solomon.shelter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.solomon.shelter.data.database.entities.Shelter

@Dao
interface ShelterDao {
    @Query("SELECT * FROM shelters WHERE packId = :packId")
    suspend fun getSheltersByPack(packId: Long): List<Shelter>

    @Query("SELECT * FROM shelters WHERE packId = :packId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomShelter(packId: Long): Shelter?

    @Insert
    suspend fun insert(shelter: Shelter): Long

    @Update
    suspend fun update(shelter: Shelter)

    @Delete
    suspend fun delete(shelter: Shelter)
}