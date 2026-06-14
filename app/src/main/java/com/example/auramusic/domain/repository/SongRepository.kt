package com.example.auramusic.domain.repository

import android.net.Uri
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.model.Playlist
import com.example.auramusic.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    // Home screen
    suspend fun getMostPlayedSongs(limit: Int = 10): Result<List<Song>>
    suspend fun getRecentSongs(limit: Int = 10): Result<List<Song>>

    // Search screen
    suspend fun searchSongs(query: String): Result<List<Song>>
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getSongsByCategory(genre: String): Result<List<Song>>

    // Song details & actions
    suspend fun getSongById(songId: String): Result<Song>

    // ĐÃ SỬA: Thêm tham số artistId
    suspend fun incrementPlayCount(songId: String, artistId: String): Result<Unit>

    // ĐÃ THÊM: Tính năng lưu lịch sử
    suspend fun addToHistory(userId: String, songId: String): Result<Unit>

    // Upload
    suspend fun uploadSong(
        title: String,
        artistId: String,
        artistName: String,
        audioUri: Uri,
        imageUri: Uri?,
        category: String,
        duration: Int
    ): Result<Unit>

    // Thích bài hát
    suspend fun checkIsLiked(userId: String, songId: String): Result<Boolean>
    suspend fun toggleLikeSong(userId: String, songId: String): Result<Boolean>
    suspend fun getFavoriteSongs(userId: String): Result<List<Song>>

    // Playlist
    suspend fun createPlaylist(userId: String, name: String): Result<Boolean>
    suspend fun getUserPlaylists(userId: String): Result<List<Playlist>>
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Boolean>
    suspend fun getSongsInPlaylist(playlistId: String): Result<List<Song>>

    // Comments
    suspend fun addComment(songId: String, userId: String, userName: String, userAvatar: String, content: String): Result<Unit>
    fun getComments(songId: String): Flow<List<Comment>>
    suspend fun getRecentlyPlayed(userId: String): Result<List<Song>>
    suspend fun incrementUploadedCount(uid: String): Result<Unit>
}