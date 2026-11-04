package com.ukhirani.leanos.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
            dailyLimits = prefs.dailyLimits(),
        )
    }

    // One "package=minutes" line per limit.
    private fun Preferences.dailyLimits(): Map<String, Int> =
        this[DAILY_LIMITS].orEmpty().lines().mapNotNull { line ->
            val (pkg, minutes) = line.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            minutes.toIntOrNull()?.let { pkg to it }
        }.toMap()

    override suspend fun setDailyLimit(packageName: String, minutes: Int?) {
        store.edit { prefs ->
            val limits = prefs.dailyLimits().toMutableMap()
            if (minutes == null) limits.remove(packageName) else limits[packageName] = minutes
            prefs[DAILY_LIMITS] = limits.entries.joinToString("\n") { "${it.key}=${it.value}" }
        }
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
        val DAILY_LIMITS = stringPreferencesKey("daily_limits")
    }
}
