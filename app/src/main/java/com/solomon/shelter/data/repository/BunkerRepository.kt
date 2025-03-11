package com.solomon.shelter.data.repository

import com.solomon.shelter.data.database.BunkerDatabase
import com.solomon.shelter.data.database.entities.*
import kotlinx.coroutines.flow.Flow

class BunkerRepository(private val database: BunkerDatabase) {
    private val packDao = database.packDao()
    private val cardDao = database.cardDao()
    private val gameDao = database.gameDao()
    private val playerDao = database.playerDao()

    // Pack methods
    fun getAllPacks(): Flow<List<Pack>> = packDao.getAllPacks()

    suspend fun getPackById(packId: Long): Pack? = packDao.getPackById(packId)

    suspend fun insertPack(pack: Pack): Long = packDao.insert(pack)

    suspend fun updatePack(pack: Pack) = packDao.update(pack)

    suspend fun deletePack(pack: Pack) = packDao.delete(pack)

    // Card methods
    suspend fun getCardsByPack(packId: Long): List<CharCard> = cardDao.getCardsByPack(packId)

    suspend fun getCardsByPackAndCharacteristic(packId: Long, characteristicId: Long): List<CharCard> =
        cardDao.getCardsByPackAndCharacteristic(packId, characteristicId)

    suspend fun getAllCharacteristics(): List<Characteristic> = cardDao.getAllCharacteristics()

    suspend fun getCardById(cardId: Long): CharCard? = cardDao.getCardById(cardId)

    suspend fun insertCard(charCard: CharCard): Long = cardDao.insert(charCard)

    suspend fun updateCard(charCard: CharCard) = cardDao.update(charCard)

    suspend fun deleteCard(charCard: CharCard) = cardDao.delete(charCard)

    suspend fun getActionCards(packId: Long): List<CharCard> = cardDao.getActionCards(packId)

    suspend fun assignCardToPlayer(playerId: Long, cardId: Long) =
        cardDao.assignCardToPlayer(playerId, cardId)

    suspend fun revealCharacteristic(playerId: Long, cardId: Long) =
        cardDao.revealCharacteristic(playerId, cardId)

    suspend fun getPlayerCards(playerId: Long): Map<PlayerCardJoin, CharCard> =
        cardDao.getPlayerCards(playerId)

    // Game methods
    suspend fun createGame(game: Game): Long = gameDao.insert(game)

    suspend fun updateGame(game: Game) = gameDao.update(game)

    suspend fun getGameById(gameId: Long): Game? = gameDao.getGameById(gameId)

    suspend fun getGamesByStatus(status: GameState): List<Game> =
        gameDao.getGamesByStatus(status)

    suspend fun updateGameStatus(gameId: Long, status: GameState) =
        gameDao.updateGameStatus(gameId, status)

    suspend fun updateGameRound(gameId: Long, round: Int) =
        gameDao.updateGameRound(gameId, round)

    suspend fun saveGameState(gameId: Long, stateJson: String) =
        gameDao.saveGameState(gameId, stateJson)

    // Player methods
    suspend fun createPlayer(player: Player): Long = playerDao.insert(player)

    suspend fun updatePlayer(player: Player) = playerDao.update(player)

    suspend fun deletePlayer(player: Player) = playerDao.delete(player)

    suspend fun getPlayerById(playerId: Long): Player? = playerDao.getPlayerById(playerId)

    suspend fun getPlayersByGameId(gameId: Long): List<Player> =
        playerDao.getPlayersByGameId(gameId)

    suspend fun getHostForGame(gameId: Long): Player? =
        playerDao.getHostForGame(gameId)

    suspend fun markPlayerEliminated(playerId: Long) =
        playerDao.markPlayerEliminated(playerId)

    suspend fun getEliminatedPlayersCount(gameId: Long): Int =
        playerDao.getEliminatedPlayersCount(gameId)

    suspend fun getRemainingPlayers(gameId: Long): List<Player> =
        playerDao.getRemainingPlayers(gameId)

    suspend fun updatePlayerOrderNumber(playerId: Long, orderNumber: Int) =
        playerDao.updatePlayerOrderNumber(playerId, orderNumber)

    // Shelter methods
    suspend fun getSheltersByPack(packId: Long): List<Shelter> =
        database.shelterDao().getSheltersByPack(packId)

    suspend fun getRandomShelter(packId: Long): Shelter? =
        database.shelterDao().getRandomShelter(packId)

    // Catastrophe methods
    suspend fun getCatastrophesByPack(packId: Long): List<Catastrophe> =
        database.catastropheDao().getCatastrophesByPack(packId)

    suspend fun getRandomCatastrophe(packId: Long): Catastrophe? =
        database.catastropheDao().getRandomCatastrophe(packId)

    // Ending methods
    suspend fun getEndingsByPack(packId: Long): List<Ending> =
        database.endingDao().getEndingsByPack(packId)

    suspend fun getRandomEnding(packId: Long): Ending? =
        database.endingDao().getRandomEnding(packId)
}