package com.ukhirani.leanos

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ukhirani.leanos.platform.Accent
import com.ukhirani.leanos.platform.BackgroundMode
import com.ukhirani.leanos.platform.ClockStyle
import com.ukhirani.leanos.platform.ColorSource
import com.ukhirani.leanos.platform.DataStoreSettingsStore
import com.ukhirani.leanos.platform.IconSize
import com.ukhirani.leanos.platform.Settings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scopes = mutableListOf<CoroutineScope>()

    private fun dataStore(file: File) = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it },
        produceFile = { file },
    )

    @After fun closeStores() = scopes.forEach { it.cancel() }

    @Test
    fun `a fresh install has the default look`() = runBlocking {
        val store = DataStoreSettingsStore(dataStore(File(tmp.root, "a.preferences_pb")))
        assertEquals(
            Settings(
                use24Hour = true,
                clockStyle = ClockStyle.CHUNKY,
                colorSource = ColorSource.WALLPAPER,
                accent = Accent.BLUE,
                background = BackgroundMode.WALLPAPER,
                iconSize = IconSize.MEDIUM,
                grayscaleIcons = true,
            ),
            store.settings.first(),
        )
    }

    @Test
    fun `every look setting survives a restart`() = runBlocking {
        val file = File(tmp.root, "b.preferences_pb")
        val first = DataStoreSettingsStore(dataStore(file))
        first.setUse24Hour(false)
        first.setClockStyle(ClockStyle.CONDENSED)
        first.setColorSource(ColorSource.FIXED_ACCENT)
        first.setAccent(Accent.ROSE)
        first.setBackground(BackgroundMode.SOLID)
        first.setIconSize(IconSize.LARGE)
        first.setGrayscaleIcons(false)
        scopes.forEach { it.cancel() }

        val reopened = DataStoreSettingsStore(dataStore(file))
        assertEquals(
            Settings(
                use24Hour = false,
                clockStyle = ClockStyle.CONDENSED,
                colorSource = ColorSource.FIXED_ACCENT,
                accent = Accent.ROSE,
                background = BackgroundMode.SOLID,
                iconSize = IconSize.LARGE,
                grayscaleIcons = false,
            ),
            reopened.settings.first(),
        )
    }

    @Test
    fun `an unknown stored value falls back to the default instead of crashing`() = runBlocking {
        val ds = dataStore(File(tmp.root, "c.preferences_pb"))
        ds.edit { it[stringPreferencesKey("clock_style")] = "RETIRED_PRESET" }
        assertEquals(ClockStyle.CHUNKY, DataStoreSettingsStore(ds).settings.first().clockStyle)
    }
}
