package com.example.flipperdroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.flipperdroid.model.Accent
import com.example.flipperdroid.model.ThemeMode

private val DarkBackground = Color(0xFF07110F)
private val DarkSurface = Color(0xFF0F1A17)
private val DarkSurfaceVariant = Color(0xFF182520)
private val LightBackground = Color(0xFFF4FAF7)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFE5F0EB)
private val Error = Color(0xFFFF6B6B)

private fun accentColor(accent: Accent): Color = when (accent) {
    Accent.MINT -> Color(0xFF6FFFD3)
    Accent.CYAN -> Color(0xFF62D8FF)
    Accent.AMBER -> Color(0xFFFFC857)
}

private fun darkScheme(accent: Accent): ColorScheme {
    val primary = accentColor(accent)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF002019),
        primaryContainer = primary.copy(alpha = 0.16f),
        onPrimaryContainer = primary,
        secondary = Color(0xFFFF8A5B),
        onSecondary = Color(0xFF2B0A00),
        background = DarkBackground,
        onBackground = Color(0xFFE9F5F0),
        surface = DarkSurface,
        onSurface = Color(0xFFE9F5F0),
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = Color(0xFFB8CBC3),
        outline = Color(0xFF4D625A),
        error = Error
    )
}

private fun lightScheme(accent: Accent): ColorScheme {
    val primary = when (accent) {
        Accent.MINT -> Color(0xFF006C56)
        Accent.CYAN -> Color(0xFF006687)
        Accent.AMBER -> Color(0xFF785900)
    }
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        secondary = Color(0xFF9A4520),
        background = LightBackground,
        onBackground = Color(0xFF101714),
        surface = LightSurface,
        onSurface = Color(0xFF101714),
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = Color(0xFF43534C),
        outline = Color(0xFF73847C),
        error = Color(0xFFB3261E)
    )
}

@Composable
fun FlipperTheme(
    themeMode: ThemeMode,
    accent: Accent,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (dark) darkScheme(accent) else lightScheme(accent),
        typography = Typography(),
        content = content
    )
}
