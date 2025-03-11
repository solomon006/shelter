package com.solomon.shelter.game

import android.content.Context
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.GameState
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.data.models.GameInfo
import com.solomon.shelter.data.models.GameSettings
import com.solomon.shelter.game.states.AppGameState
import com.solomon.shelter.network.client.GameWebSocketClient
import com.solomon.shelter.network.discovery.GameDiscoveryService
import com.solomon.shelter.network.server.GameServer
import com.solomon.shelter.network.server.createGame
import com.solomon.shelter.network.server.kickPlayer
import com.solomon.shelter.network.server.leaveGame
import com.solomon.shelter.network.server.selectOrderNumber
import com.solomon.shelter.network.server.startGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class GameManager(private val context: Context) {
    private val server = GameServer(context)
    private val discovery = GameDiscoveryService(context)

    // Player info
    private val playerId = java.util.UUID.randomUUID().toString()
    private val _playerName = MutableStateFlow<String>("")
    val playerName: StateFlow<String> = _playerName

    // Game state
    private val _gameState = MutableStateFlow<AppGameState>(AppGameState.Idle)
    val gameState: StateFlow<AppGameState> = _gameState

    // Available games for joining
    private val _availableGames = MutableStateFlow<List<GameInfo>>(emptyList())
    val availableGames: StateFlow<List<GameInfo>> = _availableGames

    // Game settings for creating games
    private val _gameSettings = MutableStateFlow(GameSettings())
    val gameSettings: StateFlow<GameSettings> = _gameSettings

    init {
        // Observe discovered games
        CoroutineScope(Dispatchers.IO).launch {
            discovery.discoveredGames.collectLatest {
                games: Map<String, GameInfo> ->
                _availableGames.value = games.values.toList()
            }
        }

        // Observe server state
        CoroutineScope(Dispatchers.IO).launch {
            server.serverState.collectLatest { state ->
                when (state) {
                    GameServer.ServerState.RUNNING -> {
                        if (_gameState.value is AppGameState.Hosting) {
                            // Server is running, start advertising the game
                            val hostingState = _gameState.value as AppGameState.Hosting
                            discovery.advertiseGame(hostingState.game, hostingState.players.size)
                        }
                    }
                    GameServer.ServerState.STOPPED -> {
                        // Server stopped, stop advertising
                        discovery.stopAdvertising()
                    }
                }
            }
        }
    }

    /**
     * Set the player's name
     */
    fun setPlayerName(name: String) {
        _playerName.value = name
    }

    /**
     * Update game settings
     */
    fun updateGameSettings(settings: GameSettings) {
        _gameSettings.value = settings
    }

    /**
     * Create a new game as host
     */
    fun createGame() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Start the server
                if (server.start()) {
                    // Server started successfully

                    // Create a new game
                    val gameId = server.createGame(playerId, _gameSettings.value)

                    // Update state
                    _gameState.value = AppGameState.Hosting(
                        game = Game(
                            id = gameId,
                            hostId = playerId.toLong(),
                            numberOfPlayers = _gameSettings.value.numberOfPlayers,
                            packId = _gameSettings.value.selectedPackId,
                            discussionTime = _gameSettings.value.discussionTime,
                            voteTime = _gameSettings.value.voteTime,
                            voteType = _gameSettings.value.voteType,
                            status = GameState.LOBBY,
                            balance = _gameSettings.value.balanceLevel
                        ),
                        players = listOf(
                            Player(
                                id = playerId.toLong(),
                                gameId = gameId,
                                name = _playerName.value,
                                connectionId = "",
                                userId = playerId,
                                orderNumber = -1,
                                isEliminated = false,
                                isHost = true
                            )
                        )
                    )
                } else {
                    // Failed to start server
                    _gameState.value = AppGameState.Error("Failed to start game server", _gameState.value)
                }
            } catch (e: Exception) {
                _gameState.value = AppGameState.Error("Error creating game: ${e.message}", _gameState.value)
            }
        }
    }

    /**
     * Join an existing game
     */
    fun joinGame(gameInfo: GameInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create WebSocket client
                val client = GameWebSocketClient(
                    context = context,
                    baseUrl = "${gameInfo.hostAddress}:${gameInfo.port}",
                    userId = playerId
                )

                // Connect to the game
                client.connect()

                // Join the game
                client.joinGame(gameInfo.id, _playerName.value)

                // Update state
                _gameState.value = AppGameState.Joining(
                    gameInfo = gameInfo,
                    client = client
                )

                // Observe client state
                client.gameState.collectLatest { clientState ->
                    if (clientState.currentGame != null && clientState.currentPlayer != null) {
                        // Successfully joined the game
                        _gameState.value = AppGameState.Playing(
                            game = clientState.currentGame,
                            player = clientState.currentPlayer,
                            players = clientState.players,
                            charCards = clientState.myCharCards,
                            revealedCards = clientState.revealedCards,
                            client = client
                        )
                    } else if (clientState.error != null) {
                        _gameState.value = AppGameState.Error(clientState.error)
                    }
                }
            } catch (e: Exception) {
                _gameState.value = AppGameState.Error("Error joining game: ${e.message}")
            }
        }
    }

    /**
     * Start a game (host only)
     */
    fun startGame() {
        val state = _gameState.value
        if (state is AppGameState.Hosting) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    server.startGame(state.game.id, playerId)
                } catch (e: Exception) {
                    _gameState.value = AppGameState.Error("Error starting game: ${e.message}")
                }
            }
        }
    }

    /**
     * Select a player's order number
     */
    fun selectOrderNumber(orderNumber: Int) {
        val state = _gameState.value
        when (state) {
            is AppGameState.Hosting -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        server.selectOrderNumber(state.game.id, playerId, orderNumber)
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error selecting order number: ${e.message}")
                    }
                }
            }
            is AppGameState.Playing -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        state.client.selectOrderNumber(state.game.id, orderNumber)
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error selecting order number: ${e.message}")
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
    }

    /**
     * Reveal a characteristic
     */
    fun revealCharacteristic(cardId: Long) {
        val state = _gameState.value
        when (state) {
            is AppGameState.Playing -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        state.client.revealCharacteristic(state.game.id, cardId)
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error revealing characteristic: ${e.message}", state)
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
    }

    /**
     * Vote for a player
     */
    fun vote(targetPlayerId: Long) {
        val state = _gameState.value
        when (state) {
            is AppGameState.Playing -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        state.client.vote(state.game.id, targetPlayerId)
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error voting: ${e.message}", state)
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
    }

    /**
     * Use an action card
     */
    fun useAction(actionCardId: Long, targetPlayerId: Long) {
        val state = _gameState.value
        when (state) {
            is AppGameState.Playing -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        state.client.useAction(state.game.id, actionCardId, targetPlayerId)
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error using action card: ${e.message}", state)
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
    }

    /**
     * Kick a player (host only)
     */
    fun kickPlayer(playerId: Long) {
        val state = _gameState.value
        when (state) {
            is AppGameState.Hosting -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        server.kickPlayer(state.game.id, this@GameManager.playerId, playerId)
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error kicking player: ${e.message}", state)
                    }
                }
            }
            is AppGameState.Playing -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        state.client.kickPlayer(state.game.id, playerId)
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error kicking player: ${e.message}", state)
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
    }

    /**
     * Leave the current game
     */
    fun leaveGame() {
        val state = _gameState.value
        when (state) {
            is AppGameState.Hosting -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        server.leaveGame(state.game.id, playerId)
                        server.stop()
                        discovery.stopAdvertising()
                        _gameState.value = AppGameState.Idle
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error leaving game: ${e.message}", state)
                    }
                }
            }
            is AppGameState.Playing -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        state.client.leaveGame(state.game.id)
                        state.client.disconnect()
                        _gameState.value = AppGameState.Idle
                    } catch (e: Exception) {
                        _gameState.value = AppGameState.Error("Error leaving game: ${e.message}", state)
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
    }

    /**
     * Scan for available games
     */
    fun scanForGames() {
        discovery.startDiscovery()
    }

    /**
     * Stop scanning for games
     */
    fun stopScanningForGames() {
        discovery.stopDiscovery()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                server.stop()
                discovery.cleanup()

                val state = _gameState.value
                if (state is AppGameState.Playing) {
                    state.client.disconnect()
                }

                _gameState.value = AppGameState.Idle
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}