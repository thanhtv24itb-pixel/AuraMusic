package com.example.auramusic.domain.model

import com.google.firebase.Timestamp

data class Transaction(
    val transactionId: String = "",
    val sepayRefCode: String = "", // Mã giao dịch trả về từ SePay
    val userId: String = "",
    val amount: Long = 0,
    val status: String = "Pending", // "Pending", "Success", "Failed"
    val createdAt: Timestamp? = null
)