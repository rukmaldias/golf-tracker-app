package com.rapsodo.golf.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.repository.PlayerRepository
import com.rapsodo.golf.domain.usecase.GetPlayersUseCase
import com.rapsodo.golf.domain.logger.AppLogger

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import io.github.aakira.napier.Napier

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Success(val players: List<Player>) : PlayerUiState
    data object Empty : PlayerUiState  // DB empty + API failed
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getPlayers: GetPlayersUseCase,
    private val repository: PlayerRepository
) : ViewModel() {

    private val _syncFailed = MutableStateFlow(false)

    val uiState: StateFlow<PlayerUiState> = combine(
        getPlayers(),
        _syncFailed
    ) { players, failed ->
        when {
            players.isNotEmpty() -> PlayerUiState.Success(players)
            failed               -> PlayerUiState.Empty
            else                 -> PlayerUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState.Loading
    )

    init {
        Napier.i(tag = AppLogger.TAG) { "Calls sync from network.." }
        syncFromNetwork()
    }

    fun syncFromNetwork() {
        viewModelScope.launch {
            _syncFailed.value = false
            runCatching { repository.sync() }
                .onSuccess {
                    Napier.i(tag = AppLogger.TAG) { "Sync succeeded" }
                }
                .onFailure {
                    Napier.e(tag = AppLogger.TAG, throwable = it) { "Sync failed" }
                    _syncFailed.value = true
                }
        }
    }
}