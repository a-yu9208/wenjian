package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.HomeSubTab
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.OrderDishItem
import com.example.yueyeushaokaojiaoziguan.merchant.OrderItem

@Composable
fun HomeWorkbenchScreen(
    uiState: MerchantUiState,
    onAdvanceOrder: (String, String) -> Unit,
    onToggleDishServed: (String, String, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subTabName by rememberSaveable { mutableStateOf(HomeSubTab.Pending.name) }
    val subTab = HomeSubTab.entries.find { it.name == subTabName } ?: HomeSubTab.Pending
    var expandedOrder by remember { mutableStateOf<String?>(null) }

    val filteredOrders = remember(subTab, uiState.orders) {
        when (subTab) {
            HomeSubTab.Pending -> uiState.orders.filter { it.status == "待处理" }
            HomeSubTab.QuickServe -> uiState.orders.filter {
                it.status in listOf("待处理", "制作中") && it.dishes.any { d -> d.quickServe && !d.served }
            }
            HomeSubTab.Cooking -> uiState.orders.filter { it.status == "制作中" }
            HomeSubTab.AwaitingPayment -> {
                // 按桌号合并同桌订单
                val raw = uiState.orders.filter { it.status == "待结账" }
                raw.groupBy { it.tableLabel }.map { (_, group) ->
                    if (group.size == 1) group.first()
                    else {
                        val allDishes = group.flatMap { it.dishes }
                        val totalAmount = group.sumOf {
                            it.amount.replace("¥", "").replace(",", "").toDoubleOrNull() ?: 0.0
                        }
                        val first = group.first()
                        first.copy(
                            summary = group.joinToString("、") { it.summary },
                            amount = "¥%.2f".format(totalAmount),
                            dishes = allDishes,
                            isAppend = false
                        )
                    }
                }
            }
        }
    }

    val counts = remember(uiState.orders) {
        mapOf(
            HomeSubTab.Pending to uiState.orders.count { it.status == "待处理" },
            HomeSubTab.QuickServe to uiState.orders.count {
                it.status in listOf("待处理", "制作中") && it.dishes.any { d -> d.quickServe && !d.served }
            },
            HomeSubTab.Cooking to uiState.orders.count { it.status == "制作中" },
            // 按桌号去重计数
            HomeSubTab.AwaitingPayment to uiState.orders.filter { it.status == "待结账" }.map { it.tableLabel }.distinct().size
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部子 Tab + 刷新
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScrollableTabRow(
                selectedTabIndex = HomeSubTab.entries.indexOf(subTab),
                edgePadding = 8.dp,
                divider = {},
                modifier = Modifier.weight(1f)
            ) {
                HomeSubTab.entries.forEach { tab ->
                    Tab(
                        selected = subTab == tab,
                        onClick = { subTabName = tab.name },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.label)
                                val count = counts[tab] ?: 0
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Badge { Text("$count") }
                                }
                            }
                        }
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Text("🔄", fontSize = 18.sp)
            }
        }

        // 订单列表
        if (uiState.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无订单", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders, key = { it.tableLabel + it.time }) { order ->
                    OrderWorkCard(
                        order = order,
                        subTab = subTab,
                        expanded = expandedOrder == (order.tableLabel + order.time),
                        onCardClick = {
                            expandedOrder = if (expandedOrder == (order.tableLabel + order.time)) null
                            else (order.tableLabel + order.time)
                        },
                        onStatusClick = { onAdvanceOrder(order.tableLabel, order.time) },
                        onToggleServed = { dishName -> onToggleDishServed(order.tableLabel, order.time, dishName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderWorkCard(
    order: OrderItem,
    subTab: HomeSubTab,
    expanded: Boolean,
    onCardClick: () -> Unit,
    onStatusClick: () -> Unit,
    onToggleServed: (String) -> Unit
) {
    val statusColor = when (order.status) {
        "待处理" -> Color(0xFFD32F2F)
        "制作中" -> Color(0xFFFF6B35)
        "待结账" -> Color(0xFFF9A825)
        else -> Color(0xFF2E7D32)
    }

    val displayLabel = if (order.isAppend) "${order.area}-${order.tableLabel}-追加${order.appendIndex}" else "${order.area}-${order.tableLabel}"
    val canSlash = subTab == HomeSubTab.Cooking || subTab == HomeSubTab.QuickServe

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(displayLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${order.time}  共${order.dishes.size}道菜  ${order.amount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                // 状态标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { onStatusClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(order.status, color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // 快速上菜提醒
            if (order.dishes.any { it.quickServe && !it.served }) {
                Spacer(Modifier.height(6.dp))
                Text("🔔 有可直接上菜的菜品", color = Color(0xFFFF6B35), fontSize = 12.sp)
            }

            // 展开菜品详情
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                val dishesToShow = when (subTab) {
                    HomeSubTab.QuickServe -> order.dishes.filter {
                        it.quickServe || it.subItems.any { s -> s.quickServe }
                    }
                    else -> order.dishes
                }

                dishesToShow.forEach { dish ->
                    if (dish.subItems.isNotEmpty()) {
                        // 套餐标题
                        val allServed = dish.subItems.all { it.served }
                        Text(
                            "📦 ${dish.name} x${dish.quantity}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (allServed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (allServed) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        // 套餐子菜品
                        dish.subItems.forEach { sub ->
                            DishRow(dish = sub, canSlash = canSlash, indent = true, onToggleServed = onToggleServed)
                        }
                    } else {
                        DishRow(dish = dish, canSlash = canSlash, indent = false, onToggleServed = onToggleServed)
                    }
                }
            }
        }
    }
}

@Composable
private fun DishRow(
    dish: OrderDishItem,
    canSlash: Boolean,
    indent: Boolean,
    onToggleServed: (String) -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 24.dp else 0.dp)
            .clickable(enabled = canSlash) {
                if (dish.served) showConfirm = true
                else onToggleServed(dish.name)
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${if (indent) "└ " else ""}${dish.name} x${dish.quantity}",
            textDecoration = if (dish.served) TextDecoration.LineThrough else TextDecoration.None,
            color = if (dish.served) Color.Gray else Color.Unspecified,
            fontSize = if (indent) 14.sp else 15.sp
        )
        if (dish.served) Text("已上", color = Color(0xFF2E7D32), fontSize = 13.sp)
        if (dish.quickServe && !dish.served) Text("可直接上", color = Color(0xFFFF6B35), fontSize = 13.sp)
    }
    if (dish.note.isNotBlank()) {
        Box(
            Modifier.fillMaxWidth().padding(start = if (indent) 24.dp else 0.dp)
                .clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF3E0))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) { Text("备注：${dish.note}", color = Color(0xFFE65100), fontSize = 13.sp) }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("取消已上菜") },
            text = { Text("确认取消「${dish.name}」的已上菜标记？") },
            confirmButton = { TextButton(onClick = { onToggleServed(dish.name); showConfirm = false }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } }
        )
    }
}
