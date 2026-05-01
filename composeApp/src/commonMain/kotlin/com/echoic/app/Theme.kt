package com.echoic.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors (from theme.css)
private val LightBackground = Color(0xFFFFFFFF)
private val LightForeground = Color(0xFF1C1917)
private val LightCard = Color(0xFFFFFFFF)
private val LightMuted = Color(0xFFF5F5F4)
private val LightMutedFg = Color(0xFF78716C)
private val LightPrimary = Color(0xFF1C1917)
private val LightPrimaryFg = Color(0xFFFAFAF9)
private val LightSecondary = Color(0xFFF5F5F4)
private val LightSecondaryFg = Color(0xFF1C1917)
private val LightAccent = Color(0xFFF5F5F4)
private val LightAccentFg = Color(0xFF1C1917)
private val LightBorder = Color(0xFFE7E5E4)
private val LightInput = Color(0xFFE7E5E4)
private val LightRing = Color(0xFFA8A29E)
private val LightDestructive = Color(0xFFDC2626)

// Dark theme colors (from theme.css)
private val DarkBackground = Color(0xFF0C0A09)
private val DarkForeground = Color(0xFFFAFAF9)
private val DarkCard = Color(0xFF1C1917)
private val DarkMuted = Color(0xFF292524)
private val DarkMutedFg = Color(0xFFA8A29E)
private val DarkPrimary = Color(0xFFE7E5E4)
private val DarkPrimaryFg = Color(0xFF1C1917)
private val DarkSecondary = Color(0xFF292524)
private val DarkSecondaryFg = Color(0xFFFAFAF9)
private val DarkAccent = Color(0xFF292524)
private val DarkAccentFg = Color(0xFFFAFAF9)
private val DarkBorder = Color(0x1AFFFFFF)
private val DarkInput = Color(0x26FFFFFF)
private val DarkRing = Color(0xFF78716C)
private val DarkDestructive = Color(0xFFEF4444)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedFg,
    primary = LightPrimary,
    onPrimary = LightPrimaryFg,
    secondary = LightSecondary,
    onSecondary = LightSecondaryFg,
    tertiary = LightAccent,
    onTertiary = LightAccentFg,
    outline = LightBorder,
    outlineVariant = LightInput,
    error = LightDestructive,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedFg,
    primary = DarkPrimary,
    onPrimary = DarkPrimaryFg,
    secondary = DarkSecondary,
    onSecondary = DarkSecondaryFg,
    tertiary = DarkAccent,
    onTertiary = DarkAccentFg,
    outline = DarkBorder,
    outlineVariant = DarkInput,
    error = DarkDestructive,
    onError = Color.White,
)

@Composable
fun EchoicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
