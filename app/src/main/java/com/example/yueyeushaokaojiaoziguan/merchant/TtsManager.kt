package com.example.yueyeushaokaojiaoziguan.merchant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsManager {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_ADD, null, System.currentTimeMillis().toString())
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
