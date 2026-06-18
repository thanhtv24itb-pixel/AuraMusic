package com.example.auramusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.model.User
import com.example.auramusic.util.CloudinaryUtils
import android.net.Uri
import com.example.auramusic.domain.usecase.LoginUseCase
import com.example.auramusic.domain.usecase.SignupUseCase
import com.example.auramusic.domain.usecase.GoogleLoginUseCase
import com.example.auramusic.domain.usecase.ResetPasswordUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val signupUseCase: SignupUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState

    private val _userSongs = MutableStateFlow<List<Song>>(emptyList())
    val userSongs: StateFlow<List<Song>> = _userSongs

    private var songsListener: ListenerRegistration? = null

    // CHÍNH LÀ 3 BIẾN NÀY ĐÃ GIÚP CHỮA HẾT BÁO ĐỎ:
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var userListener: ListenerRegistration? = null

    // KHỐI INIT: Kích hoạt ống nghe ngay khi mở App
    init {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            listenToCurrentUser(currentUser.uid)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState(isLoading = true)
            val result = loginUseCase(email, password)
            result.onSuccess { user ->
                _authState.value = AuthUiState(user = user, success = true)
                listenToCurrentUser(user.uid)
            }
            result.onFailure { exception ->
                _authState.value = AuthUiState(
                    error = exception.message ?: "Đăng nhập thất bại",
                    isLoading = false
                )
            }
        }
    }

     fun signup(email: String, password: String, displayName: String) {
         viewModelScope.launch {
             _authState.value = AuthUiState(isLoading = true)
             val result = signupUseCase(email, password, displayName)
             result.onSuccess { user ->
                 _authState.value = AuthUiState(user = user, success = true)
                 listenToCurrentUser(user.uid)
             }
             result.onFailure { exception ->
                 _authState.value = AuthUiState(
                     error = exception.message ?: "Đăng ký thất bại",
                     isLoading = false
                 )
             }
         }
     }

     fun loginWithGoogle(idToken: String, displayName: String = "") {
         viewModelScope.launch {
             _authState.value = AuthUiState(isLoading = true)
             val result = googleLoginUseCase(idToken, displayName)
             result.onSuccess { user ->
                 _authState.value = AuthUiState(user = user, success = true)
                 listenToCurrentUser(user.uid)
             }
             result.onFailure { exception ->
                 _authState.value = AuthUiState(
                     error = exception.message ?: "Đăng nhập bằng Google thất bại",
                     isLoading = false
                 )
             }
         }
     }

     fun resetPassword(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
         viewModelScope.launch {
             _authState.value = AuthUiState(isLoading = true)
             val result = resetPasswordUseCase(email)
             _authState.value = AuthUiState(isLoading = false)
             result.onSuccess {
                 onSuccess()
             }
             result.onFailure { exception ->
                 val errorMsg = exception.message ?: "Gửi email khôi phục thất bại"
                 _authState.value = AuthUiState(error = errorMsg)
                 onFailure(errorMsg)
             }
         }
     }

     // Hàm Lắng nghe thời gian thực (đã ép kiểu chuẩn)
    private fun listenToCurrentUser(uid: String) {
        userListener?.remove()

        userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot: DocumentSnapshot?, error: FirebaseFirestoreException? ->

                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val updatedUser = snapshot.toObject(User::class.java)

                    if (updatedUser != null) {
                        _authState.value = _authState.value.copy(
                            user = updatedUser,
                            success = true
                        )
                    }
                }
            }

        // Nghe danh sách bài hát của user để thông báo duyệt
        songsListener?.remove()
        songsListener = firestore.collection("songs")
            .whereEqualTo("artistId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _userSongs.value = snapshot.toObjects(Song::class.java)
                }
            }
    }

    fun updateProfile(displayName: String, avatarUri: Uri?) {
        val user = _authState.value.user ?: return
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            try {
                val avatarUrl = if (avatarUri != null) {
                    CloudinaryUtils.uploadToCloudinary(avatarUri)
                } else {
                    user.avatarUrl
                }

                val updateData = mutableMapOf<String, Any>(
                    "displayName" to displayName,
                    "avatarUrl" to avatarUrl
                )

                firestore.collection("users").document(user.uid).update(updateData).await()
                _authState.value = _authState.value.copy(isLoading = false, success = true)
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun logout() {
        firebaseAuth.signOut()
        userListener?.remove()
        resetState()
    }

    fun setErrorMessage(message: String) {
        _authState.value = _authState.value.copy(error = message, success = false)
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun resetState() {
        _authState.value = AuthUiState()
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        songsListener?.remove()
    }
}