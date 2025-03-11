package com.solomon.shelter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.GameState
import com.solomon.shelter.game.GameManagerViewModel
import com.solomon.shelter.game.states.AppGameState
import com.solomon.shelter.ui.components.CharacteristicCard
import com.solomon.shelter.ui.components.PlayerItem
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameManagerViewModel
) {
    val scope = rememberCoroutineScope()
    val gameState by viewModel.gameState.collectAsState()
    val characteristicNames by viewModel.characteristics.collectAsState()

    when (val state = gameState) {
        is AppGameState.Playing -> {
            // Navigate to results when game ends
            LaunchedEffect(state.game.status) {
                if (state.game.status == GameState.FINISHED) {
                    navController.navigate("results") {
                        popUpTo("game") { inclusive = true }
                    }
                }
            }

            val game = state.game
            val player = state.player
            val players = state.players
            val cards = state.cards
            val revealedCards = state.revealedCards
            val timer = remember { mutableStateOf(0) }

            // Update timer
            LaunchedEffect(state.currentPhase, state.timeRemaining) {
                timer.value = state.timeRemaining
                val interval = 1000L // 1 second

                while (timer.value > 0) {
                    kotlinx.coroutines.delay(interval)
                    timer.value -= 1
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Game header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Раунд ${state.currentRound}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (state.currentPhase == "discussion") "Обсуждение" else "Голосование",
                            fontSize = 16.sp
                        )

                        Text(
                            text = "Осталось: ${timer.value}с",
                            fontSize = 14.sp,
                            color = if (timer.value <= 10) Color.Red else Color.White
                        )
                    }
                }

                Divider()

                // Main game content
                Row(modifier = Modifier.weight(1f)) {
                    // Left panel - my cards
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Мои характеристики",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyColumn {
                            items(cards) { charCard ->
                                CharacteristicCard(
                                    charCard = charCard,
                                    isRevealed = revealedCards[player.id]?.contains(charCard) == true,
                                    onReveal = {
                                        scope.launch {
                                            viewModel.revealCharacteristic(charCard.id)
                                        }
                                    },
                                    characteristicName = characteristicNames[charCard.characteristicId] ?: "Характеристика"
                                )
                            }
                        }
                    }

                    // Right panel - players and voting
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Игроки",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyColumn {
                            items(players.filter { !it.isEliminated }) { otherPlayer ->
                                PlayerItem(
                                    player = otherPlayer,
                                    isCurrentUser = otherPlayer.id == player.id,
                                    isHost = otherPlayer.isHost,
                                    revealedCharCards = revealedCards[otherPlayer.id] ?: emptyList(),
                                    isVotingPhase = state.currentPhase == "voting",
                                    voteCount = state.votes[otherPlayer.id] ?: 0,
                                    isSelected = state.myVote == otherPlayer.id,
                                    onPlayerClick = {
                                        if (state.currentPhase == "voting" && otherPlayer.id != player.id) {
                                            scope.launch {
                                                viewModel.vote(otherPlayer.id)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Eliminated players section
                        val eliminatedPlayers = players.filter { it.isEliminated }
                        if (eliminatedPlayers.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "Выбывшие игроки",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            LazyColumn(modifier = Modifier.height(100.dp)) {
                                items(eliminatedPlayers) { eliminatedPlayer ->
                                    PlayerItem(
                                        player = eliminatedPlayer,
                                        isCurrentUser = eliminatedPlayer.id == player.id,
                                        revealedCharCards = revealedCards[eliminatedPlayer.id] ?: emptyList()
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom row with catastrophe, shelter, etc.
                if (game.catastrophe != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Катастрофа: ${game.catastrophe}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }

                if (game.shelter != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Бункер: ${game.shelter}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        else -> {
            // Loading or error state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}