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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
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
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }

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
    fun `onboarding completion survives a restart and can be reset`() = runBlocking {
        val file = File(tmp.root, "o.preferences_pb")
        val first = DataStoreSettingsStore(dataStore(file))
        assertEquals(false, first.settings.first().onboardingCompleted)
        first.setOnboardingCompleted(true)
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }

        val reopened = DataStoreSettingsStore(dataStore(file))
        assertEquals(true, reopened.settings.first().onboardingCompleted)
        reopened.setOnboardingCompleted(false)
        assertEquals(false, reopened.settings.first().onboardingCompleted)
    }

    @Test
    fun `weather city, units and the cached weather survive a restart`() = runBlocking {
        val file = File(tmp.root, "w.preferences_pb")
        val city = com.ukhirani.leanos.platform.City("Paris, Île-de-France, France", 48.85341, 2.3488)
        val weather = com.ukhirani.leanos.platform.CurrentWeather(-3.5, 61, java.time.Instant.parse("2026-11-04T11:00:00Z"))
        val first = DataStoreSettingsStore(dataStore(file))
        first.setUseFahrenheit(true)
        first.setWeatherCity(city)
        first.setWeather(weather)
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }

        val reopened = DataStoreSettingsStore(dataStore(file)).settings.first()
        assertEquals(Triple(true, city, weather), Triple(reopened.useFahrenheit, reopened.weatherCity, reopened.weather))
    }

    @Test
    fun `picking another city drops the old city's cached weather`() = runBlocking {
        val store = DataStoreSettingsStore(dataStore(File(tmp.root, "x.preferences_pb")))
        store.setWeatherCity(com.ukhirani.leanos.platform.City("Paris, France", 48.85, 2.35))
        store.setWeather(com.ukhirani.leanos.platform.CurrentWeather(9.0, 0, java.time.Instant.EPOCH))
        store.setWeatherCity(com.ukhirani.leanos.platform.City("Oslo, Norway", 59.91, 10.75))
        assertEquals(null, store.settings.first().weather)
    }

    @Test
    fun `daily limits can be set, changed and removed, and survive a restart`() = runBlocking {
        val file = File(tmp.root, "d.preferences_pb")
        val first = DataStoreSettingsStore(dataStore(file))
        first.setDailyLimit("video", 30)
        first.setDailyLimit("chat", 15)
        first.setDailyLimit("video", 45)
        first.setDailyLimit("chat", null)
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }

        assertEquals(mapOf("video" to 45), DataStoreSettingsStore(dataStore(file)).settings.first().dailyLimits)
    }

    @Test
    fun `an unknown stored value falls back to the default instead of crashing`() = runBlocking {
        val ds = dataStore(File(tmp.root, "c.preferences_pb"))
        ds.edit { it[stringPreferencesKey("clock_style")] = "RETIRED_PRESET" }
        assertEquals(ClockStyle.CHUNKY, DataStoreSettingsStore(ds).settings.first().clockStyle)
    }

    @Test
    fun `hidden apps survive a restart`() = runBlocking {
        val file = File(tmp.root, "d.preferences_pb")
        DataStoreSettingsStore(dataStore(file)).setHidden(setOf("pkg.a", "pkg.b"))
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        assertEquals(setOf("pkg.a", "pkg.b"), DataStoreSettingsStore(dataStore(file)).settings.first().hidden)
    }

    @Test
    fun `limit extensions survive a restart`() = runBlocking {
        val file = File(tmp.root, "e.preferences_pb")
        val first = DataStoreSettingsStore(dataStore(file))
        first.setLimitExtension("video", java.time.Instant.ofEpochMilli(1_000))
        first.setLimitExtension("chat", java.time.Instant.ofEpochMilli(2_000))
        first.setLimitExtension("video", java.time.Instant.ofEpochMilli(3_000))
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() }
        assertEquals(
            mapOf("video" to java.time.Instant.ofEpochMilli(3_000), "chat" to java.time.Instant.ofEpochMilli(2_000)),
            DataStoreSettingsStore(dataStore(file)).settings.first().limitExtensions,
        )
    }
}
