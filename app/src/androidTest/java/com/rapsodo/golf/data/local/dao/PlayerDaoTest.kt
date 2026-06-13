package com.rapsodo.golf.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rapsodo.golf.data.local.AppDatabase
import com.rapsodo.golf.data.local.entity.PlayerEntity
import com.rapsodo.golf.data.local.entity.ShotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PlayerDao

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

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.playerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndObservePlayers() = runTest {
        dao.upsertPlayers(listOf(playerEntity))
        dao.upsertShots(listOf(shotEntity))

        val result = dao.observeAllPlayers().first()

        assertEquals(1, result.size)
        assertEquals("player_01", result[0].player.id)
        assertEquals("Rukmal Dias", result[0].player.name)
    }

    @Test
    fun observeAllPlayers_orderedByHandicapAsc() = runTest {
        dao.upsertPlayers(listOf(
            playerEntity.copy(id = "p1", handicap = 18.0),
            playerEntity.copy(id = "p2", handicap = 2.0),
            playerEntity.copy(id = "p3", handicap = 10.0)
        ))

        val result = dao.observeAllPlayers().first()

        assertEquals("p2", result[0].player.id) // handicap 2.0 first
        assertEquals("p3", result[1].player.id) // handicap 10.0
        assertEquals("p1", result[2].player.id) // handicap 18.0 last
    }

    @Test
    fun upsertPlayers_updatesExistingPlayer() = runTest {
        dao.upsertPlayers(listOf(playerEntity))
        dao.upsertPlayers(listOf(playerEntity.copy(name = "Rukmal Updated")))

        val result = dao.observeAllPlayers().first()

        assertEquals(1, result.size) // no duplicate
        assertEquals("Rukmal Updated", result[0].player.name)
    }

    @Test
    fun shotsAreLinkedToPlayer() = runTest {
        dao.upsertPlayers(listOf(playerEntity))
        dao.upsertShots(listOf(
            shotEntity,
            shotEntity.copy(id = "shot_002", clubType = "7-Iron")
        ))

        val result = dao.observeAllPlayers().first()

        assertEquals(2, result[0].shots.size)
    }

    @Test
    fun deleteAll_removesPlayersAndShots() = runTest {
        dao.upsertPlayers(listOf(playerEntity))
        dao.upsertShots(listOf(shotEntity))
        dao.deleteAll()

        val result = dao.observeAllPlayers().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observePlayer_byId() = runTest {
        dao.upsertPlayers(listOf(
            playerEntity,
            playerEntity.copy(id = "player_02", name = "Jane Smith")
        ))

        val result = dao.observePlayer("player_02").first()

        assertEquals("player_02", result.player.id)
        assertEquals("Jane Smith", result.player.name)
    }

    @Test
    fun shotsDeletedWhenPlayerDeleted_cascades() = runTest {
        dao.upsertPlayers(listOf(playerEntity))
        dao.upsertShots(listOf(shotEntity))
        dao.deleteAll()

        // Re-insert player without shots to verify shots were cascade-deleted
        dao.upsertPlayers(listOf(playerEntity))
        val result = dao.observeAllPlayers().first()

        assertTrue(result[0].shots.isEmpty())
    }
}