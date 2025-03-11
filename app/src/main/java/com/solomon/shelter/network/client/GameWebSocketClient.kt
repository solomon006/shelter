package com.solomon.shelter.network.client

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.solomon.shelter.data.database.entities.BalanceLevel
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.GameState
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.data.database.entities.VoteType
import com.solomon.shelter.data.models.GameResult
import com.solomon.shelter.data.models.GameSettings
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText

// Message Types for WebSocket Communication
sealed class GameMessage {
    data class CreateGame(val settings: GameSettings) : GameMessage()
    data class JoinGame(val gameId: Long, val playerName: String) : GameMessage()
    data class StartGame(val gameId: Long) : GameMessage()
    data class RevealCharacteristic(val gameId: Long, val characteristicId: Long) : GameMessage()
    data class Vote(val gameId: Long, val targetPlayerId: Long) : GameMessage()
    data class UseAction(val gameId: Long, val actionCardId: Long, val targetPlayerId: Long) : GameMessage()
    data class LeaveGame(val gameId: Long) : GameMessage()
    data class KickPlayer(val gameId: Long, val playerId: Long) : GameMessage()
    data class SelectOrderNumber(val gameId: Long, val orderNumber: Int) : GameMessage()

    // Message wrapper for serialization
    data class MessageWrapper(val type: String, val data: String)
}

// Response Types for WebSocket Communication
sealed class GameResponse {
    data class GameCreated(val gameId: Long) : GameResponse()
    data class GameJoined(val gameId: Long, val player: Player) : GameResponse()
    data class GameState(val game: Game, val players: List<Player>, val yourCharCards: List<CharCard>) : GameResponse()
    data class PlayerJoined(val player: Player) : GameResponse()
    data class PlayerLeft(val playerId: Long) : GameResponse()
    data class CharacteristicRevealed(val playerId: Long, val cardId: Long, val charCard: CharCard) : GameResponse()
    data class VoteUpdate(val votes: Map<Long, Int>) : GameResponse()
    data class RoundStarted(val round: Int, val phase: String, val timeRemaining: Int) : GameResponse()
    data class RoundEnded(val eliminatedPlayerId: Long, val nextRound: Int) : GameResponse()
    data class GameEnded(val result: GameResult?) : GameResponse()
    data class Error(val message: String) : GameResponse()

    // Response wrapper for deserialization
    data class ResponseWrapper(val type: String, val data: String)
}

// Game Client State
data class GameClientState(
    val currentGame: Game? = null,
    val currentPlayer: Player? = null,
    val players: List<Player> = emptyList(),
    val myCharCards: List<CharCard> = emptyList(),
    val revealedCards: Map<Long, List<CharCard>> = emptyMap(), // playerId to cards
    val currentRound: Int = 0,
    val currentPhase: String = "",
    val timeRemaining: Int = 0,
    val votes: Map<Long, Int> = emptyMap(), // playerId to vote count
    val myVote: Long? = null,
    val gameResult: GameResult? = null,
    val error: String? = null,
    val isConnected: Boolean = false
)

