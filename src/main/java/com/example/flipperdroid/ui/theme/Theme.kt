package com.example.flipperdroid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    error = FlipperError,
    onError = FlipperInk
)

private val FlipperLightColorScheme = lightColorScheme(
    primary = FlipperOrange,
    onPrimary = FlipperLightText,
    primaryContainer = ColorCompatOrangeContainer,
    onPrimaryContainer = FlipperLightText,
    secondary = ColorCompatBlue,
    onSecondary = FlipperLightText,
    background = FlipperLightBackground,
    onBackground = FlipperLightText,
    surface = FlipperLightSurface,
    onSurface = FlipperLightText,
    surfaceVariant = FlipperLightPanel,
    onSurfaceVariant = FlipperLightText,
    outline = FlipperLine,
    error = FlipperError
)

private val ColorCompatOrangeContainer = androidx.compose.ui.graphics.Color(0xFFFFE0C7)
private val ColorCompatBlue = androidx.compose.ui.graphics.Color(0xFF2E6E9E)

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
        content = content
    )
}
