package com.solomon.shelter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Characteristic
import com.solomon.shelter.data.database.entities.PlayerCardJoin

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE packId = :packId")
    suspend fun getCardsByPack(packId: Long): List<CharCard>

    @Query("SELECT * FROM cards WHERE packId = :packId AND characteristicId = :characteristicId")
    suspend fun getCardsByPackAndCharacteristic(packId: Long, characteristicId: Long): List<CharCard>

    @Query("SELECT * FROM characteristics")
    suspend fun getAllCharacteristics(): List<Characteristic>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): CharCard?

    @Insert
    suspend fun insert(charCard: CharCard): Long

    @Update
    suspend fun update(charCard: CharCard)

    @Delete
    suspend fun delete(charCard: CharCard)

    // Method to get action cards (assuming they have characteristicId = 0 or some specific ID)
    @Query("SELECT * FROM cards WHERE packId = :packId AND characteristicId = 0")
    suspend fun getActionCards(packId: Long): List<CharCard>

    // Method to assign a card to a player
    @Insert
    suspend fun assignCardToPlayer(playerCardJoin: PlayerCardJoin)

    // Helper method that creates and inserts the PlayerCardJoin
    suspend fun assignCardToPlayer(playerId: Long, cardId: Long) {
        val playerCardJoin = PlayerCardJoin(playerId = playerId, cardId = cardId)
        assignCardToPlayer(playerCardJoin)
    }

    // Method to reveal a characteristic
    @Query("UPDATE player_cards SET isRevealed = 1 WHERE playerId = :playerId AND cardId = :cardId")
    suspend fun revealCharacteristic(playerId: Long, cardId: Long)

    // Get player's cards
    @Query("SELECT pc.*, c.* FROM player_cards pc JOIN cards c ON pc.cardId = c.id WHERE pc.playerId = :playerId")
    suspend fun getPlayerCards(playerId: Long): Map<PlayerCardJoin, CharCard>

    // Insert a characteristic
    @Insert
    suspend fun insertCharacteristic(characteristic: Characteristic): Long
}