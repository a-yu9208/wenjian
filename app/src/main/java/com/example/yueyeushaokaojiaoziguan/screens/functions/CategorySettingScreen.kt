package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel

@Composable
fun CategorySettingScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    var batchTarget by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.categoryDraft,
                    onValueChange = vm::updateCategoryDraft,
                    modifier = Modifier.weight(1f),
                    label = { Text("新分类名称") },
                    singleLine = true
                )
                FilledTonalButton(
                    onClick = vm::addCategoryFromDraft,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) { Text("添加") }
            }
        }

        items(uiState.categories) { category ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(category, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${uiState.dishes.count { it.category == category }} 道菜",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { batchTarget = category }) {
                            Text("添加菜品")
                        }
                        if (category != "未分类") {
                            TextButton(onClick = { vm.removeCategory(category) }) {
                                Text("删除", color = Color(0xFFD32F2F))
                            }
                        }
                    }
                }
            }
        }
    }

    // 批量添加菜品到分类弹窗
    batchTarget?.let { target ->
        BatchAddToCategoryDialog(
            category = target,
            uiState = uiState,
            onConfirm = { names ->
                vm.batchUpdateCategory(names, target)
                batchTarget = null
            },
            onDismiss = { batchTarget = null }
        )
    }
}

@Composable
private fun BatchAddToCategoryDialog(
    category: String,
    uiState: MerchantUiState,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // 显示不在该分类下的菜品供选择
    val candidates = uiState.dishes.filter { it.category != category }
    var selected by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加菜品到「$category」") },
        text = {
            if (candidates.isEmpty()) {
                Text("没有其他分类的菜品可添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(candidates, key = { it.name }) { dish ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = dish.name in selected,
                                onCheckedChange = {
                                    selected = if (it) selected + dish.name else selected - dish.name
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(dish.name, fontSize = 15.sp)
                                Text("当前分类：${dish.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected.isNotEmpty()) onConfirm(selected) },
                enabled = selected.isNotEmpty()
            ) { Text("确认移入 (${selected.size})") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
