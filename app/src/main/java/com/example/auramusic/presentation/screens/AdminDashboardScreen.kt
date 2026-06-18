
package com.example.auramusic.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.presentation.viewmodel.AdminViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel) {
    // Trạng thái lưu tab đang chọn ("revenue" cho Doanh thu, "songs" cho Bài hát)
    var selectedChartTab by remember { mutableStateOf("revenue") }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardStats()
        viewModel.loadAllTransactions()
        viewModel.loadAllSongs()
    }

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFEF4444))
        }
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CÁC THẺ TỔNG QUAN
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(Modifier.weight(1f), "Người dùng", viewModel.totalUsers.toString(), Icons.Default.People, Color(0xFF3B82F6))
            SummaryCard(Modifier.weight(1f), "Premium", viewModel.totalPremium.toString(), Icons.Default.WorkspacePremium, Color(0xFFF59E0B))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(Modifier.weight(1f), "Bài hát", viewModel.totalSongs.toString(), Icons.Default.LibraryMusic, Color(0xFF10B981))
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            SummaryCard(Modifier.weight(1f), "Doanh thu", formatter.format(viewModel.totalRevenue), Icons.Default.AttachMoney, Color(0xFFEF4444))
        }

        // ==========================================
        // 2. CARD BIỂU ĐỒ TỔNG HỢP
        // ==========================================
        // ==========================================
