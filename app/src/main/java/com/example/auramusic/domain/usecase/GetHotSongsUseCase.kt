package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository

class GetHotSongsUseCase(private val songRepository: SongRepository) {
    suspend operator fun invoke(): Result<List<Song>> {
        // "Hot songs" tương ứng với các bài hát được nghe nhiều nhất (Most Played)
        return songRepository.getMostPlayedSongs(limit = 10)
    }
}