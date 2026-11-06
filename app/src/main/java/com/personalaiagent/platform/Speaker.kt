package com.personalaiagent.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin wrapper around the system text-to-speech engine used for the optional
 * "speak replies" setting. Nothing is spoken unless the user enables it.
 */
class Speaker(context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false
    private val appContext = context.applicationContext

    fun initialize() {
        if (engine != null) return
        engine = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        engine?.stop()
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        engine?.stop()
    }

    fun release() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private companion object {
        const val UTTERANCE_ID = "assistant-reply"
    }
}
