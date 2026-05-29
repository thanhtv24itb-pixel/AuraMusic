package com.example.auramusic.data.repository

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.auramusic.domain.model.Category
import com.example.auramusic.domain.model.Song
import com.example.auramusic.domain.model.Comment
import com.example.auramusic.domain.repository.SongRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.example.auramusic.domain.model.Playlist
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SongRepositoryImpl(
    private val firestore: FirebaseFirestore
) : SongRepository {

    override suspend fun getMostPlayedSongs(limit: Int): Result<List<Song>> = try {
        val snapshot = firestore.collection("songs")
            .orderBy("playCount", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        Result.success(snapshot.toObjects(Song::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getRecentSongs(limit: Int): Result<List<Song>> = try {
        val snapshot = firestore.collection("songs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        Result.success(snapshot.toObjects(Song::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> = try {
        val snapshot = firestore.collection("songs")
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", query + "\uf8ff")
            .get()
            .await()
        Result.success(snapshot.toObjects(Song::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCategories(): Result<List<Category>> = try {
        val snapshot = firestore.collection("categories").get().await()
        Result.success(snapshot.toObjects(Category::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSongsByCategory(genre: String): Result<List<Song>> = try {
        val snapshot = firestore.collection("songs")
            .whereEqualTo("genre", genre)
            .get()
            .await()
        Result.success(snapshot.toObjects(Song::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSongById(songId: String): Result<Song> = try {
        val snapshot = firestore.collection("songs").document(songId).get().await()
        val song = snapshot.toObject(Song::class.java)
        if (song != null) Result.success(song) else Result.failure(Exception("Không tìm thấy bài hát"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun incrementPlayCount(songId: String): Result<Unit> = try {
        val songRef = firestore.collection("songs").document(songId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(songRef)
            val currentPlays = snapshot.getLong("playCount") ?: 0
            transaction.update(songRef, "playCount", currentPlays + 1)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ====================================================================
    // PHẦN MỚI THÊM: CLOUDINARY UPLOAD LOGIC
    // ====================================================================

    // Hàm hỗ trợ upload lên Cloudinary (ẩn đi, chỉ dùng nội bộ trong class này)
    private suspend fun uploadToCloudinary(uri: Uri, presetName: String): String = suspendCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned(presetName)
            .option("resource_type", "auto")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // Lấy link an toàn (https) trả về từ Cloudinary
                    val url = resultData["secure_url"] as String
                    continuation.resume(url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(Exception(error.description))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    // Ghi đè lại hàm uploadSong: Đẩy file lên Cloudinary trước, lấy URL rồi mới lưu Firestore
    override suspend fun uploadSong(
        title: String,
        artistId: String,
        artistName: String,
        audioUri: Uri,
        imageUri: Uri?,
        category: String,
        duration: Int
    ): Result<Unit> = try {
        // 1. Tải file MP3 lên Cloudinary với preset "auraumusic"
        val audioUrl = uploadToCloudinary(audioUri, "auramusic")

        // 2. Tải file Ảnh lên Cloudinary với preset "ml_default" (Nếu có chọn ảnh)
        val imageUrl = if (imageUri != null) {
            uploadToCloudinary(imageUri, "ml_default")
        } else {
            "" // Không có ảnh bìa thì để chuỗi rỗng
        }

        // 3. Tạo ID mới cho bài hát trên Firestore
        val docRef = firestore.collection("songs").document()
        val songId = docRef.id

        // 4. Tạo Object Song với URL vừa lấy được từ Cloudinary
        val newSong = Song(
            songId = songId,
            title = title,
            artistId = artistId,
            artistName = artistName,
            audioUrl = audioUrl,      // Link Cloudinary MP3
            imageUrl = imageUrl,      // Link Cloudinary Ảnh
            genre = category,         // Dùng thuộc tính genre theo code của bạn
            duration = duration,
            playCount = 0,
            createdAt = System.currentTimeMillis()
        )

        // 5. Lưu Object Song lên Firestore
        docRef.set(newSong).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    // --- PHẦN MỚI THÊM: LOGIC THÍCH BÀI HÁT ---
    override suspend fun checkIsLiked(userId: String, songId: String): Result<Boolean> = try {
        val likeId = "${userId}_${songId}"
        val snapshot = firestore.collection("likes").document(likeId).get().await()
        Result.success(snapshot.exists()) // Nếu tồn tại document thì là đã like
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun toggleLikeSong(userId: String, songId: String): Result<Boolean> = try {
        val likeId = "${userId}_${songId}"
        val likeRef = firestore.collection("likes").document(likeId)
        val songRef = firestore.collection("songs").document(songId)

        val snapshot = likeRef.get().await()
        var isCurrentlyLiked = false

        firestore.runTransaction { transaction ->
            val songSnapshot = transaction.get(songRef)
            val currentLikes = songSnapshot.getLong("likeCount") ?: 0

            if (snapshot.exists()) {
                // Đã like rồi -> Hủy like (Xóa dữ liệu và trừ điểm)
                transaction.delete(likeRef)
                transaction.update(songRef, "likeCount", currentLikes - 1)
                isCurrentlyLiked = false
            } else {
                // Chưa like -> Bấm like (Lưu dữ liệu mới và cộng điểm)
                // Nhớ import com.example.auramusic.domain.model.Like và com.google.firebase.Timestamp
                val newLike = com.example.auramusic.domain.model.Like(
                    likeId = likeId,
                    userId = userId,
                    songId = songId,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(likeRef, newLike)
                transaction.update(songRef, "likeCount", currentLikes + 1)
                isCurrentlyLiked = true
            }
        }.await()

        Result.success(isCurrentlyLiked)
    } catch (e: Exception) {
        Result.failure(e)
    }
    // --- TÍNH NĂNG BÀI HÁT YÊU THÍCH ---
    override suspend fun getFavoriteSongs(userId: String): Result<List<Song>> = try {
        // 1. Vào bảng "likes" tìm tất cả những document do bạn (userId) đã thả tim
        val likesSnapshot = firestore.collection("likes")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Lấy ra một danh sách chứa toàn mã songId
        val songIds = likesSnapshot.documents.mapNotNull { it.getString("songId") }

        if (songIds.isEmpty()) {
            Result.success(emptyList()) // Nếu chưa like bài nào thì trả về mảng rỗng
        } else {
            // 2. Chạy vòng lặp lấy thông tin chi tiết của từng bài hát bên bảng "songs"
            val favoriteSongs = mutableListOf<Song>()
            for (id in songIds) {
                val songSnapshot = firestore.collection("songs").document(id).get().await()
                val song = songSnapshot.toObject(Song::class.java)
                if (song != null) {
                    favoriteSongs.add(song)
                }
            }
            Result.success(favoriteSongs)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    // ==========================================
    // TÍNH NĂNG PLAYLIST
    // ==========================================

    override suspend fun createPlaylist(userId: String, name: String): Result<Boolean> = try {
        // Tạo một ID ngẫu nhiên cho Playlist mới
        val docRef = firestore.collection("playlists").document()
        val playlist = Playlist(
            playlistId = docRef.id,
            name = name,
            userId = userId,
            songIds = emptyList() // Mới tạo nên chưa có bài hát nào
        )
        docRef.set(playlist).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserPlaylists(userId: String): Result<List<Playlist>> = try {
        // Tìm tất cả playlist do user này tạo
        val snapshot = firestore.collection("playlists")
            .whereEqualTo("userId", userId)
            .get()
            .await()
        val playlists = snapshot.toObjects(Playlist::class.java)
        Result.success(playlists)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Boolean> = try {
        // Dùng lệnh arrayUnion để nhét ID bài hát vào mảng songIds mà không làm đè bài cũ
        firestore.collection("playlists").document(playlistId)
            .update("songIds", FieldValue.arrayUnion(songId))
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSongsInPlaylist(playlistId: String): Result<List<Song>> = try {
        // Bước 1: Lấy thông tin Playlist để xem mảng songIds có những bài nào
        val playlistSnapshot = firestore.collection("playlists").document(playlistId).get().await()
        val playlist = playlistSnapshot.toObject(Playlist::class.java)

        val songIds = playlist?.songIds ?: emptyList()

        if (songIds.isEmpty()) {
            Result.success(emptyList())
        } else {
            // Bước 2: Dùng vòng lặp lấy chi tiết từng bài hát ra
            val songs = mutableListOf<Song>()
            for (id in songIds) {
                val songSnap = firestore.collection("songs").document(id).get().await()
                val song = songSnap.toObject(Song::class.java)
                if (song != null) songs.add(song)
            }
            Result.success(songs)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Comments Implementation
    override suspend fun addComment(
        songId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        content: String
    ): Result<Unit> = try {
        val docRef = firestore.collection("songs").document(songId).collection("comments").document()
        val comment = Comment(
            id = docRef.id,
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        docRef.set(comment).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getComments(songId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = firestore.collection("songs").document(songId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                    trySend(comments)
                }
            }
        awaitClose { subscription.remove() }
    }
}
