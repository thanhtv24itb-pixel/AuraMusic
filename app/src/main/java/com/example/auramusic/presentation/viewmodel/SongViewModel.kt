package com.example.auramusic.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.model.Playlist
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.repository.SongRepository
import com.example.auramusic.domain.usecase.*
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
)

class SongViewModel(
    private val getMostPlayedSongsUseCase: GetMostPlayedSongsUseCase,
    private val getRecentSongsUseCase: GetRecentSongsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val searchSongsUseCase: SearchSongsUseCase,
    private val getSongsByCategoryUseCase: GetSongsByCategoryUseCase,
    private val uploadSongUseCase: UploadSongUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val songRepository: SongRepository // Đã xóa biến _favoriteSongs khỏi đây
) : ViewModel() {

    private val _songState = MutableStateFlow(SongUiState())
    val songState: StateFlow<SongUiState> = _songState

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    // --- ĐÃ SỬA: Đưa _favoriteSongs vào trong thân class ---
    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs
    private val _myPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val myPlaylists: StateFlow<List<Playlist>> = _myPlaylists

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs

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

    fun playSong(song: Song) {
        _songState.value = _songState.value.copy(
            currentSong = song,
            isPlaying = true,
            currentPosition = 0
        )
    }

    fun pauseSong() {
        _songState.value = _songState.value.copy(isPlaying = false)
    }

    fun resumeSong() {
        _songState.value = _songState.value.copy(isPlaying = true)
    }

    fun updateProgress(position: Int) {
        _songState.value = _songState.value.copy(currentPosition = position)
    }

    fun uploadSong(
        title: String,
        artistId: String,
        artistName: String,
        audioUri: Uri,
        imageUri: Uri?,
        category: String,
        duration: Int = 0,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true

            val result = uploadSongUseCase(
                title = title,
                artistId = artistId,
                artistName = artistName,
                audioUri = audioUri,
                imageUri = imageUri,
                category = category,
                duration = duration
            )

            result.onSuccess {
                _isUploading.value = false
                onSuccess()
            }.onFailure { error ->
                _isUploading.value = false
                onError(error.message ?: "Có lỗi xảy ra khi tải bài hát")
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
    // Load tất cả Playlist của mình về máy
    fun loadMyPlaylists(userId: String) {
        viewModelScope.launch {
            songRepository.getUserPlaylists(userId).onSuccess { playlists ->
                _myPlaylists.value = playlists
            }.onFailure {
                _myPlaylists.value = emptyList()
            }
        }
    }

    // Tạo Playlist mới
    fun createPlaylist(userId: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (name.isBlank()) {
            onError("Tên playlist không được để trống")
            return
        }
        viewModelScope.launch {
            songRepository.createPlaylist(userId, name).onSuccess {
                // Tạo xong thì load lại danh sách để màn hình tự cập nhật
                loadMyPlaylists(userId)
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Lỗi tạo Playlist")
            }
        }
    }

    // Thêm bài hát đang nghe vào Playlist
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
                onError(e.message ?: "Lỗi thêm bài hát")
            }
        }
    }

    // Load các bài hát nằm trong 1 Playlist khi người dùng bấm vào xem
    fun loadSongsInPlaylist(playlistId: String) {
        viewModelScope.launch {
            songRepository.getSongsInPlaylist(playlistId).onSuccess { songs ->
                _playlistSongs.value = songs
            }.onFailure {
                _playlistSongs.value = emptyList()
            }
        }
    }
}