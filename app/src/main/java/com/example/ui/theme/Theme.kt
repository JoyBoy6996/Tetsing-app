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

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    background = NeutralSurfaceLight,
    onBackground = TextPrimaryLight,
    surface = NeutralSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = NeutralSurfaceContainerLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = NeutralSurfaceContainerLight,
    surfaceContainerHigh = NeutralSurfaceContainerHighLight,
    outline = OutlineLight,
    error = StatusCritical,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = Color(0xFF041E49),
    primaryContainer = Color(0xFF174EA6),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF9AA0A6),
    onSecondary = Color(0xFF1E1F23),
    background = NeutralSurfaceDark,
    onBackground = TextPrimaryDark,
    surface = NeutralSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NeutralSurfaceContainerDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = NeutralSurfaceContainerDark,
    surfaceContainerHigh = NeutralSurfaceContainerHighDark,
    outline = OutlineDark,
    error = StatusCriticalDark,
    onError = Color(0xFF410002)
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF174EA6),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF9AA0A6),
    onSecondary = Color.Black,
    background = AmoledBackground,
    onBackground = Color(0xFFE8EAED),
    surface = AmoledBackground,
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = AmoledSurfaceContainer,
    onSurfaceVariant = Color(0xFF9AA0A6),
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    outline = Color(0xFF2C2C2C),
    error = StatusCriticalDark,
    onError = Color.Black
)

@Composable
fun HyperPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    dynamicColor: Boolean = true, // Supports Material You dynamic colors on Android 12+
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        isAmoled && darkTheme -> AmoledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