// CARD BIỂU ĐỒ TỔNG HỢP (DOANH THU & PREMIUM)
// ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. Thanh chuyển đổi chế độ hiển thị
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    ChartTabButton("Doanh thu", selectedChartTab == "revenue", Modifier.weight(1f)) { selectedChartTab = "revenue" }
                    ChartTabButton("Tỉ lệ Premium", selectedChartTab == "premium", Modifier.weight(1f)) { selectedChartTab = "premium" }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Nội dung hiển thị thay đổi dựa trên tab
                if (selectedChartTab == "revenue") {
                    // Nút chọn thời gian cho biểu đồ Doanh thu
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Row(modifier = Modifier.background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))) {
                            TimeFilterButton("7 Ngày", viewModel.revenueFilter == "7days") { viewModel.revenueFilter = "7days" }
                            TimeFilterButton("Tháng", viewModel.revenueFilter == "month") { viewModel.revenueFilter = "month" }
                            TimeFilterButton("Năm", viewModel.revenueFilter == "year") { viewModel.revenueFilter = "year" }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SmoothLineChartCanvas(
                        data = viewModel.getRevenueChartData(),
                        isRevenue = true,
                        lineColor = Color(0xFFEF4444)
                    )
                } else {
                    // Hiển thị biểu đồ tròn Tỉ lệ Premium
                    Row(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                            val freeUsers = viewModel.totalUsers - viewModel.totalPremium
                            MultiPieChartCanvas(
                                listOf(viewModel.totalPremium.toFloat(), freeUsers.toFloat()),
                                listOf(Color(0xFFF59E0B), Color(0xFFE5E7EB))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${viewModel.totalPremium}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("VIP", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        Column {
                            LegendItem(Color(0xFFF59E0B), "Premium (${viewModel.totalPremium})")
                            Spacer(modifier = Modifier.height(8.dp))
                            LegendItem(Color(0xFFE5E7EB), "Miễn phí (${viewModel.totalUsers - viewModel.totalPremium})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiPieChartCanvas(data: List<Float>, colors: List<Color>) {
    val total = data.sum()
    if (total == 0f) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        for (i in data.indices) {
            val sweepAngle = (data[i] / total) * 360f
            if (sweepAngle > 0) {
                drawArc(
                    color = colors[i],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 30f, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun ChartTabButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFFEF4444) else Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

// ==========================================
// CÁC COMPOSABLE PHỤ TRỢ (VẼ BIỂU ĐỒ BẰNG TAY)
// ==========================================

@Composable
fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = color) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TimeFilterButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFEF4444) else Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PieChartCanvas(premiumCount: Int, freeCount: Int) {
    val total = premiumCount + freeCount
    if (total == 0) return

    // Tính góc xoay (360 độ)
    val premiumAngle = (premiumCount.toFloat() / total) * 360f

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Vẽ phần Premium (Màu vàng)
        drawArc(
            color = Color(0xFFF59E0B),
            startAngle = -90f,
            sweepAngle = premiumAngle,
            useCenter = false,
            style = Stroke(width = 40f, cap = StrokeCap.Butt)
        )
        // Vẽ phần Free (Màu xám)
        drawArc(
            color = Color(0xFFE5E7EB),
            startAngle = -90f + premiumAngle,
            sweepAngle = 360f - premiumAngle,
            useCenter = false,
            style = Stroke(width = 40f, cap = StrokeCap.Butt)
        )
    }
}

// BẢN NÂNG CẤP BIỂU ĐỒ ĐƯỜNG MƯỢT MÀ
@Composable
fun SmoothLineChartCanvas(data: List<Pair<String, Float>>, isRevenue: Boolean, lineColor: Color) {
    if (data.isEmpty() || data.all { it.second == 0f }) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Chưa có dữ liệu", color = Color.Gray)
        }
        return
    }

    val maxAmount = data.maxOf { it.second }.coerceAtLeast(1f)

    // Hàm format số cho gọn (Ví dụ: 1.500.000 -> 1.5M, 50.000 -> 50K)
    fun formatValue(value: Float): String {
        if (!isRevenue) return value.toInt().toString()
        return when {
            value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000)
            value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000)
            else -> value.toInt().toString()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 16.dp, bottom = 8.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val xStep = canvasWidth / (data.size - 1).coerceAtLeast(1)

            // 1. Vẽ các đường kẻ ngang mờ (Grid lines)
            for (i in 0..4) {
                val y = canvasHeight * (i / 4f)
                drawLine(
                    color = Color(0xFFE5E7EB).copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 2f
                )
            }

            // 2. Tính toán đường cong Bezier
            val path = Path()
            val fillPath = Path() // Đường viền để đổ bóng
            val points = mutableListOf<Offset>()

            data.forEachIndexed { index, pair ->
                val x = index * xStep
                // Dịch xuống một chút (0.8f) để số liệu text không bị lẹm ra ngoài viền trên
                val y = canvasHeight - ((pair.second / maxAmount) * canvasHeight * 0.8f)
                points.add(Offset(x, y))
            }

            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, points[0].y)

                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val cur = points[i]
                    // Tạo hai điểm điều khiển để vuốt cong
                    val controlX1 = (prev.x + cur.x) / 2
                    val controlX2 = (prev.x + cur.x) / 2

                    path.cubicTo(controlX1, prev.y, controlX2, cur.y, cur.x, cur.y)
                    fillPath.cubicTo(controlX1, prev.y, controlX2, cur.y, cur.x, cur.y)
                }

                // 3. Đổ bóng màu Gradient bên dưới đường cong
                fillPath.lineTo(points.last().x, canvasHeight)
                fillPath.lineTo(points.first().x, canvasHeight)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.4f), Color.Transparent),
                        startY = 0f,
                        endY = canvasHeight
                    )
                )

                // 4. Vẽ đường cong chính
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // 5. Vẽ dấu chấm và Hiển thị Text số liệu (K, M)
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f // Cỡ chữ của số liệu
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                points.forEachIndexed { index, point ->
                    val value = data[index].second
                    // Chỉ vẽ chấm và số nếu giá trị > 0
                    if (value > 0f) {
                        drawCircle(color = Color.White, radius = 8f, center = point)
                        drawCircle(color = lineColor, radius = 6f, center = point)

                        drawContext.canvas.nativeCanvas.drawText(
                            formatValue(value),
                            point.x,
                            point.y - 15f,
                            textPaint
                        )
                    }
                }
            }
        }

        // 6. Chú thích trục X (Ngày / Tháng)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { pair ->
                Text(
                    text = pair.first,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
