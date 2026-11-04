package com.ukhirani.leanos.weather

import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.SettingsStore
import com.ukhirani.leanos.platform.WeatherSource
import java.time.Duration
import kotlinx.coroutines.flow.first

/** Keeps the cached weather fresh. Call it off the main thread when home becomes visible. */
class WeatherRefresh(
    private val source: WeatherSource,
    private val store: SettingsStore,
    private val clock: ClockSource,
) {
    /** Fetches when a city is set and the cache is missing or [MAX_AGE] old. Any failure keeps the cache as it is. */
    suspend fun refreshIfStale() {
        val settings = store.settings.first()
        val city = settings.weatherCity ?: return
        val cached = settings.weather
        if (cached != null && Duration.between(cached.at, clock.now().toInstant()) < MAX_AGE) return
        val fresh = try {
            source.current(city)
        } catch (_: Exception) {
            return // Offline, timeout or a malformed reply: the card shows the last known weather.
        }
        // The user may have picked another city while this was in flight.
        if (store.settings.first().weatherCity == city) store.setWeather(fresh)
    }

    companion object {
        val MAX_AGE: Duration = Duration.ofMinutes(30)
    }
}
