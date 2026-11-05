@file:OptIn(ExperimentalTextApi::class)

package com.ukhirani.leanos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
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
    MaterialTheme(colorScheme = colors, typography = leanTypography, content = content)
}

private fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val primary = if (dark) lerp(accent, onSurface, 0.35f) else accent
    val container = lerp(accent, surface, if (dark) 0.6f else 0.75f)
    val onContainer = lerp(accent, onSurface, 0.55f)
    return copy(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        tertiary = primary,
        onTertiary = readableOn(primary),
        tertiaryContainer = lerp(container, surface, 0.35f),
        onTertiaryContainer = onContainer,
    )
}

/** Whichever of this scheme's surface and onSurface contrasts more with [background]: the text color to put on it. */
private fun ColorScheme.readableOn(background: Color): Color {
    fun contrast(a: Color, b: Color) = (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)
    return if (contrast(background, surface) >= contrast(background, onSurface)) surface else onSurface
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

@Composable
fun rememberClockFontFamily(style: ClockStyle): FontFamily = remember(style) { clockFontFamily(style) }

/** The Material type scale, unchanged in size/weight/line height, but all in Google Sans; Flex is for the clock only. */
private val leanTypography: Typography = Typography().let { base ->
    fun TextStyle.sans(): TextStyle {
        val w = fontWeight ?: FontWeight.Normal
        return copy(
            fontFamily = FontFamily(
                Font(R.font.google_sans, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight))),
            ),
        )
    }
    return@let base.copy(
        displayLarge = base.displayLarge.sans(), displayMedium = base.displayMedium.sans(), displaySmall = base.displaySmall.sans(),
        headlineLarge = base.headlineLarge.sans(), headlineMedium = base.headlineMedium.sans(), headlineSmall = base.headlineSmall.sans(),
        titleLarge = base.titleLarge.sans(), titleMedium = base.titleMedium.sans(), titleSmall = base.titleSmall.sans(),
        bodyLarge = base.bodyLarge.sans(), bodyMedium = base.bodyMedium.sans(), bodySmall = base.bodySmall.sans(),
        labelLarge = base.labelLarge.sans(), labelMedium = base.labelMedium.sans(), labelSmall = base.labelSmall.sans(),
    )
}
