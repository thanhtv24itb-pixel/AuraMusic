package com.example.auramusic.domain.usecase



import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository

class GetSongsByCategoryUseCase(private val songRepository: SongRepository) {
    suspend operator fun invoke(category: String): Result<List<Song>> {
        return songRepository.getSongsByCategory(category)
    }
}