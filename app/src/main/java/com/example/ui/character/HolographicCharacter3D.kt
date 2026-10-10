package com.example.ui.character

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.HoloBlue
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloPink
import com.example.ui.theme.HoloPurple
import com.example.ui.theme.HoloTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AiVisualState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun HolographicCharacter3D(
    state: AiVisualState,
    isSpeaking: Boolean,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    useAvatarHologramMode: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle pause check for energy efficiency & 6GB RAM friendliness
    var isAppResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Avatar bitmap cache
    val avatarBitmap = remember {
        try {
            val stream = context.resources.openRawResource(R.drawable.nayra_avatar)
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    // Interactive 3D drag rotation with spring return
    var targetRotX by remember { mutableFloatStateOf(0f) }
    var targetRotY by remember { mutableFloatStateOf(0f) }
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Blinking animation controller (natural Poisson-like blinks every ~3.5s)
    val blinkAnim = remember { Animatable(0f) }
    LaunchedEffect(isAppResumed) {
        while (isActive && isAppResumed) {
            delay((2800L..4200L).random())
            // Fast blink close and open
            blinkAnim.animateTo(1f, tween(90, easing = LinearEasing))
            blinkAnim.animateTo(0f, tween(110, easing = LinearEasing))
            if ((1..4).random() == 1) { // Occasional double blink
                delay(120)
                blinkAnim.animateTo(1f, tween(80, easing = LinearEasing))
                blinkAnim.animateTo(0f, tween(100, easing = LinearEasing))
            }
        }
    }

    // Continuous ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "holo_ambient")
    val idleFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_float"
    )

    val orbitalSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_spin"
    )

    val scanlineY by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    // Hand gesture transition
    val gestureAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gesture"
    )

    val themeColor = when (state) {
        AiVisualState.IDLE -> HoloCyan
        AiVisualState.LISTENING -> HoloTeal
        AiVisualState.THINKING -> HoloPurple
        AiVisualState.SPEAKING -> HoloBlue
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        targetRotY = (targetRotY + dragAmount.x * 0.15f).coerceIn(-28f, 28f)
                        targetRotX = (targetRotX - dragAmount.y * 0.15f).coerceIn(-20f, 20f)
                        coroutineScope.launch {
                            rotY.snapTo(targetRotY)
                            rotX.snapTo(targetRotX)
                        }
                    },
                    onDragEnd = {
                        targetRotX = 0f
                        targetRotY = 0f
                        coroutineScope.launch {
                            rotX.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
                            rotY.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!isAppResumed) return@Canvas

            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Natural breathing & floating vertical sway
            val idleSwayY = sin(idleFloat) * 7f
            val idleTiltHead = sin(idleFloat * 0.7f) * 2f

            val effectiveRotY = rotY.value + idleTiltHead
            val effectiveRotX = rotX.value

            // 1. Holographic Base Pedestal (Hexagonal cyber projector pad)
            drawHolographicPedestal(
                cx = cx,
                baseY = h * 0.88f,
                radius = w * 0.38f,
                themeColor = themeColor,
                orbitalSpin = orbitalSpin
            )

            // 2. 3D Holographic Orbiting Rings (Concentric tilted elliptical rings)
            drawOrbitalHoloRings(
                cx = cx,
                cy = cy + idleSwayY,
                radius = w * 0.44f,
                spin = orbitalSpin,
                rotY = effectiveRotY,
                rotX = effectiveRotX,
                themeColor = themeColor,
                state = state,
                amplitude = audioAmplitude
            )

            // 3. Central Character Rendering (Matrix translated and tilted)
            translate(left = 0f, top = idleSwayY) {
                if (useAvatarHologramMode && avatarBitmap != null) {
                    // Enhanced Hologram Avatar Mode: 3D perspective avatar with animated features
                    drawHolographicAvatarCharacter(
                        bitmap = avatarBitmap,
                        cx = cx,
                        cy = cy,
                        w = w,
                        h = h,
                        rotX = effectiveRotX,
                        rotY = effectiveRotY,
                        themeColor = themeColor,
                        state = state,
                        isSpeaking = isSpeaking,
                        audioAmplitude = audioAmplitude,
                        blinkFactor = blinkAnim.value,
                        idleFloat = idleFloat,
                        gesturePhase = gestureAnim
                    )
                } else {
                    // Pure Procedural 3D Anime Boy Hologram (Geometric stylized mesh)
                    drawProceduralAnimeBoy(
                        cx = cx,
                        cy = cy,
                        w = w,
                        h = h,
                        rotX = effectiveRotX,
                        rotY = effectiveRotY,
                        themeColor = themeColor,
                        state = state,
                        isSpeaking = isSpeaking,
                        audioAmplitude = audioAmplitude,
                        blinkFactor = blinkAnim.value,
                        gesturePhase = gestureAnim
                    )
                }
            }

            // 4. Hologram Scanning Beam and Glitch Lines
            drawHologramScanlines(
                w = w,
                h = h,
                scanlineY = scanlineY,
                themeColor = themeColor,
                amplitude = audioAmplitude
            )

            // 5. Floating Cybernetic HUD Markers
            drawHoloTelemetryHUD(
                cx = cx,
                cy = cy,
                w = w,
                h = h,
                themeColor = themeColor,
                state = state,
                spin = orbitalSpin
            )
        }
    }
}

