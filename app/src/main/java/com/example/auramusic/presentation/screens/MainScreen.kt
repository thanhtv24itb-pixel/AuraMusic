package com.example.auramusic.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.auramusic.presentation.components.SongItem
import com.example.auramusic.presentation.navigation.Screen
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import com.example.auramusic.presentation.viewmodel.SongViewModel
import com.example.auramusic.presentation.viewmodel.UserViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Trang chủ")
    object Search : BottomNavItem("search", Icons.Default.Search, "Tìm kiếm")
    object Library : BottomNavItem("library", Icons.Default.LibraryMusic, "Thư viện")
    object Upload : BottomNavItem("upload", Icons.Default.Add, "Đăng bài")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    songViewModel: SongViewModel,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    navController: NavHostController
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(BottomNavItem.Home, BottomNavItem.Search, BottomNavItem.Library, BottomNavItem.Upload)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AuraMusic", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.Gray))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFF0F0F1E))

        when (selectedItem) {
            0 -> HomeScreen(songViewModel, onSongClick = { song ->
                songViewModel.playSong(song)
                navController.navigate(Screen.Player.route)
            }, modifier = modifier)
            1 -> SearchContent(songViewModel, navController, modifier)
            // ĐÃ SỬA: Truyền thêm authViewModel vào LibraryContent
            2 -> LibraryContent(songViewModel, navController, modifier, authViewModel)
            3 -> UploadContent(authViewModel, songViewModel, modifier)
        }
    }
}

@Composable
fun SearchContent(viewModel: SongViewModel, navController: NavHostController, modifier: Modifier) {
    val state by viewModel.songState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; viewModel.searchSongs(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm kiếm bài hát...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isNotEmpty()) {
            LazyColumn {
                items(state.searchResults) { song ->
                    SongItem(song = song, onPlayClick = {
                        viewModel.playSong(song)
                        navController.navigate(Screen.Player.route)
                    })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CategoryCard("Top Tác giả", "") { navController.navigate(Screen.TopArtists.route) }
                }
                items(state.categories) { category ->
                    CategoryCard(category.name, category.imageUrl) {
                        navController.navigate(Screen.CategoryDetail.createRoute(category.name))
                    }
                }
            }
        }
    }
}

// --- ĐÃ CẬP NHẬT GIAO DIỆN THƯ VIỆN Ở ĐÂY ---
@Composable
fun LibraryContent(
    viewModel: SongViewModel,
    navController: NavHostController,
    modifier: Modifier,
    authViewModel: AuthViewModel // Nhận authViewModel để lấy UID
) {
    val authState by authViewModel.authState.collectAsState()
    val myUid = authState.user?.uid

    // Lấy danh sách Playlist từ ViewModel
    val myPlaylists by viewModel.myPlaylists.collectAsState()

    // Trạng thái hiển thị hộp thoại tạo Playlist
    var showDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Vừa vào Thư viện là load danh sách Playlist ngay
    LaunchedEffect(myUid) {
        if (myUid != null) {
            viewModel.loadMyPlaylists(myUid)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Thư viện của bạn",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. NÚT BẤM MỞ DANH SÁCH BÀI HÁT YÊU THÍCH
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2E))
                .clickable {
                    navController.navigate(Screen.FavoriteSongs.route)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF2C2C3E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = "Favorite Songs", tint = Color.Red, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text("Bài hát yêu thích", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Danh sách đã thả tim", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. NÚT TẠO PLAYLIST MỚI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2E))
                .clickable {
                    if (myUid != null) showDialog = true
                    else Toast.makeText(context, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF2C2C3E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Playlist", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Tạo danh sách phát mới", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Playlist của bạn", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // 3. DANH SÁCH CÁC PLAYLIST ĐÃ TẠO
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(myPlaylists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate(Screen.PlaylistDetail.createRoute(playlist.playlistId, playlist.name))
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C2C3E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(playlist.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${playlist.songIds.size} bài hát", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // 4. HỘP THOẠI NHẬP TÊN PLAYLIST
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tạo danh sách phát mới") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Tên danh sách phát") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (myUid != null && newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(
                            userId = myUid,
                            name = newPlaylistName,
                            onSuccess = {
                                showDialog = false
                                newPlaylistName = ""
                                Toast.makeText(context, "Tạo thành công!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun UploadContent(authViewModel: AuthViewModel, songViewModel: SongViewModel, modifier: Modifier) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    val isUploading by songViewModel.isUploading.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        audioUri = uri
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Đăng tải Bài hát", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Chia sẻ âm nhạc của bạn với mọi người", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Tên bài hát", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Thể loại (VD: Pop, Lofi, Rap...)", color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { imageLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A))
        ) {
            Text(
                text = if (imageUri != null) "✅ Đã chọn Ảnh bìa" else "🖼️ Chọn Ảnh bìa (Không bắt buộc)",
                color = if (imageUri != null) Color(0xFF1DB954) else Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { audioLauncher.launch("audio/*") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A))
        ) {
            Text(
                text = if (audioUri != null) "✅ Đã chọn file MP3" else "🎵 Chọn file Nhạc (*.mp3)",
                color = if (audioUri != null) Color(0xFF1DB954) else Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (isUploading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đang tải lên máy chủ...", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tên bài hát!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (audioUri == null) {
                        Toast.makeText(context, "Vui lòng chọn file nhạc MP3!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (currentUser == null) {
                        Toast.makeText(context, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    songViewModel.uploadSong(
                        title = title,
                        artistId = currentUser.uid,
                        artistName = currentUser.displayName,
                        audioUri = audioUri!!,
                        imageUri = imageUri,
                        category = category,
                        onSuccess = {
                            Toast.makeText(context, "🎉 Đăng tải thành công!", Toast.LENGTH_SHORT).show()
                            title = ""
                            category = ""
                            audioUri = null
                            imageUri = null
                        },
                        onError = { error ->
                            Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                Text("ĐĂNG TẢI NGAY", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun CategoryCard(name: String, imageUrl: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E2E))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.5f
            )
        }
        Text(name, color = Color.White, fontWeight = FontWeight.Bold)
    }
}