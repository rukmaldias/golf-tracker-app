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

class GetPlayersUseCaseTest {

    // Fake repository — no Mockito needed
    private class FakePlayerRepository(
        private val players: List<Player> = emptyList()
    ) : PlayerRepository {
        override fun getPlayers(): Flow<List<Player>> = flowOf(players)
        override fun getPlayer(id: String): Flow<Player> = flowOf(players.first { it.id == id })
        override suspend fun sync() {}
        override suspend fun syncPlayer(id: String) {}
    }

    private val testPlayer = Player(
        id        = "player_01",
        name      = "Rukmal Dias",
        handicap  = 10.4,
        avatarUrl = "https://i.pravatar.cc/150?img=68",
        shots     = emptyList()
    )

    @Test
    fun `invoke returns players from repository`() = runTest {
        val useCase = GetPlayersUseCase(FakePlayerRepository(listOf(testPlayer)))

        val result = useCase().first()

        assertEquals(1, result.size)
        assertEquals("player_01", result[0].id)
        assertEquals("Rukmal Dias", result[0].name)
    }

    @Test
    fun `invoke returns empty list when repository is empty`() = runTest {
        val useCase = GetPlayersUseCase(FakePlayerRepository(emptyList()))

        val result = useCase().first()

        assertEquals(emptyList<Player>(), result)
    }

    @Test
    fun `invoke returns all players`() = runTest {
        val players = listOf(
            testPlayer,
            testPlayer.copy(id = "player_02", name = "Jane Smith", handicap = 4.2)
        )
        val useCase = GetPlayersUseCase(FakePlayerRepository(players))

        val result = useCase().first()

        assertEquals(2, result.size)
    }
}