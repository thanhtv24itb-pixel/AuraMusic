package com.example.auramusic.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.domain.model.Transaction
import com.example.auramusic.presentation.viewmodel.AdminViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminTransactionScreen(viewModel: AdminViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadAllTransactions()
    }

    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(viewModel.allTransactions) { transaction ->
            TransactionItem(transaction = transaction, amountFormatted = formatter.format(transaction.amount))
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, amountFormatted: String) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = transaction.createdAt?.toDate()?.let { sdf.format(it) } ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Ref: ${transaction.sepayRefCode}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "User: ${transaction.userId}", fontSize = 12.sp, color = Color.Gray)
                Text(text = dateStr, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+ $amountFormatted VNĐ",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (transaction.status == "Success") Color(0xFF10B981) else Color.Gray,
                    fontSize = 16.sp
                )
                Surface(
                    color = when(transaction.status) {
                        "Success" -> Color(0xFF10B981).copy(alpha = 0.1f)
                        "Pending" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        else -> Color.Red.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = transaction.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when(transaction.status) {
                            "Success" -> Color(0xFF10B981)
                            "Pending" -> Color(0xFFF59E0B)
                            else -> Color.Red
                        }
                    )
                }
            }
        }
    }
}
