package com.example.yueyeushaokaojiaoziguan.merchant

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.yueyeushaokaojiaoziguan.MainActivity
import com.example.yueyeushaokaojiaoziguan.R
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class SseService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val foregroundChannelId = "sse_channel"
    private val alertChannelId = "order_alert_channel"
    private var notifId = 100

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(1, buildForegroundNotification())
        startSseLoop()
    }

    private fun startSseLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val url = URL("${MerchantApiConfig.baseApiUrl}/merchant/events")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 0
                    conn.setRequestProperty("Accept", "text/event-stream")
                    conn.inputStream.bufferedReader().use { reader ->
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            if (line.startsWith("data:")) {
                                val payload = line.removePrefix("data:").trim()
                                handleEvent(payload)
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(3000L)
            }
        }
    }

    private fun handleEvent(payload: String) {
        try {
            val json = org.json.JSONObject(payload)
            val type = json.optString("type")
            val area = json.optString("area")
            val table = json.optString("table")
            val role = getSharedPreferences("shaokao_prefs", MODE_PRIVATE).getString("user_role", "Boss")
            val prefix = if (role == "Staff") "注意" else "老板"
            val (alert, tts, tab) = when (type) {
                "checkout" -> Triple("${area} ${table} 的客人申请结账啦！", "${prefix}，${area}${table}的客人要结账啦", "Home")
                "new_order" -> Triple("${area} ${table} 有新订单！", "${prefix}，${area}${table}来新单啦", "Home")
                "append_order" -> Triple("${area} ${table} 加单啦！", "${prefix}，${area}${table}的客人又加单啦", "Home")
                else -> return
            }
            TtsManager.speak(tts)
            // 系统通知，点击打开APP对应页面
            showAlertNotification(alert, tab)
            // 广播给 ViewModel 刷新 UI
            sendBroadcast(Intent("com.shaokao.SSE_EVENT").apply {
                putExtra("alert", alert)
                setPackage(packageName)
            })
        } catch (_: Exception) {}
    }

    private fun showAlertNotification(text: String, tab: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_tab", tab)
        }
        val pi = PendingIntent.getActivity(this, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, alertChannelId)
            .setContentTitle("🔔 月月烧烤")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()
        getSystemService(NotificationManager::class.java).notify(notifId++, notif)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            // 前台服务常驻通知（低优先级，不打扰）
            nm.createNotificationChannel(
                NotificationChannel(foregroundChannelId, "后台运行", NotificationManager.IMPORTANCE_LOW)
            )
            // 订单提醒通知（高优先级，弹出横幅）
            nm.createNotificationChannel(
                NotificationChannel(alertChannelId, "订单提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "新订单、结账等实时提醒"
                    enableVibration(true)
                }
            )
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, foregroundChannelId)
            .setContentTitle("月月烧烤")
            .setContentText("营业中，等待订单...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
