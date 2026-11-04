package com.ukhirani.leanos.platform

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
}
