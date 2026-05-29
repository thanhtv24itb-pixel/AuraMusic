package com.example.auramusic.domain.usecase

import android.net.Uri
import com.example.auramusic.domain.repository.SongRepository

class UploadSongUseCase(private val repository: SongRepository) {
    suspend operator fun invoke(
        title: String,
        artistId: String,
        artistName: String,
        audioUri: Uri,
        imageUri: Uri?,
        category: String,
        duration: Int
    ): Result<Unit> {
        // Kiểm tra xem người dùng đã nhập tên bài hát chưa
        if (title.isBlank()) {
            return Result.failure(Exception("Vui lòng nhập tên bài hát!"))
        }

        // Gọi repository để thực hiện upload
        return repository.uploadSong(
            title = title,
            artistId = artistId,
            artistName = artistName,
            audioUri = audioUri,
            imageUri = imageUri,
            category = category,
            duration = duration
        )
    }
}