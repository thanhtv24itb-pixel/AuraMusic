package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository
import kotlinx.coroutines.flow.first

class GetHotSongsUseCase(private val songRepository: SongRepository) {
    suspend operator fun invoke(): Result<List<Song>> {
        return try {
            // "Hot songs" tương ứng với các bài hát được nghe nhiều nhất (Most Played)
            val songs = songRepository.getMostPlayedSongs(limit = 10).first()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
