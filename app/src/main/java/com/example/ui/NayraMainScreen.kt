package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.character.AiVisualState
import com.example.ui.character.AudioVisualizerWave
import com.example.ui.character.HolographicCharacter3D
import com.example.ui.components.ChatTranscriptDrawer
import com.example.ui.components.HolographicHudBadge
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.HoloBlue
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloPink
import com.example.ui.theme.HoloPurple
import com.example.ui.theme.HoloTeal
import com.example.ui.theme.TextHoloMuted
import com.example.ui.theme.TextHoloPrimary
import com.example.ui.theme.TextHoloSecondary

@Composable
fun NayraMainScreen(
    viewModel: NayraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val visualState by viewModel.visualState.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isMuted by viewModel.voiceManager.isMuted.collectAsState()
    val audioAmplitude by viewModel.voiceManager.audioAmplitude.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val useAvatarMode by viewModel.useAvatarMode.collectAsState()
    val showSettings by viewModel.showSettingsSheet.collectAsState()
    val showChatPanel by viewModel.showChatPanel.collectAsState()

    val availableVoices by viewModel.voiceManager.availableVoices.collectAsState()
    val selectedVoiceName by viewModel.voiceManager.selectedVoiceName.collectAsState()
    val speechPitch by viewModel.voiceManager.speechPitch.collectAsState()
    val speechSpeed by viewModel.voiceManager.speechSpeed.collectAsState()
    val currentLanguage by viewModel.voiceManager.currentLanguage.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.onMicClicked(true) {}
        }
    }

    val latestMessage = messages.lastOrNull()

    val quickPrompts = listOf(
        "Explain Quantum Computing" to "Explain Quantum Computing in simple terms",
        "नमस्ते NAYRA! आप कौन हैं?" to "नमस्ते NAYRA! अपने बारे में कुछ बताइए।",
        "Write Kotlin Coroutine Code" to "Write a clean Kotlin code snippet using Coroutines and StateFlow",
        "Tell a mind-bending sci-fi fact" to "Tell me a fascinating futuristic sci-fi or science fact"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberVoid
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Futuristic Cyber Ambient Background Canvas
            CyberAmbientBackground(visualState = visualState)

            // 2. Main Foreground Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action & Status Bar
                TopCyberBar(
                    visualState = visualState,
                    isMuted = isMuted,
                    useAvatarMode = useAvatarMode,
                    onToggleMute = { viewModel.toggleMute() },
                    onOpenChat = { viewModel.toggleChatPanel(true) },
                    onOpenSettings = { viewModel.toggleSettingsSheet(true) },
                    onToggleAvatarMode = { viewModel.toggleAvatarMode() }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Error Notification Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    errorMessage?.let { errorText ->
                        CyberErrorBanner(
                            errorText = errorText,
                            onDismiss = { viewModel.dismissError() }
                        )
                    }
                }

                // 3. Central 3D Holographic Character Stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 3D Character Canvas
                    HolographicCharacter3D(
                        state = visualState,
                        isSpeaking = isSpeaking,
                        audioAmplitude = audioAmplitude,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("holographic_character_3d"),
                        useAvatarHologramMode = useAvatarMode
                    )

                    // Latest Speech Card (Floating HUD glass bubble)
                    if (latestMessage != null) {
                        LatestSpeechCard(
                            message = latestMessage,
                            isSpeaking = isSpeaking,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                                .clickable { viewModel.toggleChatPanel(true) }
                        )
                    }

                    // Interactive Drag Hint Text
                    Text(
                        text = "❖ Drag hologram to tilt & rotate ❖",
                        color = TextHoloMuted.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                    )
                }

                // 4. Real-time Audio Frequency Wave
                AudioVisualizerWave(
                    isActive = isSpeaking || isListening,
                    amplitude = audioAmplitude,
                    activeColor = if (isListening) HoloTeal else HoloCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 6.dp)
                )

                // 5. Quick Action Suggestion Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(quickPrompts) { (label, prompt) ->
                        QuickPromptChip(
                            label = label,
                            onClick = { viewModel.sendMessage(prompt) }
                        )
                    }
                }

                // 6. Bottom Holographic Control & Input HUD
                BottomInputHUD(
                    textInput = textInput,
                    isListening = isListening,
                    visualState = visualState,
                    onTextInputChange = { textInput = it },
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        }
                    },
                    onMicClick = {
                        viewModel.onMicClicked(hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Chat Transcript Drawer
            if (showChatPanel) {
                ChatTranscriptDrawer(
                    messages = messages,
                    onDismiss = { viewModel.toggleChatPanel(false) },
                    onReplayMessage = { viewModel.replayMessage(it) },
                    onClearChat = { viewModel.clearChat() }
                )
            }

            // Settings Sheet
            if (showSettings) {
                SettingsDialog(
                    availableVoices = availableVoices,
                    selectedVoiceName = selectedVoiceName,
                    speechPitch = speechPitch,
                    speechSpeed = speechSpeed,
                    currentLanguage = currentLanguage,
                    useAvatarMode = useAvatarMode,
                    customApiKey = viewModel.geminiRepository.customApiKey,
                    onVoiceSelected = { viewModel.selectVoice(it) },
                    onPitchChanged = { viewModel.setSpeechPitch(it) },
                    onSpeedChanged = { viewModel.setSpeechSpeed(it) },
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onToggleAvatarMode = { viewModel.toggleAvatarMode() },
                    onApiKeyChanged = { viewModel.setCustomApiKey(it) },
                    onClearChat = { viewModel.clearChat() },
                    onDismiss = { viewModel.toggleSettingsSheet(false) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Top Cyber Bar
// -------------------------------------------------------------
@Composable
private fun TopCyberBar(
    visualState: AiVisualState,
    isMuted: Boolean,
    useAvatarMode: Boolean,
    onToggleMute: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleAvatarMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NAYRA",
                    color = HoloCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI",
                    color = TextHoloPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            HolographicHudBadge(state = visualState)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Mode toggle
            IconButton(
                onClick = onToggleAvatarMode,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, CircleShape)
                    .testTag("avatar_mode_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "Toggle 3D display mode",
                    tint = if (useAvatarMode) HoloCyan else TextHoloSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Mute toggle
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, CircleShape)
                    .testTag("mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute audio" else "Mute audio",
                    tint = if (isMuted) HoloPink else HoloCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Chat Drawer button
            IconButton(
                onClick = onOpenChat,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, CircleShape)
                    .testTag("chat_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Open HUD chat log",
                    tint = HoloTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open system settings",
                    tint = TextHoloSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Floating Speech Card
// -------------------------------------------------------------
@Composable
private fun LatestSpeechCard(
    message: ChatMessage,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val borderColor = if (isUser) HoloPurple.copy(alpha = 0.5f) else HoloCyan.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurface.copy(alpha = 0.88f))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSpeaking && !isUser) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Speaking indicator",
                            tint = HoloCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isUser) "YOU // OPERATOR" else "NAYRA AI // VOICE RESPONSE",
                        color = if (isUser) HoloPurple else HoloCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "TAP FOR LOG ❯",
                    color = TextHoloMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.text,
                color = TextHoloPrimary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
    }
}

// -------------------------------------------------------------
// Bottom Input HUD & Holographic Mic Button
// -------------------------------------------------------------
@Composable
private fun BottomInputHUD(
    textInput: String,
    isListening: Boolean,
    visualState: AiVisualState,
    onTextInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_waves")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_scale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberSurface.copy(alpha = 0.95f))
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text Input Field
        OutlinedTextField(
            value = textInput,
            onValueChange = onTextInputChange,
            placeholder = {
                Text(
                    text = if (isListening) "Listening to speech…" else "Type message in English or Hindi…",
                    color = TextHoloMuted,
                    fontSize = 13.sp
                )
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextHoloPrimary,
                unfocusedTextColor = TextHoloPrimary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier
                .weight(1f)
                .testTag("text_input")
        )

        // Send Button (Visible when text entered)
        if (textInput.isNotBlank()) {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(HoloCyan)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = CyberVoid,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Futuristic Glowing Microphone Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(54.dp)
        ) {
            // Radiating radar pulse when active listening
            if (isListening) {
                Canvas(modifier = Modifier.size(54.dp)) {
                    drawCircle(
                        color = HoloTeal.copy(alpha = waveAlpha),
                        radius = (size.minDimension / 2f) * waveScale,
                        style = Stroke(width = 3f)
                    )
                }
            }

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (isListening) listOf(HoloTeal, HoloBlue)
                            else listOf(HoloCyan, HoloBlue)
                        )
                    )
                    .border(
                        2.dp,
                        if (isListening) HoloTeal else HoloCyan.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .testTag("mic_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = if (isListening) "Stop listening" else "Start speech recognition",
                    tint = CyberVoid,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Quick Prompt Chip
// -------------------------------------------------------------
@Composable
private fun QuickPromptChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface.copy(alpha = 0.8f))
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = TextHoloSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// -------------------------------------------------------------
// Cyber Ambient Background Canvas
// -------------------------------------------------------------
@Composable
private fun CyberAmbientBackground(visualState: AiVisualState) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_bg")
    val gridPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Background dark gradient
        val bgBrush = Brush.radialGradient(
            colors = listOf(
                CyberDark,
                CyberVoid,
                Color.Black
            ),
            center = Offset(w / 2f, h * 0.45f),
            radius = w * 0.9f
        )
        drawRect(brush = bgBrush)

        // Subtle perspective grid lines at bottom
        val gridColor = HoloCyan.copy(alpha = 0.04f)
        for (i in 0..10) {
            val y = h * 0.75f + (i * 24f) + (gridPhase % 24f)
            if (y <= h) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }
        }
        for (i in 0..8) {
            val x = (w / 8f) * i
            drawLine(
                color = gridColor,
                start = Offset(w / 2f, h * 0.72f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
        }
    }
}

// -------------------------------------------------------------
// Cyber Error Banner
// -------------------------------------------------------------
@Composable
private fun CyberErrorBanner(
    errorText: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HoloPink.copy(alpha = 0.15f))
            .border(1.dp, HoloPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = errorText,
            color = HoloPink,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss error",
                tint = HoloPink,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
