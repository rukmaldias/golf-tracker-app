package com.rapsodo.golf.ui.players

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.usecase.GetPlayerUseCase
import com.rapsodo.golf.domain.logger.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import javax.inject.Inject

sealed interface PlayerDetailUiState {
    data object Loading : PlayerDetailUiState
    data class Success(val player: Player) : PlayerDetailUiState
    data class Error(val message: String) : PlayerDetailUiState
}

@HiltViewModel
class PlayerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlayer: GetPlayerUseCase
) : ViewModel() {

    private val playerId: String = checkNotNull(savedStateHandle["playerId"])

    val uiState: StateFlow<PlayerDetailUiState> = getPlayer(playerId)
        .map<Player, PlayerDetailUiState> { player ->
            Napier.d(tag = AppLogger.TAG) { "Player loaded: ${player.name}" }
            PlayerDetailUiState.Success(player)
        }
        .catch { e ->
            Napier.e(tag = AppLogger.TAG, throwable = e) { "Failed to load player $playerId" }
            emit(PlayerDetailUiState.Error(e.message ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerDetailUiState.Loading
        )
}