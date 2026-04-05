package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import kotlinx.coroutines.launch

@Composable
fun PointsActivityScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    val tabs = listOf("查询积分", "增减积分", "积分规则")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage, divider = {}) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = pagerState.currentPage == i, onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = { Text(title, fontWeight = if (pagerState.currentPage == i) FontWeight.Bold else FontWeight.Normal) })
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> PointsQueryTab(vm)
                1 -> PointsAdjustTab(uiState, vm)
                2 -> PointsRuleTab(uiState, vm)
            }
        }
    }
}

@Composable
private fun PointsQueryTab(vm: MerchantViewModel) {
    var queryPhone by remember { mutableStateOf("") }
    var queryResult by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔍", fontSize = 32.sp); Spacer(Modifier.width(12.dp))
                Column { Text("查询积分", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("输入手机号查询用户积分", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        OutlinedTextField(value = queryPhone, onValueChange = { queryPhone = it.filter(Char::isDigit) },
            label = { Text("手机号") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        FilledTonalButton(onClick = { if (queryPhone.length == 11) vm.queryPoints(queryPhone) { queryResult = it } }, modifier = Modifier.fillMaxWidth()) { Text("查询") }
        queryResult?.let {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(12.dp)) {
                Text(it, fontSize = 15.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun PointsAdjustTab(uiState: MerchantUiState, vm: MerchantViewModel) {
    var target by remember { mutableStateOf("") }
    var deltaInput by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var isDeduct by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("桌号或手机号") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = deltaInput, onValueChange = { deltaInput = it.filter(Char::isDigit) }, label = { Text("积分数量") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                FilterChip(selected = !isDeduct, onClick = { isDeduct = false }, label = { Text("增加") }, shape = RoundedCornerShape(20.dp))
                FilterChip(selected = isDeduct, onClick = { isDeduct = true }, label = { Text("扣减") }, shape = RoundedCornerShape(20.dp))
            }
            OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("备注（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            FilledTonalButton(onClick = {
                val amt = deltaInput.toIntOrNull() ?: 0
                vm.addPointsLog(target, if (isDeduct) -amt else amt, reason)
                target = ""; deltaInput = ""; reason = ""
            }, modifier = Modifier.fillMaxWidth()) { Text("确认") }
        }

        // 积分流水
        if (uiState.pointsLogs.isNotEmpty()) {
            Text("积分流水", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.pointsLogs) { log ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text(log.target, fontWeight = FontWeight.SemiBold); Text("${log.reason}  ${log.time}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(if (log.delta > 0) "+${log.delta}" else "${log.delta}", fontWeight = FontWeight.Bold, color = if (log.delta > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsRuleTab(uiState: MerchantUiState, vm: MerchantViewModel) {
    var earnInput by remember { mutableStateOf(uiState.pointsConfig.earnRate.toString()) }
    var deductInput by remember { mutableStateOf(uiState.pointsConfig.deductRate.toString()) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚙️", fontSize = 32.sp); Spacer(Modifier.width(12.dp))
                Column { Text("积分规则", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("设置积分获取与抵扣比例", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        OutlinedTextField(value = earnInput, onValueChange = { earnInput = it.filter(Char::isDigit) }, label = { Text("消费1元得N积分") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = deductInput, onValueChange = { deductInput = it.filter(Char::isDigit) }, label = { Text("N积分抵扣1元") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        FilledTonalButton(onClick = { vm.updatePointsConfig(earnRate = earnInput.toIntOrNull() ?: 1, deductRate = deductInput.toIntOrNull() ?: 10) }, modifier = Modifier.fillMaxWidth()) { Text("保存规则") }

        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("当前规则", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("消费 1 元得 ${uiState.pointsConfig.earnRate} 积分", fontSize = 14.sp)
                Text("${uiState.pointsConfig.deductRate} 积分抵扣 1 元", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ProfileScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    var shopName by remember { mutableStateOf(uiState.profile.shopName) }
    var h5Url by remember { mutableStateOf(uiState.profile.h5BaseUrl) }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👤", fontSize = 48.sp); Spacer(Modifier.height(8.dp))
                    Text(uiState.profile.account, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(uiState.profile.mode, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("店铺设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("店铺名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = h5Url, onValueChange = { h5Url = it }, label = { Text("H5 点餐地址") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    FilledTonalButton(onClick = { vm.updateProfile(shopName = shopName.trim(), h5BaseUrl = h5Url.trim()) }) { Text("保存") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("积分规则概览", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("消费 1 元得 ${uiState.pointsConfig.earnRate} 积分", fontSize = 14.sp)
                    Text("${uiState.pointsConfig.deductRate} 积分抵扣 1 元", fontSize = 14.sp)
                }
            }
        }
    }
}
