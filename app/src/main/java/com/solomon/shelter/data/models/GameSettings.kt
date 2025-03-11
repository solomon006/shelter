package com.solomon.shelter.data.models


import com.solomon.shelter.data.database.entities.BalanceLevel
import com.solomon.shelter.data.database.entities.VoteType

data class GameSettings(
    val numberOfPlayers: Int = 4,
    val discussionTime: Int = 60,
    val voteTime: Int = 30,
    val voteType: VoteType = VoteType.ANONYMOUS,
    val balanceLevel: BalanceLevel = BalanceLevel.MEDIUM,
    val selectedPackId: Long = 1
)