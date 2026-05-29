package com.example.auramusic.domain.model

import com.google.firebase.Timestamp

data class Like(
    val likeId: String = "",
    val userId: String = "",
    val songId: String = "",
    val createdAt: Timestamp? = null
)