// -------------------------------------------------------------
// Base Pedestal
// -------------------------------------------------------------
private fun DrawScope.drawHolographicPedestal(
    cx: Float,
    baseY: Float,
    radius: Float,
    themeColor: Color,
    orbitalSpin: Float
) {
    // Holographic emitter cone / gradient
    val emitterBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            themeColor.copy(alpha = 0.08f),
            themeColor.copy(alpha = 0.25f)
        ),
        startY = baseY - 160f,
        endY = baseY
    )
    drawOval(
        brush = emitterBrush,
        topLeft = Offset(cx - radius * 0.9f, baseY - 35f),
        size = Size(radius * 1.8f, 70f)
    )

    // Concentric base rings
    drawOval(
        color = themeColor.copy(alpha = 0.45f),
        topLeft = Offset(cx - radius, baseY - 24f),
        size = Size(radius * 2f, 48f),
        style = Stroke(width = 2.5f)
    )
    drawOval(
        color = themeColor.copy(alpha = 0.7f),
        topLeft = Offset(cx - radius * 0.7f, baseY - 16f),
        size = Size(radius * 1.4f, 32f),
        style = Stroke(width = 1.8f)
    )

    // Rotating emitter notches
    rotate(degrees = orbitalSpin, pivot = Offset(cx, baseY)) {
        for (i in 0 until 6) {
            val angle = (i * 60f) * (PI / 180f).toFloat()
            val px = cx + cos(angle) * radius * 0.85f
            val py = baseY + sin(angle) * 20f
            drawCircle(
                color = themeColor,
                radius = 3.5f,
                center = Offset(px, py)
            )
        }
    }
}

