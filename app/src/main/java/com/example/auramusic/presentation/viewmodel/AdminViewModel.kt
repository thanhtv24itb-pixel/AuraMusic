package com.example.auramusic.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var categoriesListener: ListenerRegistration? = null
    private var songsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null

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
    var allUsers by mutableStateOf<List<User>>(emptyList())
        private set
    var allTransactions by mutableStateOf<List<Transaction>>(emptyList())
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
        categoriesListener?.remove()
        categoriesListener = db.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { document ->
                val cat = document.toObject(Category::class.java)
                cat?.copy(id = document.id)
            }
            categories = list
        }
    }

    fun addCategory(name: String, imageUrl: String) {
        val newDocRef = db.collection("categories").document()
        val newCategory = hashMapOf(
            "id" to newDocRef.id,
            "name" to name,
            "imageUrl" to imageUrl
        )
        newDocRef.set(newCategory)
    }

    fun updateCategory(id: String, newName: String, newImageUrl: String) {
        db.collection("categories").document(id)
            .update(
                mapOf(
                    "name" to newName,
                    "imageUrl" to newImageUrl
                )
            )
    }

    fun deleteCategory(id: String) {
        db.collection("categories").document(id).delete()
    }

    fun loadAllSongs() {
        songsListener?.remove()
        songsListener = db.collection("songs")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { document ->
                    val song = document.toObject(Song::class.java)
                    song?.copy(songId = document.id)
                }
                allSongs = list
            }
    }

    fun approveSong(songId: String) {
        if (songId.isBlank()) return
        db.collection("songs").document(songId).update("status", "approved")
            .addOnSuccessListener {
                allSongs = allSongs.map {
                    if (it.songId == songId) it.copy().apply { status = "approved" } else it
                }
            }
    }

    // ĐÃ SỬA: Hàm từ chối cập nhật trạng thái và lý do bằng cách dùng biến db có sẵn
    fun rejectSong(songId: String, reason: String) {
        if (songId.isBlank()) return
        db.collection("songs").document(songId)
            .update(
                mapOf(
                    "status" to "rejected",
                    "rejectReason" to reason // Đẩy lý do lên database
                )
            )
            .addOnSuccessListener {
                // Cập nhật lại giao diện ngay lập tức
                allSongs = allSongs.map {
                    if (it.songId == songId) {
                        it.copy().apply {
                            status = "rejected"
                            // Firebase Snapshot listener cũng sẽ tự fetch và đồng bộ lại rejectReason nếu ở giao diện khác
                        }
                    } else it
                }
            }
    }

    fun updateSongStatus(songId: String, newStatus: String) {
        db.collection("songs").document(songId).update("status", newStatus)
    }

    fun editSong(songId: String, title: String, genre: String) {
        db.collection("songs").document(songId).update(
            mapOf(
                "title" to title,
                "genre" to genre
            )
        )
    }

    // --- QUẢN LÝ USER ---
    fun loadAllUsers() {
        usersListener?.remove()
        usersListener = db.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                allUsers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }
            }
        }
    }

    fun updateUserRole(userId: String, newRole: String) {
        db.collection("users").document(userId).update("role", newRole)
    }

    fun toggleUserLock(userId: String, isLocked: Boolean) {
        if (userId.isBlank()) return

        allUsers = allUsers.map {
            if (it.uid == userId) {
                it.copy().apply { this.isLocked = isLocked }
            } else it
        }

        db.collection("users").document(userId)
            .update("isLocked", isLocked)
            .addOnFailureListener { e ->
                allUsers = allUsers.map {
                    if (it.uid == userId) {
                        it.copy().apply { this.isLocked = !isLocked }
                    } else it
                }
                e.printStackTrace()
            }
    }

    fun toggleUserPremium(userId: String, isPremium: Boolean) {
        db.collection("users").document(userId).update("premium", isPremium)
    }

    // --- QUẢN LÝ GIAO DỊCH ---
    fun loadAllTransactions() {
        transactionsListener?.remove()
        transactionsListener = db.collection("transactions")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allTransactions = snapshot.toObjects(Transaction::class.java)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        categoriesListener?.remove()
        songsListener?.remove()
        usersListener?.remove()
        transactionsListener?.remove()
    }
}