package com.example.auramusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.usecase.LoginUseCase
import com.example.auramusic.domain.usecase.SignupUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val signupUseCase: SignupUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState

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
    }

    fun logout() {
        firebaseAuth.signOut()
        userListener?.remove()
        resetState()
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
    }
}