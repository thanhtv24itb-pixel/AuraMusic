package com.example.auramusic.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.model.User
import com.example.auramusic.presentation.components.SongItem
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import com.example.auramusic.presentation.viewmodel.SongViewModel
import com.example.auramusic.presentation.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    songViewModel: SongViewModel,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavigateToPremium: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val myUid = authState.user?.uid
    val context = LocalContext.current

    // Nếu userId truyền vào bị rỗng hoặc trùng với myUid thì hiểu là đang xem trang của mình
    val isMyProfile = (userId.isBlank() || userId == myUid)

    // =========================================================================
    // XỬ LÝ LOGIC "KHÁCH" (Chưa đăng nhập mà đòi vào Profile của mình)
    // =========================================================================
    if (isMyProfile && myUid.isNullOrBlank()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Trang cá nhân") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(100.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bạn chưa đăng nhập", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đăng nhập để xem hồ sơ và bài hát của bạn", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Đăng nhập ngay")
                }
            }
        }
        return
    }

    // =========================================================================
    // NẾU ĐÃ ĐĂNG NHẬP HOẶC ĐANG XEM TRANG NGƯỜI KHÁC
    // =========================================================================

    val user by userViewModel.artistProfile.collectAsState()
    val isLiked by userViewModel.isArtistLiked.collectAsState()
    val songState by songViewModel.songState.collectAsState()
    val userSongs by authViewModel.userSongs.collectAsState()

    val artistSongs = songState.artistSongs

    var showEditDialog by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    // Biến trạng thái để chứa bài hát đang muốn xóa từ màn hình thông báo
    var songToDeleteFromNotification by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(userId) {
        userViewModel.loadArtistProfile(userId, myUid)
        songViewModel.loadSongsByArtist(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMyProfile) "Trang cá nhân" else "Hồ sơ tác giả") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (isMyProfile) {
                        IconButton(onClick = { showNotifications = true }) {
                            BadgedBox(badge = {
                                // Chỉ báo đỏ nếu có bài hát CHƯA XÓA mà bị reject hoặc pending
                                if (userSongs.any { it.status != "approved" && it.status != "deleted" }) {
                                    Badge()
                                }
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                        TextButton(onClick = onLogoutClick) {
                            Text("Đăng xuất", color = Color.Red, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                val avatarModifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .then(
                        if (user?.premium == true) Modifier.border(3.dp, Color(0xFFFFD700), CircleShape)
                        else Modifier
                    )

                AsyncImage(
                    model = user?.avatarUrl?.ifBlank { "https://cdn-icons-png.flaticon.com/512/149/149071.png" },
                    contentDescription = null,
                    modifier = avatarModifier.clickable(enabled = isMyProfile) { showEditDialog = true },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(user?.displayName ?: "Người dùng", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                    if (user?.premium == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFD700), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = "VIP", tint = Color.Black, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("PREMIUM", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isMyProfile) {
                    if (user?.premium == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thành viên VIP", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Button(
                            onClick = onNavigateToPremium,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nâng cấp Premium", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (!isMyProfile) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = { if (myUid != null) userViewModel.toggleLikeArtist(myUid, userId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiked) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isLiked) "Đã Thích" else "Thích")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat("Lượt thích", user?.totalLikesReceived ?: 0)
                    ProfileStat("Lượt nghe", user?.totalPlays ?: 0)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(if (isMyProfile) "Bài hát của tôi" else "Bài hát của tác giả", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Lọc danh sách hiển thị
            val displayedSongs = if (isMyProfile) {
                artistSongs.filter { it.status != "deleted" } // Hiện tất cả bài chưa xóa
            } else {
                artistSongs.filter { it.status == "approved" } // Khách chỉ thấy bài đã duyệt
            }

            if (displayedSongs.isEmpty()) {
                item { Text("Chưa có bài hát nào", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp)) }
            } else {
                items(displayedSongs) { song ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                SongItem(song = song, onPlayClick = {
                                    if (song.status == "approved") {
                                        songViewModel.playSong(song)
                                    }
                                })
                            }

                            // NÚT XÓA Ở NGOÀI DANH SÁCH BÀI HÁT PROFILE
                            if (isMyProfile) {
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa bài hát",
                                        tint = Color.Red.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        if (isMyProfile && song.status != "approved") {
                            Surface(
                                color = if (song.status == "rejected") Color.Red else Color(0xFFF59E0B),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            ) {
                                Text(
                                    text = if (song.status == "rejected") "Bị từ chối" else "Chờ duyệt",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // HỘP THOẠI XÁC NHẬN XÓA TỪ PROFILE
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Xóa bài hát") },
                            text = { Text("Bạn có chắc chắn muốn gỡ bài hát \"${song.title}\"? Bạn sẽ được hoàn lại 1 lượt tải lên.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (myUid != null) {
                                            songViewModel.deleteSong(
                                                songId = song.songId,
                                                artistId = myUid,
                                                onSuccess = {
                                                    showDeleteConfirm = false
                                                    Toast.makeText(context, "Đã xóa bài hát", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { errorMsg ->
                                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) { Text("Xóa", color = Color.White) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") }
                            }
                        )
                    }
                }
            }
        }

        if (showEditDialog) {
            EditProfileDialog(
                user = user!!,
                onDismiss = { showEditDialog = false },
                onConfirm = { name, uri ->
                    authViewModel.updateProfile(name, uri)
                    showEditDialog = false
                }
            )
        }

        // =====================================================================
        // MÀN HÌNH THÔNG BÁO TÍCH HỢP XÓA
        // =====================================================================
        if (showNotifications) {
            NotificationDialog(
                songs = userSongs.filter { it.status != "deleted" },
                onDismiss = { showNotifications = false },
                onDeleteClick = { song ->
                    songToDeleteFromNotification = song
                }
            )
        }

        // HỘP THOẠI XÁC NHẬN XÓA TỪ THÔNG BÁO
        if (songToDeleteFromNotification != null) {
            AlertDialog(
                onDismissRequest = { songToDeleteFromNotification = null },
                title = { Text("Xóa bài hát") },
                text = { Text("Bạn có chắc chắn muốn gỡ bài hát \"${songToDeleteFromNotification!!.title}\"? Bạn sẽ được hoàn lại 1 lượt tải lên.") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (myUid != null) {
                                songViewModel.deleteSong(
                                    songId = songToDeleteFromNotification!!.songId,
                                    artistId = myUid,
                                    onSuccess = {
                                        songToDeleteFromNotification = null
                                        Toast.makeText(context, "Đã xóa bài hát", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Xóa", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { songToDeleteFromNotification = null }) { Text("Hủy") }
                }
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cập nhật thông tin") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = selectedUri ?: user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape).clickable { picker.launch("image/*") },
                    contentScale = ContentScale.Crop
                )
                Text("Nhấn để đổi ảnh đại diện", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên hiển thị") })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, selectedUri) }) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
fun NotificationDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onDeleteClick: (Song) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ================================
                // HEADER
                // ================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔔 Trung tâm Thông báo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ================================
                // BODY
                // ================================
                if (songs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Bạn không có thông báo nào.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        items(songs) { song ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. ICON TRẠNG THÁI
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (song.status) {
                                                    "approved" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    "rejected" -> Color.Red.copy(alpha = 0.15f)
                                                    else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (song.status) {
                                                "approved" -> Icons.Default.CheckCircle
                                                "rejected" -> Icons.Default.Cancel
                                                else -> Icons.Default.Pending
                                            },
                                            contentDescription = null,
                                            tint = when (song.status) {
                                                "approved" -> Color(0xFF10B981)
                                                "rejected" -> Color.Red
                                                else -> Color(0xFFF59E0B)
                                            },
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // 2. NỘI DUNG THÔNG BÁO CHI TIẾT
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Bài hát: ${song.title}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // CHỖ NÀY ĐÃ ĐƯỢC THAY ĐỔI ĐỂ HIỂN THỊ LÝ DO (REJECT REASON) TỪ FIREBASE
                                        Text(
                                            text = when (song.status) {
                                                "approved" -> "🎉 Admin đã duyệt bài hát của bạn. Mọi người đã có thể lắng nghe!"
                                                "rejected" -> "❌ Từ chối: ${song.rejectReason ?: "File âm thanh bị lỗi hoặc vi phạm quy định."}"
                                                else -> "⏳ Bài hát đang trong hàng chờ kiểm duyệt."
                                            },
                                            fontSize = 13.sp,
                                            color = when (song.status) {
                                                "approved" -> Color(0xFF10B981)
                                                "rejected" -> Color.Red
                                                else -> Color(0xFFF59E0B)
                                            },
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 3. NÚT XÓA Ở BÊN PHẢI CÙNG
                                    IconButton(
                                        onClick = { onDeleteClick(song) },
                                        modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Xóa",
                                            tint = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}