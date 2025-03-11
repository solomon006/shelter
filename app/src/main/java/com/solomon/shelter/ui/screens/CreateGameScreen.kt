package com.solomon.shelter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.solomon.shelter.data.database.entities.VoteType
import com.solomon.shelter.game.GameManagerViewModel
import kotlinx.coroutines.launch

@Composable
fun CreateGameScreen(
    navController: NavController,
    viewModel: GameManagerViewModel
) {
    val scope = rememberCoroutineScope()
    val currentSettings by viewModel.gameSettings.collectAsState()
    var settings by remember { mutableStateOf(currentSettings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Создать игру",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Number of players
        Text(
            text = "Количество игроков: ${settings.numberOfPlayers}",
            modifier = Modifier.align(Alignment.Start)
        )
        Slider(
            value = settings.numberOfPlayers.toFloat(),
            onValueChange = {
                settings = settings.copy(numberOfPlayers = it.toInt())
                viewModel.updateGameSettings(settings)
            },
            valueRange = 4f..18f,
            steps = 13,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // ... rest of the sliders ...

        // Vote type selection
        Text(
            text = "Тип голосования:",
            modifier = Modifier.align(Alignment.Start)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VoteTypeButton(
                text = "Анонимно",
                selected = settings.voteType == VoteType.ANONYMOUS,
                onClick = {
                    settings = settings.copy(voteType = VoteType.ANONYMOUS)
                    viewModel.updateGameSettings(settings)
                }
            )
            // ... other buttons ...
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Назад")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        viewModel.createGame()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Создать")
            }
        }
    }
}

@Composable
fun VoteTypeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = if (selected) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
        },
        modifier = Modifier.padding(4.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun BalanceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = if (selected) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
        },
        modifier = Modifier.padding(4.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}