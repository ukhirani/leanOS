package com.ukhirani.leanos.home

import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.Settings
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Text for the glance chips under the clock. */
class GlanceFormat(private val clock: ClockSource, private val locale: Locale = Locale.getDefault()) {

    /** "⏰ 7:00 · in 8h 12m" for an alarm today or tomorrow, "⏰ Fri 7:00" later, null when none or past. */
    fun alarmText(at: Instant?, settings: Settings): String? {
        val now = clock.now()
        if (at == null || !at.isAfter(now.toInstant())) return null
        val ring = at.atZone(now.zone)
        val time = ring.format(DateTimeFormatter.ofPattern(if (settings.use24Hour) "H:mm" else "h:mm a", locale))
        val daysAway = ring.toLocalDate().toEpochDay() - now.toLocalDate().toEpochDay()
        if (daysAway > 1) return "⏰ ${ring.format(DateTimeFormatter.ofPattern("EEE", locale))} $time"
        return "⏰ $time · in ${durationText(Duration.between(now.toInstant(), at))}"
    }

    /** "Wed, 4 Nov" */
    fun dateText(): String = clock.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))

    /** "82%", or "⚡ 82%" while charging. */
    fun batteryText(battery: BatteryState): String =
        if (battery.charging) "⚡ ${battery.level}%" else "${battery.level}%"

    /** "1h 05m" or "12m". A started minute counts as a whole one. */
    fun durationText(duration: Duration): String {
        val minutes = (duration.toSeconds() + 59) / 60
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${"%02d".format(m)}m" else "${m}m"
    }
}
