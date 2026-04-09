package com.example.yueyeushaokaojiaoziguan.merchant

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*
import java.net.URLEncoder

object TtsManager {
    private var player: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val queue = ArrayDeque<String>()
    private var playing = false

    fun init(context: Context) {}

    fun speak(text: String) {
        synchronized(queue) { queue.addLast(text) }
        playNext()
    }

    private fun playNext() {
        synchronized(queue) {
            if (playing || queue.isEmpty()) return
            playing = true
        }
        val text = synchronized(queue) { queue.removeFirst() }
        scope.launch {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = "${MerchantApiConfig.baseApiUrl}/merchant/tts?text=$encoded"
                withContext(Dispatchers.Main) {
                    player?.release()
                    player = MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build())
                        setDataSource(url)
                        setOnPreparedListener { it.start() }
                        setOnCompletionListener {
                            synchronized(queue) { playing = false }
                            playNext()
                        }
                        setOnErrorListener { _, _, _ ->
                            synchronized(queue) { playing = false }
                            playNext()
                            true
                        }
                        prepareAsync()
                    }
                }
            } catch (_: Exception) {
                synchronized(queue) { playing = false }
                playNext()
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        player?.release()
        player = null
    }
}
