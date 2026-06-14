
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.auramusic.domain.model.Song
import com.example.auramusic.presentation.components.SongItem
import com.example.auramusic.presentation.components.neumorphic
import com.example.auramusic.presentation.navigation.Screen
import com.example.auramusic.presentation.theme.ThemeMode
import com.example.auramusic.presentation.viewmodel.*
import kotlinx.coroutines.delay

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
    themeViewModel: ThemeViewModel,
    navController: NavHostController
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(BottomNavItem.Home, BottomNavItem.Search, BottomNavItem.Library, BottomNavItem.Upload)
    val themeMode by themeViewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AuraMusic", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val newMode = if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                        themeViewModel.setThemeMode(newMode)
                    }) {
                        Icon(
                            imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Box(modifier = Modifier
                            .size(36.dp)
                            .neumorphic(elevation = 4.dp, cornerRadius = 18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Column {
                MiniPlayer(
                    viewModel = songViewModel,
                    currentUser = authViewModel.authState.collectAsState().value.user, // Truyền user vào đây
                    onMiniPlayerClick = { navController.navigate(Screen.Player.route) }
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)

        when (selectedItem) {
            0 -> HomeScreen(songViewModel, onSongClick = { song ->
                songViewModel.playSong(song)
                navController.navigate(Screen.Player.route)
            }, modifier = modifier)
            1 -> SearchContent(songViewModel, navController, modifier)
            2 -> LibraryContent(songViewModel, navController, modifier, authViewModel)
            3 -> UploadContent(
                authViewModel = authViewModel,
                songViewModel = songViewModel,
                modifier = modifier,
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) })
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
            onValueChange = {
                searchQuery = it
                viewModel.searchSongs(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(elevation = 2.dp, cornerRadius = 12.dp),
            placeholder = { Text("Tìm kiếm bài hát...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            // NÚT CLEAR (Dấu X) ĐỂ XÓA NHANH
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.searchSongs("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Xóa")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (searchQuery.isNotEmpty()) {
            if (state.searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy kết quả phù hợp", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.searchResults) { song ->
                        SongItem(song = song, onPlayClick = {
                            viewModel.playSong(song)
                            navController.navigate(Screen.Player.route)
                        })
                    }
                }
            }
        } else {
            // Hiển thị danh mục khi chưa gõ tìm kiếm
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

@Composable
fun LibraryContent(
    viewModel: SongViewModel,
    navController: NavHostController,
    modifier: Modifier,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val myUid = authState.user?.uid
    val myPlaylists by viewModel.myPlaylists.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(myUid) {
        if (myUid != null) {
            viewModel.loadMyPlaylists(myUid)
        }
    }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            text = "Thư viện của bạn",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Favorite Songs Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .clickable { navController.navigate(Screen.FavoriteSongs.route) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .neumorphic(elevation = 4.dp, cornerRadius = 28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Bài hát yêu thích", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Danh sách đã thả tim", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // LỊCH SỬ NGHE NHẠC
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .clickable { navController.navigate(Screen.History.route) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Lịch sử nghe gần đây", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create Playlist Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .clickable {
                    if (myUid != null) showDialog = true
                    else Toast.makeText(context, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .neumorphic(elevation = 4.dp, cornerRadius = 28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Tạo danh sách phát mới", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Playlist của bạn", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(myPlaylists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            navController.navigate(
                                Screen.PlaylistDetail.createRoute(
                                    playlist.playlistId,
                                    playlist.name
                                )
                            )
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(playlist.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${playlist.songIds.size} bài hát", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tạo danh sách phát mới") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Tên danh sách phát") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (myUid != null && newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(myUid, newPlaylistName, {
                            showDialog = false; newPlaylistName = ""; Toast.makeText(context, "Tạo thành công!", Toast.LENGTH_SHORT).show()
                        }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() })
                    }
                }) { Text("Tạo") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Hủy") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Yêu cầu cho ExposedDropdownMenuBox
@Composable
fun UploadContent(
    authViewModel: AuthViewModel,
    songViewModel: SongViewModel,
    modifier: Modifier,
    onNavigateToPremium: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    val isUploading by songViewModel.isUploading.collectAsState()
    val context = LocalContext.current

    // KIỂM TRA ĐIỀU KIỆN VIP Ở ĐÂY
    val uploadedCount = currentUser?.uploadedCount ?: 0
    val isPremium = currentUser?.premium == true

    if (uploadedCount >= 1 && !isPremium) {
        // GIAO DIỆN CHẶN VIP
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "VIP",
                modifier = Modifier.size(100.dp),
                tint = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Đã đạt giới hạn tải lên",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Tài khoản thường chỉ được đăng tối đa 1 bài hát. Hãy nâng cấp Premium để đăng tải nhạc không giới hạn!",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onNavigateToPremium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .neumorphic(elevation = 8.dp, cornerRadius = 28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
            ) {
                Text("NÂNG CẤP PREMIUM NGAY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    } else {
        // GIAO DIỆN ĐĂNG TẢI BÌNH THƯỜNG
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var audioUri by remember { mutableStateOf<Uri?>(null) }
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var expanded by remember { mutableStateOf(false) } // BIẾN QUẢN LÝ ĐÓNG MỞ DROPDOWN

        // Lấy state chứa danh sách categories đã load từ database
        val songState by songViewModel.songState.collectAsState()
        val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { audioUri = it }
        val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Đăng tải Bài hát", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Chia sẻ âm nhạc của bạn với mọi người", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tên bài hát") },
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(elevation = 2.dp, cornerRadius = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ô CHỌN THỂ LOẠI (Dropdown Menu lấy từ Firebase)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category.ifBlank { "Bấm để chọn thể loại..." },
                    onValueChange = {},
                    readOnly = true, // Không cho phép gõ tay
                    label = { Text("Thể loại nhạc") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .neumorphic(elevation = 2.dp, cornerRadius = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (songState.categories.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Đang tải thể loại...", color = Color.Gray) },
                            onClick = {}
                        )
                    } else {
                        songState.categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    category = item.name // Gán tên thể loại
                                    expanded = false     // Đóng menu
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { imageLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .neumorphic(elevation = 4.dp, cornerRadius = 28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text(if (imageUri != null) "✅ Đã chọn Ảnh bìa" else "🖼️ Chọn Ảnh bìa")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { audioLauncher.launch("audio/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .neumorphic(elevation = 4.dp, cornerRadius = 28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text(if (audioUri != null) "✅ Đã chọn file MP3" else "🎵 Chọn file Nhạc (*.mp3)")
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isUploading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        if (title.isBlank() || category.isBlank() || audioUri == null || currentUser == null) {
                            Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin và chọn thể loại!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        songViewModel.uploadSong(title, currentUser.uid, currentUser.displayName, audioUri!!, imageUri, category, 0, {
                            Toast.makeText(context, "🎉 Đăng tải thành công!", Toast.LENGTH_SHORT).show()
                            title = ""
                            category = ""
                            audioUri = null
                            imageUri = null
                        }, {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .neumorphic(elevation = 8.dp, cornerRadius = 30.dp),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text("ĐĂNG TẢI NGAY", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    viewModel: SongViewModel,
    currentUser: com.example.auramusic.domain.model.User?,
    onMiniPlayerClick: () -> Unit
) {
    val state by viewModel.songState.collectAsState()
    val currentSong = state.currentSong ?: return // PHẢI CÓ DÒNG NÀY

    // TỰ ĐỘNG ĐẾM THỜI GIAN NGHE (10 GIÂY = 1 LƯỢT XEM)
    LaunchedEffect(state.isPlaying) {
        while(state.isPlaying) {
            delay(1000L)
            viewModel.updateProgress((state.currentPosition + 1), currentUser?.uid)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(64.dp)
            .neumorphic(elevation = 8.dp, cornerRadius = 12.dp)
            .clickable { onMiniPlayerClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = currentSong.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = {
                if (state.isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
            }) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CategoryCard(name: String, imageUrl: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth()
            .neumorphic(elevation = 4.dp, cornerRadius = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.4f)
        }
        Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
