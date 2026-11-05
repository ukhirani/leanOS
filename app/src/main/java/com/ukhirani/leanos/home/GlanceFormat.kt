package com.ukhirani.leanos.home

import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.weather.WeatherRefresh
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

    /** "11°C", or "52°F" when switched. Whole degrees. */
    fun temperatureText(celsius: Double, settings: Settings): String =
        if (settings.useFahrenheit) "${Math.round(celsius * 9 / 5 + 32)}°F" else "${Math.round(celsius)}°C"

    /** A plain condition for a WMO weather code: "Clear", "Rain", ... */
    fun conditionText(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown"
    }

    /** "Updated 45m ago" once the weather is past the refresh age, else null. */
    fun weatherAgeText(at: Instant): String? {
        val age = Duration.between(at, clock.now().toInstant())
        return when {
            age < WeatherRefresh.MAX_AGE -> null
            age.toHours() < 1 -> "Updated ${age.toMinutes()}m ago"
            age.toDays() < 1 -> "Updated ${age.toHours()}h ago"
            else -> "Updated ${age.toDays()}d ago"
        }
    }
}
