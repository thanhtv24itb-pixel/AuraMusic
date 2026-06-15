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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    var selectedFilter by remember { mutableStateOf("pending") } // Mặc định hiện bài chờ duyệt

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSongId by remember { mutableStateOf<String?>(null) }

    var editingSong by remember { mutableStateOf<Song?>(null) }

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
                Surface(
                    modifier = Modifier.clickable { selectedFilter = filter },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFFEF4444) else Color.White,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = when(filter) { "pending" -> "Chờ duyệt"; "approved" -> "Đã duyệt"; "rejected" -> "Bị từ chối"; else -> "Tất cả" },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredSongs) { song ->
                key(song.songId, song.status) { // Ép recompose khi status đổi
                    AdminSongItem(
                        song = song,
                        isPlaying = playingSongId == song.songId,
                        onPlayClick = { togglePlay(song.songId, song.audioUrl) },
                        onApprove = { viewModel.approveSong(song.songId) },
                        onReject = { viewModel.rejectSong(song.songId) },
                        onStatusChange = { newStatus -> viewModel.updateSongStatus(song.songId, newStatus) },
                        onEditClick = { editingSong = song }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (editingSong != null) {
            EditSongDialog(
                song = editingSong!!,
                onDismiss = { editingSong = null },
                onConfirm = { title, genre ->
                    viewModel.editSong(editingSong!!.songId, title, genre)
                    editingSong = null
                }
            )
        }
    }
}

@Composable
fun EditSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var genre by remember { mutableStateOf(song.genre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa bài hát") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề") })
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Thể loại") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, genre) }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun AdminSongItem(
    song: Song,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onStatusChange: (String) -> Unit,
    onEditClick: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = song.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = song.artistName, color = Color.Gray, fontSize = 13.sp)
                    Text(text = "Thể loại: ${song.genre}", color = Color.Gray, fontSize = 11.sp)
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
                IconButton(onClick = onPlayClick, modifier = Modifier.background(if (isPlaying) Color(0xFFEF4444) else Color(0xFFF3F4F6), CircleShape)) {
                    Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = if (isPlaying) Color.White else Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = song.status)
                
                if (song.status == "pending") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.1f), contentColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Từ chối", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Duyệt", fontSize = 12.sp)
                        }
                    }
                } else {
                    StatusDropdown(currentStatus = song.status, onStatusSelected = onStatusChange)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, color) = when(status) {
        "approved" -> "Đã duyệt" to Color(0xFF10B981)
        "rejected" -> "Bị từ chối" to Color.Red
        else -> "Chờ duyệt" to Color(0xFFF59E0B)
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            modifier = Modifier.menuAnchor().width(140.dp).height(40.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("pending", "approved", "rejected").forEach { status ->
                DropdownMenuItem(text = { Text(when(status) { "pending" -> "Chờ duyệt"; "approved" -> "Đã duyệt"; "rejected" -> "Bị từ chối"; else -> status }, fontSize = 12.sp) }, onClick = { onStatusSelected(status); expanded = false })
            }
        }
    }
}
