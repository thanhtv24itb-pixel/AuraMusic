package com.example.auramusic.domain.usecase

import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return authRepository.login(email, password)
    }
}