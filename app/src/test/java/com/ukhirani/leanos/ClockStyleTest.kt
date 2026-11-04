package com.ukhirani.leanos

import com.ukhirani.leanos.platform.ClockStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockStyleTest {
    @Test
    fun `every preset is a distinct set of axes within Google Sans Flex's ranges`() {
        val axes = ClockStyle.entries.map { Triple(it.weight, it.width, it.roundness) }
        assertEquals(axes.size, axes.toSet().size)
        ClockStyle.entries.forEach {
            assertTrue(it.name, it.weight in 100..1000)
            assertTrue(it.name, it.width in 25f..151f)
            assertTrue(it.name, it.roundness in 0f..100f)
        }
    }

    @Test
    fun `chunky is the default clock face, heavy wide and fully rounded`() {
        assertEquals(ClockStyle.CHUNKY, com.ukhirani.leanos.platform.Settings().clockStyle)
        assertEquals(Triple(900, 151f, 100f), ClockStyle.CHUNKY.let { Triple(it.weight, it.width, it.roundness) })
    }
}
