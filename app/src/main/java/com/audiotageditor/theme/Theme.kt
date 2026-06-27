package com.audiotageditor.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    secondary = PrimaryPurple,
    tertiary = GlowPurple,
    background = DarkNavy,
    surface = SurfaceDark,
    onPrimary = DarkNavy,
    onSecondary = Color.White,
    onTertiary = DarkNavy,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00667B), // Rich Cyan/Teal
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5EBEF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF7B00E0), // Deep Purple
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E7FF),
    onSecondaryContainer = Color(0xFF280058),
    tertiary = Color(0xFF705575),
    onTertiary = Color.White,
    background = Color(0xFFF6F8FA), // Clean, modern off-white background
    surface = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2E5),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    error = AccentRed,
    onError = Color.White
)

@Composable
fun AudioTagEditorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
