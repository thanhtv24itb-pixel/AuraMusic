package com.example.auramusic.domain.model

data class Playlist(
    val playlistId: String = "",
    val userId: String = "",
    val name: String = "",
    val songIds: List<String> = emptyList()
)