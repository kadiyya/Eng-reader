package com.kadiyya.engreader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Listens continuously while the prompter runs. Android's recognizer stops
 * after each phrase, so it is restarted until [stop] is called.
 *
 * [onPartial] receives the words of the phrase still being spoken (replacing
 * the previous partial); [onFinal] receives each finished phrase once.
 */
class SpeechTracker(
    private val context: Context,
    private val onLevel: (Float) -> Unit,
    private val onPartial: (List<String>) -> Unit,
    private val onFinal: (List<String>) -> Unit,
) : RecognitionListener {

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false

    private var partialWords = emptyList<String>()

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
    }

    fun start() {
        if (active) return
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(this)
            }
        }
        active = true
        listen()
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        commitPartial()
        recognizer?.cancel()
        onLevel(0f)
    }

    fun destroy() {
        stop()
        recognizer?.destroy()
        recognizer = null
    }

    private fun listen() {
        if (!active) return
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            scheduleRestart(500)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!active) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ listen() }, delayMs)
    }

    private fun commitPartial() {
        val words = partialWords
        partialWords = emptyList()
        if (words.isNotEmpty()) onFinal(words)
    }

    private fun extractWords(bundle: Bundle?): List<String> {
        val text = bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?: return emptyList()
        return normalizeWords(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val words = extractWords(partialResults)
        if (words.isNotEmpty()) {
            partialWords = words
            onPartial(words)
        }
    }

    override fun onResults(results: Bundle?) {
        val words = extractWords(results)
        if (words.isNotEmpty()) partialWords = words
        commitPartial()
        scheduleRestart(50)
    }

    override fun onError(error: Int) {
        commitPartial()
        onLevel(0f)
        when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> active = false
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                recognizer?.cancel()
                scheduleRestart(400)
            }
            else -> scheduleRestart(150)
        }
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Typical values range from about -2 (silence) to 10 (loud speech).
        onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    companion object {
        fun isAvailable(context: Context): Boolean =
            SpeechRecognizer.isRecognitionAvailable(context)
    }
}

/** Lowercases and strips punctuation so spoken and written words can be compared. */
fun normalizeWords(text: String): List<String> =
    text.lowercase()
        .replace('’', '\'')
        .split(Regex("[^a-z0-9']+"))
        .map { it.trim('\'') }
        .filter { it.isNotEmpty() }
