package com.solomon.shelter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = Pack::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Ending::class,
            parentColumns = ["id"],
            childColumns = ["endingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Catastrophe::class,
            parentColumns = ["id"],
            childColumns = ["catastropheId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Shelter::class,
            parentColumns = ["id"],
            childColumns = ["shelterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("packId"),
        Index("endingId"),
        Index("catastropheId"),
        Index("shelterId"),
        Index("status")
    ]
)
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostId: Long,
    val numberOfPlayers: Int,
    val packId: Long,
    val discussionTime: Int,
    val voteTime: Int,
    val voteType: VoteType,
    val status: GameState = GameState.LOBBY,
    val balance: BalanceLevel,
    val currentRound: Int = 1,
    val gameStateJson: String? = null,
    val endingId: Long? = null,
    val catastropheId: Long? = null,
    val shelterId: Long? = null,
    val ending: String? = null,
    val catastrophe: String? = null,
    val shelter: String? = null
) {
    companion object {
        /** Количество мест в бункере */
        fun getCapacity(numPlayers: Int): Int {
            return numPlayers / 2
        }

        /** Общее число изгнанных */
        fun getTotalEliminated(numPlayers: Int): Int {
            return numPlayers - getCapacity(numPlayers)
        }

        /** Сколько человек выбывает в определённом раунде */
        fun getPlayersToEliminateByRoundAndCapacity(numPlayers: Int, round: Int): Int {
            if (round <= 1 || round > 5) return 0

            val totalEliminated = getTotalEliminated(numPlayers)

            val distribution = when {
                numPlayers >= 15 -> listOf(2, 2, 2, 2) // 15-16 игроков
                numPlayers >= 13 -> listOf(1, 2, 2, 2) // 13-14 игроков
                numPlayers >= 11 -> listOf(1, 1, 2, 2) // 11-12 игроков
                numPlayers >= 9  -> listOf(1, 1, 1, 2) // 9-10 игроков
                numPlayers >= 7  -> listOf(1, 1, 1, 1) // 7-8 игроков
                numPlayers >= 5  -> listOf(0, 1, 1, 1) // 5-6 игроков
                numPlayers >= 4  -> listOf(0, 0, 1, 1) // 4 игрока
                else -> listOf(0, 0, 0, 0)
            }

            return if (round <= distribution.size) distribution[round - 1] else 0
        }
    }
}