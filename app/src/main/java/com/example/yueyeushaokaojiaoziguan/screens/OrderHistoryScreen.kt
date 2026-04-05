package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import com.example.yueyeushaokaojiaoziguan.merchant.OrderItem
import com.example.yueyeushaokaojiaoziguan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(uiState: MerchantUiState, vm: MerchantViewModel, modifier: Modifier = Modifier) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var selectedTab by rememberSaveable { mutableStateOf("全部") }
    var keyword by rememberSaveable { mutableStateOf("") }
    var dateLabel by rememberSaveable { mutableStateOf("今日") }
    var dateOrders by remember { mutableStateOf<List<OrderItem>?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var customStart by remember { mutableStateOf("") }
    var customEnd by remember { mutableStateOf("") }
    val tabs = listOf("全部", "进行中", "已完成")
    val sourceOrders = dateOrders ?: uiState.orders

    val filtered = remember(selectedTab, keyword, sourceOrders) {
        sourceOrders.filter { order ->
            val tabMatch = when (selectedTab) {
                "进行中" -> order.status in listOf("待处理", "制作中", "待结账")
                "已完成" -> order.status == "已完成"
                else -> true
            }
            val kwMatch = keyword.isBlank() || order.tableLabel.contains(keyword, true) || order.summary.contains(keyword, true)
            tabMatch && kwMatch
        }
    }

    Column(modifier.fillMaxSize()) {
        // 日期快捷筛选
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("今日" to 0, "昨日" to 1, "近7天" to 7, "近30天" to 30).forEach { (label, days) ->
                FilterChip(
                    selected = dateLabel == label,
                    onClick = {
                        dateLabel = label
                        val c = Calendar.getInstance()
                        val end = fmt.format(c.time)
                        if (days == 1) { c.add(Calendar.DAY_OF_YEAR, -1); val d = fmt.format(c.time); vm.queryOrdersByDate(d, d) { dateOrders = it } }
                        else if (days > 1) { c.add(Calendar.DAY_OF_YEAR, -days + 1); vm.queryOrdersByDate(fmt.format(c.time), end) { dateOrders = it } }
                        else dateOrders = null
                    },
                    label = { Text(label, fontSize = 12.sp) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
            FilterChip(selected = dateLabel.contains("~"), onClick = { showStartPicker = true }, label = { Text("自定义", fontSize = 12.sp) }, shape = RoundedCornerShape(20.dp))
        }

        if (dateLabel.contains("~")) {
            Text("  $dateLabel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
        }

        // Tab 栏
        TabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            indicator = { tabPositions ->
                Box(Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]).height(3.dp).padding(horizontal = 24.dp).clip(RoundedCornerShape(1.5.dp)).background(Brush.horizontalGradient(GradientFire)))
            },
            divider = {}
        ) {
            tabs.forEach { tab ->
                Tab(selected = selectedTab == tab, onClick = { selectedTab = tab },
                    text = { Text(tab, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) })
            }
        }

        OutlinedTextField(
            value = keyword, onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            label = { Text("搜索桌号或菜品") }, singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无订单记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(filtered) { index, order ->
                    val visible = remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible.value = true }
                    AnimatedVisibility(visible.value, enter = slideInVertically(tween(300, delayMillis = index * 40)) { it / 4 } + fadeIn(tween(300, delayMillis = index * 40))) {
                        OrderHistoryCard(order)
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showStartPicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { customStart = fmt.format(Date(it)) }; showStartPicker = false; showEndPicker = true }) { Text("下一步") } },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
    if (showEndPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showEndPicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { customEnd = fmt.format(Date(it)) }; showEndPicker = false; if (customStart.isNotBlank() && customEnd.isNotBlank()) { dateLabel = "$customStart ~ $customEnd"; vm.queryOrdersByDate(customStart, customEnd) { dateOrders = it } } }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun OrderHistoryCard(order: OrderItem) {
    val statusColor = when (order.status) {
        "待处理" -> StatusRed; "制作中" -> BrandOrange; "待结账" -> StatusYellow; "已完成" -> StatusGreen; else -> MediumGray
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.shadow(3.dp, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.tableLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(statusColor.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(order.status, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(order.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.time, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order.amount, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
