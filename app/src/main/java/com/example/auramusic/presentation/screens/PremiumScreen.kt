package com.example.auramusic.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.auramusic.presentation.viewmodel.AuthViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(navController: NavController, authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsState()
    val user = authState.user
    val userId = user?.uid ?: "unknown"
    val transferContent = "AURA $userId"
    val context = LocalContext.current

    // SỬA LỖI NGẦM: Mã hóa URL để tránh lỗi dấu cách trong nội dung chuyển khoản khiến QR bị trắng
    val encodedContent = URLEncoder.encode(transferContent, "UTF-8")

    // URL VietQR tạo mã tự động (Đã dùng encodedContent)
    val qrLink = "https://img.vietqr.io/image/MB-0795662691-compact2.png?amount=2000&addInfo=$encodedContent&accountName=TRAN VAN THANH"

    // Tự động thoát và BÁO THÀNH CÔNG khi webhook cập nhật premium thành true
    LaunchedEffect(user?.premium) {
        if (user?.premium == true) {
            Toast.makeText(context, "🎉 Chúc mừng! Bạn đã trở thành thành viên VIP!", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nâng cấp Premium", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Tiêu đề với Icon Vương miện vàng
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "VIP",
                    tint = Color(0xFFFFD700), // Màu Vàng Gold
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AuraMusic Premium",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Quét mã QR bằng App ngân hàng",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // --- KHU VỰC QR CODE & VÒNG QUAY XANH LÁ ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(320.dp)
            ) {
                // Vòng quay tiến trình màu Xanh lá ôm bên ngoài
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF22C55E),
                    strokeWidth = 3.dp
                )

                // Khung chứa mã QR nằm gọn bên trong
                Card(
                    modifier = Modifier.size(280.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    AsyncImage(
                        model = qrLink,
                        contentDescription = "Mã QR Thanh Toán",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dòng chữ thông báo đang chờ
            Text(
                text = "Hệ thống đang chờ giao dịch...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22C55E)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- THẺ THÔNG TIN CHUYỂN KHOẢN ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("THÔNG TIN THANH TOÁN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    DetailRow(label = "Ngân hàng:", value = "MB Bank")
                    DetailRow(label = "Số tài khoản:", value = "0795662691")
                    DetailRow(label = "Chủ tài khoản:", value = "TRAN VAN THANH")
                    DetailRow(label = "Số tiền:", value = "2.000 VNĐ")

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nội dung bắt buộc:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = transferContent, // Vẫn hiển thị bình thường cho người dùng tự gõ nếu muốn
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Lưu ý: Giữ nguyên màn hình này.\nHệ thống sẽ tự động kích hoạt VIP và chuyển hướng sau khi nhận được tiền (thường mất 10 - 30 giây).",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 30.dp)
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}