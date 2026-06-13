package com.rapsodo.golf.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rapsodo.golf.data.local.entity.PlayerEntity
import com.rapsodo.golf.data.local.entity.PlayerWithShots
import com.rapsodo.golf.data.local.entity.ShotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Transaction
    @Query("SELECT * FROM players ORDER BY handicap ASC")
    fun observeAllPlayers(): Flow<List<PlayerWithShots>>

    @Transaction
    @Query("SELECT * FROM players WHERE id = :id")
    fun observePlayer(id: String): Flow<PlayerWithShots>

    @Upsert
    suspend fun upsertPlayers(players: List<PlayerEntity>)

    @Upsert
    suspend fun upsertShots(shots: List<ShotEntity>)

    @Query("DELETE FROM players")
    suspend fun deleteAll()
}