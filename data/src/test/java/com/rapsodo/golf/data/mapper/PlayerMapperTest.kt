package com.rapsodo.golf.data.mapper

import com.rapsodo.golf.data.remote.dto.PlayerDto
import com.rapsodo.golf.data.remote.dto.ShotDto
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerMapperTest {

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

    @Test
    fun `toEntity maps all fields correctly`() {
        val entity = playerDto.toEntity()

        assertEquals("player_01", entity.id)
        assertEquals("Rukmal Dias", entity.name)
        assertEquals(10.4, entity.handicap, 0.0)
        assertEquals("https://i.pravatar.cc/150?img=68", entity.avatarUrl)
    }

    @Test
    fun `shotsToEntities maps playerId correctly`() {
        val entities = playerDto.shotsToEntities()

        assertEquals(1, entities.size)
        assertEquals("player_01", entities[0].playerId) // foreign key wired correctly
        assertEquals("shot_001", entities[0].id)
        assertEquals("Driver", entities[0].clubType)
    }

    @Test
    fun `shotsToEntities with empty shots returns empty list`() {
        val dto = playerDto.copy(shots = emptyList())
        assertEquals(emptyList<Any>(), dto.shotsToEntities())
    }
}