package com.ukhirani.leanos

import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.platform.Settings
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherFormatTest {
    private val clock = FakeClockSource("2026-11-04T12:00:00+01:00[Europe/Paris]")
    private val format = GlanceFormat(clock, Locale.US)
    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant()

    @Test
    fun `temperature is whole degrees Celsius by default`() {
        assertEquals("11°C", format.temperatureText(11.4, Settings()))
        assertEquals("-4°C", format.temperatureText(-3.6, Settings()))
        assertEquals("0°C", format.temperatureText(-0.2, Settings()))
    }

    @Test
    fun `temperature converts to Fahrenheit when switched`() {
        val f = Settings(useFahrenheit = true)
        assertEquals("32°F", format.temperatureText(0.0, f))
        assertEquals("72°F", format.temperatureText(22.0, f))
        assertEquals("-40°F", format.temperatureText(-40.0, f))
    }

    @Test
    fun `condition reads from the WMO weather code`() {
        assertEquals("Clear", format.conditionText(0))
        assertEquals("Partly cloudy", format.conditionText(2))
        assertEquals("Overcast", format.conditionText(3))
        assertEquals("Fog", format.conditionText(45))
        assertEquals("Drizzle", format.conditionText(53))
        assertEquals("Rain", format.conditionText(63))
        assertEquals("Snow", format.conditionText(75))
        assertEquals("Showers", format.conditionText(81))
        assertEquals("Thunderstorm", format.conditionText(95))
        assertEquals("Unknown", format.conditionText(42))
    }

    @Test
    fun `recent weather carries no age`() {
        assertNull(format.weatherAgeText(at("2026-11-04T11:01:00+01:00")))
    }

    @Test
    fun `weather an hour old or more says how old it is`() {
        assertEquals("Updated 1h ago", format.weatherAgeText(at("2026-11-04T11:00:00+01:00")))
        assertEquals("Updated 5h ago", format.weatherAgeText(at("2026-11-04T06:30:00+01:00")))
        assertEquals("Updated 2d ago", format.weatherAgeText(at("2026-11-02T09:00:00+01:00")))
    }
}
