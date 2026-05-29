package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.repository.AuthRepository

class SignupUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return authRepository.signup(email, password, displayName)
    }
}