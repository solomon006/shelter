package com.solomon.shelter.game

import android.util.Log
import com.solomon.shelter.data.models.GameResult
import com.solomon.shelter.data.repository.BunkerRepository
import com.solomon.shelter.network.messages.GameResponse
import com.solomon.shelter.network.server.GameServer.ConnectionManager
import com.google.gson.Gson
import com.solomon.shelter.data.database.entities.BalanceLevel
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.GameState
import com.solomon.shelter.data.database.entities.PlayerCardJoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

class GameInstance(
    private val gameId: Long,
    private var game: Game,
    private val repository: BunkerRepository,
    private val connectionManager: ConnectionManager
) {
    private val TAG = "GameInstance"
    private var currentRound = 1
    private var discussionPhase = true
    private val votes = ConcurrentHashMap<Long, Long>() // voterId to targetId
    private val roundTimer = Timer()

    suspend fun startGame() {
        try {
            // Deal cards to players
            dealCardsToPlayers()

            // Get a random catastrophe, shelter, and ending
            val catastrophe = repository.getRandomCatastrophe(game.packId)
            val shelter = repository.getRandomShelter(game.packId)
            val ending = repository.getRandomEnding(game.packId)

            // Update game with scenario info
            game = game.copy(
                catastrophe = catastrophe?.text,
                shelter = shelter?.name,
                ending = ending?.text
            )
            repository.updateGame(game)

            // Start first round
            startRound(1)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting game", e)
            throw e
        }
    }

    private suspend fun dealCardsToPlayers() {
        val players = repository.getPlayersByGameId(gameId)
        val characteristics = repository.getAllCharacteristics()

        // Get cards for each characteristic type
        val cardsByCharacteristic = characteristics.associate { characteristic ->
            val cards = repository.getCardsByPackAndCharacteristic(game.packId, characteristic.id)
            characteristic.id to cards
        }

        // Apply balance if needed
        val adjustedCards = when (game.balance) {
            BalanceLevel.DISABLED -> cardsByCharacteristic
            BalanceLevel.MEDIUM -> balanceCards(cardsByCharacteristic, 3)
            BalanceLevel.STRICT -> balanceCards(cardsByCharacteristic, 2)
        }

        // Deal cards to players
        for (player in players) {
            for ((characteristicId, cards) in adjustedCards) {
                if (cards.isNotEmpty()) {
                    val randomCard = cards.random()
                    // Assign card to player
                    repository.assignCardToPlayer(player.id, randomCard.id)
                }
            }

            // Also assign one action card
            val actionCards = repository.getActionCards(game.packId)
            if (actionCards.isNotEmpty()) {
                repository.assignCardToPlayer(player.id, actionCards.random().id)
            }

            // Send player their cards
            sendPlayerCards(player.id)
        }
    }

    private fun balanceCards(cardsByCharacteristic: Map<Long, List<CharCard>>, maxDeviation: Int): Map<Long, List<CharCard>> {
        val result = mutableMapOf<Long, List<CharCard>>()

        for ((characteristicId, cards) in cardsByCharacteristic) {
            if (cards.isEmpty()) {
                result[characteristicId] = emptyList()
                continue
            }

            // Calculate average utility index
            val avgUtility = cards.map { it.utilityIndex }.average()

            // Filter cards that are within the acceptable deviation range
            val balancedCards = cards.filter {
                Math.abs(it.utilityIndex - avgUtility) <= maxDeviation
            }

            result[characteristicId] = if (balancedCards.isNotEmpty()) balancedCards else cards
        }

        return result
    }

    private suspend fun sendPlayerCards(playerId: Long) {
        val cards = repository.getPlayerCards(playerId)

        // Convert to list of cards
        val cardsList = cards.values.toList()

        // Send to player
        connectionManager.sendToPlayer(playerId, GameResponse.GameState(
            game = game,
            players = repository.getPlayersByGameId(gameId),
            yourCharCards = cardsList
        ))
    }

    private suspend fun startRound(round: Int) {
        currentRound = round

        // Update game in database
        repository.updateGameRound(gameId, round)

        // Update local game object
        game = game.copy(currentRound = round)

        // Start discussion phase
        startDiscussionPhase()
    }

    private suspend fun startDiscussionPhase() {
        discussionPhase = true
        votes.clear()

        // Notify all players
        connectionManager.sendToGame(gameId, GameResponse.RoundStarted(
            round = currentRound,
            phase = "discussion",
            timeRemaining = game.discussionTime
        ))

        // Schedule end of discussion phase
        schedulePhasEnd(game.discussionTime * 1000L) {
            startVotingPhase()
        }
    }

    private suspend fun startVotingPhase() {
        discussionPhase = false

        // Notify all players
        connectionManager.sendToGame(gameId, GameResponse.RoundStarted(
            round = currentRound,
            phase = "voting",
            timeRemaining = game.voteTime
        ))

        // Schedule end of voting phase
        schedulePhasEnd(game.voteTime * 1000L) {
            endVotingPhase()
        }
    }

    private fun schedulePhasEnd(durationMs: Long, onEnd: suspend () -> Unit) {
        roundTimer.cancel()
        roundTimer.purge()

        roundTimer.schedule(object : TimerTask() {
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    onEnd()
                }
            }
        }, durationMs)
    }

    suspend fun revealCharacteristic(playerId: Long, cardId: Long) {
        try {
            // Verify player has this card
            val playerCards = repository.getPlayerCards(playerId)
            val card = playerCards.values.find { it.id == cardId }
                ?: throw IllegalArgumentException("Player does not have this card")

            // Get the player card join
            val playerCardJoin = playerCards.keys.find { it.cardId == cardId }
                ?: throw IllegalArgumentException("Player card join not found")

            // Check if already revealed
            if (playerCardJoin.isRevealed) {
                return
            }

            // Mark card as revealed
            repository.revealCharacteristic(playerId, cardId)

            // Notify all players
            connectionManager.sendToGame(gameId, GameResponse.CharacteristicRevealed(
                playerId = playerId,
                cardId = cardId,
                charCard = card
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error revealing characteristic", e)
            throw e
        }
    }

    suspend fun vote(voterId: Long, targetId: Long) {
        try {
            if (discussionPhase) {
                throw IllegalStateException("Cannot vote during discussion phase")
            }

            // Verify players exist
            val players = repository.getPlayersByGameId(gameId)
            if (!players.any { it.id == voterId } || !players.any { it.id == targetId }) {
                throw IllegalArgumentException("Invalid player IDs")
            }

            // Verify target player is not eliminated
            if (players.find { it.id == targetId }?.isEliminated == true) {
                throw IllegalArgumentException("Cannot vote for eliminated player")
            }

            // Record vote
            votes[voterId] = targetId

            // Count votes for each player
            val voteCounts = players.associate { player ->
                player.id to votes.count { it.value == player.id }
            }

            // Notify all players about the vote update
            connectionManager.sendToGame(gameId, GameResponse.VoteUpdate(voteCounts))
        } catch (e: Exception) {
            Log.e(TAG, "Error voting", e)
            throw e
        }
    }

    suspend fun useAction(playerId: Long, actionCardId: Long, targetPlayerId: Long) {
        // TODO: Implement action card effects
        Log.d(TAG, "Action used: $playerId used $actionCardId on $targetPlayerId")
    }

    private suspend fun endVotingPhase() {
        try {
            // Count votes for each player
            val players = repository.getPlayersByGameId(gameId)
            val activePlayers = players.filter { !it.isEliminated }

            val voteCounts = activePlayers.associate { player ->
                player.id to votes.count { it.value == player.id }
            }

            // Find player with most votes
            val maxVotes = voteCounts.maxByOrNull { it.value }?.value ?: 0
            val playersWithMaxVotes = voteCounts.filter { it.value == maxVotes }.keys

            // If there's a tie, handle it by selecting a random player
            val eliminatedPlayerId = if (playersWithMaxVotes.size > 1) {
                playersWithMaxVotes.random()
            } else {
                playersWithMaxVotes.firstOrNull()
            }

            if (eliminatedPlayerId != null && maxVotes > 0) {
                // Mark player as eliminated
                repository.markPlayerEliminated(eliminatedPlayerId)

                // Notify all players
                connectionManager.sendToGame(gameId, GameResponse.RoundEnded(
                    eliminatedPlayerId = eliminatedPlayerId,
                    nextRound = currentRound + 1
                ))

                // Check if game should end
                val eliminatedCount = repository.getEliminatedPlayersCount(gameId)
                val requiredEliminations = Game.getTotalEliminated(game.numberOfPlayers)

                if (eliminatedCount >= requiredEliminations) {
                    endGame()
                } else {
                    // Start next round
                    startRound(currentRound + 1)
                }
            } else {
                // No one was eliminated, start a revote
                startVotingPhase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ending voting phase", e)
            throw e
        }
    }

    private suspend fun endGame() {
        try {
            // Update game status
            repository.updateGameStatus(gameId, GameState.FINISHED)

            // Create game result
            val allPlayers = repository.getPlayersByGameId(gameId)
            val survivors = allPlayers.filter { !it.isEliminated }
            val eliminated = allPlayers.filter { it.isEliminated }

            // Get all cards for all players
            val allPlayerCards = mutableMapOf<Long, Map<PlayerCardJoin, CharCard>>()
            allPlayers.forEach { player ->
                allPlayerCards[player.id] = repository.getPlayerCards(player.id)
            }

            // Get revealed and hidden cards
            val revealedCards = mutableMapOf<Long, List<CharCard>>()
            val hiddenCards = mutableMapOf<Long, List<CharCard>>()

            allPlayers.forEach { player ->
                val playerCards = allPlayerCards[player.id] ?: emptyMap()

                val revealed = playerCards.filter { it.key.isRevealed }.map { it.value }
                val hidden = playerCards.filter { !it.key.isRevealed }.map { it.value }

                revealedCards[player.id] = revealed
                hiddenCards[player.id] = hidden
            }

            // Create result object
            val result = GameResult(
                gameId = gameId,
                endTime = System.currentTimeMillis(),
                survivors = survivors,
                eliminated = eliminated,
                revealedCards = revealedCards,
                hiddenCards = hiddenCards,
                endState = Gson().toJson(game),
                catastrophe = game.catastrophe,
                shelter = game.shelter,
                ending = game.ending
            )

            // Notify all players
            connectionManager.sendToGame(gameId, GameResponse.GameEnded(result))
        } catch (e: Exception) {
            Log.e(TAG, "Error ending game", e)
            throw e
        }
    }
}