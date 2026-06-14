package com.example.auramusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.presentation.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsScreen(
    viewModel: UserViewModel,
    onBackClick: () -> Unit
) {
    val topArtists by viewModel.topArtists.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTopArtists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bảng xếp hạng Tác giả", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Dùng itemsIndexed để lấy được số thứ tự (index) của từng người
            itemsIndexed(topArtists) { index, artist ->
                val rank = index + 1 // Thứ hạng = index + 1

                // Cài đặt màu sắc Vương miện cho Top 1, 2, 3
                val rankColor = when (rank) {
                    1 -> Color(0xFFFFD700) // Vàng Gold
                    2 -> Color(0xFFC0C0C0) // Bạc Silver
                    3 -> Color(0xFFCD7F32) // Đồng Bronze
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            // Nhấn mạnh background cho top 3
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (rank <= 3) 0.3f else 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { /* Tương lai: Điều hướng đến Profile nghệ sĩ */ }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cột 1: Hiển thị thứ hạng (01, 02, 03...)
                    Text(
                        text = String.format("%02d", rank),
                        fontSize = if (rank <= 3) 24.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = rankColor,
                        modifier = Modifier.width(44.dp)
                    )

                    // Cột 2: Ảnh đại diện
                    AsyncImage(
                        model = artist.avatarUrl.ifBlank { "https://cdn-icons-png.flaticon.com/512/149/149071.png" },
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Cột 3: Tên và Số lượt nghe
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Lượt nghe",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // ĐÃ SỬA: Hiển thị biến totalPlays thay vì followerCount
                            Text(
                                text = "${artist.totalPlays} lượt nghe",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}