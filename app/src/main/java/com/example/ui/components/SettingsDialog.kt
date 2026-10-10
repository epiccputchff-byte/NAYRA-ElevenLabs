package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.HoloBlue
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloPink
import com.example.ui.theme.HoloPurple
import com.example.ui.theme.HoloTeal
import com.example.ui.theme.TextHoloMuted
import com.example.ui.theme.TextHoloPrimary
import com.example.ui.theme.TextHoloSecondary
import com.example.voice.VoiceOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    availableVoices: List<VoiceOption>,
    selectedVoiceName: String?,
    speechPitch: Float,
    speechSpeed: Float,
    currentLanguage: String,
    useAvatarMode: Boolean,
    customApiKey: String?,
    onVoiceSelected: (String) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onToggleAvatarMode: () -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apiKeyInput by remember { mutableStateOf(customApiKey ?: "") }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(HoloCyan.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(HoloCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CORE SYSTEM SETTINGS",
                        color = HoloCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = TextHoloSecondary
                    )
                }
            }

            // Section 1: Voice & Audio Tuning
            SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "MALE VOICE & SPEECH")

            // Speech Speed
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Speech Speed", color = TextHoloPrimary, fontSize = 13.sp)
                    Text(
                        text = String.format("%.2fx", speechSpeed),
                        color = HoloCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = speechSpeed,
                    onValueChange = onSpeedChanged,
                    valueRange = 0.7f..1.6f,
                    colors = SliderDefaults.colors(
                        thumbColor = HoloCyan,
                        activeTrackColor = HoloCyan,
                        inactiveTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("speech_speed_slider")
                )
            }

            // Speech Pitch
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Voice Pitch (Deep Male Tone)", color = TextHoloPrimary, fontSize = 13.sp)
                    Text(
                        text = String.format("%.2f", speechPitch),
                        color = HoloTeal,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = speechPitch,
                    onValueChange = onPitchChanged,
                    valueRange = 0.6f..1.3f,
                    colors = SliderDefaults.colors(
                        thumbColor = HoloTeal,
                        activeTrackColor = HoloTeal,
                        inactiveTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("speech_pitch_slider")
                )
            }

            // Voice Selector (if multiple available)
            if (availableVoices.isNotEmpty()) {
                Text(
                    text = "Detected Voices (${availableVoices.size})",
                    color = TextHoloSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableVoices.take(4).forEach { voice ->
                        val isSelected = voice.name == selectedVoiceName
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberSurfaceElevated else CyberSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) HoloCyan else CyberSurfaceBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onVoiceSelected(voice.name) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = voice.name.substringAfterLast("/").take(28),
                                        color = if (isSelected) HoloCyan else TextHoloPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${voice.locale.displayLanguage} • ${if (voice.isMale) "Male Profile" else "Standard Profile"}",
                                        color = TextHoloMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "ACTIVE",
                                        color = HoloCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Language Preference
            SettingsSectionHeader(icon = Icons.Default.Translate, title = "PRIMARY LANGUAGE")
            val languages = listOf(
                "auto" to "Auto-Detect (English / हिन्दी)",
                "en-IN" to "English (India)",
                "hi-IN" to "हिन्दी (Hindi)",
                "en-US" to "English (US)"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.take(2).forEach { (code, label) ->
                    LanguageChip(
                        label = label,
                        isSelected = currentLanguage == code,
                        onClick = { onLanguageSelected(code) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.drop(2).forEach { (code, label) ->
                    LanguageChip(
                        label = label,
                        isSelected = currentLanguage == code,
                        onClick = { onLanguageSelected(code) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: 3D Hologram Mode
            SettingsSectionHeader(icon = Icons.Default.ViewInAr, title = "3D VISUALIZATION ENGINE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (useAvatarMode) "Holographic 3D Anime Mode" else "Procedural 3D Mesh Mode",
                        color = TextHoloPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (useAvatarMode)
                            "Render stylized anime hologram with depth & lip-sync"
                        else "High-performance procedural anime vector mesh",
                        color = TextHoloMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = useAvatarMode,
                    onCheckedChange = { onToggleAvatarMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HoloCyan,
                        checkedTrackColor = HoloBlue.copy(alpha = 0.5f),
                        uncheckedThumbColor = TextHoloMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Gemini AI Key
            SettingsSectionHeader(icon = Icons.Default.Key, title = "GEMINI API KEY OVERRIDE")
            Text(
                text = "Default key is linked via AI Studio Secrets. You can also specify an override key below:",
                color = TextHoloMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    onApiKeyChanged(it)
                },
                placeholder = { Text("AIzaSy...", color = TextHoloMuted, fontSize = 12.sp) },
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle API Key visibility",
                            tint = TextHoloSecondary
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextHoloPrimary,
                    unfocusedTextColor = TextHoloPrimary,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedIndicatorColor = HoloCyan,
                    unfocusedIndicatorColor = CyberSurfaceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 5: Reset Session
            OutlinedButton(
                onClick = onClearChat,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HoloPink
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(HoloPink.copy(alpha = 0.5f))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_memory_button")
            ) {
                Text(
                    text = "CLEAR SESSION MEMORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HoloCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = HoloCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun LanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyberSurfaceElevated else CyberSurface)
            .border(
                1.dp,
                if (isSelected) HoloCyan else CyberSurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) HoloCyan else TextHoloSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
