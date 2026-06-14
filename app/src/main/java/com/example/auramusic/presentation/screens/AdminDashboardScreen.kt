package com.example.auramusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.auramusic.presentation.viewmodel.AdminViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel) {
    // Gọi hàm load dữ liệu khi màn hình vừa bật lên
    LaunchedEffect(Unit) {
        viewModel.loadDashboardStats()
    }

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFEF4444))
        }
        return
    }

    // Format tiền tệ VNĐ (VD: 20000 -> 20.000 VNĐ)
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    val formattedRevenue = "${formatter.format(viewModel.totalRevenue)} VNĐ"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Thống kê nhanh", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            // Hàng 1: User & VIP
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Tổng Người Dùng",
                    value = viewModel.totalUsers.toString(),
                    icon = Icons.Default.People,
                    iconColor = Color(0xFF3B82F6) // Xanh dương
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "User Premium",
                    value = viewModel.totalPremium.toString(),
                    icon = Icons.Default.WorkspacePremium,
                    iconColor = Color(0xFFF59E0B) // Vàng cam
                )
            }
        }

        item {
            // Hàng 2: Doanh thu & Bài hát
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Tổng Doanh Thu",
                    value = formattedRevenue,
                    icon = Icons.Default.MonetizationOn,
                    iconColor = Color(0xFF10B981) // Xanh lá mạ
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Tổng Bài Hát",
                    value = viewModel.totalSongs.toString(),
                    icon = Icons.Default.LibraryMusic,
                    iconColor = Color(0xFF8B5CF6) // Tím
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Tính năng Thống kê Biểu đồ & Giao dịch gần đây sẽ cập nhật sau...",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

// Khuôn mẫu (Component) để vẽ 1 cái Thẻ thống kê
@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}