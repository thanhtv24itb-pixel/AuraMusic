package com.example.auramusic.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.domain.model.Song
import com.example.auramusic.presentation.components.SongItem
import com.example.auramusic.presentation.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playlistName: String,
    songViewModel: SongViewModel,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    val playlistSongs by songViewModel.playlistSongs.collectAsState()
    val context = LocalContext.current // Lấy context để hiển thị thông báo Toast

    // Tải danh sách bài hát khi vào màn hình
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
            if (playlistSongs.isEmpty()) {
                Text(
                    "Danh sách phát này chưa có bài hát nào",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    items(playlistSongs) { song ->
                        // ĐÃ SỬA: Thêm Row bọc ngoài để thêm nút Xóa bài hát
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Phần thông tin bài hát chiếm 1 không gian tối đa (weight = 1f)
                            Box(modifier = Modifier.weight(1f)) {
                                SongItem(song = song, onPlayClick = { onSongClick(song) })
                            }

                            // Nút gỡ bài hát nằm bên phải
                            IconButton(
                                onClick = {
                                    // Gọi hàm xóa đã tạo ở SongViewModel
                                    songViewModel.removeSongFromPlaylist(
                                        playlistId = playlistId,
                                        songId = song.songId,
                                        onSuccess = {
                                            Toast.makeText(context, "Đã gỡ bài hát", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = {
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Gỡ bài hát",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}