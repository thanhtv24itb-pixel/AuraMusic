package com.example.auramusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _topArtists = MutableStateFlow<List<User>>(emptyList())
    val topArtists: StateFlow<List<User>> = _topArtists

    private val _artistProfile = MutableStateFlow<User?>(null)
    val artistProfile: StateFlow<User?> = _artistProfile

    private val _isArtistLiked = MutableStateFlow(false)
    val isArtistLiked: StateFlow<Boolean> = _isArtistLiked

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadTopArtists(limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getTopArtists(limit).onSuccess {
                _topArtists.value = it
                _isLoading.value = false
            }.onFailure {
                _error.value = it.message ?: "Lỗi tải danh sách tác giả"
                _isLoading.value = false
            }
        }
    }

    // --- ĐÃ SỬA: Thêm chặn ID rỗng và try-catch ---
    fun loadArtistProfile(artistId: String, myUserId: String?) {
        // 1. CHẶN ĐỨNG ID RỖNG ĐỂ KHÔNG BỊ CRASH APP
        if (artistId.isBlank()) {
            _error.value = "ID người dùng không hợp lệ"
            return
        }

        viewModelScope.launch {
            try {
                // 2. Load thông tin User
                val snapshot = firestore.collection("users").document(artistId).get().await()
                _artistProfile.value = snapshot.toObject(User::class.java)

                // 3. Kiểm tra xem mình đã Like chưa
                if (myUserId != null && myUserId.isNotBlank()) {
                    userRepository.checkIsProfileLiked(myUserId, artistId).onSuccess {
                        _isArtistLiked.value = it
                    }
                }
            } catch (e: Exception) {
                // Nếu lỗi Firebase thì báo lỗi chứ không văng app
                _error.value = e.message
            }
        }
    }

    fun toggleLikeArtist(myUserId: String, artistId: String) {
        // Chặn ID rỗng
        if (artistId.isBlank() || myUserId.isBlank()) return

        viewModelScope.launch {
            userRepository.toggleLikeProfile(myUserId, artistId).onSuccess { isLiked ->
                _isArtistLiked.value = isLiked
                // Sau khi Like xong thì load lại profile để cập nhật số lượt Like hiển thị
                loadArtistProfile(artistId, myUserId)
            }
        }
    }
}