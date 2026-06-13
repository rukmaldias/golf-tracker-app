package com.rapsodo.golf.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    @SerialName("id")        val id: String,
    @SerialName("name")      val name: String,
    @SerialName("handicap")  val handicap: Double,
    @SerialName("avatarUrl") val avatarUrl: String,
    @SerialName("shots")     val shots: List<ShotDto> = emptyList()
)