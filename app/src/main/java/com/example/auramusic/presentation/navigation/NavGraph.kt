package com.example.auramusic.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.media3.exoplayer.ExoPlayer
import com.example.auramusic.presentation.screens.*
import com.example.auramusic.presentation.viewmodel.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Main : Screen("main")
    object Player : Screen("player")
    object CategoryDetail : Screen("category_detail/{genre}") {
        fun createRoute(genre: String) = "category_detail/$genre"
    }
    object TopArtists : Screen("top_artists")
    object Profile : Screen("profile")
    object FavoriteSongs : Screen("favorite_songs")

    object ArtistProfile : Screen("artist_profile/{artistId}") {
        fun createRoute(artistId: String) = "artist_profile/$artistId"
    }
    object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}") {
        fun createRoute(playlistId: String, playlistName: String) = "playlist_detail/$playlistId/$playlistName"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    songViewModel: SongViewModel,
    userViewModel: UserViewModel,
    themeViewModel: ThemeViewModel,
    exoPlayer: ExoPlayer,

    isUserLoggedIn: Boolean
) {
    val startDestination = if (isUserLoggedIn) Screen.Main.route else Screen.Login.route

    // Lấy UID của bạn để truyền vào Profile
    val authState by authViewModel.authState.collectAsState()
    val myUid = authState.user?.uid ?: ""

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                viewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                songViewModel = songViewModel,
                authViewModel = authViewModel,
                userViewModel = userViewModel,
                themeViewModel = themeViewModel,
                navController = navController
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                viewModel = songViewModel,
                authViewModel = authViewModel,
                exoPlayer = exoPlayer,
                onBackClick = { navController.popBackStack() },
                onArtistClick = { artistId ->
                    navController.navigate(Screen.ArtistProfile.createRoute(artistId))
                }
            )
        }

        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(navArgument("genre") { type = NavType.StringType })
        ) { backStackEntry ->
            val genre = backStackEntry.arguments?.getString("genre") ?: ""
            CategoryDetailScreen(
                genre = genre,
                viewModel = songViewModel,
                onBackClick = { navController.popBackStack() },
                onSongClick = { song ->
                    songViewModel.playSong(song)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(Screen.TopArtists.route) {
            TopArtistsScreen(
                viewModel = userViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- MỞ TRANG CỦA CHÍNH MÌNH ---
        composable(Screen.Profile.route) {
            ProfileScreen(
                userId = myUid,
                authViewModel = authViewModel,
                userViewModel = userViewModel,
                songViewModel = songViewModel,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // --- MỞ TRANG CỦA TÁC GIẢ KHÁC ---
        composable(
            route = Screen.ArtistProfile.route,
            arguments = listOf(navArgument("artistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ProfileScreen(
                userId = artistId,
                authViewModel = authViewModel,
                userViewModel = userViewModel,
                songViewModel = songViewModel,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {} // Trống vì nút đăng xuất bị ẩn ở trang người khác
            )
        }

        // --- MÀN HÌNH BÀI HÁT YÊU THÍCH ---
        composable(Screen.FavoriteSongs.route) {
            FavoriteSongsScreen(
                authViewModel = authViewModel,
                songViewModel = songViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("playlistName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            val playlistName = backStackEntry.arguments?.getString("playlistName") ?: "Playlist"

            PlaylistDetailScreen(
                playlistId = playlistId,
                playlistName = playlistName,
                songViewModel = songViewModel,
                onBackClick = { navController.popBackStack() },
                onSongClick = { song ->
                    songViewModel.playSong(song)
                    navController.navigate(Screen.Player.route)
                }
            )
        }
    }
}