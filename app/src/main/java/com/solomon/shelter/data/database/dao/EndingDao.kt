package com.solomon.shelter.data.database.dao


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.solomon.shelter.data.database.entities.Ending

@Dao
interface EndingDao {
    @Query("SELECT * FROM endings WHERE packId = :packId")
    suspend fun getEndingsByPack(packId: Long): List<Ending>

    @Query("SELECT * FROM endings WHERE packId = :packId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomEnding(packId: Long): Ending?

    @Insert
    suspend fun insert(ending: Ending): Long

    @Update
    suspend fun update(ending: Ending)

    @Delete
    suspend fun delete(ending: Ending)
}