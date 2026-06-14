package com.example.auramusic.presentation.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.domain.model.Song
import com.example.auramusic.presentation.viewmodel.AdminViewModel

@Composable
fun AdminSongScreen(viewModel: AdminViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadAllSongs()
    }

    val filterOptions = listOf("Tất cả", "pending", "approved", "rejected")
    var selectedFilter by remember { mutableStateOf("Tất cả") }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSongId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    fun togglePlay(songId: String, songUrl: String) {
        if (playingSongId == songId) {
            mediaPlayer?.pause()
            playingSongId = null
        } else {
            mediaPlayer?.release()
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(songUrl)
                    prepareAsync()
                    setOnPreparedListener { start(); playingSongId = songId }
                    setOnCompletionListener { playingSongId = null }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val filteredSongs = if (selectedFilter == "Tất cả") viewModel.allSongs
    else viewModel.allSongs.filter { it.status == selectedFilter }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        LazyRow(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filterOptions) { filter ->
                val isSelected = selectedFilter == filter
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) Color(0xFF3B82F6) else Color.White).clickable { selectedFilter = filter }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(text = when(filter) { "pending" -> "Chờ duyệt"; "approved" -> "Đã duyệt"; "rejected" -> "Bị từ chối"; else -> "Tất cả" }, color = if (isSelected) Color.White else Color.DarkGray, fontSize = 14.sp)
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredSongs) { song ->
                AdminSongItem(
                    song = song,
                    isPlaying = playingSongId == song.songId,
                    onPlayClick = { togglePlay(song.songId, song.audioUrl) },
                    onStatusChange = { newStatus -> viewModel.updateSongStatus(song.songId, newStatus) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSongItem(song: Song, isPlaying: Boolean, onPlayClick: () -> Unit, onStatusChange: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = song.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = song.artistName, color = Color.Gray, fontSize = 13.sp)

                // ComboBox Trạng thái
                StatusDropdown(currentStatus = song.status, onStatusSelected = onStatusChange)
            }
            IconButton(onClick = onPlayClick, modifier = Modifier.background(if (isPlaying) Color(0xFFEF4444) else Color(0xFFF3F4F6), CircleShape)) {
                Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDropdown(currentStatus: String, onStatusSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = when(currentStatus) { "pending" -> "Chờ duyệt"; "approved" -> "Đã duyệt"; "rejected" -> "Bị từ chối"; else -> "Không rõ" },
            onValueChange = {}, readOnly = true,
            modifier = Modifier.menuAnchor().width(140.dp).height(45.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("pending", "approved", "rejected").forEach { status ->
                DropdownMenuItem(text = { Text(when(status) { "pending" -> "Chờ duyệt"; "approved" -> "Đã duyệt"; "rejected" -> "Bị từ chối"; else -> status }) }, onClick = { onStatusSelected(status); expanded = false })
            }
        }
    }
}