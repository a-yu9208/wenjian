package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState

@Composable
fun OrderHistoryScreen(
    uiState: MerchantUiState,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf("全部") }
    var keyword by rememberSaveable { mutableStateOf("") }
    val tabs = listOf("全部", "进行中", "已完成")

    val filtered = remember(selectedTab, keyword, uiState.orders) {
        uiState.orders.filter { order ->
            val tabMatch = when (selectedTab) {
                "进行中" -> order.status in listOf("待处理", "制作中", "待结账")
                "已完成" -> order.status == "已完成"
                else -> true
            }
            val kwMatch = keyword.isBlank() ||
                order.tableLabel.contains(keyword, true) ||
                order.summary.contains(keyword, true)
            tabMatch && kwMatch
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
            tabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab) }
                )
            }
        }

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            label = { Text("搜索桌号或菜品") },
            singleLine = true
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无订单记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { order ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(order.tableLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(order.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(order.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(order.time, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.amount, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
