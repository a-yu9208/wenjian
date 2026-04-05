package com.example.yueyeushaokaojiaoziguan.merchant

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import java.io.File

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String
)

object AppUpdater {
    private var downloadId: Long = -1
    private val mainHandler = Handler(Looper.getMainLooper())

    fun checkUpdate(onResult: (UpdateInfo?) -> Unit) {
        Thread {
            try {
                val url = java.net.URL("${MerchantApiConfig.baseApiUrl}/merchant/check-update")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = org.json.JSONObject(text)
                val data = json.optJSONObject("data") ?: return@Thread mainHandler.post { onResult(null) }
                val info = UpdateInfo(
                    versionCode = data.optInt("versionCode"),
                    versionName = data.optString("versionName"),
                    downloadUrl = data.optString("downloadUrl"),
                    changelog = data.optString("changelog")
                )
                mainHandler.post { onResult(info) }
            } catch (_: Exception) { mainHandler.post { onResult(null) } }
        }.start()
    }

    fun downloadAndInstall(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit) {
        val fullUrl = if (info.downloadUrl.startsWith("http")) info.downloadUrl
            else "${MerchantApiConfig.baseApiUrl}${info.downloadUrl}"

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(fullUrl))
            .setTitle("月月烧烤 v${info.versionName}")
            .setDescription("正在下载更新...")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        // 监听下载完成
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    ctx.unregisterReceiver(this)
                    onProgress(100)
                    installApk(ctx, file)
                }
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)

        // 轮询进度
        Thread {
            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (total > 0) mainHandler.post { onProgress((downloaded * 100 / total).toInt()) }
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    cursor.close()
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) break
                } else { cursor.close(); break }
                Thread.sleep(500)
            }
        }.start()
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
