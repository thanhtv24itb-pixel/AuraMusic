package com.example.auramusic.domain.repository

import com.example.auramusic.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signup(email: String, password: String, displayName: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
}