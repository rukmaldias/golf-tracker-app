package com.rapsodo.golf.domain.usecase

import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.model.Shot
import com.rapsodo.golf.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPlayerUseCaseTest {

    private val testShot = Shot(
        id               = "shot_001",
        clubType         = "Driver",
        ballSpeedMph     = 158.5,
        launchAngleDeg   = 11.2,
        carryDistanceYds = 268,
        totalDistanceYds = 285,
        spinRateRpm      = 2350,
        recordedAt       = "2026-06-12T14:30:00Z"
    )

    private val testPlayer = Player(
        id        = "player_01",
        name      = "Rukmal Dias",
        handicap  = 10.4,
        avatarUrl = "https://i.pravatar.cc/150?img=68",
        shots     = listOf(testShot)
    )

    private val player2 = Player(
        id        = "player_02",
        name      = "Jane Smith",
        handicap  = 4.2,
        avatarUrl = "https://i.pravatar.cc/150?img=47",
        shots     = emptyList()
    )

    private class FakePlayerRepository(
        private val players: List<Player>
    ) : PlayerRepository {
        override fun getPlayers(): Flow<List<Player>> = flowOf(players)
        override fun getPlayer(id: String): Flow<Player> =
            flowOf(players.first { it.id == id })
        override suspend fun sync() {}
        override suspend fun syncPlayer(id: String) {}
    }

    @Test
    fun `invoke returns correct player by id`() = runTest {
        val useCase = GetPlayerUseCase(FakePlayerRepository(listOf(testPlayer, player2)))

        val result = useCase("player_01").first()

        assertEquals("player_01", result.id)
        assertEquals("Rukmal Dias", result.name)
        assertEquals(10.4, result.handicap, 0.0)
    }

    @Test
    fun `invoke returns second player correctly`() = runTest {
        val useCase = GetPlayerUseCase(FakePlayerRepository(listOf(testPlayer, player2)))

        val result = useCase("player_02").first()

        assertEquals("player_02", result.id)
        assertEquals("Jane Smith", result.name)
        assertEquals(4.2, result.handicap, 0.0)
    }

    @Test
    fun `invoke returns player with shots`() = runTest {
        val useCase = GetPlayerUseCase(FakePlayerRepository(listOf(testPlayer)))

        val result = useCase("player_01").first()

        assertEquals(1, result.shots.size)
        assertEquals("shot_001", result.shots[0].id)
        assertEquals("Driver", result.shots[0].clubType)
        assertEquals(158.5, result.shots[0].ballSpeedMph, 0.0)
        assertEquals(268, result.shots[0].carryDistanceYds)
        assertEquals(2350, result.shots[0].spinRateRpm)
    }

    @Test
    fun `invoke returns player with no shots`() = runTest {
        val useCase = GetPlayerUseCase(FakePlayerRepository(listOf(player2)))

        val result = useCase("player_02").first()

        assertEquals(emptyList<Shot>(), result.shots)
    }
}