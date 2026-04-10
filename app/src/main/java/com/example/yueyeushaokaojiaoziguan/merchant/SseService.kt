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
    private val channelId = "sse_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification("月月烧烤营业中"))
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
            val (alert, tts) = when (type) {
                "checkout" -> "${area} ${table} 的客人申请结账啦！" to "老板，${area}${table}的客人要结账啦"
                "new_order" -> "${area} ${table} 有新订单！" to "老板，${area}${table}来新单啦"
                "append_order" -> "${area} ${table} 加单啦！" to "老板，${area}${table}的客人又加单啦"
                else -> return
            }
            TtsManager.speak(tts)
            // 广播给 ViewModel 刷新 UI
            sendBroadcast(Intent("com.shaokao.SSE_EVENT").apply {
                putExtra("alert", alert)
                setPackage(packageName)
            })
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "订单提醒", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("月月烧烤")
            .setContentText(text)
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
