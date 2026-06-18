package com.example.auramusic.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.domain.model.Song
import com.example.auramusic.presentation.components.SongItem
import com.example.auramusic.presentation.viewmodel.SongViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: SongViewModel,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.songState.collectAsState()

    // Gradient nền tạo chiều sâu
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp) // Đẩy các mục lên trên, giữ lại 8dp để không dính sát Header
    ) {
        // ĐÃ XÓA MỤC TEXT "🎵 AuraMusic" Ở ĐÂY ĐỂ TIẾT KIỆM KHÔNG GIAN

        // ==========================================
        // 1. NGHE NHIỀU NHẤT (BIG FRAME CAROUSEL)
        // ==========================================
        if (state.mostPlayedSongs.isNotEmpty()) {
            item {
                SectionTitle("🔥 Đang thịnh hành")
            }

            item {
                val trendingSongs = state.mostPlayedSongs.take(5)
                val pagerState = rememberPagerState(pageCount = { trendingSongs.size })

                // TỰ ĐỘNG CUỘN (ĐÃ SỬA LỖI GIẬT ẢNH)
                LaunchedEffect(pagerState) {
                    while (true) {
                        delay(3000)
                        if (!pagerState.isScrollInProgress) {
                            val nextPage = (pagerState.currentPage + 1) % trendingSongs.size
                            pagerState.animateScrollToPage(nextPage)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(0.dp),
                        pageSpacing = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) { page ->
                        val song = trendingSongs[page]

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(0.dp),
                                pageSpacing = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            ) { page ->
                                val song = trendingSongs[page]

                                // ĐÂY LÀ CHỖ CẦN SỬA NÈ
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            // 1. Nạp bài hát và truyền danh sách "Nghe nhiều nhất" vào queue
                                            viewModel.playSong(song, queue = state.mostPlayedSongs)
                                            // 2. Chuyển màn hình
                                            onSongClick(song)
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = song.imageUrl.ifBlank { "https://via.placeholder.com/300" },
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Lớp phủ đen mờ để chữ nổi bật
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                                        startY = 150f
                                                    )
                                                )
                                        )

                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = song.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artistName,
                                                color = Color.LightGray,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } // Kết thúc Card
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. MỚI RA MẮT (GIỮ NGUYÊN)
        // ==========================================
        item {
            SectionTitle("✨ Khám phá mới")
        }

        if (state.isLoading && state.recentSongs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            items(state.recentSongs) { song ->
                SongItem(
                    song = song,
                    onPlayClick = {
                        // 1. Nạp bài hát và toàn bộ danh sách "Nhạc mới" vào hệ thống
                        viewModel.playSong(song, state.recentSongs)
                        // 2. Chuyển màn hình
                        onSongClick(song)
                    }
                )
            }
        }

        // Đệm dưới cùng để thanh MiniPlayer không đè lên bài hát cuối cùng
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}