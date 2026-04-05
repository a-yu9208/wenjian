package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import com.example.yueyeushaokaojiaoziguan.merchant.generateQrBitmap

@Composable
fun TableManageScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    val clipboardManager = LocalClipboardManager.current
    var qrDialogUrl by remember { mutableStateOf<String?>(null) }

    if (qrDialogUrl != null) {
        Dialog(onDismissRequest = { qrDialogUrl = null }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("扫码点餐", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    val bmp = remember(qrDialogUrl) { generateQrBitmap(qrDialogUrl!!, 512) }
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "二维码",
                        modifier = Modifier.size(240.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(qrDialogUrl!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { qrDialogUrl = null }) { Text("关闭") }
                }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
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
                            TextButton(onClick = { qrDialogUrl = uiState.qrPreviewUrl }) {
                                Text("查看二维码")
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
                    Column(Modifier.weight(1f)) {
                        Text("${table.area} ${table.label}", fontWeight = FontWeight.SemiBold)
                        Text(table.status, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (table.customerLink.isNotBlank()) {
                            Text(table.customerLink, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (table.customerLink.isNotBlank()) {
                        val bmp = remember(table.customerLink) { generateQrBitmap(table.customerLink, 256) }
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "${table.label}二维码",
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        TextButton(onClick = { vm.deleteTable(table.label) }) {
                            Text("删除", fontSize = 12.sp, color = Color(0xFFD32F2F))
                        }
                    }
                }
            }
        }
    }
}
