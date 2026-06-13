package com.rapsodo.golf.domain.model

data class Shot(
    val id: String,
    val clubType: String,
    val ballSpeedMph: Double,
    val launchAngleDeg: Double,
    val carryDistanceYds: Int,
    val totalDistanceYds: Int,
    val spinRateRpm: Int,
    val recordedAt: String
)