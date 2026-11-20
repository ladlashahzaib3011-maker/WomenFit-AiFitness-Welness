package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkRose,
    onPrimary = Color.Black,
    primaryContainer = WomanFitDeepRose,
    onPrimaryContainer = WomanFitLightRose,
    secondary = DarkViolet,
    onSecondary = Color.Black,
    secondaryContainer = WomanFitDarkViolet,
    onSecondaryContainer = WomanFitLightViolet,
    tertiary = WomanFitCoral,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE2D9E2)
)

private val LightColorScheme = lightColorScheme(
    primary = WomanFitRose,
    onPrimary = Color.White,
    primaryContainer = WomanFitLightRose,
    onPrimaryContainer = WomanFitDeepRose,
    secondary = WomanFitViolet,
    onSecondary = Color.White,
    secondaryContainer = WomanFitLightViolet,
    onSecondaryContainer = WomanFitDarkViolet,
    tertiary = WomanFitCoral,
    background = WomanFitBlush,
    surface = White,
    surfaceVariant = Color(0xFFF9F5F8),
    onBackground = Neutral900,
    onSurface = Neutral900,
    onSurfaceVariant = Neutral600,
    outline = Color(0xFFE5D5E1)
)

@Composable
fun WomanFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep WomanFit signature brand identity by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    WomanFitTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
