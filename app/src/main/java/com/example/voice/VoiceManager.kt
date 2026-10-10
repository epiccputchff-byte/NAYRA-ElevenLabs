package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class VoiceOption(
    val name: String,
    val locale: Locale,
    val isMale: Boolean,
    val isDefault: Boolean = false
)

class VoiceManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // 0f to 1f normalized audio amplitude for visualizer & lip-sync
    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _speechPitch = MutableStateFlow(0.85f) // Confident deep male tone
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _speechSpeed = MutableStateFlow(1.0f)
    val speechSpeed: StateFlow<Float> = _speechSpeed.asStateFlow()

    private val _currentLanguage = MutableStateFlow("auto") // "auto", "en-IN", "hi-IN", "en-US"
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var amplitudeJob: Job? = null
    private var lastRecognizedTime = 0L
    private var lastRecognizedText = ""

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    setupTtsEngine(engine)
                    _isTtsReady.value = true
                }
            } else {
                Log.e("VoiceManager", "Failed to initialize TextToSpeech: status=$status")
            }
        }
    }

    private fun setupTtsEngine(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                startSpeakingAnimationSimulation()
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                stopSpeakingAnimationSimulation()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                stopSpeakingAnimationSimulation()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                stopSpeakingAnimationSimulation()
            }
        })

        // Search for natural male voices
        try {
            val systemVoices = engine.voices
            if (systemVoices != null && systemVoices.isNotEmpty()) {
                val voiceList = mutableListOf<VoiceOption>()
                var preferredMaleVoice: Voice? = null

                for (voice in systemVoices) {
                    val voiceNameLower = voice.name.lowercase(Locale.ROOT)
                    val isMale = voiceNameLower.contains("male") ||
                            voiceNameLower.contains("-m-") ||
                            voiceNameLower.contains("#male") ||
                            voiceNameLower.contains("deep")

                    val locale = voice.locale
                    val matchesTargetLocale = locale.language == "en" || locale.language == "hi"

                    if (matchesTargetLocale) {
                        voiceList.add(
                            VoiceOption(
                                name = voice.name,
                                locale = locale,
                                isMale = isMale
                            )
                        )

                        // Pick first detected English/Hindi male voice
                        if (preferredMaleVoice == null && isMale) {
                            preferredMaleVoice = voice
                        }
                    }
                }

                _availableVoices.value = voiceList

                if (preferredMaleVoice != null) {
                    engine.voice = preferredMaleVoice
                    _selectedVoiceName.value = preferredMaleVoice.name
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceManager", "Error querying TTS voices", e)
        }

        engine.setPitch(_speechPitch.value)
        engine.setSpeechRate(_speechSpeed.value)
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted && _isSpeaking.value) {
            stopSpeaking()
        }
    }

    fun toggleMute() {
        setMuted(!_isMuted.value)
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch.coerceIn(0.5f, 1.5f)
        tts?.setPitch(_speechPitch.value)
    }

    fun setSpeechSpeed(speed: Float) {
        _speechSpeed.value = speed.coerceIn(0.5f, 1.8f)
        tts?.setSpeechRate(_speechSpeed.value)
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun selectVoice(voiceName: String) {
        _selectedVoiceName.value = voiceName
        val engine = tts ?: return
        try {
            val matchingVoice = engine.voices?.find { it.name == voiceName }
            if (matchingVoice != null) {
                engine.voice = matchingVoice
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error selecting voice", e)
        }
    }

    fun speak(text: String) {
        if (_isMuted.value) return
        val engine = tts ?: return

        stopSpeaking()

        val cleanText = text
            .replace(Regex("""[*_~`#>]"""), "")
            .replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
            .trim()

        if (cleanText.isEmpty()) return

        // Auto-detect or select locale
        val localeToUse = when (_currentLanguage.value) {
            "hi-IN" -> Locale("hi", "IN")
            "en-US" -> Locale.US
            "en-IN" -> Locale("en", "IN")
            else -> {
                // If contains Devanagari characters, speak in Hindi
                val containsHindi = cleanText.any { it in '\u0900'..'\u097F' }
                if (containsHindi) Locale("hi", "IN") else Locale("en", "IN")
            }
        }

        try {
            engine.language = localeToUse
            engine.setPitch(_speechPitch.value)
            engine.setSpeechRate(_speechSpeed.value)

            val utteranceId = UUID.randomUUID().toString()
            engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error executing speak", e)
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        stopSpeakingAnimationSimulation()
    }

    private fun startSpeakingAnimationSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch(Dispatchers.Default) {
            var tick = 0
            while (isActive && _isSpeaking.value) {
                tick++
                // Modulate speech wave (mouth movement and hologram audio wave)
                val base = 0.35f + 0.55f * kotlin.math.abs(kotlin.math.sin(tick * 0.45).toFloat())
                val modulation = if (tick % 7 == 0) 0.15f else 0.05f
                _audioAmplitude.value = (base + modulation).coerceIn(0.1f, 1f)
                delay(75)
            }
            _audioAmplitude.value = 0f
        }
    }

    private fun stopSpeakingAnimationSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _audioAmplitude.value = 0f
    }

    // --- Speech Recognition ---

    fun startListening() {
        if (_isListening.value) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError?.invoke("Speech recognition is not available on this device.")
            return
        }

        try {
            stopSpeaking()
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Map rmsdB (-2 to 10 typical) to 0f..1f
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition engine busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            onSpeechError?.invoke(message)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim()

                        if (!spokenText.isNullOrEmpty()) {
                            val now = System.currentTimeMillis()
                            // Guard against duplicate speech recognition events within 1.5 seconds
                            if (now - lastRecognizedTime > 1500 || spokenText != lastRecognizedText) {
                                lastRecognizedTime = now
                                lastRecognizedText = spokenText
                                onSpeechRecognized?.invoke(spokenText)
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // Keep listening indicator alive
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                val selectedLang = _currentLanguage.value
                val languageTag = when (selectedLang) {
                    "hi-IN" -> "hi-IN"
                    "en-US" -> "en-US"
                    "en-IN" -> "en-IN"
                    else -> Locale.getDefault().toLanguageTag()
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to start speech recognition", e)
            _isListening.value = false
            onSpeechError?.invoke("Failed to start listening: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping listening", e)
        }
        _isListening.value = false
        _audioAmplitude.value = 0f
    }

    fun destroy() {
        stopSpeaking()
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
    }
}
