package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository

class SearchSongsUseCase(private val repository: SongRepository) {
    suspend operator fun invoke(query: String): Result<List<Song>> = repository.searchSongs(query)
}