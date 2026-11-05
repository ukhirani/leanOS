package com.ukhirani.leanos

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.ukhirani.leanos.drawer.AppCatalog
import com.ukhirani.leanos.drawer.AppDrawer
import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.home.HomeClock
import com.ukhirani.leanos.home.HomeGestures
import com.ukhirani.leanos.home.HomeLayout
import com.ukhirani.leanos.home.HomeScreen
import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.City
import com.ukhirani.leanos.platform.CurrentWeather
import com.ukhirani.leanos.platform.NextAlarm
import com.ukhirani.leanos.platform.NowPlaying
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.ui.LeanTheme
import com.ukhirani.leanos.usage.AppUsage
import com.ukhirani.leanos.usage.ScreenTimeToday
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders home, the drawer and the Block screen to PNGs under app/build/screenshots, in light, dark and
 * 1.3x font scale, for design review. Nothing is asserted: look at the pictures.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotsTest(private val variant: String, private val dark: Boolean, private val fontScale: Float) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = listOf(arrayOf("light", false, 1f), arrayOf("dark", true, 1f), arrayOf("font1.3", false, 1.3f))
    }

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val clock = FakeClockSource("2026-11-04T09:41:00+01:00[Europe/Paris]")
    private val apps = listOf("Calendar", "Camera", "Chat", "Clock", "Maps", "Music", "Notes", "Phone", "Photos", "Settings").map { app(it) }
    private val store = FakeSettingsStore(
        Settings(
            favorites = apps.take(4).map { it.packageName },
            weatherCity = City("Paris, France", 48.85, 2.35),
            weather = CurrentWeather(11.4, 3, clock.now().toInstant()),
        ),
    )

    private fun shot(name: String, content: @Composable () -> Unit) {
        // The clock glow drifts forever and would keep Compose from going idle; animations off holds it still.
        android.provider.Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f,
        )
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(
                LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
            ) { LeanTheme(Settings()) { content() } }
        }
        compose.waitForIdle()
        // Drawing the window by hand: captureToImage waits on a real display frame, which Robolectric never delivers.
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val out = File("build/screenshots").apply { mkdirs() }
        File(out, "$name-$variant.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun home() = shot("home") {
        val alarm = FakeAlarmSource(NextAlarm(ZonedDateTime.parse("2026-11-05T07:00:00+01:00[Europe/Paris]").toInstant()))
        val media = FakeMediaSource().apply { play(NowPlaying("pkg.music", "Song title", "Artist", playing = true)) }
        val usage = ScreenTimeToday(
            Duration.ofMinutes(95),
            listOf(AppUsage("pkg.chat", Duration.ofMinutes(50)), AppUsage("pkg.maps", Duration.ofMinutes(30)), AppUsage("pkg.mail", Duration.ofMinutes(15))),
        )
        val format = GlanceFormat(clock, Locale.US)
        HomeScreen(
            HomeClock(clock), store, HomeGestures(FakeFocusService()), HomeLayout(format), alarm,
            FakeBatterySource(BatteryState(82, true)),
            usageAccess = true, screenTime = usage, mediaSource = media, mediaAccess = true,
        )
    }

    @Test
    fun drawer() = shot("drawer") {
        AppDrawer(apps, AppCatalog(FakeInstalledAppsSource(*apps.toTypedArray()), store), { null }, onMenu = {}, onLaunch = {})
    }

    @Test
    fun block() = shot("block") {
        BlockScreen("Chat", "1h 00m", canExtend = true, onGoHome = {}, onExtend = {})
    }
}
