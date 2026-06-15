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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.example.auramusic.domain.model.Playlist
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SongRepositoryImpl(
    private val firestore: FirebaseFirestore
) : SongRepository {

    override fun getMostPlayedSongs(limit: Int): Flow<List<Song>> = callbackFlow {
        val subscription = firestore.collection("songs")
            .whereEqualTo("status", "approved")
            .orderBy("playCount", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        android.util.Log.e("SongRepositoryImpl", "Index missing for getMostPlayedSongs. Please create it using the link in Logcat.")
                        trySend(emptyList())
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val songs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Song::class.java)?.apply { songId = doc.id }
                    }
                    trySend(songs)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getRecentSongs(limit: Int): Flow<List<Song>> = callbackFlow {
        val subscription = firestore.collection("songs")
            .whereEqualTo("status", "approved")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        android.util.Log.e("SongRepositoryImpl", "Index missing for getRecentSongs. Please create it using the link in Logcat.")
                        trySend(emptyList())
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val songs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Song::class.java)?.apply { songId = doc.id }
                    }
                    trySend(songs)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> = try {
        val snapshot = firestore.collection("songs")
            .whereEqualTo("status", "approved")
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", query + "\uf8ff")
            .get()
            .await()
        val songs = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Song::class.java)?.apply { songId = doc.id }
        }
        Result.success(songs)
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
            .whereEqualTo("status", "approved")
            .get()
            .await()
        val songs = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Song::class.java)?.apply { songId = doc.id }
        }
        Result.success(songs)
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

    // --- ĐÃ SỬA LỖI READ BEFORE WRITE: Tăng lượt nghe cho Bài hát & Ca sĩ ---
    override suspend fun incrementPlayCount(songId: String, artistId: String): Result<Unit> = try {
        android.util.Log.d("TEST_VIEW", "1. Bắt đầu gọi Transaction cho bài hát ID: $songId")
        val songRef = firestore.collection("songs").document(songId)
        val artistRef = if (artistId.isNotBlank()) firestore.collection("users").document(artistId) else null

        firestore.runTransaction { transaction ->
            // ==========================================
            // PHẦN 1: TẤT CẢ LỆNH ĐỌC (READ) NẰM TRƯỚC
            // ==========================================
            val songSnapshot = transaction.get(songRef)
            val artistSnapshot = if (artistRef != null) transaction.get(artistRef) else null

            // ==========================================
            // PHẦN 2: TẤT CẢ LỆNH SỬA/GHI (WRITE) NẰM SAU
            // ==========================================
            if (songSnapshot.exists()) {
                val currentPlays = songSnapshot.getLong("playCount") ?: 0
                transaction.update(songRef, "playCount", currentPlays + 1)
                android.util.Log.d("TEST_VIEW", "2. Đã update view bài hát lên: ${currentPlays + 1}")
            }

            if (artistRef != null) {
                val totalPlays = if (artistSnapshot?.exists() == true) artistSnapshot.getLong("totalPlays") ?: 0 else 0
                transaction.set(artistRef, hashMapOf("totalPlays" to totalPlays + 1), com.google.firebase.firestore.SetOptions.merge())
                android.util.Log.d("TEST_VIEW", "3. Đã update view cho ca sĩ: $artistId")
            }
        }.await()

        android.util.Log.d("TEST_VIEW", "4. Transaction hoàn tất thành công 100%!")
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("TEST_VIEW", "LỖI TRANSACTION BỊ SẬP: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }

    // --- Tính năng lưu lịch sử ---
    override suspend fun addToHistory(userId: String, songId: String): Result<Unit> = try {
        val historyRef = firestore.collection("users").document(userId)
            .collection("recentlyPlayed").document(songId)

        val data = hashMapOf("timestamp" to FieldValue.serverTimestamp())
        historyRef.set(data, SetOptions.merge()).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun uploadToCloudinary(uri: Uri, presetName: String): String = suspendCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned(presetName)
            .option("resource_type", "auto")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
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

    // --- ĐÃ THÊM: Gọi hàm incrementUploadedCount khi up thành công ---
    override suspend fun uploadSong(
        title: String,
        artistId: String,
        artistName: String,
        audioUri: Uri,
        imageUri: Uri?,
        category: String,
        duration: Int
    ): Result<Unit> = try {
        val audioUrl = uploadToCloudinary(audioUri, "auramusic")
        val imageUrl = if (imageUri != null) {
            uploadToCloudinary(imageUri, "ml_default")
        } else {
            ""
        }

        val docRef = firestore.collection("songs").document()
        val songId = docRef.id

        val newSong = Song(
            songId = songId,
            title = title,
            artistId = artistId,
            artistName = artistName,
            audioUrl = audioUrl,
            imageUrl = imageUrl,
            genre = category,
            duration = duration,
            createdAt = System.currentTimeMillis(),
        )

        docRef.set(newSong).await()

        // CỘNG 1 BÀI HÁT VÀO TÀI KHOẢN NGƯỜI DÙNG TẠI ĐÂY
        incrementUploadedCount(artistId)

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkIsLiked(userId: String, songId: String): Result<Boolean> = try {
        val likeId = "${userId}_${songId}"
        val snapshot = firestore.collection("likes").document(likeId).get().await()
        Result.success(snapshot.exists())
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
                transaction.delete(likeRef)
                transaction.update(songRef, "likeCount", currentLikes - 1)
                isCurrentlyLiked = false
            } else {
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

    override suspend fun getFavoriteSongs(userId: String): Result<List<Song>> = try {
        val likesSnapshot = firestore.collection("likes")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        val songIds = likesSnapshot.documents.mapNotNull { it.getString("songId") }

        if (songIds.isEmpty()) {
            Result.success(emptyList())
        } else {
            val favoriteSongs = mutableListOf<Song>()
            for (id in songIds) {
                val songSnapshot = firestore.collection("songs").document(id).get().await()
                val song = songSnapshot.toObject(Song::class.java)
                // CHỈ ADD VÀO DANH SÁCH NẾU BÀI HÁT ĐÓ ĐÃ ĐƯỢC DUYỆT
                if (song != null && song.status == "approved") {
                    favoriteSongs.add(song)
                }
            }
            Result.success(favoriteSongs)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createPlaylist(userId: String, name: String): Result<Boolean> = try {
        val docRef = firestore.collection("playlists").document()
        val playlist = Playlist(
            playlistId = docRef.id,
            name = name,
            userId = userId,
            songIds = emptyList()
        )
        docRef.set(playlist).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserPlaylists(userId: String): Result<List<Playlist>> = try {
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
        firestore.collection("playlists").document(playlistId)
            .update("songIds", FieldValue.arrayUnion(songId))
            .await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getSongsInPlaylist(playlistId: String): Result<List<Song>> = try {
        val playlistSnapshot = firestore.collection("playlists").document(playlistId).get().await()
        val playlist = playlistSnapshot.toObject(Playlist::class.java)

        val songIds = playlist?.songIds ?: emptyList()

        if (songIds.isEmpty()) {
            Result.success(emptyList())
        } else {
            val songs = mutableListOf<Song>()
            for (id in songIds) {
                val songSnap = firestore.collection("songs").document(id).get().await()
                val song = songSnap.toObject(Song::class.java)

                // ĐÃ SỬA: Bắt buộc bài hát tồn tại VÀ phải đang được duyệt (approved)
                if (song != null && song.status == "approved") {
                    songs.add(song)
                }
            }
            Result.success(songs)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

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

    override suspend fun getRecentlyPlayed(userId: String): Result<List<Song>> = try {
        val historySnapshot = firestore.collection("users").document(userId)
            .collection("recentlyPlayed")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        val songIds = historySnapshot.documents.map { it.id }

        if (songIds.isEmpty()) {
            Result.success(emptyList())
        } else {
            val songs = mutableListOf<Song>()
            for (id in songIds) {
                val songSnap = firestore.collection("songs").document(id).get().await()
                val song = songSnap.toObject(Song::class.java)

                // ĐÃ SỬA: Lịch sử cũng chỉ hiện những bài hát còn hợp lệ
                if (song != null && song.status == "approved") {
                    songs.add(song)
                }
            }
            Result.success(songs)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- ĐÃ THÊM: Hàm tăng số lượng bài hát đã upload của người dùng ---
    override suspend fun incrementUploadedCount(uid: String): Result<Unit> = try {
        val userRef = firestore.collection("users").document(uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentCount = snapshot.getLong("uploadedCount") ?: 0
            transaction.update(userRef, "uploadedCount", currentCount + 1)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getSongsByArtist(artistId: String): Flow<List<Song>> = callbackFlow {
        val subscription = firestore.collection("songs")
            .whereEqualTo("artistId", artistId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("SongRepositoryImpl", "Error getting songs by artist: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val songs = snapshot.documents.mapNotNull { doc ->
                        val song = doc.toObject(Song::class.java)
                        android.util.Log.d("SongRepositoryImpl", "Song: ${song?.title}, Status: ${doc.getString("status")}")
                        song?.apply { songId = doc.id }
                    }
                    trySend(songs)
                }
            }
        awaitClose { subscription.remove() }
    }
}