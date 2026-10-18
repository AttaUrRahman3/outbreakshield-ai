package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClinicalLightColorScheme = lightColorScheme(
    primary = ClinicalNavy,
    onPrimary = Color.White,
    primaryContainer = ClinicalCyanContainer,
    onPrimaryContainer = ClinicalCyanText,
    secondary = ClinicalCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = ClinicalNavy,
    tertiary = SafetyHighAmber,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = SafetyCriticalRed,
    errorContainer = SafetyCriticalContainer,
    onErrorContainer = SafetyCriticalText
)

private val ClinicalDarkColorScheme = darkColorScheme(
    primary = ClinicalCyanLight,
    onPrimary = ClinicalNavyDark,
    primaryContainer = ClinicalNavyLight,
    onPrimaryContainer = Color.White,
    secondary = ClinicalCyan,
    onSecondary = Color.White,
    background = ClinicalNavyDark,
    onBackground = Color(0xFFF8FAFC),
    surface = ClinicalNavy,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = ClinicalNavyLight,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = SafetyCriticalRed,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun OutbreakShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ClinicalDarkColorScheme else ClinicalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards-compatible alias for existing tests
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OutbreakShieldTheme(darkTheme = darkTheme, content = content)
}
