package com.rapsodo.golf.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShotDto(
    @SerialName("id")               val id: String,
    @SerialName("clubType")         val clubType: String,
    @SerialName("ballSpeedMph")     val ballSpeedMph: Double,
    @SerialName("launchAngleDeg")   val launchAngleDeg: Double,
    @SerialName("carryDistanceYds") val carryDistanceYds: Int,
    @SerialName("totalDistanceYds") val totalDistanceYds: Int,
    @SerialName("spinRateRpm")      val spinRateRpm: Int,
    @SerialName("recordedAt")       val recordedAt: String
)