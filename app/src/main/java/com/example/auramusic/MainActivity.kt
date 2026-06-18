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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.platform.LocalContext
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
            signupUseCase = SignupUseCase(authRepository),
            googleLoginUseCase = GoogleLoginUseCase(authRepository),
            resetPasswordUseCase = ResetPasswordUseCase(authRepository)
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
        
        val themeViewModel = ThemeViewModel()

        setContent {
            val context = LocalContext.current
            val songState by songViewModel.songState.collectAsState()

            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                // ĐÃ SỬA: Giao quyền quyết định qua cho ViewModel khi hết bài hát
                                songViewModel.handleSongEnded()
                                seekTo(0)
                            }
                        }
                    })
                }
            }

            LaunchedEffect(songState.currentSong?.audioUrl) {
                songState.currentSong?.audioUrl?.let { url ->
                    val mediaItem = MediaItem.fromUri(url)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    if (songState.isPlaying) {
                        exoPlayer.play()
                    }
                }
            }

            LaunchedEffect(songState.isPlaying) {
                if (songState.isPlaying) {
                    exoPlayer.play()
                } else {
                    exoPlayer.pause()
                }
            }

            LaunchedEffect(songState.isPlaying) {
                while (true) {
                    if (songState.isPlaying) {
                        // ĐÃ SỬA: Lấy current user ID truyền vào để hàm updateProgress có ID lưu Lịch sử
                        val currentUserId = firebaseAuth.currentUser?.uid
                        songViewModel.updateProgress((exoPlayer.currentPosition / 1000).toInt(), currentUserId)
                    }
                    kotlinx.coroutines.delay(1000L)
                }
            }

            val themeMode by themeViewModel.themeMode.collectAsState()
            AuraMusicTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val currentUser = firebaseAuth.currentUser

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            songViewModel = songViewModel,
                            userViewModel = userViewModel,
                            themeViewModel = themeViewModel,
                            exoPlayer = exoPlayer,
                            isUserLoggedIn = currentUser != null
                        )
                    }
                }
            }
        }
    }
}