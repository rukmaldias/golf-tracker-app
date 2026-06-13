package com.rapsodo.golf.data.mapper

import com.rapsodo.golf.data.local.entity.PlayerEntity
import com.rapsodo.golf.data.local.entity.PlayerWithShots
import com.rapsodo.golf.data.local.entity.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerWithShotsMapperTest {

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

    @Test
    fun `toDomain maps player fields correctly`() {
        val domain = PlayerWithShots(playerEntity, listOf(shotEntity)).toDomain()

        assertEquals("player_01", domain.id)
        assertEquals("Rukmal Dias", domain.name)
        assertEquals(10.4, domain.handicap, 0.0)
        assertEquals("https://i.pravatar.cc/150?img=68", domain.avatarUrl)
    }

    @Test
    fun `toDomain maps nested shots correctly`() {
        val domain = PlayerWithShots(playerEntity, listOf(shotEntity)).toDomain()

        assertEquals(1, domain.shots.size)
        assertEquals("shot_001", domain.shots[0].id)
        assertEquals("Driver", domain.shots[0].clubType)
        assertEquals(158.5, domain.shots[0].ballSpeedMph, 0.0)
        assertEquals(268, domain.shots[0].carryDistanceYds)
        assertEquals(2350, domain.shots[0].spinRateRpm)
    }

    @Test
    fun `toDomain with no shots maps to empty list`() {
        val domain = PlayerWithShots(playerEntity, emptyList()).toDomain()

        assertEquals(emptyList<Any>(), domain.shots)
    }

    @Test
    fun `toDomain with multiple shots maps all`() {
        val shot2 = shotEntity.copy(id = "shot_002", clubType = "7-Iron")
        val domain = PlayerWithShots(playerEntity, listOf(shotEntity, shot2)).toDomain()

        assertEquals(2, domain.shots.size)
        assertEquals("7-Iron", domain.shots[1].clubType)
    }
}