package com.example.auramusic.domain.repository

import android.net.Uri
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.model.Playlist
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
    suspend fun incrementPlayCount(songId: String): Result<Unit>
    
    // Upload
    suspend fun uploadSong(
        title: String,
        artistId: String,
        artistName: String,
        audioUri: Uri,    // File mp3
        imageUri: Uri?,   // File ảnh bìa
        category: String,
        duration: Int
    ): Result<Unit>
    // Thêm chức năng Like
    suspend fun checkIsLiked(userId: String, songId: String): Result<Boolean>
    suspend fun toggleLikeSong(userId: String, songId: String): Result<Boolean>
    suspend fun getFavoriteSongs(userId: String): Result<List<Song>>
    suspend fun createPlaylist(userId: String, name: String): Result<Boolean>
    suspend fun getUserPlaylists(userId: String): Result<List<Playlist>>
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Boolean>
    suspend fun getSongsInPlaylist(playlistId: String): Result<List<Song>>
}