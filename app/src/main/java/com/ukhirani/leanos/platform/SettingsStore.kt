package com.ukhirani.leanos.platform

import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class Settings(
    val use24Hour: Boolean = true,
    val clockStyle: ClockStyle = ClockStyle.CHUNKY,
    val colorSource: ColorSource = ColorSource.WALLPAPER,
    val accent: Accent = Accent.BLUE,
    val background: BackgroundMode = BackgroundMode.WALLPAPER,
    val iconSize: IconSize = IconSize.MEDIUM,
    /** Favorite apps' package names, in row order (at most 6). */
    val favorites: List<String> = emptyList(),
    /** °F instead of the default °C. */
    val useFahrenheit: Boolean = false,
    /** The city the Weather card is for; null until the user picks one. */
    val weatherCity: City? = null,
    /** The last weather fetched for [weatherCity]; null until the first fetch succeeds. */
    val weather: CurrentWeather? = null,
    /** Daily limits: package name to minutes per day. */
    val dailyLimits: Map<String, Int> = emptyMap(),
    /** App icons drawn in grayscale (less eye candy, less pull to open apps). */
    val grayscaleIcons: Boolean = true,
    /** Hidden apps' package names: kept out of the drawer, search and favorites. */
    val hidden: Set<String> = emptySet(),
    /** Daily-limit extensions: package name to when its last "5 more minutes" was granted. */
    val limitExtensions: Map<String, Instant> = emptyMap(),
    /** The first-run walkthrough has been finished or skipped through; false shows it. */
    val onboardingCompleted: Boolean = false,
)

/** Clock style presets: each one is a set of Google Sans Flex axes, never a different font. */
enum class ClockStyle(val weight: Int, val width: Float, val roundness: Float) {
    CHUNKY(weight = 900, width = 151f, roundness = 100f),
    CONDENSED(weight = 700, width = 25f, roundness = 0f),
    LIGHT(weight = 200, width = 100f, roundness = 100f),
}

enum class ColorSource { WALLPAPER, FIXED_ACCENT }

/** The fixed accents offered when the color source is [ColorSource.FIXED_ACCENT]. */
enum class Accent(val argb: Long) {
    BLUE(0xFF3D6AF2),
    GREEN(0xFF3C8A5A),
    ROSE(0xFFC4467A),
    AMBER(0xFFC07A12),
}

enum class BackgroundMode { WALLPAPER, SOLID }

enum class IconSize(val dp: Int) { SMALL(40), MEDIUM(48), LARGE(56) }

/** LeanOS settings. Reads emit the current value, then every change. */
interface SettingsStore {
    val settings: Flow<Settings>
    suspend fun setUse24Hour(enabled: Boolean)
    suspend fun setClockStyle(style: ClockStyle)
    suspend fun setColorSource(source: ColorSource)
    suspend fun setAccent(accent: Accent)
    suspend fun setBackground(mode: BackgroundMode)
    suspend fun setIconSize(size: IconSize)
    suspend fun setFavorites(packages: List<String>)
    suspend fun setUseFahrenheit(enabled: Boolean)
    /** Picking a new city (or clearing it) drops the cached weather of the old one. */
    suspend fun setWeatherCity(city: City?)
    suspend fun setWeather(weather: CurrentWeather)

    /** Sets [packageName]'s daily limit in minutes; null removes it. */
    suspend fun setDailyLimit(packageName: String, minutes: Int?)
    suspend fun setGrayscaleIcons(enabled: Boolean)
    suspend fun setHidden(packages: Set<String>)

    /** Records that [packageName] was given its daily-limit extension at [at]. */
    suspend fun setLimitExtension(packageName: String, at: Instant)

    /** Remembers that first-run setup is done; false re-runs it. */
    suspend fun setOnboardingCompleted(completed: Boolean)
}
