package com.rapsodo.golf.data.mapper

import com.rapsodo.golf.data.local.entity.PlayerEntity
import com.rapsodo.golf.data.local.entity.PlayerWithShots
import com.rapsodo.golf.data.local.entity.ShotEntity
import com.rapsodo.golf.data.remote.dto.PlayerDto
import com.rapsodo.golf.domain.model.Player
import com.rapsodo.golf.domain.model.Shot

// DTO → Entity
fun PlayerDto.toEntity() = PlayerEntity(
    id        = id,
    name      = name,
    handicap  = handicap,
    avatarUrl = avatarUrl
)

fun PlayerDto.shotsToEntities() = shots.map { shot ->
    ShotEntity(
        id               = shot.id,
        playerId         = id,
        clubType         = shot.clubType,
        ballSpeedMph     = shot.ballSpeedMph,
        launchAngleDeg   = shot.launchAngleDeg,
        carryDistanceYds = shot.carryDistanceYds,
        totalDistanceYds = shot.totalDistanceYds,
        spinRateRpm      = shot.spinRateRpm,
        recordedAt       = shot.recordedAt
    )
}

// Entity → Domain
fun PlayerWithShots.toDomain() = Player(
    id        = player.id,
    name      = player.name,
    handicap  = player.handicap,
    avatarUrl = player.avatarUrl,
    shots     = shots.map { it.toDomain() }
)

fun ShotEntity.toDomain() = Shot(
    id               = id,
    clubType         = clubType,
    ballSpeedMph     = ballSpeedMph,
    launchAngleDeg   = launchAngleDeg,
    carryDistanceYds = carryDistanceYds,
    totalDistanceYds = totalDistanceYds,
    spinRateRpm      = spinRateRpm,
    recordedAt       = recordedAt
)