package com.padelaragon.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = NavyDark,
    primaryContainer = BlueLight,
    onPrimaryContainer = NavyDark,
    secondary = BlueSecondary,
    onSecondary = NavyDark,
    secondaryContainer = BlueLighter,
    onSecondaryContainer = NavyMedium,
    tertiary = BlueTertiary,
    onTertiary = PureWhite,
    tertiaryContainer = BlueLightest,
    onTertiaryContainer = NavyDark,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    background = PureWhite,
    onBackground = NavyDark,
    surface = BlueSurface,
    onSurface = NavyDark,
    surfaceVariant = BlueSecondary,
    onSurfaceVariant = NavyMedium,
    outline = NavySoft
)

@Composable
fun PadelAragonTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = LightColorScheme.primaryContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
