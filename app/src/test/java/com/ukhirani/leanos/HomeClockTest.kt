package com.ukhirani.leanos

import com.ukhirani.leanos.home.HomeClock
import com.ukhirani.leanos.platform.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeClockTest {
    private val clock = FakeClockSource("2026-11-04T13:05:42+01:00[Europe/Paris]")
    private val homeClock = HomeClock(clock)

    @Test
    fun `shows 24-hour time by default`() {
        assertEquals("13:05", homeClock.text(Settings()))
    }

    @Test
    fun `shows 12-hour time without a leading zero when 24-hour is off`() {
        assertEquals("1:05", homeClock.text(Settings(use24Hour = false)))
    }

    @Test
    fun `switching off 24-hour in settings switches the clock to 12-hour`() = runBlocking {
        val store = FakeSettingsStore()
        assertEquals("13:05", homeClock.text(store.settings.first()))
        store.setUse24Hour(false)
        assertEquals("1:05", homeClock.text(store.settings.first()))
    }

    @Test
    fun `next update is due exactly at the start of the next minute`() {
        // 13:05:42.000 -> 13:06:00.000
        assertEquals(18_000L, homeClock.millisUntilNextMinute())
        clock.current = clock.current.withSecond(59).withNano(999_000_000)
        assertEquals(1L, homeClock.millisUntilNextMinute())
        clock.current = clock.current.withSecond(0).withNano(0)
        assertEquals(60_000L, homeClock.millisUntilNextMinute())
    }

    @Test
    fun `shows midnight as 12 in 12-hour time`() {
        clock.current = clock.current.withHour(0)
        assertEquals("00:05", homeClock.text(Settings()))
        assertEquals("12:05", homeClock.text(Settings(use24Hour = false)))
    }
}
