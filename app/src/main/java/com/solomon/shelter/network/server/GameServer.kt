package com.solomon.shelter.network.server

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.solomon.shelter.data.database.BunkerDatabase
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.GameState
import com.solomon.shelter.data.models.GameSettings
import com.solomon.shelter.data.repository.BunkerRepository
import com.solomon.shelter.game.GameController
import com.solomon.shelter.game.GameInstance
import com.solomon.shelter.network.client.GameMessage
import com.solomon.shelter.network.messages.GameResponse
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebSocket server for hosting a Bunker game
 */
class GameServer(private val context: Context) {
    private val TAG = "GameServer"

    private var server: NettyApplicationEngine? = null
    val connectionManager = ConnectionManager()
    val gameController = GameController(context, connectionManager)
    private val gson = Gson()

    // Ports to try for the server
    private val PORTS = listOf(8080, 8081, 8082, 8083, 8084, 8085)
    private var currentPort = 0

    // Server state
    private val _serverState = MutableStateFlow(ServerState.STOPPED)
    val serverState: StateFlow<ServerState> = _serverState

    /**
     * Start the WebSocket server
     */
    fun start(): Boolean {
        if (server != null) {
            Log.d(TAG, "Server already running")
            return true
        }

        for (port in PORTS) {
            try {
                server = embeddedServer(Netty, port = port) {
                    install(ContentNegotiation) {
                        gson()
                    }

                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(30)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }

                    routing {
                        webSocket("/game") {
                            val connectionId = UUID.randomUUID().toString()
                            connectionManager.addConnection(connectionId, this)

                            try {
                                incoming.consumeEach { frame ->
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        handleMessage(connectionId, text)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in WebSocket connection", e)
                            } finally {
                                connectionManager.removeConnection(connectionId)
                            }
                        }
                    }
                }

                server?.start(wait = false)
                currentPort = port
                _serverState.value = ServerState.RUNNING

                Log.d(TAG, "Server started on port $port")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server on port $port", e)
            }
        }

        Log.e(TAG, "Failed to start server on any port")
        return false
    }

    /**
     * Stop the WebSocket server
     */
    fun stop() {
        server?.stop(1000, 2000)
        server = null
        currentPort = 0
        _serverState.value = ServerState.STOPPED

        Log.d(TAG, "Server stopped")
    }

    /**
     * Get the server port
     */
    fun getPort(): Int {
        return currentPort
    }