// -------------------------------------------------------------
// 3D Orbital Hologram Rings
// -------------------------------------------------------------
private fun DrawScope.drawOrbitalHoloRings(
    cx: Float,
    cy: Float,
    radius: Float,
    spin: Float,
    rotY: Float,
    rotX: Float,
    themeColor: Color,
    state: AiVisualState,
    amplitude: Float
) {
    val ringAlpha = when (state) {
        AiVisualState.IDLE -> 0.35f
        AiVisualState.LISTENING -> 0.75f
        AiVisualState.THINKING -> 0.85f
        AiVisualState.SPEAKING -> 0.9f
    }

    // Outer primary orbital ring (tilted 3D ellipse)
    rotate(degrees = spin * 0.5f + rotY * 0.4f, pivot = Offset(cx, cy)) {
        scale(scaleX = 1f, scaleY = 0.42f, pivot = Offset(cx, cy)) {
            drawCircle(
                color = themeColor.copy(alpha = ringAlpha * 0.6f),
                radius = radius * (1f + amplitude * 0.08f),
                center = Offset(cx, cy),
                style = Stroke(
                    width = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(24f, 16f, 8f, 16f),
                        spin * 2f
                    )
                )
            )
        }
    }

    // Secondary counter-spinning ring
    rotate(degrees = -spin * 0.8f - rotY * 0.3f, pivot = Offset(cx, cy)) {
        scale(scaleX = 0.48f, scaleY = 1f, pivot = Offset(cx, cy)) {
            drawCircle(
                color = themeColor.copy(alpha = ringAlpha * 0.45f),
                radius = radius * 0.82f,
                center = Offset(cx, cy),
                style = Stroke(
                    width = 1.5f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(18f, 22f),
                        -spin
                    )
                )
            )
        }
    }

    // Energy pulse ring when listening/speaking
    if (state == AiVisualState.LISTENING || state == AiVisualState.SPEAKING) {
        val pulseSize = radius * (0.85f + amplitude * 0.35f)
        drawCircle(
            color = themeColor.copy(alpha = (0.25f * (1f - amplitude)).coerceAtLeast(0.05f)),
            radius = pulseSize,
            center = Offset(cx, cy),
            style = Stroke(width = 3.5f)
        )
    }
}

