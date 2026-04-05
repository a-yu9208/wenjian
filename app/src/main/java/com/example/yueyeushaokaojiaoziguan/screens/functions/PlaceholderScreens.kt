package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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

@Composable
fun PointsActivityScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    var earnInput by remember { mutableStateOf(uiState.pointsConfig.earnRate.toString()) }
    var deductInput by remember { mutableStateOf(uiState.pointsConfig.deductRate.toString()) }
    var target by remember { mutableStateOf("") }
    var deltaInput by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var isDeduct by remember { mutableStateOf(false) }
    var queryPhone by remember { mutableStateOf("") }
    var queryResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 查询积分
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("查询积分", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = queryPhone,
                        onValueChange = { queryPhone = it.filter(Char::isDigit) },
                        label = { Text("手机号") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(onClick = {
                        if (queryPhone.length == 11) {
                            vm.queryPoints(queryPhone) { result -> queryResult = result }
                        }
                    }) { Text("查询") }
                    queryResult?.let {
                        Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 积分规则
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("积分规则", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = earnInput,
                        onValueChange = { earnInput = it.filter(Char::isDigit) },
                        label = { Text("消费1元得N积分") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deductInput,
                        onValueChange = { deductInput = it.filter(Char::isDigit) },
                        label = { Text("N积分抵扣1元") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(onClick = {
                        vm.updatePointsConfig(
                            earnRate = earnInput.toIntOrNull() ?: 1,
                            deductRate = deductInput.toIntOrNull() ?: 10
                        )
                    }) { Text("保存规则") }
                }
            }
        }

        // 手动增减积分
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("手动增减积分", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("桌号或手机号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deltaInput,
                            onValueChange = { deltaInput = it.filter(Char::isDigit) },
                            label = { Text("积分数量") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isDeduct,
                            onClick = { isDeduct = false },
                            label = { Text("增加") }
                        )
                        FilterChip(
                            selected = isDeduct,
                            onClick = { isDeduct = true },
                            label = { Text("扣减") }
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("备注（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(onClick = {
                        val amt = deltaInput.toIntOrNull() ?: 0
                        vm.addPointsLog(target, if (isDeduct) -amt else amt, reason)
                        target = ""; deltaInput = ""; reason = ""
                    }) { Text("确认") }
                }
            }
        }

        // 积分流水
        if (uiState.pointsLogs.isNotEmpty()) {
            item {
                Text("积分流水", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }
            items(uiState.pointsLogs) { log ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(log.target, fontWeight = FontWeight.SemiBold)
                            Text("${log.reason}  ${log.time}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (log.delta > 0) "+${log.delta}" else "${log.delta}",
                            fontWeight = FontWeight.Bold,
                            color = if (log.delta > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    var shopName by remember { mutableStateOf(uiState.profile.shopName) }
    var h5Url by remember { mutableStateOf(uiState.profile.h5BaseUrl) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👤", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.profile.account, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(uiState.profile.mode, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("店铺设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("店铺名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = h5Url,
                        onValueChange = { h5Url = it },
                        label = { Text("H5 点餐地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(onClick = {
                        vm.updateProfile(shopName = shopName.trim(), h5BaseUrl = h5Url.trim())
                    }) { Text("保存") }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("积分规则概览", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("消费 1 元得 ${uiState.pointsConfig.earnRate} 积分", fontSize = 14.sp)
                    Text("${uiState.pointsConfig.deductRate} 积分抵扣 1 元", fontSize = 14.sp)
                }
            }
        }
    }
}
