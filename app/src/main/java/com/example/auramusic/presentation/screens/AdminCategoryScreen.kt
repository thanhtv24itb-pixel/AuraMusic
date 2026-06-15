package com.example.auramusic.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.auramusic.util.CloudinaryUtils
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    val scope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

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
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (inputImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = inputImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Chọn ảnh từ điện thoại")
                        }
                        
                        if (isUploading) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            scope.launch {
                                isUploading = true
                                val finalImageUrl = if (selectedImageUri != null) {
                                    try {
                                        CloudinaryUtils.uploadToCloudinary(selectedImageUri!!)
                                    } catch (e: Exception) {
                                        inputImageUrl
                                    }
                                } else {
                                    inputImageUrl
                                }
                                
                                if (editingCategory == null) {
                                    viewModel.addCategory(inputName, finalImageUrl)
                                } else {
                                    viewModel.updateCategory(editingCategory!!.id, inputName, finalImageUrl)
                                }
                                isUploading = false
                                showDialog = false
                                selectedImageUri = null
                            }
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(if (isUploading) "Đang tải..." else "Lưu")
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