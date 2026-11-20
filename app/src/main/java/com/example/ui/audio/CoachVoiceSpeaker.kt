package com.example.ui.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class CoachVoiceSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("CoachVoiceSpeaker", "Language is not supported for TTS")
            } else {
                isInitialized = true
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(0.95f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        } else {
            Log.e("CoachVoiceSpeaker", "TextToSpeech init failed with status: $status")
        }
    }

    fun speak(text: String) {
        if (!isInitialized || tts == null) return
        stop()
        // Strip out emojis and excessive asterisks for natural speech flow
        val cleanText = text
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            .replace("*", "")
            .trim()

        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "maya_speech_${System.currentTimeMillis()}"

        @Suppress("DEPRECATION")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params)
        _isSpeaking.value = true
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (_: Exception) {}
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
