package com.baithebook.garcia.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =========================================================================
// Apple Calculator & Raena AI Minimalist Color Palette
// =========================================================================

// Apple Calculator Iconic Amber / Orange Accent
val AppleCalculatorAmber = Color(0xFFFF9F0A)
val AppleCalculatorAmberLight = Color(0xFFFFB340)
val AppleCalculatorAmberContainer = Color(0x33FF9F0A)

// Apple / Raena AI Minimalist Slate & Electric Accents
val AppleSystemBlue = Color(0xFF0A84FF)
val AppleSystemBlueContainer = Color(0x260A84FF)
val AppleSystemGreen = Color(0xFF30D158)
val AppleSystemRed = Color(0xFFFF453A)
val AppleSystemRedContainer = Color(0x33FF453A)

// Dark Theme: Pure OLED Pitch Black & Slate
val OledBlack = Color(0xFF000000)                // Calculator Pure Pitch Black
val OledCardSurface = Color(0xFF1C1C1E)          // Calculator Function Key Dark Gray
val OledElevatedSurface = Color(0xFF2C2C2E)      // Calculator Secondary Surface
val OledOutline = Color(0xFF38383A)              // Subtle Divider/Border
val OledTextPrimary = Color(0xFFFFFFFF)          // Crisp White
val OledTextSecondary = Color(0xFF8E8E93)        // Muted Gray
val OledTextMuted = Color(0xFF636366)

// Light Theme: Minimalist Studio Light
val LightCanvas = Color(0xFFF2F2F7)              // Clean iOS System Gray 6
val LightSurface = Color(0xFFFFFFFF)             // Pure White
val LightElevatedSurface = Color(0xFFE5E5EA)     // Surface Highlight
val LightOutline = Color(0xFFD1D1D6)
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF6C6C70)

val DarkColorScheme = darkColorScheme(
    primary = AppleCalculatorAmber,
    onPrimary = Color.Black,
    primaryContainer = AppleCalculatorAmberContainer,
    onPrimaryContainer = AppleCalculatorAmberLight,
    secondary = AppleSystemBlue,
    onSecondary = Color.White,
    secondaryContainer = AppleSystemBlueContainer,
    onSecondaryContainer = AppleSystemBlue,
    tertiary = Color(0xFFBF5AF2),
    onTertiary = Color.White,
    background = OledBlack,
    onBackground = OledTextPrimary,
    surface = OledCardSurface,
    onSurface = OledTextPrimary,
    surfaceVariant = OledElevatedSurface,
    onSurfaceVariant = OledTextSecondary,
    outline = OledOutline,
    outlineVariant = Color(0xFF2C2C2E),
    error = AppleSystemRed,
    onError = Color.White,
    errorContainer = AppleSystemRedContainer,
    onErrorContainer = AppleSystemRed
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5BE),
    onPrimaryContainer = Color(0xFF8C5300),
    secondary = Color(0xFF007AFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F0FF),
    onSecondaryContainer = Color(0xFF0051A8),
    tertiary = Color(0xFFAF52DE),
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevatedSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9D7),
    onErrorContainer = Color(0xFF9E1B15)
)
