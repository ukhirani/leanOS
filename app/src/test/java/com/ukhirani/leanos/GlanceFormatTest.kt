package com.ukhirani.leanos

import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.Settings
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlanceFormatTest {
    // Wed 4 Nov 2026, 22:47:30
    private val clock = FakeClockSource("2026-11-04T22:47:30+01:00[Europe/Paris]")
    private val format = GlanceFormat(clock, Locale.US)

    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant()

    @Test
    fun `alarm tomorrow morning shows its time and the countdown`() {
        // 8h 12m 30s away: a started minute counts, so it never reads "in 0m" early.
        assertEquals(
            "⏰ 7:00 · in 8h 13m",
            format.alarmText(at("2026-11-05T07:00:00+01:00[Europe/Paris]"), Settings()),
        )
    }

    @Test
    fun `alarm later today under an hour away shows minutes only`() {
        assertEquals(
            "⏰ 23:00 · in 13m",
            format.alarmText(at("2026-11-04T23:00:00+01:00[Europe/Paris]"), Settings()),
        )
    }

    @Test
    fun `alarm countdown pads minutes after hours`() {
        clock.current = clock.current.withHour(21).withMinute(55).withSecond(0)
        assertEquals(
            "⏰ 23:00 · in 1h 05m",
            format.alarmText(at("2026-11-04T23:00:00+01:00[Europe/Paris]"), Settings()),
        )
    }

    @Test
    fun `alarm in 12-hour time shows am or pm`() {
        assertEquals(
            "⏰ 7:05 AM · in 8h 18m",
            format.alarmText(at("2026-11-05T07:05:00+01:00[Europe/Paris]"), Settings(use24Hour = false)),
        )
    }

    @Test
    fun `alarm after tomorrow shows its weekday instead of a countdown`() {
        assertEquals(
            "⏰ Fri 7:00",
            format.alarmText(at("2026-11-06T07:00:00+01:00[Europe/Paris]"), Settings()),
        )
    }

    @Test
    fun `no alarm, or one already past, shows nothing`() {
        assertNull(format.alarmText(null, Settings()))
        assertNull(format.alarmText(at("2026-11-04T22:00:00+01:00[Europe/Paris]"), Settings()))
    }

    @Test
    fun `date is compact weekday, day and month`() {
        assertEquals("Wed, 4 Nov", format.dateText())
    }

    @Test
    fun `battery shows its percentage, with a bolt while charging`() {
        assertEquals("82%", format.batteryText(BatteryState(level = 82, charging = false)))
        assertEquals("⚡ 82%", format.batteryText(BatteryState(level = 82, charging = true)))
    }

    @Test
    fun `durations read as hours and padded minutes, or minutes alone under an hour`() {
        assertEquals("1h 05m", format.durationText(Duration.ofMinutes(65)))
        assertEquals("12m", format.durationText(Duration.ofMinutes(12)))
        assertEquals("0m", format.durationText(Duration.ZERO))
    }
}
