package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel

@Composable
fun TableManageScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 生成桌码
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("生成桌码", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("室外" to "outside", "一楼" to "first", "二楼" to "second").forEachIndexed { i, (label, value) ->
                            SegmentedButton(
                                selected = uiState.qrDraft.section == value,
                                onClick = { vm.updateQrDraft(section = value) },
                                shape = when (i) {
                                    0 -> RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
                                    2 -> RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                            ) { Text(label) }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.qrDraft.tableNumber,
                        onValueChange = { vm.updateQrDraft(tableNumber = it.filter(Char::isDigit)) },
                        label = { Text("桌号") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.qrPreviewUrl.isNotBlank()) {
                        Text("预览：${uiState.qrPreviewUrl}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = vm::generateQrForDraft) { Text("生成") }
                        TextButton(onClick = vm::applyDraftToTable) { Text("添加桌台") }
                        if (uiState.qrPreviewUrl.isNotBlank()) {
                            TextButton(onClick = { clipboardManager.setText(AnnotatedString(uiState.qrPreviewUrl)) }) {
                                Text("复制链接")
                            }
                        }
                    }
                }
            }
        }

        // 桌台列表
        items(uiState.tables) { table ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${table.area} ${table.label}", fontWeight = FontWeight.SemiBold)
                        Text(table.status, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (table.customerLink.isNotBlank()) {
                            Text(table.customerLink, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    FilledTonalButton(onClick = { vm.toggleTableStatus(table.label) }) {
                        Text(
                            when (table.status) {
                                "空闲" -> "使用中"
                                "使用中" -> "待结账"
                                "待结账" -> "空闲"
                                else -> "更新"
                            }, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
