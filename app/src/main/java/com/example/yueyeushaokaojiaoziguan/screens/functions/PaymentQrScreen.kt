package com.example.yueyeushaokaojiaoziguan.screens.functions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantApiConfig
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel

@Composable
fun PaymentQrScreen(uiState: MerchantUiState, vm: MerchantViewModel) {
    val context = LocalContext.current

    fun uploadAndSave(uri: Uri, type: String) {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return
        vm.uploadDishImage(bytes, "${type}_qr.jpg") { url ->
            if (url != null) {
                if (type == "wechat") vm.savePaymentQr(wechat = url)
                else vm.savePaymentQr(alipay = url)
            }
        }
    }

    val wechatPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAndSave(it, "wechat") }
    }
    val alipayPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAndSave(it, "alipay") }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QrCard("微信收款码", "💚", uiState.paymentQr.wechat, Color(0xFF4CAF50)) { wechatPicker.launch("image/*") }
        QrCard("支付宝收款码", "💙", uiState.paymentQr.alipay, Color(0xFF2196F3)) { alipayPicker.launch("image/*") }
    }
}

@Composable
private fun QrCard(title: String, icon: String, imageUrl: String, accentColor: Color, onUpload: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("${MerchantApiConfig.baseApiUrl}$imageUrl").crossfade(true).build(),
                    contentDescription = title,
                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Box(Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center) {
                    Text("未上传", color = Color.Gray)
                }
                Spacer(Modifier.height(12.dp))
            }
            FilledTonalButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                Text(if (imageUrl.isNotBlank()) "更换图片" else "上传收款码")
            }
        }
    }
}
