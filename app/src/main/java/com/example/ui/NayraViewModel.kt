package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiRepository
import com.example.ui.character.AiVisualState
import com.example.voice.VoiceManager
import com.example.voice.VoiceOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class MessageSender {
    USER,
    NAYRA
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

class NayraViewModel(application: Application) : AndroidViewModel(application) {

    val geminiRepository = GeminiRepository()
    val voiceManager = VoiceManager(application.applicationContext, viewModelScope)

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.NAYRA,
                text = "Hello! I am NAYRA, your futuristic anime AI assistant. How can I help you today? आप मुझसे हिंदी या English में बात कर सकते हैं!"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _useAvatarMode = MutableStateFlow(true)
    val useAvatarMode: StateFlow<Boolean> = _useAvatarMode.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _showChatPanel = MutableStateFlow(false)
    val showChatPanel: StateFlow<Boolean> = _showChatPanel.asStateFlow()

    // Derived AI Visual State
    val visualState: StateFlow<AiVisualState> = combine(
        voiceManager.isListening,
        _isThinking,
        voiceManager.isSpeaking
    ) { isListening, isThinking, isSpeaking ->
        when {
            isListening -> AiVisualState.LISTENING
            isThinking -> AiVisualState.THINKING
            isSpeaking -> AiVisualState.SPEAKING
            else -> AiVisualState.IDLE
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AiVisualState.IDLE)

    init {
        voiceManager.onSpeechRecognized = { spokenText ->
            handleSpokenText(spokenText)
        }

        voiceManager.onSpeechError = { errorText ->
            _errorMessage.value = errorText
        }
    }

    private fun handleSpokenText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            sendMessage(trimmed)
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isThinking.value) return

        // Stop any current voice operations
        voiceManager.stopSpeaking()
        voiceManager.stopListening()

        // Append user message
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )
        _messages.value = _messages.value + userMsg
        _isThinking.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = geminiRepository.sendMessage(trimmed)
            _isThinking.value = false

            result.fold(
                onSuccess = { replyText ->
                    val nayraMsg = ChatMessage(
                        sender = MessageSender.NAYRA,
                        text = replyText
                    )
                    _messages.value = _messages.value + nayraMsg

                    // Read response aloud in male voice
                    voiceManager.speak(replyText)
                },
                onFailure = { error ->
                    val errorDesc = error.message ?: "Failed to reach NAYRA core."
                    _errorMessage.value = errorDesc
                    val errorMsg = ChatMessage(
                        sender = MessageSender.NAYRA,
                        text = "I encountered a transmission error: $errorDesc"
                    )
                    _messages.value = _messages.value + errorMsg
                    voiceManager.speak("Transmission error. Please check your connection or API key.")
                }
            )
        }
    }

    fun onMicClicked(hasRecordAudioPermission: Boolean, requestPermissionAction: () -> Unit) {
        if (!hasRecordAudioPermission) {
            requestPermissionAction()
            return
        }

        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        } else {
            if (voiceManager.isSpeaking.value) {
                voiceManager.stopSpeaking()
            }
            voiceManager.startListening()
        }
    }

    fun replayMessage(message: ChatMessage) {
        if (message.sender == MessageSender.NAYRA) {
            voiceManager.speak(message.text)
        }
    }

    fun toggleMute() {
        voiceManager.toggleMute()
    }

    fun setSpeechSpeed(speed: Float) {
        voiceManager.setSpeechSpeed(speed)
    }

    fun setSpeechPitch(pitch: Float) {
        voiceManager.setSpeechPitch(pitch)
    }

    fun setLanguage(lang: String) {
        voiceManager.setLanguage(lang)
    }

    fun selectVoice(voiceName: String) {
        voiceManager.selectVoice(voiceName)
    }

    fun toggleAvatarMode() {
        _useAvatarMode.value = !_useAvatarMode.value
    }

    fun toggleSettingsSheet(open: Boolean) {
        _showSettingsSheet.value = open
    }

    fun toggleChatPanel(open: Boolean) {
        _showChatPanel.value = open
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun clearChat() {
        geminiRepository.clearSessionHistory()
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.NAYRA,
                text = "Session memory reset. How can I assist you now, commander?"
            )
        )
        voiceManager.stopSpeaking()
    }

    fun setCustomApiKey(key: String) {
        geminiRepository.customApiKey = key.ifBlank { null }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
