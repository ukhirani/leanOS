@file:OptIn(ExperimentalTextApi::class)

package com.ukhirani.leanos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ukhirani.leanos.R
import com.ukhirani.leanos.platform.ClockStyle
import com.ukhirani.leanos.platform.ColorSource
import com.ukhirani.leanos.platform.Settings

/**
 * Material You dynamic color (minSdk 31 always has it), following system light/dark.
 * With a fixed accent, the accent replaces the wallpaper's primary and tertiary roles.
 */
@Composable
fun LeanTheme(settings: Settings = Settings(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val colors = when (settings.colorSource) {
        ColorSource.WALLPAPER -> dynamic
        ColorSource.FIXED_ACCENT -> dynamic.withAccent(Color(settings.accent.argb), dark)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val primary = if (dark) lerp(accent, Color.White, 0.35f) else accent
    val container = lerp(accent, surface, if (dark) 0.6f else 0.75f)
    val onContainer = lerp(accent, onSurface, 0.55f)
    return copy(
        primary = primary,
        onPrimary = if (dark) Color.Black else Color.White,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        tertiary = primary,
        tertiaryContainer = lerp(container, surface, 0.35f),
        onTertiaryContainer = onContainer,
    )
}

/** Google Sans Flex set to a clock preset's axes. The font never changes, only its axes. */
fun clockFontFamily(style: ClockStyle): FontFamily = FontFamily(
    Font(
        R.font.google_sans_flex,
        weight = FontWeight(style.weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(style.weight),
            FontVariation.width(style.width),
            FontVariation.Setting("ROND", style.roundness),
            FontVariation.Setting("opsz", 144f),
        ),
    ),
)

/** Google Sans Flex at its heaviest, widest, fully rounded setting: the default clock face. */
val ClockFontFamily = clockFontFamily(ClockStyle.CHUNKY)

@Composable
fun rememberClockFontFamily(style: ClockStyle): FontFamily = remember(style) { clockFontFamily(style) }
