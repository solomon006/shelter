package com.solomon.shelter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.solomon.shelter.data.database.entities.Player

@Dao
interface PlayerDao {
    @Insert
    suspend fun insert(player: Player): Long

    @Update
    suspend fun update(player: Player)

    @Delete
    suspend fun delete(player: Player)

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getPlayerById(playerId: Long): Player?

    @Query("SELECT * FROM players WHERE gameId = :gameId")
    suspend fun getPlayersByGameId(gameId: Long): List<Player>

    @Query("SELECT * FROM players WHERE gameId = :gameId AND isHost = 1 LIMIT 1")
    suspend fun getHostForGame(gameId: Long): Player?

    @Query("UPDATE players SET isEliminated = 1 WHERE id = :playerId")
    suspend fun markPlayerEliminated(playerId: Long)

    @Query("SELECT COUNT(*) FROM players WHERE gameId = :gameId AND isEliminated = 1")
    suspend fun getEliminatedPlayersCount(gameId: Long): Int

    @Query("SELECT * FROM players WHERE gameId = :gameId AND isEliminated = 0")
    suspend fun getRemainingPlayers(gameId: Long): List<Player>

    @Query("UPDATE players SET orderNumber = :orderNumber WHERE id = :playerId")
    suspend fun updatePlayerOrderNumber(playerId: Long, orderNumber: Int)
}