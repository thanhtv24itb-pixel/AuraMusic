package com.example.auramusic.domain.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val isPremium: Boolean = false,
    val uploadedCount: Int = 0,
    val totalLikesReceived: Int = 0,
    val totalPlays: Int = 0,
    val followerCount: Int = 0,
    val createdAt: Timestamp? = null
)