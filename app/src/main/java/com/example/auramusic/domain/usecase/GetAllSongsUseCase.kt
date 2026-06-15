package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetAllSongsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(): Flow<List<Song>> {
        // Cập nhật để gọi phương thức getRecentSongs (Mới ra mắt)
        return songRepository.getRecentSongs(limit = 50)
    }
}