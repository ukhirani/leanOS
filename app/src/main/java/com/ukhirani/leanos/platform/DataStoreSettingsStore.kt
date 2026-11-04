package com.ukhirani.leanos.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsStore internal constructor(private val store: DataStore<Preferences>) : SettingsStore {
    constructor(context: Context) : this(context.applicationContext.dataStore)

    override val settings: Flow<Settings> = store.data.map { prefs ->
        val defaults = Settings()
        Settings(
            use24Hour = prefs[USE_24_HOUR] ?: defaults.use24Hour,
            clockStyle = prefs.enum(CLOCK_STYLE, defaults.clockStyle),
            colorSource = prefs.enum(COLOR_SOURCE, defaults.colorSource),
            accent = prefs.enum(ACCENT, defaults.accent),
            background = prefs.enum(BACKGROUND, defaults.background),
            iconSize = prefs.enum(ICON_SIZE, defaults.iconSize),
            // Package names can't contain newlines, so one string keeps the order.
            favorites = prefs[FAVORITES]?.split('\n')?.filter { it.isNotEmpty() }.orEmpty(),
            useFahrenheit = prefs[USE_FAHRENHEIT] ?: defaults.useFahrenheit,
            weatherCity = prefs.weatherCity(),
            weather = prefs.weather(),
        )
    }

    override suspend fun setUse24Hour(enabled: Boolean) {
        store.edit { it[USE_24_HOUR] = enabled }
    }

    override suspend fun setClockStyle(style: ClockStyle) = put(CLOCK_STYLE, style)
    override suspend fun setColorSource(source: ColorSource) = put(COLOR_SOURCE, source)
    override suspend fun setAccent(accent: Accent) = put(ACCENT, accent)
    override suspend fun setBackground(mode: BackgroundMode) = put(BACKGROUND, mode)
    override suspend fun setIconSize(size: IconSize) = put(ICON_SIZE, size)

    override suspend fun setFavorites(packages: List<String>) {
        store.edit { it[FAVORITES] = packages.joinToString("\n") }
    }

    override suspend fun setUseFahrenheit(enabled: Boolean) {
        store.edit { it[USE_FAHRENHEIT] = enabled }
    }

    // City and weather are stored as newline-joined fields; a city name never contains a newline.
    override suspend fun setWeatherCity(city: City?) {
        store.edit {
            if (city == null) it.remove(WEATHER_CITY) else it[WEATHER_CITY] = "${city.name}\n${city.latitude}\n${city.longitude}"
            it.remove(WEATHER)
        }
    }

    override suspend fun setWeather(weather: CurrentWeather) {
        store.edit { it[WEATHER] = "${weather.celsius}\n${weather.code}\n${weather.at.toEpochMilli()}" }
    }

    private fun Preferences.weatherCity(): City? = this[WEATHER_CITY]?.split('\n')?.let { f ->
        val lat = f.getOrNull(1)?.toDoubleOrNull()
        val lon = f.getOrNull(2)?.toDoubleOrNull()
        if (lat == null || lon == null) null else City(f[0], lat, lon)
    }

    private fun Preferences.weather(): CurrentWeather? = this[WEATHER]?.split('\n')?.let { f ->
        val celsius = f.getOrNull(0)?.toDoubleOrNull()
        val code = f.getOrNull(1)?.toIntOrNull()
        val at = f.getOrNull(2)?.toLongOrNull()
        if (celsius == null || code == null || at == null) null else CurrentWeather(celsius, code, Instant.ofEpochMilli(at))
    }

    // Enums are stored by name; a name this version doesn't know reads as the default.
    private suspend fun put(key: Preferences.Key<String>, value: Enum<*>) {
        store.edit { it[key] = value.name }
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val COLOR_SOURCE = stringPreferencesKey("color_source")
        val ACCENT = stringPreferencesKey("accent")
        val BACKGROUND = stringPreferencesKey("background")
        val ICON_SIZE = stringPreferencesKey("icon_size")
        val FAVORITES = stringPreferencesKey("favorites")
        val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER = stringPreferencesKey("weather")
    }
}
