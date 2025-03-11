package com.solomon.shelter.network.messages

import com.solomon.shelter.data.database.entities.*
import com.solomon.shelter.data.models.GameSettings

// Client to server messages
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
}

// Message wrapper for serialization
data class MessageWrapper(val type: String, val data: String)