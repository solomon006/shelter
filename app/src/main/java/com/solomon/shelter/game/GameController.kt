package com.solomon.shelter.game

import android.content.Context
import android.util.Log
import com.solomon.shelter.data.database.BunkerDatabase
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.GameState
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.data.models.GameSettings
import com.solomon.shelter.data.repository.BunkerRepository
import com.solomon.shelter.network.client.GameResponse
import com.solomon.shelter.network.server.GameServer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * Game controller for managing game state
 */
class GameController(
    private val context: Context,
    private val connectionManager: GameServer.ConnectionManager
) {
    private val TAG = "GameController"
    private val repository = BunkerRepository(BunkerDatabase.getDatabase(context))
    private val activeGames = ConcurrentHashMap<Long, GameInstance>()
    private val playerGameMap = ConcurrentHashMap<Long, Long>() // playerId to gameId
    private val mutex = Mutex()

    suspend fun createGame(connectionId: String, settings: GameSettings): Long = mutex.withLock {
        try {
            // Get or create player
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: createPlayer(connectionId, "Host", true)

            // Create game in database
            val game = Game(
                hostId = playerId,
                numberOfPlayers = settings.numberOfPlayers,
                packId = settings.selectedPackId,
                discussionTime = settings.discussionTime,
                voteTime = settings.voteTime,
                voteType = settings.voteType,
                status = GameState.LOBBY,
                balance = settings.balanceLevel
            )

            val gameId = repository.createGame(game)

            // Create game instance
            val gameInstance = GameInstance(gameId, game, repository, connectionManager)
            activeGames[gameId] = gameInstance

            // Associate player with game
            playerGameMap[playerId] = gameId
            connectionManager.addPlayerToGame(gameId, connectionId)

            // Send response
            connectionManager.sendToPlayer(playerId, GameResponse.GameCreated(gameId))

            return gameId
        } catch (e: Exception) {
            Log.e(TAG, "Error creating game", e)
            throw e
        }
    }

    private suspend fun createPlayer(connectionId: String, name: String, isHost: Boolean): Long {
        val userId = UUID.randomUUID().toString()
        val player = Player(
            gameId = 0, // Will be updated after game creation
            name = name,
            connectionId = connectionId,
            userId = userId,
            orderNumber = -1,
            isEliminated = false,
            isHost = isHost
        )

        val playerId = repository.createPlayer(player)
        connectionManager.registerPlayerConnection(playerId, connectionId)
        return playerId
    }

    suspend fun joinGame(connectionId: String, gameId: Long, playerName: String): Player = mutex.withLock {
        try {
            val game = repository.getGameById(gameId)
                ?: throw IllegalArgumentException("Game not found")

            if (game.status != GameState.LOBBY) {
                throw IllegalStateException("Cannot join game in progress")
            }

            // Check if player count limit has been reached
            val currentPlayers = repository.getPlayersByGameId(gameId)
            if (currentPlayers.size >= game.numberOfPlayers) {
                throw IllegalStateException("Game is full")
            }

            // Create new player
            val player = Player(
                gameId = gameId,
                name = playerName,
                connectionId = connectionId,
                userId = UUID.randomUUID().toString(),
                orderNumber = -1, // Not selected yet
                isEliminated = false,
                isHost = false
            )

            val playerId = repository.createPlayer(player)
            val createdPlayer = player.copy(id = playerId)

            // Register connection
            connectionManager.registerPlayerConnection(playerId, connectionId)
            connectionManager.addPlayerToGame(gameId, connectionId)
            playerGameMap[playerId] = gameId

            // Send response to new player
            connectionManager.sendToPlayer(playerId, GameResponse.GameJoined(gameId, createdPlayer))

            // Notify other players
            connectionManager.sendToGame(gameId, GameResponse.PlayerJoined(createdPlayer), playerId)

            return createdPlayer
        } catch (e: Exception) {
            Log.e(TAG, "Error joining game", e)
            throw e
        }
    }

    suspend fun startGame(connectionId: String, gameId: Long) = mutex.withLock {
        try {
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: throw IllegalStateException("Player not found for connection")

            val game = repository.getGameById(gameId)
                ?: throw IllegalArgumentException("Game not found")

            // Verify host is calling this
            if (game.hostId != playerId) {
                throw IllegalStateException("Only host can start the game")
            }

            // Verify we have enough players
            val players = repository.getPlayersByGameId(gameId)
            if (players.size < 4) {
                throw IllegalStateException("Need at least 4 players to start")
            }

            // Check that all players have selected order numbers
            if (players.any { it.orderNumber == -1 }) {
                throw IllegalStateException("All players must select order number")
            }

            // Update game status
            repository.updateGameStatus(gameId, GameState.IN_PROGRESS)

            // Start the game
            activeGames[gameId]?.startGame()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting game", e)
            throw e
        }
    }

    suspend fun revealCharacteristic(connectionId: String, gameId: Long, characteristicId: Long) {
        try {
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: throw IllegalStateException("Player not found for connection")

            activeGames[gameId]?.revealCharacteristic(playerId, characteristicId)
        } catch (e: Exception) {
            Log.e(TAG, "Error revealing characteristic", e)
            throw e
        }
    }

    suspend fun vote(connectionId: String, gameId: Long, targetPlayerId: Long) {
        try {
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: throw IllegalStateException("Player not found for connection")

            activeGames[gameId]?.vote(playerId, targetPlayerId)
        } catch (e: Exception) {
            Log.e(TAG, "Error voting", e)
            throw e
        }
    }

    suspend fun useAction(connectionId: String, gameId: Long, actionCardId: Long, targetPlayerId: Long) {
        try {
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: throw IllegalStateException("Player not found for connection")

            activeGames[gameId]?.useAction(playerId, actionCardId, targetPlayerId)
        } catch (e: Exception) {
            Log.e(TAG, "Error using action", e)
            throw e
        }
    }

    suspend fun leaveGame(connectionId: String, gameId: Long) {
        try {
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: throw IllegalStateException("Player not found for connection")

            val game = repository.getGameById(gameId)
                ?: throw IllegalArgumentException("Game not found")

            // If player is host and game is in lobby, delete the game
            if (game.hostId == playerId && game.status == GameState.LOBBY) {
                // Delete game and all players
                repository.getPlayersByGameId(gameId).forEach { player ->
                    repository.deletePlayer(player)
                }
                // TODO: Delete game from database

                // Remove game instance
                activeGames.remove(gameId)

                // Notify all players in the
            connectionManager.sendToGame(gameId, GameResponse.GameEnded(null))
            } else {
                // Otherwise just remove the player
                val player = repository.getPlayerById(playerId)
                if (player != null) {
                    repository.deletePlayer(player)

                    // Notify other players
                    connectionManager.sendToGame(gameId, GameResponse.PlayerLeft(playerId))
                }
            }

            // Remove player from game
            connectionManager.removePlayerFromGame(gameId, connectionId)
            playerGameMap.remove(playerId)
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving game", e)
            throw e
        }
    }

    suspend fun kickPlayer(connectionId: String, gameId: Long, targetPlayerId: Long) {
        try {
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: throw IllegalStateException("Player not found for connection")

            val game = repository.getGameById(gameId)
                ?: throw IllegalArgumentException("Game not found")

            // Verify host is calling this
            if (game.hostId != playerId) {
                throw IllegalStateException("Only host can kick players")
            }

            // Get player's connection
            val targetPlayer = repository.getPlayerById(targetPlayerId)
            if (targetPlayer != null) {
                // Remove player from game
                repository.deletePlayer(targetPlayer)

                // Notify other players
                connectionManager.sendToGame(gameId, GameResponse.PlayerLeft(targetPlayerId))

                // Notify kicked player
                connectionManager.sendToPlayer(targetPlayerId, GameResponse.Error("You have been kicked from the game"))

                // Remove player from connection manager
                connectionManager.removePlayerFromGame(gameId, targetPlayer.connectionId)
                playerGameMap.remove(targetPlayerId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error kicking player", e)
            throw e
        }
    }

    suspend fun selectOrderNumber(connectionId: String, gameId: Long, orderNumber: Int) {
        try {
            val playerId = connectionManager.getPlayerIdForConnection(connectionId)
                ?: throw IllegalStateException("Player not found for connection")

            val game = repository.getGameById(gameId)
                ?: throw IllegalArgumentException("Game not found")

            if (game.status != GameState.LOBBY) {
                throw IllegalStateException("Cannot change order number after game has started")
            }

            // Check if order number is already taken
            val players = repository.getPlayersByGameId(gameId)
            if (players.any { it.orderNumber == orderNumber && it.id != playerId }) {
                throw IllegalStateException("Order number already taken")
            }

            // Update player's order number
            repository.updatePlayerOrderNumber(playerId, orderNumber)

            // Get updated player
            val updatedPlayer = repository.getPlayerById(playerId)
                ?: throw IllegalStateException("Player not found")

            // Notify all players
            connectionManager.sendToGame(gameId, GameResponse.PlayerJoined(updatedPlayer))
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting order number", e)
            throw e
        }
    }
}