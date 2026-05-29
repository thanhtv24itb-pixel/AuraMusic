package com.example.auramusic.domain.model

import com.google.firebase.Timestamp

data class ProfileLike(
    val likeId: String = "",       // Mã ID của lượt thích (Ví dụ: myUid_artistUid)
    val likerId: String = "",      // Mã UID của người đi bấm like (Là bạn)
    val artistId: String = "",     // Mã UID của tác giả được like
    val timestamp: Timestamp? = null // Thời gian bấm like
)