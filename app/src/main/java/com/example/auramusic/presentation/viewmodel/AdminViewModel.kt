package com.example.auramusic.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Các biến lưu trữ dữ liệu thống kê
    var totalUsers by mutableStateOf(0)
        private set
    var totalPremium by mutableStateOf(0)
        private set
    var totalSongs by mutableStateOf(0)
        private set
    var totalRevenue by mutableStateOf(0L)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var allSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    // Hàm gọi Firebase để lấy dữ liệu
    fun loadDashboardStats() {
        viewModelScope.launch {
            isLoading = true
            try {
                // 1. Đếm User & User VIP
                val usersSnapshot = db.collection("users").get().await()
                totalUsers = usersSnapshot.size()
                totalPremium = usersSnapshot.documents.count { it.getBoolean("premium") == true }

                // 2. Đếm tổng số bài hát
                val songsSnapshot = db.collection("songs").get().await()
                totalSongs = songsSnapshot.size()

                // 3. Tính tổng doanh thu (Chỉ cộng những giao dịch Success)
                val transactionsSnapshot = db.collection("transactions")
                    .whereEqualTo("status", "Success")
                    .get().await()

                var sum = 0L
                for (doc in transactionsSnapshot.documents) {
                    val amount = doc.getLong("amount") ?: 0L
                    sum += amount
                }
                totalRevenue = sum

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    fun loadCategories() {
        viewModelScope.launch {
            db.collection("categories").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { document ->
                    // Ép kiểu document về data class Category
                    val cat = document.toObject(Category::class.java)
                    // Cập nhật lại ID bằng chính ID của document trên Firebase
                    cat?.copy(id = document.id)
                }
                categories = list
            }
        }
    }

    // 2. Hàm Thêm Thể loại mới
    fun addCategory(name: String, imageUrl: String) {
        val newDocRef = db.collection("categories").document()
        val newCategory = hashMapOf(
            "id" to newDocRef.id,
            "name" to name,
            "imageUrl" to imageUrl
        )
        newDocRef.set(newCategory)
    }

    // 3. Hàm Sửa Thể loại
    fun updateCategory(id: String, newName: String, newImageUrl: String) {
        db.collection("categories").document(id)
            .update(
                mapOf(
                    "name" to newName,
                    "imageUrl" to newImageUrl
                )
            )
    }

    // 4. Hàm Xóa Thể loại
    fun deleteCategory(id: String) {
        db.collection("categories").document(id).delete()
    }
    fun loadAllSongs() {
        viewModelScope.launch {
            db.collection("songs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { document ->
                        val song = document.toObject(Song::class.java)
                        song?.copy(songId = document.id)
                    }
                    allSongs = list
                }
        }
    }

    // 2. Hàm Duyệt bài hát (Chuyển thành approved)
    fun approveSong(songId: String) {
        db.collection("songs").document(songId).update("status", "approved")
    }

    // 3. Hàm Từ chối bài hát (Chuyển thành rejected)
    fun rejectSong(songId: String) {
        db.collection("songs").document(songId).update("status", "rejected")
    }
    fun updateSongStatus(songId: String, newStatus: String) {
        db.collection("songs").document(songId)
            .update("status", newStatus)
            .addOnSuccessListener {
                // Có thể thêm Toast báo thành công nếu muốn
            }
    }
}