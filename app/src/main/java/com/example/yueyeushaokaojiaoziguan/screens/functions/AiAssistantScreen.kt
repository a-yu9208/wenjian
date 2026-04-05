package com.example.yueyeushaokaojiaoziguan.screens.functions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import kotlinx.coroutines.launch

private data class ChatMessage(val text: String, val isUser: Boolean, val loading: Boolean = false, val type: String = "text")

private const val FEATURES_TEXT = """我可以帮你做这些事情：

📊 查询类
• 查询今日营业额和订单数
• 查询某道菜的库存和价格
• 查询某手机号的积分
• 查看库存不足的菜品

🛠 操作指引类
• 教你如何添加新菜品
• 教你如何管理桌台和生成桌码
• 教你如何设置菜品分类
• 教你如何管理积分规则

💡 经营建议类
• 根据当前数据给出补货建议
• 提醒库存不足的菜品

直接告诉我你需要什么就行！"""

@Composable
fun AiAssistantScreen(vm: MerchantViewModel) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 动态欢迎语
    val todayRevenue = remember(uiState.dashboardStats) {
        uiState.dashboardStats.find { it.title == "今日营业额" }?.value ?: "¥0"
    }
    val todayOrders = remember(uiState.dashboardStats) {
        uiState.dashboardStats.find { it.title == "今日订单" }?.value ?: "0"
    }
    val welcomeText = "你好呀老板！我是你的烧烤助手 🔥\n今天已经有 $todayOrders 单啦，营业额 $todayRevenue，继续加油！💪\n\n有什么需要帮忙的尽管说，不清楚我能做什么就点击"

    var messages by remember(todayRevenue) {
        mutableStateOf(listOf(ChatMessage(welcomeText, isUser = false, type = "welcome")))
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                    if (!msg.isUser) { Text("🤖", fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp)); Spacer(Modifier.width(6.dp)) }
                    Box(
                        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (msg.isUser) MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5))
                            .padding(12.dp)
                    ) {
                        when {
                            msg.loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("思考中...", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                            msg.type == "welcome" -> {
                                // 欢迎语 + 蓝色可点击"查看功能"
                                val annotated = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color.Black, fontSize = 14.sp)) { append(msg.text) }
                                    pushStringAnnotation("action", "show_features")
                                    withStyle(SpanStyle(color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 14.sp)) { append("查看功能") }
                                    pop()
                                    withStyle(SpanStyle(color = Color.Black, fontSize = 14.sp)) { append(" 了解一下吧~") }
                                }
                                ClickableText(text = annotated, style = LocalTextStyle.current.copy(lineHeight = 20.sp)) { offset ->
                                    annotated.getStringAnnotations("action", offset, offset).firstOrNull()?.let {
                                        if (it.item == "show_features") {
                                            messages = messages + ChatMessage(FEATURES_TEXT, isUser = false, type = "features")
                                            scope.launch { listState.animateScrollToItem(messages.size - 1) }
                                        }
                                    }
                                }
                            }
                            msg.type == "features" -> {
                                Text(msg.text, fontSize = 14.sp, color = Color.Black, lineHeight = 20.sp)
                            }
                            else -> {
                                Text(msg.text, fontSize = 14.sp, color = if (msg.isUser) Color.White else Color.Black, lineHeight = 20.sp)
                            }
                        }
                    }
                    if (msg.isUser) { Spacer(Modifier.width(6.dp)); Text("👤", fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }

        // 输入栏
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") }, singleLine = true, shape = RoundedCornerShape(24.dp), enabled = !sending
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val msg = input.trim()
                    if (msg.isBlank() || sending) return@Button
                    input = ""
                    messages = messages + ChatMessage(msg, true)
                    messages = messages + ChatMessage("", false, loading = true)
                    sending = true
                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                    vm.sendAiMessage(msg) { reply ->
                        messages = messages.dropLast(1) + ChatMessage(reply, false)
                        sending = false
                        scope.launch { listState.animateScrollToItem(messages.size - 1) }
                    }
                },
                enabled = input.isNotBlank() && !sending,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) { Text("发送", fontWeight = FontWeight.SemiBold) }
        }
    }
}
