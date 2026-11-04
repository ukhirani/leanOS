package com.ukhirani.leanos.home

import kotlin.math.PI
import kotlin.math.sin

/** Where the glow behind the clock sits, as a fraction of its own size from its resting center. */
data class GlowOffset(val x: Float, val y: Float)

/** The glow's slow loop: a Lissajous drift that repeats every [PERIOD_MILLIS], still when animations are off. */
object GlowDrift {
    const val PERIOD_MILLIS = 30_000L
    private const val AMPLITUDE = 0.12f

    fun offset(elapsedMillis: Long, animationsEnabled: Boolean): GlowOffset {
        if (!animationsEnabled) return GlowOffset(0f, 0f)
        val t = 2 * PI * (elapsedMillis % PERIOD_MILLIS) / PERIOD_MILLIS
        return GlowOffset(
            x = (AMPLITUDE * sin(t)).toFloat(),
            y = (AMPLITUDE / 2 * sin(2 * t)).toFloat(),
        )
    }
}
