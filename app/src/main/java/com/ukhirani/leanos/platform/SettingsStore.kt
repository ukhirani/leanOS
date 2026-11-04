package com.ukhirani.leanos.platform

import kotlinx.coroutines.flow.Flow

data class Settings(
    val use24Hour: Boolean = true,
)

/** LeanOS settings. Reads emit the current value, then every change. */
interface SettingsStore {
    val settings: Flow<Settings>
    suspend fun setUse24Hour(enabled: Boolean)
}
