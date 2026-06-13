package com.rapsodo.golf.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shots",
    foreignKeys = [ForeignKey(
        entity = PlayerEntity::class,
        parentColumns = ["id"],
        childColumns = ["playerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("playerId")]
)
data class ShotEntity(
    @PrimaryKey val id: String,
    val playerId: String,
    val clubType: String,
    val ballSpeedMph: Double,
    val launchAngleDeg: Double,
    val carryDistanceYds: Int,
    val totalDistanceYds: Int,
    val spinRateRpm: Int,
    val recordedAt: String
)