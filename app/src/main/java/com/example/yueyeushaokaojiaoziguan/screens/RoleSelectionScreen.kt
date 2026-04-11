package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import com.example.yueyeushaokaojiaoziguan.merchant.UserRole
import com.example.yueyeushaokaojiaoziguan.ui.theme.GradientOrange

@Composable
fun RoleSelectionScreen(onRoleSelected: (UserRole) -> Unit) {
    var showLogin by remember { mutableStateOf(false) }

    if (showLogin) {
        LoginScreen(onBack = { showLogin = false }, onLoginSuccess = { onRoleSelected(UserRole.Boss) })
    } else {
        Box(Modifier.fillMaxSize().background(Color(0xFFFAF8F5)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("🔥", fontSize = 64.sp)
                Text("月月烧烤", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("请选择身份", fontSize = 15.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                // 老板
                Card(
                    modifier = Modifier.width(260.dp).shadow(6.dp, RoundedCornerShape(20.dp))
                        .clickable { showLogin = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(GradientOrange)), contentAlignment = Alignment.Center) {
                            Text("👑", fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("老板", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("需要登录验证", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                // 员工
                Card(
                    modifier = Modifier.width(260.dp).shadow(6.dp, RoundedCornerShape(20.dp))
                        .clickable { onRoleSelected(UserRole.Staff) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF64B5F6)))), contentAlignment = Alignment.Center) {
                            Text("👷", fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("员工", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("直接进入工作台", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onBack: () -> Unit, onLoginSuccess: () -> Unit, vm: MerchantViewModel? = null) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Use a local VM-like approach via HTTP directly
    val httpLogin: (String, String) -> Unit = { u, p ->
        loading = true; error = null
        Thread {
            try {
                val url = java.net.URL("${com.example.yueyeushaokaojiaoziguan.merchant.MerchantApiConfig.baseApiUrl}${com.example.yueyeushaokaojiaoziguan.merchant.MerchantApiConfig.loginPath}")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true; connectTimeout = 8000; readTimeout = 8000
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write("""{"username":"$u","password":"$p"}""".toByteArray()) }
                val text = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
                conn.disconnect()
                val json = org.json.JSONObject(text)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    loading = false
                    if (json.optBoolean("success")) onLoginSuccess()
                    else error = json.optString("error", "登录失败")
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post { loading = false; error = "网络错误：${e.message}" }
            }
        }.start()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFFAF8F5)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(300.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← 返回") }
            Text("👑 老板登录", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("账号") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation())
            error?.let { Text(it, color = Color.Red, fontSize = 13.sp) }
            Button(onClick = { httpLogin(username, password) }, enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Text(if (loading) "登录中..." else "登录")
            }
        }
    }
}
