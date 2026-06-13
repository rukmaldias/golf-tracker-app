package com.rapsodo.golf.ui.players

import app.cash.turbine.test
import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.repository.PlayerRepository
import com.rapsodo.golf.domain.usecase.GetPlayersUseCase
import com.rapsodo.golf.domain.model.Shot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake repository with controllable state
    private class FakePlayerRepository(
        private val playersFlow: MutableStateFlow<List<Player>> = MutableStateFlow(emptyList()),
        private val shouldSyncFail: Boolean = false
    ) : PlayerRepository {
        var syncCallCount = 0

        override fun getPlayers(): Flow<List<Player>> = playersFlow
        override fun getPlayer(id: String): Flow<Player> = throw NotImplementedError()
        override suspend fun sync() {
            syncCallCount++
            if (shouldSyncFail) throw Exception("Network error")
        }
        override suspend fun syncPlayer(id: String) {}

        fun emitPlayers(players: List<Player>) {
            playersFlow.value = players
        }
    }

    private val testPlayer = Player(
        id = "player_01",
        name = "Rukmal Dias",
        handicap = 10.4,
        avatarUrl = "https://i.pravatar.cc/150?img=68",
        shots = emptyList()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        val repo = FakePlayerRepository()
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.uiState.test {
            assertEquals(PlayerUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState is Success when players are available`() = runTest {
        val repo = FakePlayerRepository()
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.uiState.test {
            awaitItem() // Loading

            repo.emitPlayers(listOf(testPlayer))
            val success = awaitItem()

            assertTrue(success is PlayerUiState.Success)
            assertEquals(1, (success as PlayerUiState.Success).players.size)
            assertEquals("Rukmal Dias", success.players[0].name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState is Empty when DB empty and sync fails`() = runTest {
        val repo = FakePlayerRepository(shouldSyncFail = true)
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.uiState.test {
            awaitItem() // Loading

            testDispatcher.scheduler.advanceUntilIdle() // let sync complete

            assertEquals(PlayerUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncFromNetwork is called on init`() = runTest {
        val repo = FakePlayerRepository()
        PlayerViewModel(GetPlayersUseCase(repo), repo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.syncCallCount)
    }

    @Test
    fun `syncFromNetwork resets syncFailed before retrying`() = runTest {
        val repo = FakePlayerRepository(shouldSyncFail = true)
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        testDispatcher.scheduler.advanceUntilIdle() // first sync fails → Empty

        viewModel.uiState.test {
            awaitItem() // Empty (current state)

            // Now emit players so retry succeeds
            repo.emitPlayers(listOf(testPlayer))
            val success = awaitItem()

            assertTrue(success is PlayerUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success state contains all players from repository`() = runTest {
        val players = listOf(
            testPlayer.copy(id = "p1", name = "High", handicap = 18.0),
            testPlayer.copy(id = "p2", name = "Low",  handicap = 2.0),
            testPlayer.copy(id = "p3", name = "Mid",  handicap = 10.0)
        )
        val repo = FakePlayerRepository(MutableStateFlow(players))
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.uiState.test {
            // skip Loading if it appears first
            val first = awaitItem()
            val state = if (first is PlayerUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
            } else {
                first
            }

            assertTrue(state is PlayerUiState.Success)
            assertEquals(3, (state as PlayerUiState.Success).players.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private val playerWithDriver = testPlayer.copy(
        id    = "p_driver",
        name  = "Tom Watson",
        shots = listOf(
            Shot("s1", "Driver", 165.0, 11.0, 280, 295, 2100, "2026-06-12T08:00:00Z")
        )
    )

    private val playerWithIron = testPlayer.copy(
        id    = "p_iron",
        name  = "Jane Smith",
        shots = listOf(
            Shot("s2", "7-Iron", 118.0, 18.0, 155, 161, 6100, "2026-06-12T09:00:00Z")
        )
    )

    @Test
    fun `search by name filters players correctly`() = runTest {
        val players = listOf(testPlayer, playerWithDriver, playerWithIron)
        val repo = FakePlayerRepository(MutableStateFlow(players))
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.onSearchQueryChanged("Jane")

        viewModel.uiState.test {
            val first = awaitItem()
            val state = if (first is PlayerUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
            } else first

            assertTrue(state is PlayerUiState.Success)
            val result = (state as PlayerUiState.Success).players
            assertEquals(1, result.size)
            assertEquals("Jane Smith", result[0].name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search is case insensitive`() = runTest {
        val repo = FakePlayerRepository(MutableStateFlow(listOf(testPlayer, playerWithIron)))
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.onSearchQueryChanged("jane")

        viewModel.uiState.test {
            val first = awaitItem()
            val state = if (first is PlayerUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
            } else first

            val result = (state as PlayerUiState.Success).players
            assertEquals(1, result.size)
            assertEquals("Jane Smith", result[0].name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `club filter shows only players with that club`() = runTest {
        val players = listOf(playerWithDriver, playerWithIron)
        val repo = FakePlayerRepository(MutableStateFlow(players))
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.onClubSelected("Driver")

        viewModel.uiState.test {
            val first = awaitItem()
            val state = if (first is PlayerUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
            } else first

            val result = (state as PlayerUiState.Success).players
            assertEquals(1, result.size)
            assertEquals("Tom Watson", result[0].name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing search returns all players`() = runTest {
        val players = listOf(testPlayer, playerWithDriver, playerWithIron)
        val repo = FakePlayerRepository(MutableStateFlow(players))
        val viewModel = PlayerViewModel(GetPlayersUseCase(repo), repo)

        viewModel.onSearchQueryChanged("Jane")
        viewModel.onSearchQueryChanged("") // clear

        viewModel.uiState.test {
            val first = awaitItem()
            val state = if (first is PlayerUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
            } else first

            val result = (state as PlayerUiState.Success).players
            assertEquals(3, result.size)

            cancelAndIgnoreRemainingEvents()
        }
    }
}