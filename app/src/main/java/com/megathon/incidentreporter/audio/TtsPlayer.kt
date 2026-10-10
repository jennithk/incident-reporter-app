package com.megathon.incidentreporter.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

// Offline-first TTS. Verifies a locally available voice before claiming offline playback.
// Never overlaps itself: each speak() stops the previous utterance.
class TtsPlayer(ctx: Context) {
    private var tts: TextToSpeech? = null
    private var lastText: String? = null

    @Volatile var isReady: Boolean = false
        private set
    @Volatile var offlineVoiceVerified: Boolean = false
        private set

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                try {
                    val t = tts ?: return@TextToSpeech
                    t.language = Locale.getDefault()
                    offlineVoiceVerified = try {
                        t.voices?.any { v -> !v.isNetworkConnectionRequired } == true
                    } catch (_: Exception) { false }
                } catch (_: Exception) { offlineVoiceVerified = false }
            }
        }
    }

    fun speak(text: String) {
        lastText = text
        try {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "followup")
        } catch (_: Exception) { }
    }

    fun replay() { lastText?.let { speak(it) } }
    fun stop() { try { tts?.stop() } catch (_: Exception) { } }
    fun release() { try { tts?.stop(); tts?.shutdown() } catch (_: Exception) { }; tts = null }
}
