package com.example.auramusic.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.auramusic.presentation.navigation.Screen
import com.example.auramusic.presentation.viewmodel.AdminViewModel
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

data class AdminMenu(val id: String, val title: String, val icon: ImageVector)

val adminMenus = listOf(
    AdminMenu("dashboard", "Tổng quan", Icons.Default.Dashboard),
    AdminMenu("categories", "Quản lý Thể loại", Icons.Default.Category),
    AdminMenu("songs", "Kiểm duyệt Bài hát", Icons.Default.LibraryMusic),
    AdminMenu("users", "Quản lý Người dùng", Icons.Default.People),
    AdminMenu("transactions", "Lịch sử Giao dịch", Icons.Default.Receipt)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(navController: NavController, authViewModel: AuthViewModel, adminViewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    // LẤY QUYỀN CỦA TÀI KHOẢN ĐANG ĐĂNG NHẬP
    val authState by authViewModel.authState.collectAsState()
    val myRole = authState.user?.role ?: "user"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // LỌC MENU DỰA TRÊN PHÂN QUYỀN
    val allowedMenus = remember(myRole) {
        if (myRole == "admin") {
            adminMenus // Admin thấy hết 5 tab
        } else {
            // Moderator chỉ thấy 3 tab này
            adminMenus.filter { it.id in listOf("categories", "songs", "users") }
        }
    }

    var currentTab by remember { mutableStateOf(allowedMenus.firstOrNull() ?: adminMenus[0]) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "AuraMusic Admin",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
                // Hiển thị chức danh để người dùng biết họ đang đăng nhập với quyền gì
                Text(
                    text = "Quyền: ${myRole.uppercase()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp).padding(bottom = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                allowedMenus.forEach { menu ->
                    NavigationDrawerItem(
                        icon = { Icon(menu.icon, contentDescription = menu.title) },
                        label = { Text(menu.title, fontWeight = FontWeight.Medium) },
                        selected = currentTab.id == menu.id,
                        onClick = {
                            currentTab = menu
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "App", tint = MaterialTheme.colorScheme.primary) },
                    label = { Text("Mở App Nghe nhạc", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Chuyển sang MainScreen
                        navController.navigate(Screen.Main.route) {
                            // Xóa AdminDashboard khỏi ngăn xếp để tránh lỗi back nhầm
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                // Nút Đăng xuất
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Đăng xuất", tint = Color.Red) },
                    label = { Text("Đăng xuất", color = Color.Red) },
                    selected = false,
                    onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(bottom = 24.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTab.title, color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEF4444))
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                when (currentTab.id) {
                    "dashboard" -> AdminDashboardScreen(viewModel = adminViewModel)
                    "categories" -> AdminCategoryScreen(viewModel = adminViewModel)
                    "songs" -> AdminSongScreen(viewModel = adminViewModel)
                    // TRUYỀN QUYỀN CỦA MÌNH SANG MÀN HÌNH QUẢN LÝ USER
                    "users" -> AdminUserScreen(viewModel = adminViewModel, myRole = myRole)
                    "transactions" -> AdminTransactionScreen(viewModel = adminViewModel)
                }
            }
        }
    }
}