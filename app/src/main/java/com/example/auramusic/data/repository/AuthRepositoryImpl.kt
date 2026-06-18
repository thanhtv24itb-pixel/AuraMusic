package com.example.auramusic.data.repository

import com.example.auramusic.domain.model.User
import com.example.auramusic.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore // Thêm thư viện này
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore // Bổ sung công cụ kết nối Firestore
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val currentUser = firebaseAuth.currentUser

        // Nếu chưa đăng nhập thì trả về null
        if (currentUser == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // Lấy thông tin TỪ FIRESTORE thay vì FirebaseAuth
        val subscription = firestore.collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                // Nếu tìm thấy tài khoản trên Firestore, ép kiểu và gửi lên màn hình
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // --- ĐOẠN CODE MỚI: TÌM ẢNH TỪ FIRESTORE ---
                val snapshot = firestore.collection("users").document(firebaseUser.uid).get().await()
                if (snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        return Result.success(user) // Trả về User xịn có chứa link ảnh Cloudinary
                    }
                }
                // -------------------------------------------

                // Nếu không tìm thấy trên Firestore thì mới dùng mặc định
                Result.success(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        avatarUrl = firebaseUser.photoUrl?.toString() ?: ""
                    )
                )
            } else {
                Result.failure(Exception("Không thể lấy thông tin người dùng"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ĐÃ SỬA HÀM NÀY ĐỂ LƯU VÀO FIRESTORE ---
    override suspend fun signup(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return try {
            // 1. Tạo tài khoản bên Firebase Authentication
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // Cập nhật tên hiển thị
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                // 2. Tạo đối tượng User với đầy đủ thông tin
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = displayName,
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = com.google.firebase.Timestamp.now()
                )

                // 3. LƯU ĐỐI TƯỢNG ĐÓ VÀO FIRESTORE (Tạo collection "users")
                firestore.collection("users").document(firebaseUser.uid).set(newUser).await()

                Result.success(newUser)
            } else {
                Result.failure(Exception("Không thể tạo tài khoản"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String, displayName: String): Result<User> {
        return try {
            // 1. Tạo credential từ Google ID token
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // 2. Đăng nhập bằng credential
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // 3. Kiểm tra xem user đã tồn tại trong Firestore chưa
                val snapshot = firestore.collection("users").document(firebaseUser.uid).get().await()

                if (snapshot.exists()) {
                    // User tồn tại, lấy thông tin từ Firestore
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        return Result.success(user)
                    }
                }

                // 4. Nếu user chưa tồn tại, tạo mới
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = displayName.ifEmpty { firebaseUser.displayName ?: "" },
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = com.google.firebase.Timestamp.now()
                )

                // 5. Lưu vào Firestore
                firestore.collection("users").document(firebaseUser.uid).set(newUser).await()

                Result.success(newUser)
            } else {
                Result.failure(Exception("Không thể tạo tài khoản từ Google"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}