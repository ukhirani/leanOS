package com.ukhirani.leanos.platform

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsStore(context: Context) : SettingsStore {
    private val store = context.applicationContext.dataStore

    override val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(use24Hour = prefs[USE_24_HOUR] ?: true)
    }

    override suspend fun setUse24Hour(enabled: Boolean) {
        store.edit { it[USE_24_HOUR] = enabled }
    }

    private companion object {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
    }
}
