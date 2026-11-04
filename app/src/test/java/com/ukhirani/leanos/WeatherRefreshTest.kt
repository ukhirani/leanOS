package com.ukhirani.leanos

import com.ukhirani.leanos.platform.City
import com.ukhirani.leanos.platform.CurrentWeather
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.weather.WeatherRefresh
import java.io.IOException
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherRefreshTest {
    private val paris = City("Paris, France", 48.85, 2.35)
    private val clock = FakeClockSource("2026-11-04T12:00:00+01:00[Europe/Paris]")
    private val source = FakeWeatherSource()
    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant()

    private fun refresh(settings: Settings): CurrentWeather? = runBlocking {
        val store = FakeSettingsStore(settings)
        WeatherRefresh(source, store, clock).refreshIfStale()
        store.settings.first().weather
    }

    @Test
    fun `without a city nothing is fetched`() {
        assertNull(refresh(Settings()))
        assertEquals(0, source.fetches)
    }

    @Test
    fun `first visit with a city fetches and caches current weather`() {
        source.weather = CurrentWeather(celsius = 11.4, code = 3, at = at("2026-11-04T12:00:00+01:00"))
        assertEquals(source.weather, refresh(Settings(weatherCity = paris)))
        assertEquals(listOf(paris), source.fetchedFor)
    }

    @Test
    fun `a cache under 30 minutes old is kept without fetching`() {
        val cached = CurrentWeather(9.0, 0, at("2026-11-04T11:31:00+01:00"))
        assertEquals(cached, refresh(Settings(weatherCity = paris, weather = cached)))
        assertEquals(0, source.fetches)
    }

    @Test
    fun `a cache 30 minutes old or more is refreshed`() {
        val cached = CurrentWeather(9.0, 0, at("2026-11-04T11:30:00+01:00"))
        source.weather = CurrentWeather(10.0, 2, at("2026-11-04T12:00:00+01:00"))
        assertEquals(source.weather, refresh(Settings(weatherCity = paris, weather = cached)))
    }

    @Test
    fun `offline the last known weather is kept`() {
        val cached = CurrentWeather(9.0, 0, at("2026-11-04T08:00:00+01:00"))
        source.failure = IOException("offline")
        assertEquals(cached, refresh(Settings(weatherCity = paris, weather = cached)))
    }

    @Test
    fun `a failed first fetch leaves no cache and does not throw`() {
        source.failure = IOException("offline")
        assertNull(refresh(Settings(weatherCity = paris)))
    }

    @Test
    fun `a malformed reply is treated like being offline`() {
        source.failure = IllegalStateException("bad json")
        assertNull(refresh(Settings(weatherCity = paris)))
    }
}
