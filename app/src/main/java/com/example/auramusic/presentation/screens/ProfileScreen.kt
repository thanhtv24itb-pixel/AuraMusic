package com.example.auramusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    onNavigateToPremium: () -> Unit = {} // THÊM DÒNG NÀY ĐỂ CHUYỂN TRANG MUA VIP
) {
    val authState by authViewModel.authState.collectAsState()
    val myUid = authState.user?.uid

    // Nếu userId truyền vào bị rỗng hoặc trùng với myUid thì hiểu là đang xem trang của mình
    val isMyProfile = (userId.isBlank() || userId == myUid)

    // =========================================================================
    // XỬ LÝ LOGIC "KHÁCH" (Chưa đăng nhập mà đòi vào Profile của mình)
    // =========================================================================
    if (isMyProfile && (myUid == null || myUid.isBlank())) {
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
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues),
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

    // NẾU ĐÃ ĐĂNG NHẬP HOẶC ĐANG XEM TRANG NGƯỜI KHÁC THÌ CHẠY CODE BÌNH THƯỜNG DƯỚI ĐÂY:

    val user by userViewModel.artistProfile.collectAsState()
    val isLiked by userViewModel.isArtistLiked.collectAsState()
    val songState by songViewModel.songState.collectAsState()
    val profileSongs = songState.recentSongs.filter { it.artistId == userId }

    // Gọi Firebase load dữ liệu khi vào trang
    LaunchedEffect(userId) {
        userViewModel.loadArtistProfile(userId, myUid)
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // 1. GẮN VIỀN VÀNG CHO AVATAR NẾU LÀ VIP
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
                    modifier = avatarModifier,
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 2. GẮN HUY HIỆU PREMIUM BÊN CẠNH TÊN
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

                // 3. NÚT NÂNG CẤP VIP DÀNH CHO CHỦ SỞ HỮU TRANG (NẾU CHƯA LÀ VIP)
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
                            colors = ButtonDefaults.buttonColors(containerColor = if (isLiked) MaterialTheme.colorScheme.primary else Color.DarkGray, contentColor = Color.White),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, contentDescription = "Like", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isLiked) "Đã Thích" else "Thích")
                        }

                        Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Follow", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Theo dõi")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat("Lượt thích", user?.totalLikesReceived ?: 0)
                    ProfileStat("Lượt nghe", user?.totalPlays ?: 0)
                    ProfileStat("Followers", user?.followerCount ?: 0)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(if (isMyProfile) "Bài hát của tôi" else "Bài hát của tác giả", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (profileSongs.isEmpty()) {
                item { Text("Chưa có bài hát nào", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp)) }
            } else {
                items(profileSongs) { song ->
                    SongItem(song = song, onPlayClick = { songViewModel.playSong(song) })
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