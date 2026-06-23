package com.example.canteen.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppAccent,
    onPrimary = AppWhite,
    primaryContainer = AppSurface,
    onPrimaryContainer = AppText,
    secondary = AppMuted,
    onSecondary = AppWhite,
    secondaryContainer = AppSurface,
    onSecondaryContainer = AppText,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppBorder,
    onSurfaceVariant = AppMuted,
    outline = AppBorder,
    error = ErrorRed,
    onError = AppWhite
)

@Composable
fun CanteenTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
