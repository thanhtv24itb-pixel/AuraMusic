package com.example.auramusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.cloudinary.android.MediaManager
import com.example.auramusic.data.repository.AuthRepositoryImpl
import com.example.auramusic.data.repository.SongRepositoryImpl
import com.example.auramusic.data.repository.UserRepositoryImpl
import com.example.auramusic.domain.usecase.*
import com.example.auramusic.presentation.navigation.NavGraph
import com.example.auramusic.presentation.theme.AuraMusicTheme
import com.example.auramusic.presentation.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- THÊM ĐOẠN NÀY ĐỂ KHỞI TẠO CLOUDINARY ---
        try {
            val config = mapOf(
                "cloud_name" to "dfqrbtq3o", // Đã thay mã của bạn vào đây
                "secure" to true
            )
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // Đã khởi tạo rồi thì bỏ qua
        }
        // ---------------------------------------------

        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // 1. Initialize Repositories
        val authRepository = AuthRepositoryImpl(firebaseAuth, firestore)
        val songRepository = SongRepositoryImpl(firestore)
        val userRepository = UserRepositoryImpl(firestore)


        // 2. Initialize ViewModels
        val authViewModel = AuthViewModel(
            loginUseCase = LoginUseCase(authRepository),
            signupUseCase = SignupUseCase(authRepository)
        )

        val songViewModel = SongViewModel(
            getMostPlayedSongsUseCase = GetMostPlayedSongsUseCase(songRepository),
            getRecentSongsUseCase = GetRecentSongsUseCase(songRepository),
            getCategoriesUseCase = GetCategoriesUseCase(songRepository),
            searchSongsUseCase = SearchSongsUseCase(songRepository),
            getSongsByCategoryUseCase = GetSongsByCategoryUseCase(songRepository),
            uploadSongUseCase = UploadSongUseCase(songRepository),
            toggleLikeUseCase = ToggleLikeUseCase(songRepository),
            songRepository = songRepository
        )

        val userViewModel = UserViewModel(
            userRepository = userRepository,
            firestore = FirebaseFirestore.getInstance()
        )

        setContent {
            AuraMusicTheme {
                val navController = rememberNavController()
                val currentUser = firebaseAuth.currentUser

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            songViewModel = songViewModel,
                            userViewModel = userViewModel,
                            // ĐÃ DỌN SẠCH commentViewModel Ở ĐÂY
                            isUserLoggedIn = currentUser != null
                        )
                    }
                }
            }
        }
    }
}