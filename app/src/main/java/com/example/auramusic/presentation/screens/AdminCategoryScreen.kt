package com.example.auramusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.auramusic.domain.model.Category
import com.example.auramusic.presentation.viewmodel.AdminViewModel

@Composable
fun AdminCategoryScreen(viewModel: AdminViewModel) {
    // Tải dữ liệu khi vừa mở màn hình
    LaunchedEffect(Unit) {
        viewModel.loadCategories()
    }

    // Các biến trạng thái để điều khiển Popup (Dialog)
    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    // Các biến lưu chữ đang gõ trong textfield
    var inputName by remember { mutableStateOf("") }
    var inputImageUrl by remember { mutableStateOf("") }

    // Hàm mở Dialog và điền sẵn dữ liệu (nếu là Sửa)
    fun openDialog(category: Category? = null) {
        editingCategory = category
        inputName = category?.name ?: ""
        inputImageUrl = category?.imageUrl ?: ""
        showDialog = true
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openDialog(null) }, // Truyền null nghĩa là Thêm mới
                containerColor = Color(0xFFEF4444)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm thể loại", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.categories) { category ->
                CategoryAdminItem(
                    category = category,
                    onEditClick = { openDialog(category) },
                    onDeleteClick = { viewModel.deleteCategory(category.id) }
                )
            }
        }
    }

    // Popup Thêm / Sửa Thể loại
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingCategory == null) "Thêm Thể Loại" else "Sửa Thể Loại") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Tên thể loại (VD: Pop, Ballad)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputImageUrl,
                        onValueChange = { inputImageUrl = it },
                        label = { Text("Link ảnh minh họa (URL)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            if (editingCategory == null) {
                                viewModel.addCategory(inputName, inputImageUrl)
                            } else {
                                viewModel.updateCategory(editingCategory!!.id, inputName, inputImageUrl)
                            }
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Hủy", color = Color.Gray) }
            }
        )
    }
}

// Khuôn mẫu từng dòng Thể loại hiển thị trong danh sách
@Composable
fun CategoryAdminItem(category: Category, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ảnh bìa
                AsyncImage(
                    model = category.imageUrl.ifBlank { "https://via.placeholder.com/150" },
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Tên thể loại
                Text(text = category.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Nhóm 2 nút Sửa và Xóa
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color(0xFF3B82F6))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
                }
            }
        }
    }
}