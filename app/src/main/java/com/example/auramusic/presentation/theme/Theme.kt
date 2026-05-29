package com.example.auramusic.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DeepViolet,
    onPrimary = TextPrimaryDark,
    primaryContainer = DeepViolet.copy(alpha = 0.2f),
    onPrimaryContainer = TextPrimaryDark,

    secondary = NeonCyan,
    onSecondary = BackgroundDark,
    secondaryContainer = NeonCyan.copy(alpha = 0.2f),
    onSecondaryContainer = NeonCyan,

    tertiary = SoftPink,
    onTertiary = Color.White,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorRed,
    onError = Color.White,
    outline = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = DeepViolet,
    onPrimary = Color.White,
    primaryContainer = DeepViolet.copy(alpha = 0.1f),
    onPrimaryContainer = DeepViolet,

    secondary = NeonCyan,
    onSecondary = BackgroundLight,
    secondaryContainer = NeonCyan.copy(alpha = 0.1f),
    onSecondaryContainer = NeonCyan,

    tertiary = SoftPink,
    onTertiary = Color.White,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorRed,
    onError = Color.White,
    outline = TextSecondaryLight
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun AuraMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
