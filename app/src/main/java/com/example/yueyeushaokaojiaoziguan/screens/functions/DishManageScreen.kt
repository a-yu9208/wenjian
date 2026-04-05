package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DishManageScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    var manageMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (!manageMode) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("+", fontSize = 24.sp, color = Color.White)
                }
            }
        }
    ) { scaffoldPadding ->
        Box(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            Column(Modifier.fillMaxSize()) {
            // 顶部操作栏
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("共 ${uiState.dishes.size} 道菜品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    manageMode = !manageMode
                    if (!manageMode) selected = emptySet()
                }) {
                    Text(if (manageMode) "取消" else "管理")
                }
            }

            // 菜品列表
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.dishes, key = { it.name }) { dish ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (manageMode) {
                                Checkbox(
                                    checked = dish.name in selected,
                                    onCheckedChange = {
                                        selected = if (it) selected + dish.name else selected - dish.name
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                            }

                            // 图片占位
                            Box(
                                Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🍖", fontSize = 24.sp)
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(dish.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "${dish.category} · 库存${dish.stock}",
                                    fontSize = 13.sp,
                                    color = if (dish.stock <= 10) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (dish.discountEnabled && dish.discountPrice.isNotBlank()) {
                                    Row {
                                        Text(dish.price, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                        Spacer(Modifier.width(4.dp))
                                        Text(dish.discountPrice, fontSize = 13.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(dish.price, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // 库存快捷调整
                            if (!manageMode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { vm.updateDishStock(dish.name, -1) }, Modifier.size(32.dp)) {
                                        Text("−", fontSize = 18.sp)
                                    }
                                    Text("${dish.stock}", Modifier.width(30.dp), fontSize = 14.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    IconButton(onClick = { vm.updateDishStock(dish.name, 1) }, Modifier.size(32.dp)) {
                                        Text("+", fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 管理模式下的删除按钮
        if (manageMode && selected.isNotEmpty()) {
            Button(
                onClick = {
                    vm.deleteDishes(selected)
                    selected = emptySet()
                    manageMode = false
                },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("删除选中 (${selected.size})", color = Color.White)
            }
        }

        // 添加菜品弹窗
        if (showAddSheet) {
            AddDishDialog(uiState = uiState, vm = vm, onDismiss = { showAddSheet = false })
        }
    }
    }
}

@Composable
private fun AddDishDialog(uiState: MerchantUiState, vm: MerchantViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增菜品") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.dishDraft.name,
                    onValueChange = { vm.updateDishDraft(name = it) },
                    label = { Text("菜品名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.dishDraft.price,
                    onValueChange = { vm.updateDishDraft(price = it.filter { c -> c.isDigit() || c == '.' }) },
                    label = { Text("价格") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.dishDraft.stock,
                    onValueChange = { vm.updateDishDraft(stock = it.filter(Char::isDigit)) },
                    label = { Text("库存") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.dishDraft.description,
                    onValueChange = { vm.updateDishDraft(description = it) },
                    label = { Text("菜品描述") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.dishDraft.minOrder,
                    onValueChange = { vm.updateDishDraft(minOrder = it.filter(Char::isDigit)) },
                    label = { Text("最低起点数量") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 分类选择
                Text("分类：${uiState.dishDraft.category}", fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.categories.forEach { cat ->
                        AssistChip(
                            onClick = { vm.updateDishDraft(category = cat) },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
                // 开关
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("可快速上菜", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = uiState.dishDraft.quickServe, onCheckedChange = { vm.updateDishDraft(quickServe = it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("启用折扣", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = uiState.dishDraft.discountEnabled, onCheckedChange = { vm.updateDishDraft(discountEnabled = it) })
                }
                if (uiState.dishDraft.discountEnabled) {
                    OutlinedTextField(
                        value = uiState.dishDraft.discountPrice,
                        onValueChange = { vm.updateDishDraft(discountPrice = it.filter { c -> c.isDigit() || c == '.' }) },
                        label = { Text("折扣价") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (vm.addDishFromDraft()) onDismiss() }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
