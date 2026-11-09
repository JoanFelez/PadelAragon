package com.padelaragon.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = PureWhite,
    primaryContainer = RedLighter,
    onPrimaryContainer = PureBlack,
    secondary = CharcoalDark,
    onSecondary = PureWhite,
    secondaryContainer = OffWhite,
    onSecondaryContainer = CharcoalDark,
    tertiary = RedPrimaryDark,
    onTertiary = PureWhite,
    tertiaryContainer = RedLightest,
    onTertiaryContainer = PureBlack,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = OffWhite,
    onSurfaceVariant = CharcoalMedium,
    outline = GraySoftOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B6B),
    onPrimary = Color(0xFF3B0000),
    primaryContainer = Color(0xFF5C1A1A),
    onPrimaryContainer = Color(0xFFFFE9E7),
    secondary = Color(0xFFD6D6D6),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFF0F0F0),
    tertiary = Color(0xFFFFB4AB),
    onTertiary = Color(0xFF410002),
    tertiaryContainer = Color(0xFF7F1D1D),
    onTertiaryContainer = Color(0xFFFFDAD6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF8A8A8A)
)

/** Android 12+ wallpaper-based scheme; null where unsupported (older Android, desktop). */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** Syncs platform chrome (Android status-bar icon contrast) with the active theme. No-op on desktop. */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)

@Composable
fun PadelAragonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = (if (useDynamicColor) dynamicColorScheme(darkTheme) else null)
        ?: if (darkTheme) DarkColorScheme else LightColorScheme

    SystemBarsEffect(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
