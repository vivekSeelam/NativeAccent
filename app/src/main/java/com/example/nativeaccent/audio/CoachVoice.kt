package com.example.nativeaccent.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * The reference ("coach") voice, backed by the platform TextToSpeech engine in
 * US English.
 *
 * Initialisation is asynchronous, so a [speak] issued before the engine is ready
 * is held and replayed once it is — the user never taps the speaker icon and
 * gets silence just because they were fast.
 */
class CoachVoice(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, PlaybackSpeed>? = null
    private var onSpeakingChanged: ((Boolean) -> Unit)? = null

    init {
        // The engine is captured in a local and configured before it is exposed
        // through [tts], so the init callback never races the field assignment.
        val engine = TextToSpeech(context.applicationContext, ::onEngineInit)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = notifySpeaking(true)
            override fun onDone(utteranceId: String?) = notifySpeaking(false)

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) = notifySpeaking(false)

            override fun onError(utteranceId: String?, errorCode: Int) = notifySpeaking(false)
        })
        tts = engine
    }

    /** Called once the TTS service is bound; the voice list is only valid from here on. */
    private fun onEngineInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TextToSpeech init failed with status $status")
            return
        }
        val result = tts?.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "US English voice data unavailable; falling back to engine default")
        }
        ready = true
        pending?.let { (text, speed) ->
            pending = null
            speak(text, speed)
        }
    }

    /** Observes whether the coach is currently talking, so the UI can light up its button. */
    fun setSpeakingListener(listener: ((Boolean) -> Unit)?) {
        onSpeakingChanged = listener
    }

    fun speak(text: String, speed: PlaybackSpeed = PlaybackSpeed.NORMAL) {
        val engine = tts
        if (engine == null || !ready) {
            pending = text to speed
            return
        }
        engine.setSpeechRate(speed.rate)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
        notifySpeaking(false)
    }

    /** Must be called once, from the owner's teardown — TTS holds a bound service. */
    fun shutdown() {
        pending = null
        onSpeakingChanged = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun notifySpeaking(speaking: Boolean) {
        onSpeakingChanged?.invoke(speaking)
    }

    private companion object {
        const val TAG = "CoachVoice"
        const val UTTERANCE_ID = "coach"
    }
}
