package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository

class GetRecentSongsUseCase(private val repository: SongRepository) {
    suspend operator fun invoke(limit: Int = 10): Result<List<Song>> = repository.getRecentSongs(limit)
}