// WebSocket Client
class GameWebSocketClient(
    private val context: Context,
    private val baseUrl: String,
    private val userId: String
) {
    private val TAG = "GameWebSocketClient"
    private val gson = Gson()
    private var client: HttpClient? = null
    private var session: WebSocketSession? = null
    private var connectionJob: Job? = null

    private val _gameState = MutableStateFlow(GameClientState())
    val gameState: StateFlow<GameClientState> = _gameState

    private val messageChannel = Channel<GameMessage>(Channel.UNLIMITED)

    // Connect to WebSocket server
    suspend fun connect() {
        if (isConnected()) return

        try {
            client = HttpClient {
                install(WebSockets)
            }

            connectionJob = CoroutineScope(Dispatchers.IO).launch {
                client?.webSocket(
                    method = io.ktor.http.HttpMethod.Get,
                    host = baseUrl,
                    path = "/game?userId=$userId"
                ) {
                    session = this

                    // Start receiving messages
                    val receiveJob = launch {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                processResponse(text)
                            }
                        }
                    }

                    // Start sending messages
                    val sendJob = launch {
                        messageChannel.consumeEach { message ->
                            sendMessage(message)
                        }
                    }

                    _gameState.value = _gameState.value.copy(isConnected = true)

                    // Wait for jobs to complete
                    receiveJob.join()
                    sendJob.join()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
            _gameState.value = _gameState.value.copy(
                error = "Failed to connect: ${e.message}",
                isConnected = false
            )
        }
    }

    // Disconnect from WebSocket server
    suspend fun disconnect() {
        connectionJob?.cancel()
        session?.close()
        client?.close()

        session = null
        client = null
        connectionJob = null

        _gameState.value = _gameState.value.copy(isConnected = false)
    }

    // Check if connected
    fun isConnected(): Boolean {
        return session != null && connectionJob?.isActive == true
    }

    // Send a message to the server
    private suspend fun sendMessage(message: GameMessage) {
        if (!isConnected()) {
            _gameState.value = _gameState.value.copy(
                error = "Not connected to server"
            )
            return
        }

        val session = this.session ?: return

        try {
            // Create wrapper for message type
            val type = when (message) {
                is GameMessage.CreateGame -> "CreateGame"
                is GameMessage.JoinGame -> "JoinGame"
                is GameMessage.StartGame -> "StartGame"
                is GameMessage.RevealCharacteristic -> "RevealCharacteristic"
                is GameMessage.Vote -> "Vote"
                is GameMessage.UseAction -> "UseAction"
                is GameMessage.LeaveGame -> "LeaveGame"
                is GameMessage.KickPlayer -> "KickPlayer"
                is GameMessage.SelectOrderNumber -> "SelectOrderNumber"
            }

            val data = gson.toJson(message)
            val wrapper = GameMessage.MessageWrapper(type, data)
            val json = gson.toJson(wrapper)

            session.send(Frame.Text(json))
        } catch (e: Exception) {
            Log.e(TAG, "Send error", e)
            _gameState.value = _gameState.value.copy(
                error = "Failed to send message: ${e.message}"
            )
        }
    }

    // Process responses from the server
    private fun processResponse(responseText: String) {
        try {
            val wrapper = gson.fromJson(responseText, GameResponse.ResponseWrapper::class.java)

            val response: GameResponse = when (wrapper.type) {
                "GameCreated" -> gson.fromJson(wrapper.data, GameResponse.GameCreated::class.java)
                "GameJoined" -> gson.fromJson(wrapper.data, GameResponse.GameJoined::class.java)
                "GameState" -> gson.fromJson(wrapper.data, GameResponse.GameState::class.java)
                "PlayerJoined" -> gson.fromJson(wrapper.data, GameResponse.PlayerJoined::class.java)
                "PlayerLeft" -> gson.fromJson(wrapper.data, GameResponse.PlayerLeft::class.java)
                "CharacteristicRevealed" -> gson.fromJson(wrapper.data, GameResponse.CharacteristicRevealed::class.java)
                "VoteUpdate" -> gson.fromJson(wrapper.data, GameResponse.VoteUpdate::class.java)
                "RoundStarted" -> gson.fromJson(wrapper.data, GameResponse.RoundStarted::class.java)
                "RoundEnded" -> gson.fromJson(wrapper.data, GameResponse.RoundEnded::class.java)
                "GameEnded" -> gson.fromJson(wrapper.data, GameResponse.GameEnded::class.java)
                "Error" -> gson.fromJson(wrapper.data, GameResponse.Error::class.java)
                else -> GameResponse.Error("Unknown response type: ${wrapper.type}")
            }

            updateGameState(response)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Parse error", e)
            _gameState.value = _gameState.value.copy(
                error = "Invalid response format: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Process error", e)
            _gameState.value = _gameState.value.copy(
                error = "Error processing response: ${e.message}"
            )
        }
    }

    // Update game state based on response
    private fun updateGameState(response: GameResponse) {
        when (response) {
            is GameResponse.GameCreated -> {
                _gameState.value = _gameState.value.copy(
                    error = null
                )
            }

            is GameResponse.GameJoined -> {
                _gameState.value = _gameState.value.copy(
                    currentGame = Game(id = response.gameId, hostId = 0, numberOfPlayers = 0, packId = 0, discussionTime = 0, voteTime = 0, voteType = VoteType.ANONYMOUS, balance = BalanceLevel.MEDIUM),
                    currentPlayer = response.player,
                    error = null
                )
            }

            is GameResponse.GameState -> {
                _gameState.value = _gameState.value.copy(
                    currentGame = response.game,
                    players = response.players,
                    myCharCards = response.yourCharCards,
                    error = null
                )
            }

            is GameResponse.PlayerJoined -> {
                val currentPlayers = _gameState.value.players.toMutableList()
                currentPlayers.add(response.player)

                _gameState.value = _gameState.value.copy(
                    players = currentPlayers,
                    error = null
                )
            }

            is GameResponse.PlayerLeft -> {
                val currentPlayers = _gameState.value.players.toMutableList()
                currentPlayers.removeIf { it.id == response.playerId }

                _gameState.value = _gameState.value.copy(
                    players = currentPlayers,
                    error = null
                )
            }

            is GameResponse.CharacteristicRevealed -> {
                val playerId = response.playerId
                val revealedCards = _gameState.value.revealedCards.toMutableMap()
                val cards = revealedCards.getOrDefault(playerId, emptyList()).toMutableList()
                cards.add(response.charCard)
                revealedCards[playerId] = cards

                _gameState.value = _gameState.value.copy(
                    revealedCards = revealedCards,
                    error = null
                )
            }

            is GameResponse.VoteUpdate -> {
                _gameState.value = _gameState.value.copy(
                    votes = response.votes,
                    error = null
                )
            }

            is GameResponse.RoundStarted -> {
                _gameState.value = _gameState.value.copy(
                    currentRound = response.round,
                    currentPhase = response.phase,
                    timeRemaining = response.timeRemaining,
                    votes = emptyMap(),
                    myVote = null,
                    error = null
                )
            }

            is GameResponse.RoundEnded -> {
                // Mark player as eliminated in player list
                val updatedPlayers = _gameState.value.players.map { player ->
                    if (player.id == response.eliminatedPlayerId) {
                        player.copy(isEliminated = true)
                    } else {
                        player
                    }
                }

                _gameState.value = _gameState.value.copy(
                    players = updatedPlayers,
                    currentRound = response.nextRound,
                    error = null
                )
            }

            is GameResponse.GameEnded -> {
                _gameState.value = _gameState.value.copy(
                    gameResult = response.result,
                    currentGame = _gameState.value.currentGame?.copy(status = GameState.FINISHED),
                    error = null
                )
            }

            is GameResponse.Error -> {
                _gameState.value = _gameState.value.copy(
                    error = response.message
                )
            }
        }
    }

    // Public methods to send game commands

    fun createGame(settings: GameSettings) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.CreateGame(settings))
        }
    }

    fun joinGame(gameId: Long, playerName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.JoinGame(gameId, playerName))
        }
    }

    fun startGame(gameId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.StartGame(gameId))
        }
    }

    fun revealCharacteristic(gameId: Long, characteristicId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.RevealCharacteristic(gameId, characteristicId))
        }
    }

    fun vote(gameId: Long, targetPlayerId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.Vote(gameId, targetPlayerId))
            _gameState.value = _gameState.value.copy(myVote = targetPlayerId)
        }
    }

    fun useAction(gameId: Long, actionCardId: Long, targetPlayerId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.UseAction(gameId, actionCardId, targetPlayerId))
        }
    }

    fun leaveGame(gameId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.LeaveGame(gameId))

            _gameState.value = GameClientState(isConnected = isConnected())
        }
    }

    fun kickPlayer(gameId: Long, playerId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.KickPlayer(gameId, playerId))
        }
    }

    fun selectOrderNumber(gameId: Long, orderNumber: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            messageChannel.send(GameMessage.SelectOrderNumber(gameId, orderNumber))

            // Update local state with selected order number
            val currentPlayer = _gameState.value.currentPlayer
            if (currentPlayer != null) {
                _gameState.value = _gameState.value.copy(
                    currentPlayer = currentPlayer.copy(orderNumber = orderNumber)
                )
            }
        }
    }
}