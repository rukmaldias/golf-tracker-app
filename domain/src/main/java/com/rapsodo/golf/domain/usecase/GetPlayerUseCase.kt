package com.rapsodo.golf.domain.usecase

import com.rapsodo.golf.domain.repository.PlayerRepository
import javax.inject.Inject

class GetPlayerUseCase @Inject constructor(
    private val repository: PlayerRepository
) {
    operator fun invoke(id: String) = repository.getPlayer(id)
}