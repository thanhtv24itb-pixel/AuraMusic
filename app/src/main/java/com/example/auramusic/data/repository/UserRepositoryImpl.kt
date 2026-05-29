package com.example.auramusic.data.repository

import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUserData(uid: String): Flow<User?> = callbackFlow {
        val subscription = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateUserData(user: User): Result<Unit> = try {
        firestore.collection("users").document(user.uid).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTopArtists(limit: Int): Result<List<User>> = try {
        val snapshot = firestore.collection("users")
            .orderBy("totalPlays", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        Result.success(snapshot.toObjects(User::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

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

    // --- TÍNH NĂNG LIKE PROFILE ĐÃ ĐƯỢC ĐƯA VỀ ĐÚNG NHÀ ---
    override suspend fun checkIsProfileLiked(myUserId: String, artistId: String): Result<Boolean> = try {
        val likeId = "${myUserId}_${artistId}"
        val snapshot = firestore.collection("profile_likes").document(likeId).get().await()
        Result.success(snapshot.exists())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun toggleLikeProfile(myUserId: String, artistId: String): Result<Boolean> = try {
        val likeId = "${myUserId}_${artistId}"
        val likeRef = firestore.collection("profile_likes").document(likeId)
        val artistRef = firestore.collection("users").document(artistId)

        val snapshot = likeRef.get().await()
        var isCurrentlyLiked = false

        firestore.runTransaction { transaction ->
            val artistSnapshot = transaction.get(artistRef)
            val currentLikes = artistSnapshot.getLong("totalLikesReceived") ?: 0

            if (snapshot.exists()) {
                transaction.delete(likeRef)
                transaction.update(artistRef, "totalLikesReceived", currentLikes - 1)
                isCurrentlyLiked = false
            } else {
                val likeData = hashMapOf(
                    "likerId" to myUserId,
                    "artistId" to artistId,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                transaction.set(likeRef, likeData)
                transaction.update(artistRef, "totalLikesReceived", currentLikes + 1)
                isCurrentlyLiked = true
            }
        }.await()

        Result.success(isCurrentlyLiked)
    } catch (e: Exception) {
        Result.failure(e)
    }
}