package com.tagdeck.theme

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
    onPrimary = DarkNavy,
    primaryContainer = Color(0xFF004F5E),
    onPrimaryContainer = Color(0xFFACEEFF),
    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0083),
    onSecondaryContainer = Color(0xFFEDD9FF),
    tertiary = GlowPurple,
    onTertiary = DarkNavy,
    tertiaryContainer = Color(0xFF573E5C),
    onTertiaryContainer = Color(0xFFEDD9FF),
    background = DarkNavy,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Color(0xFF030508),
    surfaceContainerLow = Color(0xFF0F1319),
    surfaceContainer = Color(0xFF131820),
    surfaceContainerHigh = Color(0xFF1D2330),
    surfaceContainerHighest = Color(0xFF272F3E),
    outline = CardBorder,
    outlineVariant = Color(0xFF3A4860),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E9F5),
    inverseOnSurface = Color(0xFF2D3340),
    inversePrimary = Color(0xFF006779)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00667B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB5EBEF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF7B00E0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E7FF),
    onSecondaryContainer = Color(0xFF280058),
    tertiary = Color(0xFF705575),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2DDF7),
    onTertiaryContainer = Color(0xFF25162F),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2E5),
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainer = Color(0xFFEAEEF2),
    surfaceContainerHigh = Color(0xFFE4E8EC),
    surfaceContainerHighest = Color(0xFFDEE2E6),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7CF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFF0F0F4),
    inversePrimary = Color(0xFF4DD9F0)
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
