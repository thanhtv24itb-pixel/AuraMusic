package com.example.auramusic.domain.model

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
