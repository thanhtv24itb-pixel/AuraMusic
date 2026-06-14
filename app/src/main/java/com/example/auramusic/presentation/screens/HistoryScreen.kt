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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.presentation.components.SongItem
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import com.example.auramusic.presentation.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: SongViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val myUid = authState.user?.uid
    val historySongs by viewModel.recentlyPlayedSongs.collectAsState()

    // Tự động load lại danh sách khi vào màn hình này
    LaunchedEffect(myUid) {
        if (myUid != null) {
            viewModel.loadRecentlyPlayed(myUid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử nghe gần đây", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (myUid == null) {
                Text(
                    text = "Vui lòng đăng nhập để xem lịch sử",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (historySongs.isEmpty()) {
                Text(
                    text = "Bạn chưa nghe bài hát nào gần đây",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(historySongs) { song ->
                        SongItem(
                            song = song,
                            onPlayClick = { viewModel.playSong(song) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}