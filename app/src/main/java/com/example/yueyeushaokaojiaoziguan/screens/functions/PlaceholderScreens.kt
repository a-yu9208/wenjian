package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var selectedPhone by remember { mutableStateOf<String?>(null) }
    var detailLogs by remember { mutableStateOf<List<com.example.yueyeushaokaojiaoziguan.merchant.PointsLog>>(emptyList()) }

    LaunchedEffect(Unit) { vm.fetchAllPointsUsers() }

    if (selectedPhone != null) {
        // 积分明细页
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedPhone = null }) { Text("← 返回") }
                Spacer(Modifier.width(8.dp))
                Text("$selectedPhone 积分明细", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (detailLogs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detailLogs) { log ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column { Text(log.reason.ifBlank { "积分变动" }, fontWeight = FontWeight.Medium); Text(log.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Text(if (log.delta > 0) "+${log.delta}" else "${log.delta}", fontWeight = FontWeight.Bold, color = if (log.delta > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                            }
                        }
                    }
                }
            }
        }
    } else {
        // 用户列表
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔍", fontSize = 32.sp); Spacer(Modifier.width(12.dp))
                        Column { Text("积分用户", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("共 ${uiState.pointsUsers.size} 位用户", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            if (uiState.pointsUsers.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("暂无积分用户", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            } else {
                items(uiState.pointsUsers) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            selectedPhone = user.phone
                            vm.queryPointsDetail(user.phone) { detailLogs = it }
                        }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(user.phone, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("${user.points} 积分", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
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
    var maxDeductInput by remember { mutableStateOf(uiState.pointsConfig.maxDeductPercent.toString()) }

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
        OutlinedTextField(value = maxDeductInput, onValueChange = { maxDeductInput = it.filter(Char::isDigit) }, label = { Text("每单最多抵扣百分比（0-100）") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        FilledTonalButton(onClick = { vm.updatePointsConfig(earnRate = earnInput.toIntOrNull() ?: 1, deductRate = deductInput.toIntOrNull() ?: 10, maxDeductPercent = maxDeductInput.toIntOrNull() ?: 50) }, modifier = Modifier.fillMaxWidth()) { Text("保存规则") }

        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("当前规则", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("消费 1 元得 ${uiState.pointsConfig.earnRate} 积分", fontSize = 14.sp)
                Text("${uiState.pointsConfig.deductRate} 积分抵扣 1 元", fontSize = 14.sp)
                Text("每单最多抵扣 ${uiState.pointsConfig.maxDeductPercent}%", fontSize = 14.sp)
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
                    Text("每单最多抵扣 ${uiState.pointsConfig.maxDeductPercent}%", fontSize = 14.sp)
                }
            }
        }
        item {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val versionName = remember { try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (_: Exception) { "未知" } }
            val versionCode = remember { try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode } catch (_: Exception) { 0 } }
            var checkResult by remember { mutableStateOf<String?>(null) }
            var checking by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("月月烧烤商家版", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("版本 v$versionName ($versionCode)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = {
                    checking = true; checkResult = null
                    com.example.yueyeushaokaojiaoziguan.merchant.AppUpdater.checkUpdate { info ->
                        checking = false
                        if (info != null && info.versionCode > versionCode) {
                            vm.checkForUpdate(versionCode)
                        } else {
                            checkResult = "当前已是最新版本"
                        }
                    }
                }, enabled = !checking) { Text(if (checking) "检测中..." else "检测最新版本") }
                checkResult?.let { Spacer(Modifier.height(4.dp)); Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
