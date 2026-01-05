package com.vidora.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Rich Black & Purple Theme - Premium Color Palette
private val PrimaryPurple = Color(0xFF8B5CF6)      // Deep Violet - Primary brand
private val AccentPurple = Color(0xFFA78BFA)       // Bright Purple - Accents
private val PurpleGlow = Color(0xFFC4B5FD)         // Light Purple - Hover states
private val DeepViolet = Color(0xFF7C3AED)         // Dark Purple - Active states

private val RichBlack = Color(0xFF0A0A0A)          // Background - Deep black
private val DarkSurface = Color(0xFF1A1A1A)        // Cards/Surfaces
private val DarkerSurface = Color(0xFF141414)      // Elevated surfaces

private val PureWhite = Color(0xFFFFFFFF)          // Primary text
private val LightGray = Color(0xFFE5E5E5)          // Secondary text
private val MediumGray = Color(0xFF737373)         // Disabled text

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = AccentPurple,
    tertiary = PurpleGlow,
    
    background = RichBlack,
    surface = DarkSurface,
    surfaceVariant = DarkerSurface,
    
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onBackground = PureWhite,
    onSurface = PureWhite,
    
    primaryContainer = DeepViolet,
    onPrimaryContainer = PureWhite,
    
    secondaryContainer = DarkSurface,
    onSecondaryContainer = LightGray,
    
    error = Color(0xFFEF4444),
    onError = PureWhite
)

@Composable
fun VidoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
