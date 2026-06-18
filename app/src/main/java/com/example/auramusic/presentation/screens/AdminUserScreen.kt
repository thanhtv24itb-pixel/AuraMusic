package com.example.auramusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.domain.model.User
import com.example.auramusic.presentation.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserScreen(viewModel: AdminViewModel, myRole: String) {
    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 1. THANH TÌM KIẾM
        OutlinedTextField(
            value = viewModel.userSearchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm tên hoặc email người dùng...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (viewModel.userSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. BỘ LỌC CHỨC DANH (ROLE FILTER)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val roles = listOf(
                Pair("all", "Tất cả"),
                Pair("admin", "Admin"),
                Pair("moderator", "Moderator"),
                Pair("user", "User")
            )

            roles.forEach { (roleValue, roleLabel) ->
                val isSelected = viewModel.userRoleFilter == roleValue
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.updateRoleFilter(roleValue) },
                    label = { Text(roleLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. DANH SÁCH USER (Sử dụng filteredUsers thay vì allUsers)
        if (viewModel.filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy người dùng nào phù hợp.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.filteredUsers) { user ->
                    UserAdminItem(
                        user = user,
                        myRole = myRole,
                        onRoleChange = { newRole -> viewModel.updateUserRole(user.uid, newRole) },
                        onPremiumToggle = { isPremium -> viewModel.toggleUserPremium(user.uid, isPremium) },
                        onLockToggle = { isLocked -> viewModel.toggleUserLock(user.uid, isLocked) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserAdminItem(
    user: User,
    myRole: String, // Nhận myRole
    onRoleChange: (String) -> Unit,
    onPremiumToggle: (Boolean) -> Unit,
    onLockToggle: (Boolean) -> Unit
) {
    val isTargetAdmin = user.role == "admin"
    val amIAdmin = myRole == "admin" // Kiểm tra xem tôi có phải Admin tối cao không

    // Moderator được phép sửa user thường, nhưng KHÔNG được phép sửa account của Admin
    val canIEditThisUser = amIAdmin || (myRole == "moderator" && !isTargetAdmin)

    key(user.uid, user.isLocked) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = if (user.isLocked) androidx.compose.foundation.BorderStroke(1.dp, Color.Red) else null
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .graphicsLayer(alpha = if (user.isLocked) 0.7f else 1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AsyncImage(
                        model = user.avatarUrl.ifBlank { "https://via.placeholder.com/150" },
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    if (user.isLocked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (user.isLocked) Color.Red else Color.Unspecified
                        )
                        if (user.isLocked) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                Text("BỊ KHÓA", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Text(text = user.email, color = Color.Gray, fontSize = 12.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Role: ", fontSize = 12.sp)
                        // Bổ sung isClickable để chặn Moderator bấm
                        RoleBadge(role = user.role, isClickable = amIAdmin, onClick = {
                            if (amIAdmin) {
                                // Vòng lặp đổi Role: user -> moderator -> admin -> user
                                val nextRole = when(user.role) {
                                    "user" -> "moderator"
                                    "moderator" -> "admin"
                                    else -> "user"
                                }
                                onRoleChange(nextRole)
                            }
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        if (user.premium) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Khóa công tắc Premium nếu người dùng không có quyền chạm vào tài khoản này
                Switch(
                    checked = user.premium,
                    onCheckedChange = onPremiumToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B)),
                    enabled = !user.isLocked && canIEditThisUser
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Ẩn nút KHÓA đối với tài khoản Admin. Chỉ hiện cho User và Moderator.
                if (!isTargetAdmin) {
                    Button(
                        onClick = { onLockToggle(!user.isLocked) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.isLocked) Color(0xFFEF4444) else Color(0xFF22C55E)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = canIEditThisUser // Chặn bấm nếu không có quyền
                    ) {
                        Icon(
                            imageVector = if (user.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (user.isLocked) "MỞ KHÓA" else "KHÓA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "ADMIN",
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: String, isClickable: Boolean, onClick: () -> Unit) {
    // Thêm màu xanh dương cho Moderator để phân biệt
    val badgeColor = when(role) {
        "admin" -> Color(0xFFEF4444)
        "moderator" -> Color(0xFF3B82F6) // Màu xanh dương
        else -> Color.Gray
    }

    Surface(
        onClick = { if (isClickable) onClick() }, // Chặn thao tác click nếu isClickable = false
        color = badgeColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = role.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor
        )
    }
}