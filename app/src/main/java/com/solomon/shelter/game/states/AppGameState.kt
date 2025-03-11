package com.solomon.shelter.game.states

import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.data.models.GameInfo
import com.solomon.shelter.data.models.GameResult
import com.solomon.shelter.network.client.GameWebSocketClient

sealed class AppGameState {
    object Idle : AppGameState()

    data class Hosting(
        val game: Game,
        val players: List<Player>
    ) : AppGameState()

    data class Joining(
        val gameInfo: GameInfo,
        val client: GameWebSocketClient
    ) : AppGameState()

    data class Playing(
        val game: Game,
        val player: Player,
        val players: List<Player>,
        val cards: List<CharCard>,
        val revealedCards: Map<Long, List<CharCard>>,
        val client: GameWebSocketClient,
        val gameResult: GameResult? = null,
        val currentRound: Int = 1,
        val currentPhase: String = "",
        val timeRemaining: Int = 0,
        val votes: Map<Long, Int> = emptyMap(),
        val myVote: Long? = null
    ) : AppGameState()

    data class Error(
        val message: String,
        val previousState: AppGameState? = null
    ) : AppGameState()
}
