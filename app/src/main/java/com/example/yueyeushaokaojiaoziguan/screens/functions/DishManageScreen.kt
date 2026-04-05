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
import com.example.yueyeushaokaojiaoziguan.merchant.DishItem
import com.example.yueyeushaokaojiaoziguan.merchant.ComboItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DishManageScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    var manageMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var editingDish by remember { mutableStateOf<DishItem?>(null) }

    Box(Modifier.fillMaxSize()) {
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
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.clickable(enabled = !manageMode) { editingDish = dish }
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
        if (uiState.showAddDishDialog) {
            AddDishDialog(uiState = uiState, vm = vm, onDismiss = { vm.hideAddDishDialog() })
        }

        // 编辑菜品弹窗
        editingDish?.let { dish ->
            EditDishDialog(dish = dish, uiState = uiState, vm = vm, onDismiss = { editingDish = null })
        }
    }
}

@Composable
private fun AddDishDialog(uiState: MerchantUiState, vm: MerchantViewModel, onDismiss: () -> Unit) {
    var comboItems by remember { mutableStateOf(uiState.dishDraft.comboItems) }

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
                // 类型选择
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("类型：", fontSize = 14.sp)
                    FilterChip(selected = uiState.dishDraft.type == "单品", onClick = { vm.updateDishDraft(type = "单品") }, label = { Text("单品") })
                    FilterChip(selected = uiState.dishDraft.type == "套餐", onClick = { vm.updateDishDraft(type = "套餐") }, label = { Text("套餐") })
                }
                // 套餐子菜品
                if (uiState.dishDraft.type == "套餐") {
                    Text("套餐包含：", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    comboItems.forEachIndexed { idx, item ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${item.name} x${item.quantity}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { comboItems = comboItems.toMutableList().also { it.removeAt(idx) } }, Modifier.size(28.dp)) {
                                Text("✕", fontSize = 14.sp, color = Color(0xFFD32F2F))
                            }
                        }
                    }
                    ComboItemPicker(dishes = uiState.dishes, onAdd = { name, qty ->
                        comboItems = comboItems + ComboItem(name, qty)
                    })
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
            TextButton(onClick = {
                vm.updateDishDraftComboItems(comboItems)
                if (vm.addDishFromDraft()) vm.hideAddDishDialog()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { vm.hideAddDishDialog() }) { Text("取消") }
        }
    )
}

@Composable
private fun ComboItemPicker(dishes: List<DishItem>, onAdd: (String, Int) -> Unit) {
    var selectedDish by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    val dishNames = dishes.filter { it.type != "套餐" }.map { it.name }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        var expanded by remember { mutableStateOf(false) }
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = selectedDish,
                onValueChange = { selectedDish = it },
                label = { Text("选择菜品") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                    LaunchedEffect(it) { it.interactions.collect { expanded = true } }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                dishNames.forEach { name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { selectedDish = name; expanded = false })
                }
            }
        }
        OutlinedTextField(
            value = qty,
            onValueChange = { qty = it.filter(Char::isDigit) },
            label = { Text("数量") },
            singleLine = true,
            modifier = Modifier.width(60.dp)
        )
        FilledTonalButton(
            onClick = {
                if (selectedDish.isNotBlank()) {
                    onAdd(selectedDish, qty.toIntOrNull() ?: 1)
                    selectedDish = ""; qty = "1"
                }
            },
            modifier = Modifier.height(40.dp)
        ) { Text("+") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditDishDialog(dish: DishItem, uiState: MerchantUiState, vm: MerchantViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(dish.name) }
    var price by remember { mutableStateOf(dish.price.replace("¥", "")) }
    var stock by remember { mutableStateOf(dish.stock.toString()) }
    var desc by remember { mutableStateOf(dish.description) }
    var minOrder by remember { mutableStateOf(dish.minOrder.toString()) }
    var category by remember { mutableStateOf(dish.category) }
    var quickServe by remember { mutableStateOf(dish.quickServe) }
    var discountEnabled by remember { mutableStateOf(dish.discountEnabled) }
    var discountPrice by remember { mutableStateOf(dish.discountPrice.replace("¥", "")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑菜品") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("菜品名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("价格") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stock, onValueChange = { stock = it.filter(Char::isDigit) }, label = { Text("库存") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("描述") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minOrder, onValueChange = { minOrder = it.filter(Char::isDigit) }, label = { Text("最低起点") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("分类：$category", fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.categories.forEach { cat ->
                        AssistChip(onClick = { category = cat }, label = { Text(cat, fontSize = 12.sp) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("可快速上菜", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = quickServe, onCheckedChange = { quickServe = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("启用折扣", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = discountEnabled, onCheckedChange = { discountEnabled = it })
                }
                if (discountEnabled) {
                    OutlinedTextField(
                        value = discountPrice,
                        onValueChange = { discountPrice = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("折扣价") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = dish.copy(
                    name = name.trim(),
                    price = "¥$price",
                    stock = stock.toIntOrNull() ?: dish.stock,
                    description = desc,
                    minOrder = minOrder.toIntOrNull() ?: 1,
                    category = category,
                    quickServe = quickServe,
                    discountEnabled = discountEnabled,
                    discountPrice = if (discountEnabled) "¥$discountPrice" else ""
                )
                vm.updateDish(dish, updated)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
