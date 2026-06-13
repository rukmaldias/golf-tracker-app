package com.rapsodo.golf.data.remote

import com.rapsodo.golf.data.remote.dto.PlayerDto
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

interface PlayerApi {
    @GET("players")
    suspend fun getPlayers(): List<PlayerDto>

    @GET("players/{id}")
    suspend fun getPlayer(@Path("id") id: String): PlayerDto
}