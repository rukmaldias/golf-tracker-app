package com.rapsodo.golf.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PlayerWithShots(
    @Embedded val player: PlayerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playerId"
    )
    val shots: List<ShotEntity>
)