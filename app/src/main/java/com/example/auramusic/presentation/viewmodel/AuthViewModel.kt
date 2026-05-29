package com.example.auramusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.usecase.LoginUseCase
import com.example.auramusic.domain.usecase.SignupUseCase
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState(isLoading = true)
            val result = loginUseCase(email, password)
            result.onSuccess { user ->
                _authState.value = AuthUiState(user = user, success = true)
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
            }
            result.onFailure { exception ->
                _authState.value = AuthUiState(
                    error = exception.message ?: "Đăng ký thất bại",
                    isLoading = false
                )
            }
        }
    }
    fun logout() {
        // Gọi trực tiếp Firebase để xóa phiên đăng nhập trên thiết bị
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        // Đặt lại trạng thái AuthUiState về mặc định
        resetState()
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun resetState() {
        _authState.value = AuthUiState()
    }

}