// -------------------------------------------------------------
// Holographic Avatar Character Mode
// -------------------------------------------------------------
private fun DrawScope.drawHolographicAvatarCharacter(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    rotX: Float,
    rotY: Float,
    themeColor: Color,
    state: AiVisualState,
    isSpeaking: Boolean,
    audioAmplitude: Float,
    blinkFactor: Float,
    idleFloat: Float,
    gesturePhase: Float
) {
    val charSize = (w * 0.82f).coerceAtMost(h * 0.68f)
    val left = cx - charSize / 2f + rotY * 1.5f
    val top = cy - charSize / 2f - rotX * 1.2f

    // 1. Holographic back glow
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            themeColor.copy(alpha = 0.35f),
            themeColor.copy(alpha = 0.12f),
            Color.Transparent
        ),
        center = Offset(cx, cy),
        radius = charSize * 0.65f
    )
    drawCircle(
        brush = glowBrush,
        radius = charSize * 0.65f,
        center = Offset(cx, cy)
    )

    // 2. Draw Hologram Frame / Container with subtle 3D parallax tilt
    rotate(degrees = rotY * 0.25f, pivot = Offset(cx, cy)) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(left.toInt(), top.toInt()),
            dstSize = IntSize(charSize.toInt(), charSize.toInt())
        )
    }

    // 3. Dynamic Eye Blinking Overlay
    // Eyes center is roughly at 44% width, 45% height within the avatar frame
    if (blinkFactor > 0.05f) {
        val eyeLeftX = left + charSize * 0.40f
        val eyeRightX = left + charSize * 0.60f
        val eyeY = top + charSize * 0.455f
        val eyeW = charSize * 0.12f
        val eyeH = charSize * 0.055f * blinkFactor

        // Eyelid skin shadow over eyes
        val eyelidColor = Color(0xFF0F182A)
        drawOval(
            color = eyelidColor,
            topLeft = Offset(eyeLeftX - eyeW / 2f, eyeY - eyeH / 2f),
            size = Size(eyeW, eyeH)
        )
        drawOval(
            color = eyelidColor,
            topLeft = Offset(eyeRightX - eyeW / 2f, eyeY - eyeH / 2f),
            size = Size(eyeW, eyeH)
        )

        // Eyelash line
        drawLine(
            color = Color(0xFF080C14),
            start = Offset(eyeLeftX - eyeW / 2f, eyeY),
            end = Offset(eyeLeftX + eyeW / 2f, eyeY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF080C14),
            start = Offset(eyeRightX - eyeW / 2f, eyeY),
            end = Offset(eyeRightX + eyeW / 2f, eyeY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // 4. Synchronized Lip-Sync Mouth Overlay
    // Mouth is positioned roughly at 50% width, 62% height of the avatar frame
    val mouthCenterX = left + charSize * 0.50f
    val mouthCenterY = top + charSize * 0.625f

    if (isSpeaking && audioAmplitude > 0.08f) {
        // Dynamic speaking mouth opening synced to speech volume & syllable rhythm
        val mouthOpenY = (charSize * 0.045f * audioAmplitude).coerceAtLeast(3f)
        val mouthWidth = charSize * 0.075f + (charSize * 0.02f * audioAmplitude)

        // Mouth cavity (dark anime interior + cyber blue glow)
        val mouthPath = Path().apply {
            moveTo(mouthCenterX - mouthWidth / 2f, mouthCenterY)
            quadraticTo(
                mouthCenterX, mouthCenterY + mouthOpenY * 1.4f,
                mouthCenterX + mouthWidth / 2f, mouthCenterY
            )
            quadraticTo(
                mouthCenterX, mouthCenterY - mouthOpenY * 0.5f,
                mouthCenterX - mouthWidth / 2f, mouthCenterY
            )
            close()
        }
        drawPath(path = mouthPath, color = Color(0xFF160A18))

        // Cyber speech energy inside mouth
        drawCircle(
            color = HoloCyan.copy(alpha = 0.55f * audioAmplitude),
            radius = mouthOpenY * 0.6f,
            center = Offset(mouthCenterX, mouthCenterY + mouthOpenY * 0.4f)
        )

        // Lip outline
        drawPath(
            path = mouthPath,
            color = Color(0xFF2D3748),
            style = Stroke(width = 2f)
        )
    }

    // 5. Hologram Cyber Gauntlet / Hand Gesture Indicator
    // Renders futuristic holographic interactive hands in front
    drawHologramGestures(
        left = left,
        top = top,
        charSize = charSize,
        state = state,
        themeColor = themeColor,
        gesturePhase = gesturePhase,
        audioAmplitude = audioAmplitude
    )
}

// -------------------------------------------------------------
// Hand Gesture Projections
// -------------------------------------------------------------
private fun DrawScope.drawHologramGestures(
    left: Float,
    top: Float,
    charSize: Float,
    state: AiVisualState,
    themeColor: Color,
    gesturePhase: Float,
    audioAmplitude: Float
) {
    val handBaseY = top + charSize * 0.88f
    val handLeftX = left + charSize * 0.22f
    val handRightX = left + charSize * 0.78f

    // Hand floating offset based on state
    val leftLift = when (state) {
        AiVisualState.LISTENING -> -18f
        AiVisualState.THINKING -> -32f
        AiVisualState.SPEAKING -> -12f + sin(gesturePhase * 2f) * 14f * audioAmplitude
        else -> sin(gesturePhase) * 6f
    }

    val rightLift = when (state) {
        AiVisualState.SPEAKING -> -24f + cos(gesturePhase * 2.2f) * 18f * audioAmplitude
        AiVisualState.THINKING -> -10f
        else -> cos(gesturePhase) * 6f
    }

    // Left cyber hand / node
    drawCyberHandNode(
        cx = handLeftX,
        cy = handBaseY + leftLift,
        isLeft = true,
        themeColor = themeColor,
        state = state
    )

    // Right cyber hand / node
    drawCyberHandNode(
        cx = handRightX,
        cy = handBaseY + rightLift,
        isLeft = false,
        themeColor = themeColor,
        state = state
    )
}

private fun DrawScope.drawCyberHandNode(
    cx: Float,
    cy: Float,
    isLeft: Boolean,
    themeColor: Color,
    state: AiVisualState
) {
    val dir = if (isLeft) 1f else -1f
    // Holographic cyber palm emitter
    drawCircle(
        color = themeColor.copy(alpha = 0.8f),
        radius = 7f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = Offset(cx, cy)
    )

    // Sleek cyber fingers / projection lines
    for (i in 0..2) {
        val fingerAngle = (if (isLeft) -30f else -150f) + i * 20f
        val rad = (fingerAngle * PI / 180f).toFloat()
        val fingerLen = 14f + i * 2f
        val fx = cx + cos(rad) * fingerLen
        val fy = cy + sin(rad) * fingerLen

        drawLine(
            color = themeColor.copy(alpha = 0.7f),
            start = Offset(cx, cy),
            end = Offset(fx, fy),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = themeColor,
            radius = 2f,
            center = Offset(fx, fy)
        )
    }

    // When thinking: Quantum data crystal above hand
    if (state == AiVisualState.THINKING && isLeft) {
        val crystalY = cy - 26f
        val crystalSize = 12f
        val path = Path().apply {
            moveTo(cx, crystalY - crystalSize)
            lineTo(cx + crystalSize * 0.8f, crystalY)
            lineTo(cx, crystalY + crystalSize)
            lineTo(cx - crystalSize * 0.8f, crystalY)
            close()
        }
        drawPath(path = path, color = HoloPurple.copy(alpha = 0.85f))
        drawPath(path = path, color = Color.White, style = Stroke(width = 1.5f))
    }
}

// -------------------------------------------------------------
// Pure Procedural 3D Anime Boy (Fallback & vector representation)
// -------------------------------------------------------------
private fun DrawScope.drawProceduralAnimeBoy(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    rotX: Float,
    rotY: Float,
    themeColor: Color,
    state: AiVisualState,
    isSpeaking: Boolean,
    audioAmplitude: Float,
    blinkFactor: Float,
    gesturePhase: Float
) {
    val scale = (w * 0.5f).coerceAtMost(h * 0.45f)
    val headY = cy - scale * 0.35f + rotX * 0.8f
    val headX = cx + rotY * 1.2f

    // 1. Futuristic Cyber Collar & Outfit (Black & Blue cyber suit)
    val chestY = headY + scale * 0.65f
    val suitBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF090D1A)),
        startY = chestY,
        endY = chestY + scale * 0.8f
    )
    val chestPath = Path().apply {
        moveTo(headX - scale * 0.55f, chestY + scale * 0.7f)
        lineTo(headX - scale * 0.28f, chestY)
        lineTo(headX + scale * 0.28f, chestY)
        lineTo(headX + scale * 0.55f, chestY + scale * 0.7f)
        close()
    }
    drawPath(path = chestPath, brush = suitBrush)

    // Cyber suit glowing blue conduits
    drawLine(
        color = HoloBlue,
        start = Offset(headX - scale * 0.25f, chestY),
        end = Offset(headX - scale * 0.45f, chestY + scale * 0.6f),
        strokeWidth = 3f
    )
    drawLine(
        color = HoloBlue,
        start = Offset(headX + scale * 0.25f, chestY),
        end = Offset(headX + scale * 0.45f, chestY + scale * 0.6f),
        strokeWidth = 3f
    )

    // Center Reactor Core
    drawCircle(
        color = themeColor,
        radius = 10f + audioAmplitude * 4f,
        center = Offset(headX, chestY + scale * 0.28f)
    )

    // High cyber collar
    val collarPath = Path().apply {
        moveTo(headX - scale * 0.24f, chestY - 10f)
        lineTo(headX - scale * 0.16f, chestY + 12f)
        lineTo(headX + scale * 0.16f, chestY + 12f)
        lineTo(headX + scale * 0.24f, chestY - 10f)
        close()
    }
    drawPath(path = collarPath, color = Color(0xFF1E293B))
    drawPath(path = collarPath, color = themeColor, style = Stroke(width = 2f))

    // 2. Anime Boy Face / Head Mesh
    val facePath = Path().apply {
        moveTo(headX - scale * 0.26f, headY - scale * 0.2f)
        quadraticTo(
            headX - scale * 0.26f, headY + scale * 0.22f,
            headX, headY + scale * 0.38f // Anime sharp jaw
        )
        quadraticTo(
            headX + scale * 0.26f, headY + scale * 0.22f,
            headX + scale * 0.26f, headY - scale * 0.2f
        )
        close()
    }
    drawPath(path = facePath, color = Color(0xFFFDEFD8)) // Anime fair skin tone

    // Headset / cyber ear nodes
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(headX - scale * 0.31f, headY - scale * 0.08f),
        size = Size(scale * 0.08f, scale * 0.18f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(headX + scale * 0.23f, headY - scale * 0.08f),
        size = Size(scale * 0.08f, scale * 0.18f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawCircle(
        color = themeColor,
        radius = 4f,
        center = Offset(headX - scale * 0.27f, headY)
    )
    drawCircle(
        color = themeColor,
        radius = 4f,
        center = Offset(headX + scale * 0.27f, headY)
    )

    // 3. Expressive Anime Eyes
    val eyeSpacing = scale * 0.11f
    val eyeW = scale * 0.11f
    val eyeH = scale * 0.08f * (1f - blinkFactor)

    if (eyeH > 2f) {
        // Left Eye
        drawOval(
            color = Color.White,
            topLeft = Offset(headX - eyeSpacing - eyeW / 2f, headY - eyeH / 2f),
            size = Size(eyeW, eyeH)
        )
        drawCircle(
            color = HoloCyan,
            radius = eyeH * 0.45f,
            center = Offset(headX - eyeSpacing + rotY * 0.1f, headY)
        )
        drawCircle(
            color = Color(0xFF061A35),
            radius = eyeH * 0.24f,
            center = Offset(headX - eyeSpacing + rotY * 0.1f, headY)
        )
        drawCircle(
            color = Color.White,
            radius = 2.5f,
            center = Offset(headX - eyeSpacing - 2f, headY - 2f)
        )

        // Right Eye
        drawOval(
            color = Color.White,
            topLeft = Offset(headX + eyeSpacing - eyeW / 2f, headY - eyeH / 2f),
            size = Size(eyeW, eyeH)
        )
        drawCircle(
            color = HoloCyan,
            radius = eyeH * 0.45f,
            center = Offset(headX + eyeSpacing + rotY * 0.1f, headY)
        )
        drawCircle(
            color = Color(0xFF061A35),
            radius = eyeH * 0.24f,
            center = Offset(headX + eyeSpacing + rotY * 0.1f, headY)
        )
        drawCircle(
            color = Color.White,
            radius = 2.5f,
            center = Offset(headX + eyeSpacing - 2f, headY - 2f)
        )
    }

    // 4. Anime Mouth
    val mouthY = headY + scale * 0.22f
    if (isSpeaking && audioAmplitude > 0.08f) {
        val openH = (scale * 0.06f * audioAmplitude).coerceAtLeast(3f)
        drawOval(
            color = Color(0xFF7A1428),
            topLeft = Offset(headX - scale * 0.045f, mouthY - openH / 2f),
            size = Size(scale * 0.09f, openH)
        )
    } else {
        drawLine(
            color = Color(0xFF334155),
            start = Offset(headX - scale * 0.035f, mouthY),
            end = Offset(headX + scale * 0.035f, mouthY),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }

    // 5. Stylish Spiky Black Anime Hair with Cyan Holographic Highlights
    val hairPath = Path().apply {
        // Back volume & crown spikes
        moveTo(headX - scale * 0.32f, headY - scale * 0.05f)
        lineTo(headX - scale * 0.42f, headY - scale * 0.35f)
        lineTo(headX - scale * 0.22f, headY - scale * 0.42f)
        lineTo(headX - scale * 0.12f, headY - scale * 0.60f) // Tall top spike
        lineTo(headX, headY - scale * 0.48f)
        lineTo(headX + scale * 0.18f, headY - scale * 0.62f) // Main hair spike
        lineTo(headX + scale * 0.26f, headY - scale * 0.42f)
        lineTo(headX + scale * 0.44f, headY - scale * 0.32f)
        lineTo(headX + scale * 0.32f, headY - scale * 0.05f)
        // Face bangs
        lineTo(headX + scale * 0.18f, headY - scale * 0.18f)
        lineTo(headX + scale * 0.08f, headY - scale * 0.02f) // Center bang
        lineTo(headX, headY - scale * 0.16f)
        lineTo(headX - scale * 0.10f, headY - scale * 0.04f) // Left center bang
        lineTo(headX - scale * 0.20f, headY - scale * 0.16f)
        close()
    }
    // Deep raven black hair
    drawPath(path = hairPath, color = Color(0xFF090D18))

    // Holographic neon blue highlight strokes on hair
    val highlightPath = Path().apply {
        moveTo(headX + scale * 0.15f, headY - scale * 0.55f)
        lineTo(headX + scale * 0.22f, headY - scale * 0.40f)
        moveTo(headX - scale * 0.10f, headY - scale * 0.52f)
        lineTo(headX - scale * 0.18f, headY - scale * 0.38f)
    }
    drawPath(path = highlightPath, color = HoloCyan.copy(alpha = 0.8f), style = Stroke(width = 3.5f))
}

// -------------------------------------------------------------
// Scanline & Glitch Effect
// -------------------------------------------------------------
private fun DrawScope.drawHologramScanlines(
    w: Float,
    h: Float,
    scanlineY: Float,
    themeColor: Color,
    amplitude: Float
) {
    val currentY = h * scanlineY
    if (currentY in 0f..h) {
        val beamBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                themeColor.copy(alpha = 0.25f + amplitude * 0.3f),
                Color.Transparent
            ),
            startY = currentY - 24f,
            endY = currentY + 24f
        )
        drawRect(
            brush = beamBrush,
            topLeft = Offset(0f, currentY - 24f),
            size = Size(w, 48f)
        )
    }

    // Subtle CRT scan grid
    for (y in 0 until h.toInt() step 14) {
        drawLine(
            color = Color.Black.copy(alpha = 0.04f),
            start = Offset(0f, y.toFloat()),
            end = Offset(w, y.toFloat()),
            strokeWidth = 1f
        )
    }
}

