package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.*
import com.example.yueyeushaokaojiaoziguan.ui.theme.*
import kotlinx.coroutines.launch

// ========== Staff workbench: 3 columns (待处理/制作中/待结账), no "已完成" ==========

private enum class StaffSubTab(val label: String) { Pending("待处理"), Cooking("制作中"), AwaitingPayment("待结账") }

@Composable
fun StaffWorkbenchScreen(uiState: MerchantUiState, vm: MerchantViewModel, modifier: Modifier = Modifier) {
    val subTabs = StaffSubTab.entries
    val pagerState = rememberPagerState(pageCount = { subTabs.size })
    val scope = rememberCoroutineScope()
    var expandedOrder by remember { mutableStateOf<Int?>(null) }

    val counts = remember(uiState.orders) {
        mapOf(
            StaffSubTab.Pending to uiState.orders.count { it.status == "待处理" },
            StaffSubTab.Cooking to uiState.orders.count { it.status == "制作中" },
            StaffSubTab.AwaitingPayment to uiState.orders.filter { it.status == "待结账" }.map { it.tableLabel }.distinct().size
        )
    }

    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 8.dp, divider = {}, modifier = Modifier.weight(1f)) {
                subTabs.forEachIndexed { index, tab ->
                    Tab(selected = pagerState.currentPage == index, onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.label, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal)
                                val count = counts[tab] ?: 0
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(Modifier.size(20.dp).clip(CircleShape).background(Brush.horizontalGradient(GradientOrange)), contentAlignment = Alignment.Center) {
                                        Text("$count", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        })
                }
            }
            val refreshing = uiState.refreshing
            val rotation by animateFloatAsState(if (refreshing) 360f else 0f, animationSpec = if (refreshing) infiniteRepeatable(tween(800), RepeatMode.Restart) else tween(0), label = "rot")
            IconButton(onClick = { vm.refreshMerchantData() }) {
                Icon(Icons.Default.Refresh, "刷新", Modifier.graphicsLayer { rotationZ = rotation }, tint = MaterialTheme.colorScheme.primary)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val pageTab = subTabs[page]
            val pageOrders = remember(pageTab, uiState.orders) {
                when (pageTab) {
                    StaffSubTab.Pending -> uiState.orders.filter { it.status == "待处理" }
                    StaffSubTab.Cooking -> uiState.orders.filter { it.status == "制作中" }
                    StaffSubTab.AwaitingPayment -> {
                        val raw = uiState.orders.filter { it.status == "待结账" }
                        raw.groupBy { it.tableLabel }.map { (_, group) ->
                            if (group.size == 1) group.first()
                            else {
                                val allDishes = group.flatMap { it.dishes }
                                val total = group.sumOf { it.amount.replace("¥", "").replace(",", "").toDoubleOrNull() ?: 0.0 }
                                val origTotal = group.sumOf { it.originalAmount.replace("¥", "").replace(",", "").toDoubleOrNull() ?: 0.0 }
                                val deductTotal = origTotal - total
                                group.first().copy(
                                    summary = group.joinToString("、") { it.summary },
                                    amount = "¥%.2f".format(total),
                                    originalAmount = "¥%.2f".format(origTotal),
                                    pointsDeduct = if (deductTotal > 0.01) "¥%.2f".format(deductTotal) else "",
                                    dishes = allDishes, isAppend = false
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (pageOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp); Spacer(Modifier.height(8.dp))
                        Text("暂无订单", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(pageOrders, key = { _, it -> "${it.id}_${it.tableLabel}_${it.time}" }) { _, order ->
                        StaffOrderCard(order, expandedOrder == order.id,
                            { expandedOrder = if (expandedOrder == order.id) null else order.id },
                            { vm.advanceOrderStatus(order.id) },
                            { dishName -> vm.toggleDishServed(order.id, dishName) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffOrderCard(order: OrderItem, expanded: Boolean, onCardClick: () -> Unit, onStatusClick: () -> Unit, onToggleServed: (String) -> Unit) {
    val statusGradient = when (order.status) {
        "待处理" -> listOf(Color(0xFFFF5252), Color(0xFFFF8A80))
        "制作中" -> GradientOrange
        "待结账" -> listOf(Color(0xFFFFB74D), Color(0xFFFFE082))
        else -> listOf(Color(0xFF66BB6A), Color(0xFFA5D6A7))
    }
    val displayLabel = if (order.isAppend) "${order.tableLabel}(追加${order.appendIndex})" else order.tableLabel

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)).clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(displayLabel, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${order.time}  共${order.dishes.size}道菜  ${order.amount}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(statusGradient)).clickable { onStatusClick() }.padding(horizontal = 16.dp, vertical = 7.dp)) {
                    Text(order.status, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            AnimatedVisibility(expanded, enter = expandVertically(tween(300)) + fadeIn(tween(300)), exit = shrinkVertically(tween(200)) + fadeOut(tween(200))) {
                Column {
                    Spacer(Modifier.height(10.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant); Spacer(Modifier.height(8.dp))
                    order.dishes.forEach { dish ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onToggleServed(dish.name) }.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${dish.name} x${dish.quantity}",
                                textDecoration = if (dish.served) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (dish.served) Color.Gray else Color.Unspecified, fontSize = 15.sp)
                            if (dish.served) Text("✓ 已上", color = StatusGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (dish.quickServe && !dish.served) Text("⚡ 可直接上", color = BrandOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ========== Staff info page: dishes (read-only + stock ±), tables (read-only), switch role ==========

@Composable
fun StaffInfoScreen(uiState: MerchantUiState, vm: MerchantViewModel, modifier: Modifier = Modifier, onSwitchRole: () -> Unit) {
    var infoTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("菜品列表", "桌台一览")

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = infoTab, divider = {}) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = infoTab == i, onClick = { infoTab = i },
                    text = { Text(title, fontWeight = if (infoTab == i) FontWeight.Bold else FontWeight.Normal) })
            }
        }
        when (infoTab) {
            0 -> StaffDishList(uiState, vm)
            1 -> StaffTableList(uiState)
        }
        // Switch role button at bottom
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onSwitchRole, modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) {
            Text("🔄 切换角色")
        }
    }
}

@Composable
private fun StaffDishList(uiState: MerchantUiState, vm: MerchantViewModel) {
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uiState.dishes) { dish ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(dish.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("${dish.category}  ${dish.price}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Stock ± buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.updateDishStock(dish.name, -1) }, modifier = Modifier.size(32.dp)) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${dish.stock}", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.widthIn(min = 28.dp), maxLines = 1)
                        IconButton(onClick = { vm.updateDishStock(dish.name, 1) }, modifier = Modifier.size(32.dp)) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffTableList(uiState: MerchantUiState) {
    val grouped = uiState.tables.groupBy { it.area }
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grouped.forEach { (area, tables) ->
            item { Text(area, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp)) }
            items(tables) { table ->
                val statusColor = when (table.status) { "空闲" -> StatusGreen; "使用中" -> BrandOrange; else -> StatusYellow }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(table.label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(table.status, color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
