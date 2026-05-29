package com.example.auramusic.domain.repository

import com.example.auramusic.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserData(uid: String): Flow<User?>
    suspend fun updateUserData(user: User): Result<Unit>
    suspend fun getTopArtists(limit: Int = 10): Result<List<User>>
    suspend fun incrementUploadedCount(uid: String): Result<Unit>
    suspend fun checkIsProfileLiked(myUserId: String, artistId: String): Result<Boolean>
    suspend fun toggleLikeProfile(myUserId: String, artistId: String): Result<Boolean>
}