package com.example.auramusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auramusic.presentation.components.SongItem
import com.example.auramusic.presentation.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playlistName: String, // Truyền tên playlist sang để làm tiêu đề luôn
    songViewModel: SongViewModel,
    onBackClick: () -> Unit,
    onSongClick: (com.example.auramusic.domain.model.Song) -> Unit
) {
    // Lấy danh sách bài hát của playlist này từ State
    val playlistSongs by songViewModel.playlistSongs.collectAsState()

    // Vừa vào trang là ra lệnh tải danh sách bài hát của Playlist này ngay
    LaunchedEffect(playlistId) {
        songViewModel.loadSongsInPlaylist(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F1E))
                .padding(paddingValues)
        ) {
            if (playlistSongs.isEmpty()) {
                Text(
                    "Playlist này chưa có bài hát nào",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    items(playlistSongs) { song ->
                        SongItem(
                            song = song,
                            onPlayClick = { onSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}