package com.example.flipperdroid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ColorCompatOrangeContainer = Color(0xFFFFE0C7)
private val ColorCompatBlue = Color(0xFF2E6E9E)

private val FlipperDarkColorScheme = darkColorScheme(
    primary = FlipperOrange,
    onPrimary = FlipperInk,
    primaryContainer = FlipperPanel2,
    onPrimaryContainer = FlipperOrangeBright,
    secondary = FlipperInfo,
    onSecondary = FlipperInk,
    secondaryContainer = FlipperPanel2,
    onSecondaryContainer = FlipperText,
    tertiary = FlipperSuccess,
    onTertiary = FlipperInk,
    background = FlipperInk,
    onBackground = FlipperText,
    surface = FlipperPanel,
    onSurface = FlipperText,
    surfaceVariant = FlipperPanel2,
    onSurfaceVariant = FlipperMuted,
    outline = FlipperLine,
    outlineVariant = FlipperLine.copy(alpha = 0.55f),
    error = FlipperError,
    onError = FlipperInk
)

private val FlipperLightColorScheme = lightColorScheme(
    primary = FlipperOrange,
    onPrimary = FlipperLightText,
    primaryContainer = ColorCompatOrangeContainer,
    onPrimaryContainer = FlipperLightText,
    secondary = ColorCompatBlue,
    onSecondary = Color.White,
    background = FlipperLightBackground,
    onBackground = FlipperLightText,
    surface = FlipperLightSurface,
    onSurface = FlipperLightText,
    surfaceVariant = FlipperLightPanel,
    onSurfaceVariant = FlipperLightText.copy(alpha = 0.72f),
    outline = FlipperLine.copy(alpha = 0.45f),
    error = FlipperError
)

private val FlipperShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun FlipperDroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FlipperDarkColorScheme else FlipperLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FlipperShapes,
        content = content
    )
}
