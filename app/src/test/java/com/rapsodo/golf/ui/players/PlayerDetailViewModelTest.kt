package com.rapsodo.golf.ui.players

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.model.Shot
import com.rapsodo.golf.domain.repository.PlayerRepository
import com.rapsodo.golf.domain.usecase.GetPlayerUseCase

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakePlayerRepository(
        private val players: List<Player> = emptyList(),
        private val shouldFail: Boolean = false
    ) : PlayerRepository {
        override fun getPlayers(): Flow<List<Player>> = flowOf(players)
        override fun getPlayer(id: String): Flow<Player> = flow {
            if (shouldFail) throw Exception("Player not found")
            emit(players.first { it.id == id })
        }
        override suspend fun sync() {}
        override suspend fun syncPlayer(id: String) {}
    }

    private val testShot = Shot(
        id = "shot_001",
        clubType = "Driver",
        ballSpeedMph = 158.5,
        launchAngleDeg = 11.2,
        carryDistanceYds = 268,
        totalDistanceYds = 285,
        spinRateRpm = 2350,
        recordedAt = "2026-06-12T14:30:00Z"
    )

    private val testPlayer = Player(
        id = "player_01",
        name = "Rukmal Dias",
        handicap = 10.4,
        avatarUrl = "https://i.pravatar.cc/150?img=68",
        shots = listOf(testShot)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        playerId: String = "player_01",
        players: List<Player> = listOf(testPlayer),
        shouldFail: Boolean = false
    ): PlayerDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("playerId" to playerId))
        val repo = FakePlayerRepository(players, shouldFail)
        return PlayerDetailViewModel(savedStateHandle, GetPlayerUseCase(repo))
    }

    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(PlayerDetailUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState is Success when player is found`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading

            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state is PlayerDetailUiState.Success)
            assertEquals("Rukmal Dias", (state as PlayerDetailUiState.Success).player.name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState is Error when player not found`() = runTest {
        val viewModel = createViewModel(shouldFail = true)

        viewModel.uiState.test {
            awaitItem() // Loading

            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state is PlayerDetailUiState.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success state contains correct player id`() = runTest {
        val viewModel = createViewModel(playerId = "player_01")

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as PlayerDetailUiState.Success
            assertEquals("player_01", state.player.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success state contains player shots`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as PlayerDetailUiState.Success
            assertEquals(1, state.player.shots.size)
            assertEquals("Driver", state.player.shots[0].clubType)
            assertEquals(158.5, state.player.shots[0].ballSpeedMph, 0.0)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `correct player returned when multiple players exist`() = runTest {
        val player2 = testPlayer.copy(id = "player_02", name = "Jane Smith")
        val viewModel = createViewModel(
            playerId = "player_02",
            players = listOf(testPlayer, player2)
        )

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as PlayerDetailUiState.Success
            assertEquals("player_02", state.player.id)
            assertEquals("Jane Smith", state.player.name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `player with no shots succeeds with empty shots list`() = runTest {
        val playerNoShots = testPlayer.copy(shots = emptyList())
        val viewModel = createViewModel(players = listOf(playerNoShots))

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as PlayerDetailUiState.Success
            assertTrue(state.player.shots.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }
}