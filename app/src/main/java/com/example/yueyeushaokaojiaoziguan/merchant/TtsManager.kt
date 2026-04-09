package com.example.yueyeushaokaojiaoziguan.merchant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsManager {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pendingQueue = mutableListOf<String>()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(0.85f)   // 稍慢一点，更清晰
                tts?.setPitch(1.15f)        // 稍高一点，更活泼
                ready = true
                synchronized(pendingQueue) {
                    pendingQueue.forEach { speak(it) }
                    pendingQueue.clear()
                }
            }
        }
    }

    fun speak(text: String) {
        if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, System.currentTimeMillis().toString())
        } else {
            synchronized(pendingQueue) { pendingQueue.add(text) }
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
