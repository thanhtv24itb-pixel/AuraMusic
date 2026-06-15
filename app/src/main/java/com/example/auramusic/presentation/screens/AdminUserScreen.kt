package com.example.auramusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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

@Composable
fun AdminUserScreen(viewModel: AdminViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(viewModel.allUsers) { user ->
            UserAdminItem(
                user = user,
                onRoleChange = { newRole -> viewModel.updateUserRole(user.uid, newRole) },
                onPremiumToggle = { isPremium -> viewModel.toggleUserPremium(user.uid, isPremium) },
                onLockToggle = { isLocked -> 
                    viewModel.toggleUserLock(user.uid, isLocked)
                }
            )
        }
    }
}

@Composable
fun UserAdminItem(
    user: User,
    onRoleChange: (String) -> Unit,
    onPremiumToggle: (Boolean) -> Unit,
    onLockToggle: (Boolean) -> Unit
) {
    val isAdmin = user.role == "admin"
    
    key(user.uid, user.isLocked) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White // Luôn là màu trắng theo yêu cầu
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = if (user.isLocked) androidx.compose.foundation.BorderStroke(1.dp, Color.Red) else null
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .graphicsLayer(alpha = if (user.isLocked) 0.7f else 1f), // Làm mờ nhẹ nếu bị khóa
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
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
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
                            Surface(
                                color = Color.Red,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "BỊ KHÓA",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Text(text = user.email, color = Color.Gray, fontSize = 12.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Role: ", fontSize = 12.sp)
                        RoleBadge(role = user.role, onClick = {
                            val nextRole = if (user.role == "admin") "user" else "admin"
                            onRoleChange(nextRole)
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        if (user.premium) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                Switch(
                    checked = user.premium,
                    onCheckedChange = onPremiumToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B)),
                    enabled = !user.isLocked // Không cho đổi premium nếu đang bị khóa
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (!isAdmin) {
                    Button(
                        onClick = { onLockToggle(!user.isLocked) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.isLocked) Color(0xFFEF4444) else Color(0xFF22C55E) // Khóa: Đỏ, Mở: Xanh lá
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp)
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
                    // Đối với Admin, hiển thị một badge hoặc text thay vì nút bấm để không bị khóa nhầm
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
fun RoleBadge(role: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (role == "admin") Color(0xFFEF4444).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = role.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (role == "admin") Color(0xFFEF4444) else Color.Gray
        )
    }
}
