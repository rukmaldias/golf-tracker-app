package com.rapsodo.golf.domain.repository

import com.rapsodo.golf.domain.model.Player
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getPlayers(): Flow<List<Player>>
    fun getPlayer(id: String): Flow<Player>
    suspend fun sync()
    suspend fun syncPlayer(id: String)
}