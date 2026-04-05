package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState

@Composable
fun RevenueScreen(uiState: MerchantUiState) {
    val totalRevenue = uiState.orders.sumOf { order ->
        order.amount.replace("¥", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }
    val completedRevenue = uiState.orders.filter { it.status == "已完成" }.sumOf { order ->
        order.amount.replace("¥", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("今日营业额", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text("¥${"%.0f".format(totalRevenue)}", fontWeight = FontWeight.Bold, fontSize = 36.sp)
            }
        }
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("已结账金额", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("¥${"%.0f".format(completedRevenue)}", fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
            }
        }
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("今日订单数", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${uiState.orders.size}", fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
            }
        }
    }
}
