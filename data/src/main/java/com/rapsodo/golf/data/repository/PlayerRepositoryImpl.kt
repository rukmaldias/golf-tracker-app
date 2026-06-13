package com.rapsodo.golf.data.repository

import com.rapsodo.golf.data.local.dao.PlayerDao
import com.rapsodo.golf.data.mapper.shotsToEntities
import com.rapsodo.golf.data.mapper.toDomain
import com.rapsodo.golf.data.mapper.toEntity
import com.rapsodo.golf.data.remote.PlayerApi
import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.repository.PlayerRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val api: PlayerApi,
    private val dao: PlayerDao
) : PlayerRepository {

    // Room is the single source of truth.. UI always reads from DB
    override fun getPlayers(): Flow<List<Player>> =
        dao.observeAllPlayers().map { list -> list.map { it.toDomain() } }

    override fun getPlayer(id: String): Flow<Player> =
        dao.observePlayer(id).map { it.toDomain() }

    override suspend fun sync() {
        val dtos = api.getPlayers()
        dao.upsertPlayers(dtos.map { it.toEntity() })
        dao.upsertShots(dtos.flatMap { it.shotsToEntities() })
    }

    override suspend fun syncPlayer(id: String) {
        val dto = api.getPlayer(id)
        dao.upsertPlayers(listOf(dto.toEntity()))
        dao.upsertShots(dto.shotsToEntities())
    }
}