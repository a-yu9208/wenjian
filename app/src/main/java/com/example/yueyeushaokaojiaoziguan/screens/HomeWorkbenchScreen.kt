package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun HomeWorkbenchScreen(
    uiState: MerchantUiState,
    onAdvanceOrder: (String, String) -> Unit,
    onToggleDishServed: (String, String, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subTabs = HomeSubTab.entries
    val pagerState = rememberPagerState(pageCount = { subTabs.size })
    val scope = rememberCoroutineScope()
    var expandedOrder by remember { mutableStateOf<String?>(null) }

    val counts = remember(uiState.orders) {
        mapOf(
            HomeSubTab.Pending to uiState.orders.count { it.status == "待处理" },
            HomeSubTab.QuickServe to uiState.orders.count { it.status in listOf("待处理", "制作中") && it.dishes.any { d -> d.quickServe && !d.served } },
            HomeSubTab.Cooking to uiState.orders.count { it.status == "制作中" },
            HomeSubTab.AwaitingPayment to uiState.orders.filter { it.status == "待结账" }.map { it.tableLabel }.distinct().size
        )
    }

    Column(modifier.fillMaxSize()) {
        // 顶部 Tab 栏
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 8.dp,
                divider = {},
                modifier = Modifier.weight(1f)
            ) {
                subTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.label, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal)
                                val count = counts[tab] ?: 0
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        Modifier.size(20.dp).clip(CircleShape)
                                            .background(Brush.horizontalGradient(GradientOrange)),
                                        contentAlignment = Alignment.Center
                                    ) { Text("$count", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    )
                }
            }
            // 刷新按钮带旋转动画
            val refreshing = uiState.refreshing
            val rotation by animateFloatAsState(
                if (refreshing) 360f else 0f,
                animationSpec = if (refreshing) infiniteRepeatable(tween(800), RepeatMode.Restart) else tween(0),
                label = "rot"
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "刷新", Modifier.graphicsLayer { rotationZ = rotation }, tint = MaterialTheme.colorScheme.primary)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val pageTab = subTabs[page]
            val pageOrders = remember(pageTab, uiState.orders) {
                when (pageTab) {
                    HomeSubTab.Pending -> uiState.orders.filter { it.status == "待处理" }
                    HomeSubTab.QuickServe -> uiState.orders.filter { it.status in listOf("待处理", "制作中") && it.dishes.any { d -> d.quickServe && !d.served } }
                    HomeSubTab.Cooking -> uiState.orders.filter { it.status == "制作中" }
                    HomeSubTab.AwaitingPayment -> {
                        val raw = uiState.orders.filter { it.status == "待结账" }
                        raw.groupBy { it.tableLabel }.map { (_, group) ->
                            if (group.size == 1) group.first()
                            else {
                                val allDishes = group.flatMap { it.dishes }
                                val total = group.sumOf { it.amount.replace("¥", "").replace(",", "").toDoubleOrNull() ?: 0.0 }
                                group.first().copy(summary = group.joinToString("、") { it.summary }, amount = "¥%.2f".format(total), dishes = allDishes, isAppend = false)
                            }
                        }
                    }
                }
            }

            if (uiState.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            } else if (pageOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("暂无订单", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pageOrders, key = { _, it -> "${it.id}_${it.tableLabel}_${it.time}" }) { index, order ->
                        // 滑入动画
                        val visible = remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible.value = true }
                        AnimatedVisibility(
                            visible.value,
                            enter = slideInHorizontally(tween(300, delayMillis = index * 50)) { it / 3 } + fadeIn(tween(300, delayMillis = index * 50))
                        ) {
                            OrderWorkCard(order, pageTab, expandedOrder == (order.tableLabel + order.time),
                                { expandedOrder = if (expandedOrder == (order.tableLabel + order.time)) null else (order.tableLabel + order.time) },
                                { onAdvanceOrder(order.tableLabel, order.time) },
                                { dishName -> onToggleDishServed(order.tableLabel, order.time, dishName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderWorkCard(
    order: OrderItem, subTab: HomeSubTab, expanded: Boolean,
    onCardClick: () -> Unit, onStatusClick: () -> Unit, onToggleServed: (String) -> Unit
) {
    val statusColor = when (order.status) {
        "待处理" -> StatusRed; "制作中" -> BrandOrange; "待结账" -> StatusYellow; else -> StatusGreen
    }
    val statusGradient = when (order.status) {
        "待处理" -> listOf(Color(0xFFFF5252), Color(0xFFFF8A80))
        "制作中" -> GradientOrange
        "待结账" -> listOf(Color(0xFFFFB74D), Color(0xFFFFE082))
        else -> listOf(Color(0xFF66BB6A), Color(0xFFA5D6A7))
    }
    val displayLabel = if (order.isAppend) "${order.area}-${order.tableLabel}-追加${order.appendIndex}" else "${order.area}-${order.tableLabel}"
    val canSlash = subTab == HomeSubTab.Cooking || subTab == HomeSubTab.QuickServe

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)).clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(displayLabel, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${order.time}  共${order.dishes.size}道菜  ${order.amount}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                // 渐变状态胶囊
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(statusGradient))
                        .clickable { onStatusClick() }.padding(horizontal = 16.dp, vertical = 7.dp)
                ) { Text(order.status, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            }

            // 快速上菜提醒
            AnimatedVisibility(order.dishes.any { it.quickServe && !it.served }, enter = expandVertically() + fadeIn()) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("有可直接上菜的菜品", color = BrandOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // 展开菜品详情
            AnimatedVisibility(expanded, enter = expandVertically(tween(300)) + fadeIn(tween(300)), exit = shrinkVertically(tween(200)) + fadeOut(tween(200))) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    val dishesToShow = when (subTab) {
                        HomeSubTab.QuickServe -> order.dishes.filter { it.quickServe || it.subItems.any { s -> s.quickServe } }
                        else -> order.dishes
                    }
                    dishesToShow.forEach { dish ->
                        if (dish.subItems.isNotEmpty()) {
                            val allServed = dish.subItems.all { it.served }
                            Text("📦 ${dish.name} x${dish.quantity}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                color = if (allServed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (allServed) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.padding(top = 6.dp))
                            dish.subItems.forEach { sub -> DishRow(sub, canSlash, true, onToggleServed) }
                        } else {
                            DishRow(dish, canSlash, false, onToggleServed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DishRow(dish: OrderDishItem, canSlash: Boolean, indent: Boolean, onToggleServed: (String) -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = if (indent) 24.dp else 0.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = canSlash) { if (dish.served) showConfirm = true else onToggleServed(dish.name) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${if (indent) "└ " else ""}${dish.name} x${dish.quantity}",
            textDecoration = if (dish.served) TextDecoration.LineThrough else TextDecoration.None,
            color = if (dish.served) Color.Gray else Color.Unspecified, fontSize = if (indent) 14.sp else 15.sp)
        if (dish.served) Text("✓ 已上", color = StatusGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (dish.quickServe && !dish.served) Text("⚡ 可直接上", color = BrandOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
    if (dish.note.isNotBlank()) {
        Box(Modifier.fillMaxWidth().padding(start = if (indent) 24.dp else 0.dp)
            .clip(RoundedCornerShape(8.dp)).background(SoftCream).padding(horizontal = 10.dp, vertical = 4.dp)
        ) { Text("备注：${dish.note}", color = Color(0xFFE65100), fontSize = 13.sp) }
    }
    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false }, title = { Text("取消已上菜") },
            text = { Text("确认取消「${dish.name}」的已上菜标记？") },
            confirmButton = { TextButton(onClick = { onToggleServed(dish.name); showConfirm = false }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } })
    }
}
