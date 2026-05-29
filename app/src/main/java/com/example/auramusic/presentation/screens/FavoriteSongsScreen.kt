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
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import com.example.auramusic.presentation.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteSongsScreen(
    authViewModel: AuthViewModel,
    songViewModel: SongViewModel,
    onBackClick: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val myUid = authState.user?.uid

    // Lấy dữ liệu từ StateFlow
    val favoriteSongs by songViewModel.favoriteSongs.collectAsState()

    // Vừa vào trang là ra lệnh tải dữ liệu ngay
    LaunchedEffect(myUid) {
        if (myUid != null) {
            songViewModel.loadFavoriteSongs(myUid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài hát yêu thích", fontWeight = FontWeight.Bold) },
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
            if (myUid == null) {
                Text(
                    "Vui lòng đăng nhập để xem danh sách",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (favoriteSongs.isEmpty()) {
                Text(
                    "Bạn chưa thích bài hát nào",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    items(favoriteSongs) { song ->
                        SongItem(song = song, onPlayClick = {
                            songViewModel.playSong(song)
                        })
                    }
                }
            }
        }
    }
}