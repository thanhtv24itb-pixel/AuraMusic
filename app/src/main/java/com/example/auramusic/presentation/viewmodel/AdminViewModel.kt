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

    // --- CÁC HÀM TỔNG QUAN ---
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

    // --- QUẢN LÝ DANH MỤC ---
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

    // --- QUẢN LÝ BÀI HÁT ---
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

    fun rejectSong(songId: String, reason: String) {
        if (songId.isBlank()) return
        db.collection("songs").document(songId)
            .update(
                mapOf(
                    "status" to "rejected",
                    "rejectReason" to reason
                )
            )
            .addOnSuccessListener {
                allSongs = allSongs.map {
                    if (it.songId == songId) {
                        it.copy().apply { status = "rejected" }
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
    var userSearchQuery by mutableStateOf("")
    var userRoleFilter by mutableStateOf("all")

    val filteredUsers: List<User>
        get() = allUsers.filter { user ->
            val matchSearch = user.displayName.contains(userSearchQuery, ignoreCase = true) ||
                    user.email.contains(userSearchQuery, ignoreCase = true)
            val matchRole = if (userRoleFilter == "all") true else user.role == userRoleFilter
            matchSearch && matchRole
        }

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

    fun updateSearchQuery(query: String) {
        userSearchQuery = query
    }

    fun updateRoleFilter(role: String) {
        userRoleFilter = role
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

    // ==========================================
    // LOGIC TÍNH TOÁN DỮ LIỆU BIỂU ĐỒ
    // ==========================================
    var revenueFilter by mutableStateOf("7days") // 3 chế độ: "7days", "month", "year"

    // Hàm lấy dữ liệu Biểu đồ Doanh thu (VỪA ĐƯỢC THÊM VÀO)
    fun getRevenueChartData(): List<Pair<String, Float>> {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val formatDay = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())

        return when (revenueFilter) {
            "7days" -> {
                val map = mutableMapOf<String, Float>()
                for (i in 6 downTo 0) {
                    calendar.timeInMillis = System.currentTimeMillis() - i * 86400000L
                    map[formatDay.format(calendar.time)] = 0f
                }
                allTransactions.forEach { tx ->
                    val createdAtMs = tx.createdAt?.toDate()?.time ?: return@forEach

                    if (System.currentTimeMillis() - createdAtMs <= 7 * 86400000L) {
                        calendar.timeInMillis = createdAtMs
                        val key = formatDay.format(calendar.time)
                        val amount = tx.amount.toFloat()
                        if (map.containsKey(key)) map[key] = map[key]!! + amount
                    }
                }
                map.toList()
            }
            "month" -> {
                val map = mutableMapOf("Tuần 1" to 0f, "Tuần 2" to 0f, "Tuần 3" to 0f, "Tuần 4" to 0f)
                allTransactions.forEach { tx ->
                    val createdAtMs = tx.createdAt?.toDate()?.time ?: return@forEach
                    calendar.timeInMillis = createdAtMs

                    if (calendar.get(java.util.Calendar.YEAR) == currentYear && calendar.get(java.util.Calendar.MONTH) == currentMonth) {
                        val amount = tx.amount.toFloat()
                        when (calendar.get(java.util.Calendar.DAY_OF_MONTH)) {
                            in 1..7 -> map["Tuần 1"] = map["Tuần 1"]!! + amount
                            in 8..14 -> map["Tuần 2"] = map["Tuần 2"]!! + amount
                            in 15..21 -> map["Tuần 3"] = map["Tuần 3"]!! + amount
                            else -> map["Tuần 4"] = map["Tuần 4"]!! + amount
                        }
                    }
                }
                map.toList()
            }
            "year" -> {
                val map = mutableMapOf<String, Float>()
                for (i in 1..12) map["T$i"] = 0f
                allTransactions.forEach { tx ->
                    val createdAtMs = tx.createdAt?.toDate()?.time ?: return@forEach
                    calendar.timeInMillis = createdAtMs

                    if (calendar.get(java.util.Calendar.YEAR) == currentYear) {
                        val month = calendar.get(java.util.Calendar.MONTH) + 1
                        val amount = tx.amount.toFloat()
                        map["T$month"] = map["T$month"]!! + amount
                    }
                }
                map.toList()
            }
            else -> emptyList()
        }
    }



    // onCleared PHẢI NẰM Ở CUỐI CÙNG
    override fun onCleared() {
        super.onCleared()
        categoriesListener?.remove()
        songsListener?.remove()
        usersListener?.remove()
        transactionsListener?.remove()
    }
}