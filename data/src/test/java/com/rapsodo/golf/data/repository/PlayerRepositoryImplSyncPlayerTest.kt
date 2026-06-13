package com.rapsodo.golf.data.repository

import com.rapsodo.golf.data.local.dao.PlayerDao
import com.rapsodo.golf.data.local.entity.PlayerEntity
import com.rapsodo.golf.data.local.entity.PlayerWithShots
import com.rapsodo.golf.data.local.entity.ShotEntity
import com.rapsodo.golf.data.remote.PlayerApi
import com.rapsodo.golf.data.remote.dto.PlayerDto
import com.rapsodo.golf.data.remote.dto.ShotDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRepositoryImplSyncPlayerTest {

    // Fakes --

    private class FakePlayerDao(
        initial: List<PlayerWithShots> = emptyList()
    ) : PlayerDao {
        private val store = MutableStateFlow(initial)
        val upsertedPlayers = mutableListOf<PlayerEntity>()
        val upsertedShots = mutableListOf<ShotEntity>()

        override fun observeAllPlayers(): Flow<List<PlayerWithShots>> = store
        override fun observePlayer(id: String): Flow<PlayerWithShots> =
            store.map { list -> list.first { it.player.id == id } }

        override suspend fun upsertPlayers(players: List<PlayerEntity>) {
            upsertedPlayers.addAll(players)
            val existing = store.value.map { it.player.id }
            val merged = store.value.toMutableList()
            players.forEach { p ->
                if (p.id !in existing) merged.add(PlayerWithShots(p, emptyList()))
            }
            store.value = merged
        }

        override suspend fun upsertShots(shots: List<ShotEntity>) {
            upsertedShots.addAll(shots)
        }

        override suspend fun deleteAll() {
            store.value = emptyList()
        }
    }

    private class FakePlayerApi(
        private val players: List<PlayerDto> = emptyList(),
        private val shouldFail: Boolean = false
    ) : PlayerApi {
        override suspend fun getPlayers(): List<PlayerDto> = players
        override suspend fun getPlayer(id: String): PlayerDto {
            if (shouldFail) throw Exception("Network error")
            return players.first { it.id == id }
        }
    }

    // Test data --

    private val shotDto = ShotDto(
        id               = "shot_001",
        clubType         = "Driver",
        ballSpeedMph     = 158.5,
        launchAngleDeg   = 11.2,
        carryDistanceYds = 268,
        totalDistanceYds = 285,
        spinRateRpm      = 2350,
        recordedAt       = "2026-06-12T14:30:00Z"
    )

    private val playerDto = PlayerDto(
        id        = "player_01",
        name      = "Rukmal Dias",
        handicap  = 10.4,
        avatarUrl = "https://i.pravatar.cc/150?img=68",
        shots     = listOf(shotDto)
    )

    // Tests --

    @Test
    fun `syncPlayer fetches single player by id from API`() = runTest {
        val dao = FakePlayerDao()
        val repo = PlayerRepositoryImpl(FakePlayerApi(listOf(playerDto)), dao)

        repo.syncPlayer("player_01")

        assertEquals(1, dao.upsertedPlayers.size)
        assertEquals("player_01", dao.upsertedPlayers[0].id)
    }

    @Test
    fun `syncPlayer saves only that player not others`() = runTest {
        val player2 = playerDto.copy(id = "player_02", name = "Jane Smith")
        val dao = FakePlayerDao()
        val repo = PlayerRepositoryImpl(FakePlayerApi(listOf(playerDto, player2)), dao)

        repo.syncPlayer("player_01")

        // Only one player upserted — not player_02
        assertEquals(1, dao.upsertedPlayers.size)
        assertEquals("player_01", dao.upsertedPlayers[0].id)
    }

    @Test
    fun `syncPlayer saves shots for that player`() = runTest {
        val dao = FakePlayerDao()
        val repo = PlayerRepositoryImpl(FakePlayerApi(listOf(playerDto)), dao)

        repo.syncPlayer("player_01")

        assertEquals(1, dao.upsertedShots.size)
        assertEquals("shot_001", dao.upsertedShots[0].id)
        assertEquals("player_01", dao.upsertedShots[0].playerId)
    }

    @Test
    fun `syncPlayer maps all shot fields correctly`() = runTest {
        val dao = FakePlayerDao()
        val repo = PlayerRepositoryImpl(FakePlayerApi(listOf(playerDto)), dao)

        repo.syncPlayer("player_01")

        val shot = dao.upsertedShots[0]
        assertEquals("Driver", shot.clubType)
        assertEquals(158.5, shot.ballSpeedMph, 0.0)
        assertEquals(11.2, shot.launchAngleDeg, 0.0)
        assertEquals(268, shot.carryDistanceYds)
        assertEquals(285, shot.totalDistanceYds)
        assertEquals(2350, shot.spinRateRpm)
        assertEquals("2026-06-12T14:30:00Z", shot.recordedAt)
    }

    @Test
    fun `syncPlayer throws when API fails`() = runTest {
        val repo = PlayerRepositoryImpl(
            FakePlayerApi(listOf(playerDto), shouldFail = true),
            FakePlayerDao()
        )

        var threw = false
        runCatching { repo.syncPlayer("player_01") }.onFailure { threw = true }

        assertTrue(threw)
    }

    @Test
    fun `syncPlayer with no shots saves empty shots list`() = runTest {
        val playerWithNoShots = playerDto.copy(shots = emptyList())
        val dao = FakePlayerDao()
        val repo = PlayerRepositoryImpl(FakePlayerApi(listOf(playerWithNoShots)), dao)

        repo.syncPlayer("player_01")

        assertEquals(1, dao.upsertedPlayers.size)
        assertTrue(dao.upsertedShots.isEmpty())
    }
}