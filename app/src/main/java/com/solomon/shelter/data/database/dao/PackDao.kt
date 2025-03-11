package com.solomon.shelter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.solomon.shelter.data.database.entities.Pack
import kotlinx.coroutines.flow.Flow

@Dao
interface PackDao {
    @Query("SELECT * FROM packs")
    fun getAllPacks(): Flow<List<Pack>>

    @Query("SELECT * FROM packs WHERE id = :packId")
    suspend fun getPackById(packId: Long): Pack?

    @Insert
    suspend fun insert(pack: Pack): Long

    @Update
    suspend fun update(pack: Pack)

    @Delete
    suspend fun delete(pack: Pack)
}