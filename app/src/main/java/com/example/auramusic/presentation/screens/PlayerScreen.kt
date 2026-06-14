package com.example.auramusic.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import com.example.auramusic.presentation.viewmodel.SongViewModel
import com.example.auramusic.presentation.components.RotatingVinyl
import com.example.auramusic.presentation.components.glassmorphism
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: SongViewModel,
    authViewModel: AuthViewModel,
    exoPlayer: ExoPlayer,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit,
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    val state by viewModel.songState.collectAsState()
    val currentSong = state.currentSong

    var showPlaylists by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var totalDuration by remember { mutableLongStateOf(1000L) }

    LaunchedEffect(currentSong?.audioUrl) {
        currentSong?.audioUrl?.let { url ->
            if (currentUser != null) {
                viewModel.checkIsSongLiked(currentUser.uid, currentSong.songId)
            }
        }
    }

    // ĐÃ SỬA: Đồng bộ thời gian để đếm View (Thủ phạm khiến view không tăng là đây)
    LaunchedEffect(state.isPlaying) {
        while(state.isPlaying) {
            if (exoPlayer.duration > 0) totalDuration = exoPlayer.duration
            val currentPosInSec = (exoPlayer.currentPosition / 1000).toInt()
            viewModel.updateProgress(currentPosInSec, currentUser?.uid)
            delay(1000L)
        }
    }

    if (currentSong == null) return

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.background)
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(backgroundBrush).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground) }
                Text("ĐANG PHÁT", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                IconButton(onClick = { if (currentUser != null) { viewModel.loadMyPlaylists(currentUser.uid); showPlaylists = true } }) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            RotatingVinyl(imageUrl = currentSong.imageUrl, isPlaying = state.isPlaying, modifier = Modifier.fillMaxWidth(0.85f))

            Spacer(modifier = Modifier.height(40.dp))

            // Info & Nút Like, Comment
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentSong.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    Text(currentSong.artistName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onArtistClick(currentSong.artistId) })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showComments = true }) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comments", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { currentUser?.let { viewModel.toggleLike(it.uid) } }) {
                        Icon(if (state.isCurrentSongLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = if (state.isCurrentSongLiked) Color.Red else Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Slider & Timer
            val durationInSeconds = (totalDuration / 1000).toInt().coerceAtLeast(1)
            Slider(
                value = state.currentPosition.toFloat(),
                onValueChange = { newValue ->
                    viewModel.updateProgress(newValue.toInt(), currentUser?.uid)
                    exoPlayer.seekTo(newValue.toLong() * 1000)
                },
                valueRange = 0f..durationInSeconds.toFloat(),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(state.currentPosition), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { showTimerDialog = true }) { Icon(Icons.Default.Timer, contentDescription = "Hẹn giờ", tint = MaterialTheme.colorScheme.primary) }
                Text(formatTime(durationInSeconds), style = MaterialTheme.typography.bodySmall)
            }

            // Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(40.dp)) }
                Surface(
                    onClick = { if (state.isPlaying) viewModel.pauseSong() else viewModel.resumeSong() },
                    modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp)) }
                }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(40.dp)) }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Modal Hẹn giờ
    if (showTimerDialog) {
        ModalBottomSheet(onDismissRequest = { showTimerDialog = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Hẹn giờ tắt nhạc", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.startSleepTimerByMinutes(15); showTimerDialog = false }) { Text("Sau 15 phút") }
                TextButton(onClick = { viewModel.startSleepTimerByMinutes(30); showTimerDialog = false }) { Text("Sau 30 phút") }
                TextButton(onClick = { viewModel.setStopAfterCurrentSong(); showTimerDialog = false }) { Text("Tắt sau bài này") }
                TextButton(onClick = { viewModel.cancelSleepTimer(); showTimerDialog = false }) { Text("Hủy hẹn giờ", color = Color.Red) }
            }
        }
    }

    // Modal Playlist (Khôi phục lại)
    if (showPlaylists) {
        ModalBottomSheet(onDismissRequest = { showPlaylists = false }) {
            PlaylistSelectSection(
                viewModel = viewModel,
                onPlaylistSelected = { playlistId ->
                    viewModel.addCurrentSongToPlaylist(
                        playlistId = playlistId,
                        onSuccess = {
                            Toast.makeText(context, "Đã thêm vào Playlist!", Toast.LENGTH_SHORT).show()
                            showPlaylists = false
                        },
                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                    )
                }
            )
        }
    }

    // Modal Bình Luận (Khôi phục lại)
    if (showComments) {
        val comments = state.comments
        val songId = currentSong.songId

        LaunchedEffect(songId) { viewModel.loadComments(songId) }

        ModalBottomSheet(onDismissRequest = { showComments = false }, containerColor = Color.Transparent) {
            CommentSection(
                comments = comments,
                onSendComment = { text ->
                    if (currentUser != null) {
                        viewModel.addComment(songId, currentUser.uid, currentUser.displayName ?: "User", currentUser.avatarUrl ?: "", text)
                    } else {
                        Toast.makeText(context, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// CÁC COMPOSABLE PHỤ DƯỚI ĐÂY (Cũng bị bạn xóa mất nên giờ phải thêm lại)
@Composable
fun CommentSection(comments: List<com.example.auramusic.domain.model.Comment>, onSendComment: (String) -> Unit) {
    var commentText by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth().glassmorphism(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Bình luận (${comments.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            if (comments.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Chưa có bình luận nào. Hãy là người đầu tiên!") }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(comments) { comment -> CommentItem(comment) } }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(value = commentText, onValueChange = { commentText = it }, placeholder = { Text("Thêm bình luận...") }, modifier = Modifier.weight(1f), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                IconButton(onClick = { if (commentText.isNotBlank()) { onSendComment(commentText); commentText = "" } }) { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
fun CommentItem(comment: com.example.auramusic.domain.model.Comment) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(model = comment.userAvatar.ifBlank { "https://cdn-icons-png.flaticon.com/512/149/149071.png" }, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(comment.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(comment.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PlaylistSelectSection(viewModel: SongViewModel, onPlaylistSelected: (String) -> Unit) {
    val myPlaylists by viewModel.myPlaylists.collectAsState()
    Column(modifier = Modifier.fillMaxHeight(0.5f).padding(16.dp)) {
        Text("Thêm vào danh sách phát", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        if (myPlaylists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Bạn chưa có danh sách phát nào.\nHãy tạo ở tab Thư viện!", color = Color.Gray, fontSize = 14.sp) }
        } else {
            LazyColumn {
                items(myPlaylists) { playlist ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onPlaylistSelected(playlist.playlistId) }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(playlist.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("${playlist.songIds.size} bài hát", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String { return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60) }