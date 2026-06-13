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

class PlayerRepositoryImplTest {

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
        var getPlayersCallCount = 0

        override suspend fun getPlayers(): List<PlayerDto> {
            getPlayersCallCount++
            if (shouldFail) throw Exception("Network error")
            return players
        }

        override suspend fun getPlayer(id: String): PlayerDto =
            players.first { it.id == id }
    }

    // Test data

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

    private val playerEntity = PlayerEntity(
        id        = "player_01",
        name      = "Rukmal Dias",
        handicap  = 10.4,
        avatarUrl = "https://i.pravatar.cc/150?img=68"
    )

    private val shotEntity = ShotEntity(
        id               = "shot_001",
        playerId         = "player_01",
        clubType         = "Driver",
        ballSpeedMph     = 158.5,
        launchAngleDeg   = 11.2,
        carryDistanceYds = 268,
        totalDistanceYds = 285,
        spinRateRpm      = 2350,
        recordedAt       = "2026-06-12T14:30:00Z"
    )

    // Tests

    @Test
    fun `getPlayers returns data from Room not API`() = runTest {
        val dao = FakePlayerDao(listOf(PlayerWithShots(playerEntity, listOf(shotEntity))))
        val api = FakePlayerApi() // empty API
        val repo = PlayerRepositoryImpl(api, dao)

        val result = repo.getPlayers().first()

        // Room has data — API never called
        assertEquals(0, api.getPlayersCallCount)
        assertEquals(1, result.size)
        assertEquals("player_01", result[0].id)
        assertEquals("Rukmal Dias", result[0].name)
    }

    @Test
    fun `getPlayers returns empty when Room is empty`() = runTest {
        val dao = FakePlayerDao(emptyList())
        val repo = PlayerRepositoryImpl(FakePlayerApi(), dao)

        val result = repo.getPlayers().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `sync fetches from API and saves to Room`() = runTest {
        val dao = FakePlayerDao()
        val api = FakePlayerApi(listOf(playerDto))
        val repo = PlayerRepositoryImpl(api, dao)

        repo.sync()

        assertEquals(1, api.getPlayersCallCount)
        assertEquals(1, dao.upsertedPlayers.size)
        assertEquals("player_01", dao.upsertedPlayers[0].id)
        assertEquals(1, dao.upsertedShots.size)
        assertEquals("shot_001", dao.upsertedShots[0].id)
        assertEquals("player_01", dao.upsertedShots[0].playerId) // FK wired
    }

    @Test
    fun `sync maps DTO fields correctly to entities`() = runTest {
        val dao = FakePlayerDao()
        val repo = PlayerRepositoryImpl(FakePlayerApi(listOf(playerDto)), dao)

        repo.sync()

        val savedPlayer = dao.upsertedPlayers[0]
        assertEquals("Rukmal Dias", savedPlayer.name)
        assertEquals(10.4, savedPlayer.handicap, 0.0)
        assertEquals("https://i.pravatar.cc/150?img=68", savedPlayer.avatarUrl)

        val savedShot = dao.upsertedShots[0]
        assertEquals("Driver", savedShot.clubType)
        assertEquals(158.5, savedShot.ballSpeedMph, 0.0)
        assertEquals(268, savedShot.carryDistanceYds)
        assertEquals(2350, savedShot.spinRateRpm)
    }

    @Test
    fun `sync with multiple players saves all shots with correct playerIds`() = runTest {
        val player2Dto = playerDto.copy(
            id   = "player_02",
            name = "Jane Smith",
            shots = listOf(shotDto.copy(id = "shot_002"))
        )
        val dao = FakePlayerDao()
        val repo = PlayerRepositoryImpl(FakePlayerApi(listOf(playerDto, player2Dto)), dao)

        repo.sync()

        assertEquals(2, dao.upsertedPlayers.size)
        assertEquals(2, dao.upsertedShots.size)
        // each shot linked to its own player
        assertEquals("player_01", dao.upsertedShots.first { it.id == "shot_001" }.playerId)
        assertEquals("player_02", dao.upsertedShots.first { it.id == "shot_002" }.playerId)
    }

    @Test
    fun `sync throws when API fails`() = runTest {
        val repo = PlayerRepositoryImpl(FakePlayerApi(shouldFail = true), FakePlayerDao())

        var threw = false
        runCatching { repo.sync() }.onFailure { threw = true }

        assertTrue(threw)
    }

    @Test
    fun `getPlayer returns correct player by id`() = runTest {
        val entity2 = playerEntity.copy(id = "player_02", name = "Jane Smith")
        val dao = FakePlayerDao(listOf(
            PlayerWithShots(playerEntity, emptyList()),
            PlayerWithShots(entity2, emptyList())
        ))
        val repo = PlayerRepositoryImpl(FakePlayerApi(), dao)

        val result = repo.getPlayer("player_02").first()

        assertEquals("player_02", result.id)
        assertEquals("Jane Smith", result.name)
    }
}