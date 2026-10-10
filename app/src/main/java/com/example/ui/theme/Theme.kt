package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NayraDarkColorScheme = darkColorScheme(
    primary = HoloCyan,
    onPrimary = CyberVoid,
    primaryContainer = HoloBlueDeep,
    onPrimaryContainer = HoloCyan,
    secondary = HoloBlue,
    onSecondary = Color.White,
    secondaryContainer = CyberSurfaceElevated,
    onSecondaryContainer = HoloTeal,
    tertiary = HoloPurple,
    onTertiary = Color.White,
    background = CyberVoid,
    onBackground = TextHoloPrimary,
    surface = CyberDark,
    onSurface = TextHoloPrimary,
    surfaceVariant = CyberSurface,
    onSurfaceVariant = TextHoloSecondary,
    outline = CyberSurfaceBorder,
    error = HoloPink,
    onError = Color.White
)

@Composable
fun NayraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NayraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
