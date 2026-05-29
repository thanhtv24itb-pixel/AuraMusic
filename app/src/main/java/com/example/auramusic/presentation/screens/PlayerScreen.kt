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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import com.example.auramusic.presentation.viewmodel.SongViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: SongViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit,
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    val state by viewModel.songState.collectAsState()
    val currentSong = state.currentSong

    // Chỉ giữ lại trạng thái bảng chọn Playlist
    var showPlaylists by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var totalDuration by remember { mutableLongStateOf(1000L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        totalDuration = this@apply.duration.coerceAtLeast(1000L)
                    } else if (playbackState == Player.STATE_ENDED) {
                        viewModel.pauseSong()
                        viewModel.updateProgress(0)
                        this@apply.seekTo(0)
                    }
                }
            })
        }
    }

    LaunchedEffect(currentSong?.audioUrl) {
        currentSong?.audioUrl?.let { url ->
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (state.isPlaying) {
                exoPlayer.play()
            }
            if (currentUser != null) {
                viewModel.checkIsSongLiked(currentUser.uid, currentSong.songId)
            }
        }
    }

    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            viewModel.updateProgress((exoPlayer.currentPosition / 1000).toInt())
            delay(1000L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (currentSong == null) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Không có bài hát nào", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.background)
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("ĐANG PHÁT", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

                // Đã dọn dẹp nút Comment, chỉ giữ lại nút Thêm vào Playlist
                IconButton(onClick = {
                    if (currentUser != null) {
                        viewModel.loadMyPlaylists(currentUser.uid)
                        showPlaylists = true
                    } else {
                        Toast.makeText(context, "Vui lòng đăng nhập để dùng tính năng này", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Album Art
            AsyncImage(
                model = currentSong.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Info & Nút Like
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text(currentSong.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = currentSong.artistName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { onArtistClick(currentSong.artistId) }
                    )
                }

                IconButton(onClick = {
                    if (currentUser != null) {
                        viewModel.toggleLike(currentUser.uid)
                    } else {
                        Toast.makeText(context, "Vui lòng đăng nhập để Like", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = if (state.isCurrentSongLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (state.isCurrentSongLiked) Color.Red else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val durationInSeconds = (totalDuration / 1000).toInt().coerceAtLeast(1)

            // Slider
            Slider(
                value = state.currentPosition.toFloat(),
                onValueChange = { newValue ->
                    viewModel.updateProgress(newValue.toInt())
                    exoPlayer.seekTo(newValue.toLong() * 1000)
                },
                valueRange = 0f..durationInSeconds.toFloat(),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.currentPosition), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTime(durationInSeconds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(40.dp)) }
                Surface(
                    onClick = { if (state.isPlaying) viewModel.pauseSong() else viewModel.resumeSong() },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(40.dp)) }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // BẢNG CHỌN PLAYLIST KHI BẤM NÚT
    if (showPlaylists) {
        ModalBottomSheet(onDismissRequest = { showPlaylists = false }) {
            PlaylistSelectSection(
                viewModel = viewModel,
                onPlaylistSelected = { playlistId ->
                    viewModel.addCurrentSongToPlaylist(
                        playlistId = playlistId,
                        onSuccess = {
                            Toast.makeText(context, "Đã thêm bài hát vào danh sách phát!", Toast.LENGTH_SHORT).show()
                            showPlaylists = false
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun PlaylistSelectSection(
    viewModel: SongViewModel,
    onPlaylistSelected: (String) -> Unit
) {
    val myPlaylists by viewModel.myPlaylists.collectAsState()

    Column(modifier = Modifier.fillMaxHeight(0.5f).padding(16.dp)) {
        Text(
            text = "Thêm vào danh sách phát",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (myPlaylists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bạn chưa có danh sách phát nào.\nHãy tạo ở tab Thư viện!", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn {
                items(myPlaylists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistSelected(playlist.playlistId) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}