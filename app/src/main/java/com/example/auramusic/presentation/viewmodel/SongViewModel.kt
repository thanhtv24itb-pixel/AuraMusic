package com.example.auramusic.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.model.Playlist
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.model.Comment
import com.example.auramusic.domain.repository.SongRepository
import com.example.auramusic.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SongUiState(
    val isLoading: Boolean = false,
    val mostPlayedSongs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val selectedCategorySongs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val error: String? = null,
    val isCurrentSongLiked: Boolean = false,
    val comments: List<Comment> = emptyList()
)

class SongViewModel(
    private val getMostPlayedSongsUseCase: GetMostPlayedSongsUseCase,
    private val getRecentSongsUseCase: GetRecentSongsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val searchSongsUseCase: SearchSongsUseCase,
    private val getSongsByCategoryUseCase: GetSongsByCategoryUseCase,
    private val uploadSongUseCase: UploadSongUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _songState = MutableStateFlow(SongUiState())
    val songState: StateFlow<SongUiState> = _songState

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs

    private val _myPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val myPlaylists: StateFlow<List<Playlist>> = _myPlaylists

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs

    // Biến lưu danh sách Lịch sử nghe nhạc
    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs

    // --- CÁC BIẾN QUẢN LÝ TĂNG VIEW & HẸN GIỜ ---
    private var hasCountedViewAndHistory = false
    private var sleepTimerJob: Job? = null
    var stopAfterCurrentSong = false
        private set

    init {
        loadHomeData()
        loadCategories()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _songState.value = _songState.value.copy(isLoading = true)
            val mostPlayed = getMostPlayedSongsUseCase()
            val recent = getRecentSongsUseCase()
            _songState.value = _songState.value.copy(
                mostPlayedSongs = mostPlayed.getOrDefault(emptyList()),
                recentSongs = recent.getOrDefault(emptyList()),
                isLoading = false
            )
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val result = getCategoriesUseCase()
            _songState.value = _songState.value.copy(categories = result.getOrDefault(emptyList()))
        }
    }

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _songState.value = _songState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            val result = searchSongsUseCase(query)
            _songState.value = _songState.value.copy(searchResults = result.getOrDefault(emptyList()))
        }
    }

    fun loadSongsByCategory(genre: String) {
        viewModelScope.launch {
            _songState.value = _songState.value.copy(isLoading = true)
            val result = getSongsByCategoryUseCase(genre)
            _songState.value = _songState.value.copy(
                selectedCategorySongs = result.getOrDefault(emptyList()),
                isLoading = false
            )
        }
    }

    // --- LOGIC PHÁT NHẠC VÀ THEO DÕI TIẾN ĐỘ ---
    fun playSong(song: Song) {
        _songState.value = _songState.value.copy(
            currentSong = song,
            isPlaying = true,
            currentPosition = 0
        )
        // Reset cờ khi người dùng bấm nghe bài hát mới
        hasCountedViewAndHistory = false
    }

    fun pauseSong() {
        _songState.value = _songState.value.copy(isPlaying = false)
    }

    fun resumeSong() {
        _songState.value = _songState.value.copy(isPlaying = true)
    }

    fun updateProgress(positionInSeconds: Int, currentUserId: String?) {
        _songState.value = _songState.value.copy(currentPosition = positionInSeconds)

        // Tính năng: Lớn hơn hoặc bằng 10 giây sẽ cộng view và lưu lịch sử
        if (positionInSeconds >= 10 && !hasCountedViewAndHistory) {
            hasCountedViewAndHistory = true
            val currentSong = _songState.value.currentSong

            if (currentSong != null) {
                viewModelScope.launch {
                    // 1. Tăng lượt nghe (View)
                    songRepository.incrementPlayCount(currentSong.songId, currentSong.artistId)

                    // 2. LƯU LỊCH SỬ NGHE NHẠC (Đoạn bạn bị thiếu)
                    if (currentUserId != null) {
                        songRepository.addToHistory(currentUserId, currentSong.songId)
                    }
                }
            }
        }
    }

    // --- LOGIC KHI BÀI HÁT KẾT THÚC ---
    fun handleSongEnded() {
        // ĐÃ SỬA: Reset lại cờ khi hết bài. Để nếu bài tự phát lại (Loop) thì tính 1 lượt nghe mới.
        hasCountedViewAndHistory = false

        if (stopAfterCurrentSong) {
            pauseSong()
            updateProgress(0, null)
            stopAfterCurrentSong = false // Tắt hẹn giờ đi
        } else {
            // Logic chuyển bài (ví dụ Auto-next hoặc Lặp lại tùy bạn phát triển sau)
            pauseSong()
            updateProgress(0, null)
        }
    }

    // --- LOGIC HẸN GIỜ TẮT NHẠC ---
    fun startSleepTimerByMinutes(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60 * 1000L)
            pauseSong()
        }
    }

    fun setStopAfterCurrentSong() {
        cancelSleepTimer()
        stopAfterCurrentSong = true
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        stopAfterCurrentSong = false
    }

    // --- CÁC HÀM LIÊN QUAN ĐẾN USER, LỊCH SỬ & PLAYLIST ---
    fun loadRecentlyPlayed(userId: String) {
        viewModelScope.launch {
            songRepository.getRecentlyPlayed(userId).onSuccess { songs ->
                _recentlyPlayedSongs.value = songs
            }.onFailure {
                _recentlyPlayedSongs.value = emptyList()
            }
        }
    }

    fun uploadSong(
        title: String, artistId: String, artistName: String, audioUri: Uri, imageUri: Uri?,
        category: String, duration: Int = 0, onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = uploadSongUseCase(
                title, artistId, artistName, audioUri, imageUri, category, duration
            )
            result.onSuccess {
                _isUploading.value = false
                onSuccess()
            }.onFailure { error ->
                _isUploading.value = false
                onError(error.message ?: "Có lỗi xảy ra khi tải bài hát lên")
            }
        }
    }

    fun checkIsSongLiked(userId: String, songId: String) {
        viewModelScope.launch {
            val result = songRepository.checkIsLiked(userId, songId)
            _songState.value = _songState.value.copy(
                isCurrentSongLiked = result.getOrDefault(false)
            )
        }
    }

    fun toggleLike(userId: String) {
        val currentSong = _songState.value.currentSong ?: return
        viewModelScope.launch {
            val result = toggleLikeUseCase(userId, currentSong.songId)
            if (result.isSuccess) {
                _songState.value = _songState.value.copy(
                    isCurrentSongLiked = result.getOrNull() ?: false
                )
            }
        }
    }

    fun loadFavoriteSongs(userId: String) {
        viewModelScope.launch {
            songRepository.getFavoriteSongs(userId).onSuccess { songs ->
                _favoriteSongs.value = songs
            }.onFailure {
                _favoriteSongs.value = emptyList()
            }
        }
    }

    fun loadMyPlaylists(userId: String) {
        viewModelScope.launch {
            songRepository.getUserPlaylists(userId).onSuccess { playlists ->
                _myPlaylists.value = playlists
            }.onFailure {
                _myPlaylists.value = emptyList()
            }
        }
    }

    fun createPlaylist(userId: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (name.isBlank()) {
            onError("Tên playlist không được để trống")
            return
        }
        viewModelScope.launch {
            songRepository.createPlaylist(userId, name).onSuccess {
                loadMyPlaylists(userId)
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Lỗi khi tạo Playlist")
            }
        }
    }

    fun addCurrentSongToPlaylist(playlistId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentSongId = _songState.value.currentSong?.songId
        if (currentSongId == null) {
            onError("Không có bài hát nào đang phát")
            return
        }
        viewModelScope.launch {
            songRepository.addSongToPlaylist(playlistId, currentSongId).onSuccess {
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Lỗi khi thêm bài hát")
            }
        }
    }

    fun loadSongsInPlaylist(playlistId: String) {
        viewModelScope.launch {
            songRepository.getSongsInPlaylist(playlistId).onSuccess { songs ->
                _playlistSongs.value = songs
            }.onFailure {
                _playlistSongs.value = emptyList()
            }
        }
    }

    fun loadComments(songId: String) {
        viewModelScope.launch {
            songRepository.getComments(songId).collect { comments ->
                _songState.value = _songState.value.copy(comments = comments)
            }
        }
    }

    fun addComment(songId: String, userId: String, userName: String, userAvatar: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            songRepository.addComment(songId, userId, userName, userAvatar, content)
        }
    }
}