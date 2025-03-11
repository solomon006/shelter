package com.solomon.shelter.network.server

import android.util.Log
import com.google.gson.Gson
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Connection manager for WebSockets
 */
class ConnectionManager {
    private val TAG = "ConnectionManager"
    private val connections = ConcurrentHashMap<String, WebSocketSession>()
    private val playerConnections = ConcurrentHashMap<Long, String>() // playerId to connectionId
    private val gameConnections = ConcurrentHashMap<Long, MutableSet<String>>() // gameId to set of connectionIds
    private val gson = Gson()

    fun addConnection(connectionId: String, session: WebSocketSession) {
        Log.d(TAG, "Adding connection: $connectionId")
        connections[connectionId] = session
    }

    fun removeConnection(connectionId: String) {
        Log.d(TAG, "Removing connection: $connectionId")
        connections.remove(connectionId)

        // Remove player association
        playerConnections.entries.removeIf { it.value == connectionId }

        // Remove from game connections
        gameConnections.forEach { (gameId, connections) ->
            connections.remove(connectionId)
        }
    }

    fun registerPlayerConnection(playerId: Long, connectionId: String) {
        Log.d(TAG, "Registering player connection: $playerId -> $connectionId")
        playerConnections[playerId] = connectionId
    }

    fun getPlayerIdForConnection(connectionId: String): Long? {
        return playerConnections.entries.find { it.value == connectionId }?.key
    }

    fun addPlayerToGame(gameId: Long, connectionId: String) {
        Log.d(TAG, "Adding player to game: $gameId, $connectionId")
        gameConnections.computeIfAbsent(gameId) { mutableSetOf() }.add(connectionId)
    }

    fun removePlayerFromGame(gameId: Long, connectionId: String) {
        Log.d(TAG, "Removing player from game: $gameId, $connectionId")
        gameConnections[gameId]?.remove(connectionId)
    }

    suspend fun sendToPlayer(playerId: Long, message: Any) {
        val connectionId = playerConnections[playerId]
        if (connectionId == null) {
            Log.d(TAG, "Player not connected: $playerId")
            return
        }

        val session = connections[connectionId]
        if (session == null) {
            Log.d(TAG, "Session not found for player: $playerId, connectionId: $connectionId")
            return
        }

        try {
            val json = gson.toJson(message)
            session.send(Frame.Text(json))
            Log.d(TAG, "Sent message to player $playerId: ${message.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to player $playerId", e)
        }
    }

    suspend fun sendToGame(gameId: Long, message: Any, excludePlayerId: Long? = null) {
        val connections = gameConnections[gameId]
        if (connections == null) {
            Log.d(TAG, "No connections for game: $gameId")
            return
        }

        try {
            val json = gson.toJson(message)
            Log.d(TAG, "Sending message to game $gameId: ${message.javaClass.simpleName}")

            for (connectionId in connections) {
                val playerId = getPlayerIdForConnection(connectionId)
                if (playerId == excludePlayerId) {
                    continue
                }

                val session = this.connections[connectionId]
                if (session != null) {
                    session.send(Frame.Text(json))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to game $gameId", e)
        }
    }
}