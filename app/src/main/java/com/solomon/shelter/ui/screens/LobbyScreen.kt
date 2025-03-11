package com.solomon.shelter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.database.entities.GameState
import com.solomon.shelter.data.database.entities.Player
import com.solomon.shelter.game.GameManagerViewModel
import com.solomon.shelter.game.states.AppGameState
import com.solomon.shelter.ui.components.OrderNumberSelector
import com.solomon.shelter.ui.components.PlayerItem
import kotlinx.coroutines.launch

@Composable
fun LobbyScreen(
    navController: NavController,
    viewModel: GameManagerViewModel
) {
    val scope = rememberCoroutineScope()
    val gameState by viewModel.gameState.collectAsState()

    // Extract relevant game data from game state
    val game: Game?
    val currentPlayer: Player?
    val players: List<Player>
    val isHost: Boolean

    when (val state = gameState) {
        is AppGameState.Hosting -> {
            game = state.game
            currentPlayer = state.players.firstOrNull { it.isHost }
            players = state.players
            isHost = true
        }
        is AppGameState.Playing -> {
            game = state.game
            currentPlayer = state.player
            players = state.players
            isHost = state.player.isHost
        }
        else -> {
            game = null
            currentPlayer = null
            players = emptyList()
            isHost = false
        }
    }

    // Check if game has started
    LaunchedEffect(game?.status) {
        if (game?.status == GameState.IN_PROGRESS) {
            navController.navigate("game") {
                popUpTo("lobby") { inclusive = true }
            }
        }
    }

    if (game == null || currentPlayer == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val takenNumbers = players.filter { it.orderNumber != -1 }.map { it.orderNumber }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Лобби игры #${game.id}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OrderNumberSelector(
            maxPlayers = game.numberOfPlayers,
            takenNumbers = takenNumbers.filter { it != currentPlayer.orderNumber },
            selectedNumber = currentPlayer.orderNumber,
            onNumberSelected = { number ->
                scope.launch {
                    viewModel.selectOrderNumber(number)
                }
            }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Игроки (${players.size}/${game.numberOfPlayers}):",
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(players.sortedBy { it.orderNumber }) { player ->
                PlayerItem(
                    player = player,
                    isCurrentUser = player.id == currentPlayer.id,
                    isHost = player.isHost,
                    canKick = isHost && player.id != currentPlayer.id,
                    onKick = {
                        scope.launch {
                            viewModel.kickPlayer(player.id)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    scope.launch {
                        viewModel.leaveGame()
                        navController.navigate("home")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Выйти")
            }

            // Only show start game button for the host
            if (isHost) {
                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.startGame()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = players.size >= 4 && players.all { it.orderNumber != -1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Начать игру")
                }
            }
        }
    }
}