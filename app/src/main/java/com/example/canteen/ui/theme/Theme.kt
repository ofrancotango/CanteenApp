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

private val LightColorScheme = lightColorScheme(
    primary = MBlack,
    onPrimary = MWhite,
    primaryContainer = MLightGray,
    onPrimaryContainer = MBlack,
    secondary = MGray,
    onSecondary = MWhite,
    secondaryContainer = MLightGray,
    onSecondaryContainer = MBlack,
    background = MWhite,
    onBackground = MBlack,
    surface = MWhite,
    onSurface = MBlack,
    surfaceVariant = MLightGray,
    onSurfaceVariant = MGray,
    outline = MOutline,
    error = ErrorRed,
    onError = MWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = MWhite,
    onPrimary = MBlack,
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = MWhite,
    secondary = Color(0xFFAAAAAA),
    onSecondary = MBlack,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = MWhite,
    background = Color(0xFF111111),
    onBackground = MWhite,
    surface = Color(0xFF1C1C1E),
    onSurface = MWhite,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF3A3A3C),
    error = ErrorRed,
    onError = MWhite
)

@Composable
fun CanteenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
