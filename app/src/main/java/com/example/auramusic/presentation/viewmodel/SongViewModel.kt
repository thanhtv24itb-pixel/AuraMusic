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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SongUiState(
    val isLoading: Boolean = false,
    val mostPlayedSongs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val selectedCategorySongs: List<Song> = emptyList(),
    val artistSongs: List<Song> = emptyList(),
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

    private val _myPlaylists = MutableStateFlow<List<Playlist>>(emptyStringList()) // Lưu ý: Cần import nếu lỗi
    private fun emptyStringList(): List<Playlist> = emptyList() // Fix tạm nếu lỗi type

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs

    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs

    private var hasCountedViewAndHistory = false
    private var sleepTimerJob: Job? = null
    private var artistSongsJob: Job? = null
    var stopAfterCurrentSong = false
        private set

    val myPlaylists: StateFlow<List<Playlist>> = _myPlaylists

    init {
        observeHomeData()
        loadCategories()
    }

    // ĐÃ SỬA: Chuyển sang Observe (Theo dõi) thay vì chỉ Load 1 lần
    private fun observeHomeData() {
        viewModelScope.launch {
            _songState.value = _songState.value.copy(isLoading = true)

            // Theo dõi bài hát nghe nhiều nhất
            launch {
                getMostPlayedSongsUseCase().collectLatest { songs ->
                    _songState.value = _songState.value.copy(
                        mostPlayedSongs = songs,
                        isLoading = false
                    )
                }
            }

            // Theo dõi bài hát mới nhất
            launch {
                getRecentSongsUseCase().collectLatest { songs ->
                    _songState.value = _songState.value.copy(
                        recentSongs = songs,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadHomeData() {
        // Hàm này giữ lại để tương thích nhưng logic đã chuyển vào observeHomeData
        observeHomeData()
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

    fun loadSongsByArtist(artistId: String) {
        artistSongsJob?.cancel()
        artistSongsJob = viewModelScope.launch {
            songRepository.getSongsByArtist(artistId).collect { songs ->
                _songState.value = _songState.value.copy(artistSongs = songs)
            }
        }
    }

    fun playSong(song: Song) {
        _songState.value = _songState.value.copy(
            currentSong = song,
            isPlaying = true,
            currentPosition = 0
        )
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

        if (positionInSeconds >= 10 && !hasCountedViewAndHistory) {
            hasCountedViewAndHistory = true
            val currentSong = _songState.value.currentSong

            if (currentSong != null) {
                viewModelScope.launch {
                    songRepository.incrementPlayCount(currentSong.songId, currentSong.artistId)
                    if (currentUserId != null) {
                        songRepository.addToHistory(currentUserId, currentSong.songId)
                    }
                }
            }
        }
    }

    fun handleSongEnded() {
        hasCountedViewAndHistory = false
        if (stopAfterCurrentSong) {
            pauseSong()
            updateProgress(0, null)
            stopAfterCurrentSong = false
        } else {
            pauseSong()
            updateProgress(0, null)
        }
    }

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
        if (name.isNotBlank()) {
            viewModelScope.launch {
                songRepository.createPlaylist(userId, name).onSuccess {
                    loadMyPlaylists(userId)
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Lỗi khi tạo Playlist")
                }
            }
        }
    }

    fun addCurrentSongToPlaylist(playlistId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentSongId = _songState.value.currentSong?.songId
        if (currentSongId != null) {
            viewModelScope.launch {
                songRepository.addSongToPlaylist(playlistId, currentSongId).onSuccess {
                    onSuccess()
                }.onFailure { e ->
                    onError(e.message ?: "Lỗi khi thêm bài hát")
                }
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
        if (content.isNotBlank()) {
            viewModelScope.launch {
                songRepository.addComment(songId, userId, userName, userAvatar, content)
            }
        }
    }
}
