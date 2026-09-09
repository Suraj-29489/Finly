package com.personalexpensetracker.ui.theme

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
import com.personalexpensetracker.domain.model.AppThemeMode

// ============================================================================
// FINLY COLOR SCHEMES
// ============================================================================

private val DarkColorScheme = darkColorScheme(
    primary = FinlyPurpleLight,
    onPrimary = Color.White,
    primaryContainer = FinlyPurpleDark,
    onPrimaryContainer = FinlyPurpleContainer,
    secondary = FinlyPurple,
    onSecondary = Color.White,
    secondaryContainer = FinlySurfaceSubtleDark,
    onSecondaryContainer = FinlyTextPrimaryDark,
    tertiary = FinlyGreen,
    onTertiary = Color.White,
    background = FinlyBackgroundDark,
    onBackground = FinlyTextPrimaryDark,
    surface = FinlySurfaceDark,
    onSurface = FinlyTextPrimaryDark,
    surfaceVariant = FinlySurfaceSubtleDark,
    onSurfaceVariant = FinlyTextSecondaryDark,
    outline = FinlyBorderDark,
    outlineVariant = FinlyBorderDark.copy(alpha = 0.5f),
    error = FinlyRed,
    onError = Color.White,
    errorContainer = FinlyRedContainer,
    onErrorContainer = FinlyOnRedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = FinlyPurple,
    onPrimary = Color.White,
    primaryContainer = FinlyPurpleContainer,
    onPrimaryContainer = FinlyOnPurpleContainer,
    secondary = FinlyPurpleLight,
    onSecondary = Color.White,
    secondaryContainer = FinlySurfaceSubtle,
    onSecondaryContainer = FinlyTextPrimary,
    tertiary = FinlyGreen,
    onTertiary = Color.White,
    background = FinlyBackgroundLight,
    onBackground = FinlyTextPrimary,
    surface = FinlySurfaceLight,
    onSurface = FinlyTextPrimary,
    surfaceVariant = FinlySurfaceSubtle,
    onSurfaceVariant = FinlyTextSecondary,
    outline = FinlyBorderStrong,
    outlineVariant = FinlyBorderLight,
    error = FinlyRed,
    onError = Color.White,
    errorContainer = FinlyRedContainer,
    onErrorContainer = FinlyOnRedContainer
)

@Composable
fun ExpenseTrackerTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
        shapes = FinlyShapes,
        content = content
    )
}

