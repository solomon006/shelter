package com.solomon.shelter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.data.models.GameResult
import com.solomon.shelter.game.GameManagerViewModel
import com.solomon.shelter.game.states.AppGameState
import kotlinx.coroutines.launch

@Composable
fun ResultsScreen(
    navController: NavController,
    viewModel: GameManagerViewModel
) {
    val scope = rememberCoroutineScope()
    val gameState by viewModel.gameState.collectAsState()

    when (val state = gameState) {
        is AppGameState.Playing -> {
            // In a real app, gameResult would be part of the Playing state
            // For this example, we'll create a mock result if it's null
            val result = if (state.gameResult != null) {
                state.gameResult
            } else {
                // Create a mock result for illustration
                GameResult(
                    gameId = state.game.id,
                    endTime = System.currentTimeMillis(),
                    survivors = state.players.filter { !it.isEliminated },
                    eliminated = state.players.filter { it.isEliminated },
                    revealedCards = state.revealedCards,
                    hiddenCards = mapOf(), // No hidden cards in this mock
                    endState = "",
                    catastrophe = state.game.catastrophe,
                    shelter = state.game.shelter,
                    ending = state.game.ending
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Результаты игры",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Catastrophe and ending
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        if (result.catastrophe != null) {
                            Text(
                                text = "Катастрофа:",
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = result.catastrophe)

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (result.shelter != null) {
                            Text(
                                text = "Бункер:",
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = result.shelter)

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (result.ending != null) {
                            Text(
                                text = "Исход игры:",
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = result.ending)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Survivors and eliminated players
                Row(modifier = Modifier.weight(1f)) {
                    // Survivors
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Выжившие",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = Color.Green
                        )

                        LazyColumn {
                            items(result.survivors) { player ->
                                PlayerResultCard(
                                    player = player,
                                    revealedCards = result.revealedCards[player.id] ?: emptyList(),
                                    hiddenCards = result.hiddenCards[player.id] ?: emptyList(),
                                    isEliminated = false
                                )
                            }
                        }
                    }

                    // Eliminated
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Выбывшие",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = Color.Red
                        )

                        LazyColumn {
                            items(result.eliminated) { player ->
                                PlayerResultCard(
                                    player = player,
                                    revealedCards = result.revealedCards[player.id] ?: emptyList(),
                                    hiddenCards = result.hiddenCards[player.id] ?: emptyList(),
                                    isEliminated = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.leaveGame()
                            navController.navigate("home")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Вернуться в главное меню", fontSize = 18.sp)
                }
            }
        }

        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PlayerResultCard(
    player: Player,
    revealedCards: List<CharCard>,
    hiddenCards: List<CharCard>,
    isEliminated: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEliminated)
                Color.Red.copy(alpha = 0.2f)
            else
                Color.Green.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "${player.name} (#${player.orderNumber})",
                fontWeight = FontWeight.Bold,
                color = if (isEliminated) Color.Red else Color.Green
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Revealed cards
            if (revealedCards.isNotEmpty()) {
                Text(
                    text = "Раскрытые характеристики:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                revealedCards.forEach { card ->
                    Text(
                        text = "• ${card.info}",
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Hidden cards
            if (hiddenCards.isNotEmpty()) {
                Text(
                    text = "Скрытые характеристики:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                hiddenCards.forEach { card ->
                    Text(
                        text = "• ${card.info}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}