package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifierimport androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantApiConfig
import com.example.yueyeushaokaojiaoziguan.merchant.OrderItem
import com.example.yueyeushaokaojiaoziguan.merchant.PaymentQrConfig
import com.example.yueyeushaokaojiaoziguan.ui.theme.BrandOrange
import kotlinx.coroutines.launch

@Composable
fun CheckoutDialog(
    order: OrderItem,
    paymentQr: PaymentQrConfig,
    onDismiss: () -> Unit,
    onNotPaid: () -> Unit,
    onPaid: (utensilSets: Int) -> Unit
) {
    val originalAmount = order.originalAmount.ifBlank { order.amount }
    val pointsDeduct = order.pointsDeduct
    val baseAmount = order.amount.replace("¥", "").toDoubleOrNull() ?: 0.0
    val originalNum = originalAmount.replace("¥", "").toDoubleOrNull() ?: 0.0
    var utensilSets by remember { mutableIntStateOf(0) }
    var confirmed by remember { mutableStateOf(false) }
    val utensilFee = utensilSets * 1.0
    val displayOriginal = "¥%.2f".format(originalNum + utensilFee)
    val finalAmount = "¥%.2f".format(baseAmount + utensilFee)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${order.tableLabel} 结账", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))

                if (!confirmed) {
                    // 第一步：商家选餐具数量
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("餐具 (¥1/套)", fontSize = 14.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledIconButton(onClick = { if (utensilSets > 0) utensilSets-- }, modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF5F5F5))) { Text("−", fontSize = 16.sp, color = Color.DarkGray) }
                            Text("$utensilSets", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
                            FilledIconButton(onClick = { utensilSets++ }, modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF5F5F5))) { Text("+", fontSize = 16.sp, color = Color.DarkGray) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("实收：$finalAmount", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { confirmed = true }, modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)) { Text("确认，展示给客人") }
                } else {
                    // 第二步：展示给客人看的收款页面（无餐具信息）
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("应付金额", fontSize = 15.sp, color = Color.Gray)
                        Text(displayOriginal, fontSize = 15.sp, color = Color.Gray)
                    }
                    if (pointsDeduct.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("积分抵扣", fontSize = 15.sp, color = Color(0xFF4CAF50))
                            Text("-$pointsDeduct", fontSize = 15.sp, color = Color(0xFF4CAF50))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("实收金额", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
                        Text(finalAmount, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = BrandOrange)
                    }

                    Spacer(Modifier.height(20.dp))

                    // Payment QR tabs
                    val hasWechat = paymentQr.wechat.isNotBlank()
                    val hasAlipay = paymentQr.alipay.isNotBlank()
                    if (hasWechat || hasAlipay) {
                        val tabs = buildList {
                            if (hasWechat) add("微信支付" to paymentQr.wechat)
                            if (hasAlipay) add("支付宝支付" to paymentQr.alipay)
                        }
                        val pagerState = rememberPagerState(pageCount = { tabs.size })
                        val scope = rememberCoroutineScope()

                        if (tabs.size > 1) {
                            TabRow(selectedTabIndex = pagerState.currentPage, divider = {},
                                containerColor = Color.Transparent,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5))) {
                                tabs.forEachIndexed { i, (label, _) ->
                                    Tab(selected = pagerState.currentPage == i,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                                        text = { Text(label, fontSize = 13.sp, fontWeight = if (pagerState.currentPage == i) FontWeight.Bold else FontWeight.Normal) })
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(200.dp)) { page ->
                            val url = "${MerchantApiConfig.baseApiUrl}${tabs[page].second}"
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                                contentDescription = tabs[page].first,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center) {
                            Text("未设置收款码\n请在 功能→收款码管理 中上传", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Buttons
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { onNotPaid(); onDismiss() }, modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)) { Text("暂未支付") }
                        Button(onClick = { onPaid(utensilSets); onDismiss() }, modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)) { Text("支付完成") }
                    }
                }
            }
        }
    }
}
