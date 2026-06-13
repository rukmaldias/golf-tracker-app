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
import io.github.aakira.napier.Napier
import javax.inject.Inject

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Success(val players: List<Player>) : PlayerUiState
    data object Empty : PlayerUiState
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getPlayers: GetPlayersUseCase,
    private val repository: PlayerRepository
) : ViewModel() {

    private val _syncFailed = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")
    val selectedClub = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlayerUiState> = combine(
        getPlayers(),
        _syncFailed,
        searchQuery,
        selectedClub
    ) { players, failed, query, club ->
        when {
            players.isNotEmpty() -> {
                val filtered = players
                    .filter { player ->
                        query.isBlank() || player.name.contains(query, ignoreCase = true)
                    }
                    .filter { player ->
                        club == null || player.shots.any { it.clubType == club }
                    }
                PlayerUiState.Success(filtered)
            }
            failed -> PlayerUiState.Empty
            else   -> PlayerUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState.Loading
    )

    // All unique club types across all players — for filter chips
    val availableClubs: StateFlow<List<String>> = getPlayers()
        .combine(MutableStateFlow(Unit)) { players, _ ->
            players.flatMap { it.shots }
                .map { it.clubType }
                .distinct()
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        syncFromNetwork()
    }

    fun syncFromNetwork() {
        viewModelScope.launch {
            _syncFailed.value = false
            runCatching { repository.sync() }
                .onSuccess { Napier.i(tag = AppLogger.TAG) { "Sync succeeded" } }
                .onFailure {
                    Napier.e(tag = AppLogger.TAG, throwable = it) { "Sync failed" }
                    _syncFailed.value = true
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        Napier.d(tag = AppLogger.TAG) { "Search query: $query" }
        searchQuery.value = query
    }

    fun onClubSelected(club: String?) {
        Napier.d(tag = AppLogger.TAG) { "Club filter: $club" }
        selectedClub.value = club
    }
}