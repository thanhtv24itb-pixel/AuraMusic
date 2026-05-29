package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository

class GetAllSongsUseCase(private val songRepository: SongRepository) {
    suspend operator fun invoke(): Result<List<Song>> {
        // Cập nhật để gọi phương thức getRecentSongs (Mới ra mắt)
        return songRepository.getRecentSongs(limit = 50)
    }
}