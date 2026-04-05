package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.yueyeushaokaojiaoziguan.ui.theme.GradientSunset
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    val today = fmt.format(cal.time)

    var revenue by remember { mutableStateOf<Double?>(null) }
    var orderCount by remember { mutableStateOf(0) }
    var selectedLabel by remember { mutableStateOf("今日") }
    var customStart by remember { mutableStateOf("") }
    var customEnd by remember { mutableStateOf("") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    fun query(start: String, end: String) {
        vm.queryRevenue(start, end) { r, c -> revenue = r; orderCount = c }
    }

    LaunchedEffect(Unit) { query(today, today) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 营业额显示
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.shadow(6.dp, RoundedCornerShape(20.dp))
        ) {
            Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(GradientSunset)).clip(RoundedCornerShape(20.dp))) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("$selectedLabel 营业额", color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(8.dp))
                    if (revenue != null) {
                        val animRevenue by animateFloatAsState(revenue!!.toFloat(), tween(800), label = "rev")
                        Text("¥${"%.2f".format(animRevenue.toDouble())}", fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("共 $orderCount 单", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    } else {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    }
                }
            }
        }

        // 快捷时间段
        Text("快捷查询", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("今日" to 0, "近7天" to 7, "近30天" to 30, "近3个月" to 90).forEach { (label, days) ->
                FilterChip(
                    selected = selectedLabel == label,
                    onClick = {
                        selectedLabel = label
                        val c = Calendar.getInstance()
                        val end = fmt.format(c.time)
                        if (days > 0) c.add(Calendar.DAY_OF_YEAR, -days + 1)
                        query(fmt.format(c.time), end)
                    },
                    label = { Text(label, fontSize = 12.sp) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // 自定义日期范围
        Text("自定义查询", fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showStartPicker = true }, Modifier.weight(1f)) {
                Text(customStart.ifBlank { "开始日期" }, fontSize = 13.sp)
            }
            Text("至")
            OutlinedButton(onClick = { showEndPicker = true }, Modifier.weight(1f)) {
                Text(customEnd.ifBlank { "结束日期" }, fontSize = 13.sp)
            }
        }
        FilledTonalButton(
            onClick = {
                if (customStart.isNotBlank() && customEnd.isNotBlank()) {
                    selectedLabel = "$customStart ~ $customEnd"
                    query(customStart, customEnd)
                }
            },
            enabled = customStart.isNotBlank() && customEnd.isNotBlank()
        ) { Text("查询") }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { customStart = fmt.format(Date(it)) }
                    showStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { customEnd = fmt.format(Date(it)) }
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}