// -------------------------------------------------------------
// Hologram Telemetry HUD Reticles
// -------------------------------------------------------------
private fun DrawScope.drawHoloTelemetryHUD(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    themeColor: Color,
    state: AiVisualState,
    spin: Float
) {
    // Corner Tech Brackets
    val bracketLen = 22f
    val pad = 18f
    val bracketColor = themeColor.copy(alpha = 0.45f)

    // Top-Left
    drawLine(bracketColor, Offset(pad, pad), Offset(pad + bracketLen, pad), 2f)
    drawLine(bracketColor, Offset(pad, pad), Offset(pad, pad + bracketLen), 2f)

    // Top-Right
    drawLine(bracketColor, Offset(w - pad, pad), Offset(w - pad - bracketLen, pad), 2f)
    drawLine(bracketColor, Offset(w - pad, pad), Offset(w - pad, pad + bracketLen), 2f)

    // Bottom-Left
    drawLine(bracketColor, Offset(pad, h - pad), Offset(pad + bracketLen, h - pad), 2f)
    drawLine(bracketColor, Offset(pad, h - pad), Offset(pad, h - pad - bracketLen), 2f)

    // Bottom-Right
    drawLine(bracketColor, Offset(w - pad, h - pad), Offset(w - pad - bracketLen, h - pad), 2f)
    drawLine(bracketColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - bracketLen), 2f)

    // Status target reticle hovering on right
    val reticleX = w * 0.86f
    val reticleY = h * 0.28f
    rotate(degrees = spin * 1.5f, pivot = Offset(reticleX, reticleY)) {
        drawCircle(
            color = themeColor.copy(alpha = 0.35f),
            radius = 16f,
            center = Offset(reticleX, reticleY),
            style = Stroke(width = 1.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
        )
    }
    drawCircle(
        color = themeColor,
        radius = 2.5f,
        center = Offset(reticleX, reticleY)
    )
}
