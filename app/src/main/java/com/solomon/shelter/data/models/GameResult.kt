package com.solomon.shelter.data.models

import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Player

data class GameResult(
    val gameId: Long,
    val endTime: Long,
    val survivors: List<Player>,
    val eliminated: List<Player>,
    val revealedCards: Map<Long, List<CharCard>>, // playerId to cards
    val hiddenCards: Map<Long, List<CharCard>>, // playerId to cards
    val endState: String, // JSON representation of final game state
    val catastrophe: String? = null,
    val shelter: String? = null,
    val ending: String? = null
)