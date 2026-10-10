package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.character.AiVisualState
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.HoloBlue
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloPurple
import com.example.ui.theme.HoloTeal

@Composable
fun HolographicHudBadge(
    state: AiVisualState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val (statusText, statusColor) = when (state) {
        AiVisualState.IDLE -> "CORE ONLINE // IDLE" to HoloCyan
        AiVisualState.LISTENING -> "VOICE INPUT // LISTENING" to HoloTeal
        AiVisualState.THINKING -> "NEURAL COMPUTING…" to HoloPurple
        AiVisualState.SPEAKING -> "AUDIO SYNTHESIS // ACTIVE" to HoloBlue
    }

    val animatedColor by animateColorAsState(targetValue = statusColor, label = "badge_color")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface.copy(alpha = 0.75f))
            .border(1.dp, animatedColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing LED dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = pulseAlpha))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = statusText,
                color = animatedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