    /**
     * Handle incoming WebSocket messages
     */
    private fun handleMessage(connectionId: String, message: String) {
        try {
            val wrapper = gson.fromJson(message, MessageWrapper::class.java)

            when (wrapper.type) {
                "CreateGame" -> {
                    val createGame = gson.fromJson(wrapper.data, GameMessage.CreateGame::class.java)
                    gameController.createGame(connectionId, createGame.settings)
                }

                "JoinGame" -> {
                    val joinGame = gson.fromJson(wrapper.data, GameMessage.JoinGame::class.java)
                    gameController.joinGame(connectionId, joinGame.gameId, joinGame.playerName)
                }

                "StartGame" -> {
                    val startGame = gson.fromJson(wrapper.data, GameMessage.StartGame::class.java)
                    gameController.startGame(connectionId, startGame.gameId)
                }

                "RevealCharacteristic" -> {
                    val revealChar = gson.fromJson(wrapper.data, GameMessage.RevealCharacteristic::class.java)
                    gameController.revealCharacteristic(connectionId, revealChar.gameId, revealChar.characteristicId)
                }

                "Vote" -> {
                    val vote = gson.fromJson(wrapper.data, GameMessage.Vote::class.java)
                    gameController.vote(connectionId, vote.gameId, vote.targetPlayerId)
                }

                "UseAction" -> {
                    val useAction = gson.fromJson(wrapper.data, GameMessage.UseAction::class.java)
                    gameController.useAction(connectionId, useAction.gameId, useAction.actionCardId, useAction.targetPlayerId)
                }

                "LeaveGame" -> {
                    val leaveGame = gson.fromJson(wrapper.data, GameMessage.LeaveGame::class.java)
                    gameController.leaveGame(connectionId, leaveGame.gameId)
                }

                "KickPlayer" -> {
                    val kickPlayer = gson.fromJson(wrapper.data, GameMessage.KickPlayer::class.java)
                    gameController.kickPlayer(connectionId, kickPlayer.gameId, kickPlayer.playerId)
                }

                "SelectOrderNumber" -> {
                    val selectNumber = gson.fromJson(wrapper.data, GameMessage.SelectOrderNumber::class.java)
                    gameController.selectOrderNumber(connectionId, selectNumber.gameId, selectNumber.orderNumber)
                }

                else -> {
                    Log.e(TAG, "Unknown message type: ${wrapper.type}")
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Invalid message format", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    /**
     * Connection manager for WebSockets
     */
    class ConnectionManager {
        private val connections = ConcurrentHashMap<String, WebSocketSession>()
        private val playerConnections = ConcurrentHashMap<Long, String>() // playerId to connectionId
        private val gameConnections = ConcurrentHashMap<Long, MutableSet<String>>() // gameId to set of connectionIds
        private val userIdToConnectionId = ConcurrentHashMap<String, String>() // userId to connectionId

        fun addConnection(connectionId: String, session: WebSocketSession) {
            connections[connectionId] = session
        }

        fun removeConnection(connectionId: String) {
            connections.remove(connectionId)

            // Remove player association
            playerConnections.entries.removeIf { it.value == connectionId }

            // Remove userId association
            userIdToConnectionId.entries.removeIf { it.value == connectionId }

            // Remove from game connections
            gameConnections.forEach { (gameId, connections) ->
                connections.remove(connectionId)
            }
        }

        fun registerPlayerConnection(playerId: Long, connectionId: String) {
            playerConnections[playerId] = connectionId
        }

        fun registerUserConnection(userId: String, connectionId: String) {
            userIdToConnectionId[userId] = connectionId
        }

        fun getPlayerIdForConnection(connectionId: String): Long? {
            return playerConnections.entries.find { it.value == connectionId }?.key
        }

        /**
         * Get connection ID for a player ID
         */
        fun getConnectionIdForPlayer(playerId: String): String? {
            return playerConnections.entries.find { it.key.toString() == playerId }?.value
        }

        /**
         * Get connection ID for a player by userId
         */
        fun getConnectionIdForPlayerByUserId(userId: String): String? {
            return userIdToConnectionId[userId]
        }

        fun addPlayerToGame(gameId: Long, connectionId: String) {
            gameConnections.computeIfAbsent(gameId) { mutableSetOf() }.add(connectionId)
        }

        fun removePlayerFromGame(gameId: Long, connectionId: String) {
            gameConnections[gameId]?.remove(connectionId)
        }

        suspend fun sendToPlayer(playerId: Long, message: Any) {
            val connectionId = playerConnections[playerId] ?: return
            val session = connections[connectionId] ?: return
            val json = Gson().toJson(message)
            session.send(Frame.Text(json))
        }

        suspend fun sendToGame(gameId: Long, message: Any, excludePlayerId: Long? = null) {
            val connections = gameConnections[gameId] ?: return
            val json = Gson().toJson(message)

            for (connectionId in connections) {
                val playerId = getPlayerIdForConnection(connectionId) ?: continue

                if (playerId != excludePlayerId) {
                    val session = this.connections[connectionId] ?: continue
                    session.send(Frame.Text(json))
                }
            }
        }
    }

    /**
     * Data class for game message wrapper
     */
    data class MessageWrapper(
        val type: String,
        val data: String
    )

    /**
     * Server state enum
     */
    enum class ServerState {
        RUNNING,
        STOPPED
    }
}

/**
 * Extension function to let the server create a game
 */
suspend fun GameServer.createGame(playerId: String, settings: GameSettings): Long {
    return suspendCancellableCoroutine { continuation ->
        try {
            val gameController = this.gameController
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val connectionId = connectionManager.getConnectionIdForPlayer(playerId) ?:
                    connectionManager.getConnectionIdForPlayerByUserId(playerId)

                    if (connectionId != null) {
                        val gameId = gameController.createGame(connectionId, settings)
                        continuation.resume(gameId)
                    } else {
                        throw IllegalStateException("Player connection not found")
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}

/**
 * Extension function to let the server start a game
 */
suspend fun GameServer.startGame(gameId: Long, playerId: String) {
    return suspendCancellableCoroutine { continuation ->
        try {
            val gameController = this.gameController
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val connectionId = connectionManager.getConnectionIdForPlayer(playerId) ?:
                    connectionManager.getConnectionIdForPlayerByUserId(playerId)

                    if (connectionId != null) {
                        gameController.startGame(connectionId, gameId)
                        continuation.resume(Unit)
                    } else {
                        throw IllegalStateException("Player connection not found")
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}

/**
 * Extension function to let the server select an order number
 */
suspend fun GameServer.selectOrderNumber(gameId: Long, playerId: String, orderNumber: Int) {
    return suspendCancellableCoroutine { continuation ->
        try {
            val gameController = this.gameController
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Find the connection ID for the player
                    val connectionId = connectionManager.getConnectionIdForPlayer(playerId) ?:
                    connectionManager.getConnectionIdForPlayerByUserId(playerId)

                    if (connectionId != null) {
                        gameController.selectOrderNumber(connectionId, gameId, orderNumber)
                        continuation.resume(Unit)
                    } else {
                        throw IllegalStateException("Player connection not found")
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}

/**
 * Extension function to let the server kick a player
 */
suspend fun GameServer.kickPlayer(gameId: Long, hostPlayerId: String, targetPlayerId: Long) {
    return suspendCancellableCoroutine { continuation ->
        try {
            val gameController = this.gameController
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Find the connection ID for the host player
                    val connectionId = connectionManager.getConnectionIdForPlayer(hostPlayerId) ?:
                    connectionManager.getConnectionIdForPlayerByUserId(hostPlayerId)

                    if (connectionId != null) {
                        gameController.kickPlayer(connectionId, gameId, targetPlayerId)
                        continuation.resume(Unit)
                    } else {
                        throw IllegalStateException("Host player connection not found")
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}

/**
 * Extension function to let the server handle a player leaving
 */
suspend fun GameServer.leaveGame(gameId: Long, playerId: String) {
    return suspendCancellableCoroutine { continuation ->
        try {
            val gameController = this.gameController
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Find the connection ID for the player
                    val connectionId = connectionManager.getConnectionIdForPlayer(playerId) ?:
                    connectionManager.getConnectionIdForPlayerByUserId(playerId)

                    if (connectionId != null) {
                        gameController.leaveGame(connectionId, gameId)
                        continuation.resume(Unit)
                    } else {
                        throw IllegalStateException("Player connection not found")
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}