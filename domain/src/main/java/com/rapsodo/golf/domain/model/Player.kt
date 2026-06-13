package com.rapsodo.golf.domain.model

data class Player(
    val id: String,
    val name: String,
    val handicap: Double,
    val avatarUrl: String,
    val shots: List<Shot>
)