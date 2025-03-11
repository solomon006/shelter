package com.solomon.shelter.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solomon.shelter.data.database.BunkerDatabase
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Characteristic
import com.solomon.shelter.data.models.GameInfo
import com.solomon.shelter.data.models.GameResult
import com.solomon.shelter.data.models.GameSettings
import com.solomon.shelter.data.repository.BunkerRepository
import com.solomon.shelter.game.states.AppGameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameManagerViewModel(context: Context) : ViewModel() {
    private val gameManager = GameManager(context)
    private val repository = BunkerRepository(BunkerDatabase.getDatabase(context))

    // Game state
    val gameState: StateFlow<AppGameState> = gameManager.gameState

    // Player name
    val playerName: StateFlow<String> = gameManager.playerName

    // Available games
    val availableGames: StateFlow<List<GameInfo>> = gameManager.availableGames

    // Game settings
    val gameSettings: StateFlow<GameSettings> = gameManager.gameSettings

    // Characteristics map (ID to name)
    private val _characteristics = MutableStateFlow<Map<Long, String>>(emptyMap())
    val characteristics: StateFlow<Map<Long, String>> = _characteristics

    init {
        // Load characteristic names when ViewModel is created
        loadCharacteristics()
    }

    // Load characteristics from database
    private fun loadCharacteristics() {
        viewModelScope.launch {
            try {
                val characteristicsList = repository.getAllCharacteristics()
                _characteristics.value = characteristicsList.associate { it.id to it.characteristicName }

                // Add default action card type if not present
                if (!_characteristics.value.containsKey(0L)) {
                    val updatedMap = _characteristics.value.toMutableMap()
                    updatedMap[0L] = "Карта действия"
                    _characteristics.value = updatedMap
                }
            } catch (e: Exception) {
                // Fallback to hardcoded values if database access fails
                _characteristics.value = mapOf(
                    1L to "Профессия",
                    2L to "Здоровье",
                    3L to "Психика",
                    4L to "Хобби",
                    5L to "Доп. информация",
                    6L to "Багаж",
                    0L to "Карта действия"
                )
            }
        }
    }

    // Connect to WebSocket server
    fun connect() {
        viewModelScope.launch {
            gameManager.connect()
        }
    }

    // Disconnect from WebSocket server
    fun disconnect() {
        gameManager.disconnect()
    }

    // Update player name
    fun setPlayerName(name: String) {
        gameManager.setPlayerName(name)
    }

    // Create a new game
    fun createGame() {
        gameManager.createGame()
    }

    // Update game settings
    fun updateGameSettings(settings: GameSettings) {
        gameManager.updateGameSettings(settings)
    }

    // Join an existing game
    fun joinGame(gameInfo: GameInfo) {
        gameManager.joinGame(gameInfo)
    }

    // Start the game (host only)
    fun startGame() {
        gameManager.startGame()
    }

    // Select order number
    fun selectOrderNumber(orderNumber: Int) {
        gameManager.selectOrderNumber(orderNumber)
    }

    // Reveal a characteristic
    fun revealCharacteristic(cardId: Long) {
        gameManager.revealCharacteristic(cardId)
    }

    // Vote for a player
    fun vote(targetPlayerId: Long) {
        gameManager.vote(targetPlayerId)
    }

    // Use an action card
    fun useAction(actionCardId: Long, targetPlayerId: Long) {
        gameManager.useAction(actionCardId, targetPlayerId)
    }

    // Kick a player (host only)
    fun kickPlayer(playerId: Long) {
        gameManager.kickPlayer(playerId)
    }

    // Leave the game
    fun leaveGame() {
        gameManager.leaveGame()
    }

    // Scan for available games
    fun scanForGames() {
        gameManager.scanForGames()
    }

    // Stop scanning for games
    fun stopScanningForGames() {
        gameManager.stopScanningForGames()
    }

    // Clean up resources
    fun cleanup() {
        gameManager.cleanup()
    }

    // Clear error state
    fun clearError() {
        val currentState = gameState.value
        if (currentState is AppGameState.Error) {
            // Restore previous state if available
            val previousState = currentState.previousState
            if (previousState != null) {
                (gameState as MutableStateFlow<AppGameState>).value = previousState
            } else {
                // Fall back to idle state if no previous state
                (gameState as MutableStateFlow<AppGameState>).value = AppGameState.Idle
            }
        }
    }
}

class GameManagerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameManagerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}