package com.krata.orbit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary                = White,
    onPrimary              = Black,
    primaryContainer       = DarkGray,
    onPrimaryContainer     = White,
    secondary              = LightGray,
    onSecondary            = Black,
    tertiary               = Gray,
    onTertiary             = Black,
    background             = Black,
    onBackground           = White,
    surface                = DarkGray,
    onSurface              = White,
    surfaceVariant         = MediumGray,
    onSurfaceVariant       = LightGray,
    surfaceContainer       = Color(0xFF1A1A1A),
    surfaceContainerHigh   = Color(0xFF2C2C2C),
    outline                = Gray,
    error                  = CoralRed
)

private val LightColorScheme = lightColorScheme(
    primary                = Black,
    onPrimary              = White,
    primaryContainer       = LightGray,
    onPrimaryContainer     = Black,
    secondary              = MediumGray,
    onSecondary            = White,
    tertiary               = Gray,
    onTertiary             = White,
    background             = White,
    onBackground           = Black,
    surface                = OffWhite,
    onSurface              = Black,
    surfaceVariant         = LightGray,
    onSurfaceVariant       = MediumGray,
    surfaceContainer       = Color(0xFFF0F0F0),
    surfaceContainerHigh   = Color(0xFFE5E5E5),
    outline                = Gray,
    error                  = CoralRed
)

enum class AppTheme { SYSTEM, DARK, LIGHT }

@Composable
fun OrbitTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.DARK   -> true
        AppTheme.LIGHT  -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
