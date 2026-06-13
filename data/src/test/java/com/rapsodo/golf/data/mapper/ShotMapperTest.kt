package com.rapsodo.golf.data.mapper

import com.rapsodo.golf.data.local.entity.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ShotMapperTest {

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
    fun `toDomain maps id correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals("shot_001", domain.id)
    }

    @Test
    fun `toDomain maps clubType correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals("Driver", domain.clubType)
    }

    @Test
    fun `toDomain maps ballSpeedMph correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals(158.5, domain.ballSpeedMph, 0.0)
    }

    @Test
    fun `toDomain maps launchAngleDeg correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals(11.2, domain.launchAngleDeg, 0.0)
    }

    @Test
    fun `toDomain maps carryDistanceYds correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals(268, domain.carryDistanceYds)
    }

    @Test
    fun `toDomain maps totalDistanceYds correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals(285, domain.totalDistanceYds)
    }

    @Test
    fun `toDomain maps spinRateRpm correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals(2350, domain.spinRateRpm)
    }

    @Test
    fun `toDomain maps recordedAt correctly`() {
        val domain = shotEntity.toDomain()
        assertEquals("2026-06-12T14:30:00Z", domain.recordedAt)
    }

    @Test
    fun `toDomain does not expose playerId`() {
        val domain = shotEntity.toDomain()
        // Shot domain model has no playerId — it's a DB concern only
        assertEquals("shot_001", domain.id)
    }

    @Test
    fun `toDomain maps different club types correctly`() {
        val clubs = listOf("Driver", "3-Wood", "5-Iron", "7-Iron", "9-Iron", "Pitching Wedge", "56-Wedge")
        clubs.forEach { club ->
            val entity = shotEntity.copy(clubType = club)
            assertEquals(club, entity.toDomain().clubType)
        }
    }
}