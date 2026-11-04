@file:OptIn(ExperimentalTextApi::class)

package com.ukhirani.leanos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ukhirani.leanos.R

/** Material You dynamic color (minSdk 31 always has it), following system light/dark. */
@Composable
fun LeanTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colors, content = content)
}

/** Google Sans Flex at its heaviest, widest, fully rounded setting: the clock's face. */
val ClockFontFamily = FontFamily(
    Font(
        R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(151f),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 144f),
        ),
    ),
)
