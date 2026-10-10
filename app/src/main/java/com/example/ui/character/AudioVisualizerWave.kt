package com.example.ui.character

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HoloBlue
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloPurple
import kotlin.random.Random

@Composable
fun AudioVisualizerWave(
    isActive: Boolean,
    amplitude: Float,
    barCount: Int = 28,
    modifier: Modifier = Modifier,
    activeColor: Color = HoloCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barWidth = (width / barCount) * 0.55f
        val spacing = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        val brush = Brush.verticalGradient(
            colors = listOf(
                activeColor,
                HoloBlue,
                activeColor.copy(alpha = 0.3f)
            )
        )

        for (i in 0 until barCount) {
            val normX = i.toFloat() / barCount
            // Harmonic wave modulated by amplitude
            val waveHeightFactor = if (isActive) {
                val sin1 = kotlin.math.sin(normX * 12.0 + phase)
                val sin2 = kotlin.math.cos(normX * 6.0 - phase * 0.7)
                val normalizedWave = ((sin1 + sin2) / 2.0).toFloat().coerceIn(-1f, 1f)
                val base = 0.2f + 0.8f * kotlin.math.abs(normalizedWave)
                (base * (0.35f + amplitude * 0.65f)).coerceIn(0.12f, 1f)
            } else {
                0.08f + 0.04f * kotlin.math.sin(normX * 6.28 + phase).toFloat()
            }

            val barHeight = (height * 0.85f * waveHeightFactor).coerceAtLeast(4f)
            val left = i * (barWidth + spacing)
            val top = centerY - (barHeight / 2f)

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Neon reflection point
            if (isActive && waveHeightFactor > 0.4f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f * waveHeightFactor),
                    radius = barWidth * 0.35f,
                    center = Offset(left + barWidth / 2f, top)
                )
            }
        }
    }
}
