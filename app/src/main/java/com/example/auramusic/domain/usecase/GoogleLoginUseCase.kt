package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.repository.AuthRepository

class GoogleLoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(idToken: String, displayName: String = ""): Result<User> {
        return authRepository.loginWithGoogle(idToken, displayName)
    }
}

