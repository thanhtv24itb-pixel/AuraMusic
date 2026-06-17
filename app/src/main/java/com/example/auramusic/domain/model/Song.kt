package com.example.auramusic.domain.model

import com.google.firebase.firestore.PropertyName

data class Song(
    @get:PropertyName("songId")
    @set:PropertyName("songId")
    var songId: String = "",
    
    val title: String = "",
    val artistId: String = "",
    val artistName: String = "",
    val audioUrl: String = "",
    val imageUrl: String = "",
    val genre: String = "",
    val duration: Int = 0,
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Long = 0L,
    
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "pending",
    val rejectReason: String? = null // THÊM DÒNG NÀY (Để lưu lý do)
)