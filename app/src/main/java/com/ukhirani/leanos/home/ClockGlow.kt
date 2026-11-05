package com.ukhirani.leanos.home

import android.provider.Settings.Global
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** A soft primary/tertiary container glow that drifts behind the clock; still when animations are off. */
@Composable
fun ClockGlow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animationsEnabled = remember {
        Global.getFloat(context.contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val elapsed = produceState(0L, animationsEnabled) {
        if (!animationsEnabled) return@produceState
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = it - start }
    }
    val primary = MaterialTheme.colorScheme.primaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer
    Box(
        modifier.drawBehind {
            // Read the frame time only while drawing, so the drift redraws without recomposing.
            val o = GlowDrift.offset(elapsed.value, animationsEnabled)
            val r = size.minDimension * 0.5f
            fun blob(color: Color, cx: Float, cy: Float) {
                val center = Offset(cx, cy)
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent), center, r),
                    radius = r,
                    center = center,
                )
            }
            blob(primary, size.width * (0.4f + o.x), size.height * (0.5f + o.y))
            blob(tertiary, size.width * (0.6f - o.x), size.height * (0.5f - o.y))
        },
    )
}
