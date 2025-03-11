package com.solomon.shelter.network.messages

import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.data.models.GameResult

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
    data class GameEnded(val result: GameResult) : GameResponse()
    data class Error(val message: String) : GameResponse()
}

data class ResponseWrapper(val type: String, val data: String)
