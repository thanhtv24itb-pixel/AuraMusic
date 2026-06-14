package com.example.auramusic.domain.model

data class Song(
    val songId: String = "",
    val title: String = "",
    val artistId: String = "",
    val artistName: String = "",
    val audioUrl: String = "",
    val imageUrl: String = "",
    val genre: String = "",
    val duration: Int = 0, // Thời lượng tính bằng giây
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Long = 0L,
    val status: String = "pending"

)