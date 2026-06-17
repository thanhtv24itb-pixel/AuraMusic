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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.domain.model.Song
import com.example.auramusic.presentation.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSongScreen(viewModel: AdminViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadAllSongs()
    }

    val filterOptions = listOf("Tất cả", "pending", "approved", "rejected")
    var selectedFilter by remember { mutableStateOf("pending") }

    // THANH TÌM KIẾM
    var searchQuery by remember { mutableStateOf("") }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSongId by remember { mutableStateOf<String?>(null) }

    var editingSong by remember { mutableStateOf<Song?>(null) }
    var songToReject by remember { mutableStateOf<Song?>(null) }

    // ĐÃ SỬA: Đảm bảo reset trạng thái chơi nhạc khi rời màn hình để tránh lỗi
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            playingSongId = null
        }
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

    val filteredSongs = viewModel.allSongs.filter { song ->
        val matchesStatus = if (selectedFilter == "Tất cả") true else song.status == selectedFilter
        val matchesSearch = if (searchQuery.isBlank()) true else {
            song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artistName.contains(searchQuery, ignoreCase = true)
        }
        matchesStatus && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Tìm tên bài hát hoặc tác giả...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    modifier = Modifier.clickable { selectedFilter = filter },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF1F2937) else Color.White,
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
            if (filteredSongs.isEmpty()) {
                item {
                    Text(
                        "Không có bài hát nào phù hợp.",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(filteredSongs) { song ->
                    key(song.songId, song.status) {
                        AdminSongItem(
                            song = song,
                            isPlaying = playingSongId == song.songId,
                            onPlayClick = { togglePlay(song.songId, song.audioUrl) },
                            onApprove = { viewModel.updateSongStatus(song.songId, "approved") },
                            onReject = { songToReject = song },
                            onStatusChange = { newStatus ->
                                // ĐÃ SỬA: Chặn bắt sự kiện đổi trạng thái từ Dropdown Menu
                                if (newStatus == "rejected") {
                                    songToReject = song
                                } else {
                                    viewModel.updateSongStatus(song.songId, newStatus)
                                }
                            },
                            onEditClick = { editingSong = song }
                        )
                    }
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

        if (songToReject != null) {
            var rejectReason by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { songToReject = null },
                title = { Text("Từ chối bài hát") },
                text = {
                    Column {
                        Text("Vui lòng nhập lý do từ chối để thông báo cho tác giả (Tùy chọn):", fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            placeholder = { Text("VD: File âm thanh bị rè...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // ĐÃ SỬA: Truyền lý do vào hàm rejectSong
                            viewModel.rejectSong(songToReject!!.songId, rejectReason)
                            songToReject = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Xác nhận từ chối", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { songToReject = null }) { Text("Hủy") }
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
                AsyncImage(model = song.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = song.artistName, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Từ chối", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Duyệt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text(text = text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            modifier = Modifier.menuAnchor().width(140.dp).height(46.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            listOf("pending", "approved", "rejected").forEach { status ->
                DropdownMenuItem(
                    text = { Text(when(status) { "pending" -> "Chờ duyệt"; "approved" -> "Đã duyệt"; "rejected" -> "Bị từ chối"; else -> status }, fontSize = 13.sp) },
                    onClick = { onStatusSelected(status); expanded = false }
                )
            }
        }
    }
}