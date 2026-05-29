package com.example.auramusic.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class để lưu trữ thông tin bài hát
data class Song(
    val id: Int,
    val name: String,
    val artist: String,
    val imageResId: Int // ID hình ảnh drawable
)

// Composable hiển thị từng bài hát
@Composable
fun SongItem(
    song: Song,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(
                color = Color(0xFF1E1E2E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hình ảnh bài hát
        Image(
            painter = painterResource(id = song.imageResId),
            contentDescription = "Album art for ${song.name}",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Thông tin bài hát
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                color = Color(0xFFB0B0B0),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

// Composable hiển thị danh sách bài hát
@Composable
fun SongListScreen(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1E))
            .padding(8.dp)
    ) {
        items(songs) { song ->
            SongItem(song = song)
        }
    }
}

// Preview
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun SongListPreview() {
    val sampleSongs = listOf(
        Song(
            id = 1,
            name = "Blinding Lights",
            artist = "The Weeknd",
            imageResId = android.R.drawable.ic_menu_gallery
        ),
        Song(
            id = 2,
            name = "Shape of You",
            artist = "Ed Sheeran",
            imageResId = android.R.drawable.ic_menu_gallery
        ),
        Song(
            id = 3,
            name = "Levitating",
            artist = "Dua Lipa",
            imageResId = android.R.drawable.ic_menu_gallery
        )
    )

    SongListScreen(songs = sampleSongs)
}