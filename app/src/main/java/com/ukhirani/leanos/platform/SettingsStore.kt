package com.ukhirani.leanos.platform

import kotlinx.coroutines.flow.Flow

data class Settings(
    val use24Hour: Boolean = true,
    /** Favorite apps' package names, in row order (at most 6). */
    val favorites: List<String> = emptyList(),
)

/** LeanOS settings. Reads emit the current value, then every change. */
interface SettingsStore {
    val settings: Flow<Settings>
    suspend fun setUse24Hour(enabled: Boolean)
    suspend fun setFavorites(packages: List<String>)
}
