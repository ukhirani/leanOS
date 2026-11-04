package com.ukhirani.leanos.platform

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsStore(context: Context) : SettingsStore {
    private val store = context.applicationContext.dataStore

    override val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            use24Hour = prefs[USE_24_HOUR] ?: true,
            // Package names can't contain newlines, so one string keeps the order.
            favorites = prefs[FAVORITES]?.split('\n')?.filter { it.isNotEmpty() }.orEmpty(),
        )
    }

    override suspend fun setUse24Hour(enabled: Boolean) {
        store.edit { it[USE_24_HOUR] = enabled }
    }

    override suspend fun setFavorites(packages: List<String>) {
        store.edit { it[FAVORITES] = packages.joinToString("\n") }
    }

    private companion object {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val FAVORITES = stringPreferencesKey("favorites")
    }
}
