package com.solomon.shelter.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.GameState

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: Game): Long

    @Update
    suspend fun update(game: Game)

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: Long): Game?

    @Query("SELECT * FROM games WHERE status = :status")
    suspend fun getGamesByStatus(status: GameState): List<Game>

    @Query("UPDATE games SET status = :status WHERE id = :gameId")
    suspend fun updateGameStatus(gameId: Long, status: GameState)

    @Query("UPDATE games SET currentRound = :round WHERE id = :gameId")
    suspend fun updateGameRound(gameId: Long, round: Int)

    // Save game state as JSON
    @Query("UPDATE games SET gameStateJson = :stateJson WHERE id = :gameId")
    suspend fun saveGameState(gameId: Long, stateJson: String)
}