package com.ukhirani.leanos

import com.ukhirani.leanos.home.GlowDrift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GlowDriftTest {
    @Test
    fun `glow stays still when system animations are off`() {
        val rest = GlowDrift.offset(elapsedMillis = 0, animationsEnabled = false)
        listOf(1_000L, 7_500L, 61_000L).forEach {
            assertEquals(rest, GlowDrift.offset(it, animationsEnabled = false))
        }
    }

    @Test
    fun `glow drifts slowly and loops when animations are on`() {
        val start = GlowDrift.offset(0, animationsEnabled = true)
        assertNotEquals(start, GlowDrift.offset(5_000, animationsEnabled = true))
        // A slow loop of tens of seconds: back where it started after one period, not before 10s.
        assertTrue(GlowDrift.PERIOD_MILLIS in 10_000..60_000)
        val again = GlowDrift.offset(GlowDrift.PERIOD_MILLIS, animationsEnabled = true)
        assertEquals(start.x, again.x, 1e-4f)
        assertEquals(start.y, again.y, 1e-4f)
    }

    @Test
    fun `glow never drifts more than a small fraction of its size`() {
        (0L..GlowDrift.PERIOD_MILLIS step 250).forEach {
            val o = GlowDrift.offset(it, animationsEnabled = true)
            assertTrue(abs(o.x) <= 0.2f && abs(o.y) <= 0.2f)
        }
    }
}
