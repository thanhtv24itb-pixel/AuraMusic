package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.repository.SongRepository

class ToggleLikeUseCase(private val repository: SongRepository) {
    suspend operator fun invoke(userId: String, songId: String): Result<Boolean> {
        return repository.toggleLikeSong(userId, songId)
    }
}