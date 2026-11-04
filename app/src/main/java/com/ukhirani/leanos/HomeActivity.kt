package com.ukhirani.leanos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.drawer.AppCatalog
import com.ukhirani.leanos.drawer.AppDrawer
import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.home.HomeClock
import com.ukhirani.leanos.home.HomeGestures
import com.ukhirani.leanos.home.HomeLayout
import com.ukhirani.leanos.home.HomeScreen
import com.ukhirani.leanos.platform.AndroidAlarmSource
import com.ukhirani.leanos.platform.AndroidBatterySource
import com.ukhirani.leanos.platform.FocusAccessibilityService
import com.ukhirani.leanos.platform.DataStoreSettingsStore
import com.ukhirani.leanos.platform.LauncherAppsSource
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SystemClockSource
import com.ukhirani.leanos.settings.AboutScreen
import com.ukhirani.leanos.settings.SettingsScreen
import com.ukhirani.leanos.ui.LeanTheme

/** Destinations layered over home; null means home itself. */
private enum class SettingsPage { Settings, About }

class HomeActivity : ComponentActivity() {
    // Wiring lives here: real platform sources in, logic and UI out.
    private val settingsStore by lazy { DataStoreSettingsStore(this) }
    private val homeClock = HomeClock(SystemClockSource)
    private val homeGestures = HomeGestures(FocusAccessibilityService)
    private val installedApps by lazy { LauncherAppsSource(this) }
    private val appCatalog by lazy { AppCatalog(installedApps) }
    private var drawerOpen by mutableStateOf(false)
    private val openDrawer = { drawerOpen = true }
    private var settingsPage by mutableStateOf<SettingsPage?>(null)
    private val openSettings = { settingsPage = SettingsPage.Settings }
    private val homeLayout = HomeLayout(GlanceFormat(SystemClockSource))
    private val alarmSource by lazy { AndroidAlarmSource(this) }
    private val batterySource by lazy { AndroidBatterySource(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = Settings())
            LeanTheme(settings) {
                // Back on home does nothing: a launcher has nowhere to go back to.
                BackHandler {}
                HomeScreen(
                    homeClock, settingsStore, homeGestures, homeLayout, alarmSource, batterySource,
                    onSwipeUp = openDrawer, onOpenSettings = openSettings,
                )
                val apps by appCatalog.drawer.collectAsState(initial = emptyList())
                AnimatedVisibility(
                    visible = drawerOpen,
                    enter = slideInVertically { it / 4 } + fadeIn(),
                    exit = slideOutVertically { it / 4 } + fadeOut(),
                ) {
                    BackHandler { drawerOpen = false }
                    AppDrawer(apps, appCatalog, installedApps::icon, settings.iconSize.dp.dp) { app ->
                        installedApps.launch(app)
                        drawerOpen = false
                    }
                }
                when (settingsPage) {
                    SettingsPage.Settings -> {
                        BackHandler { settingsPage = null }
                        SettingsScreen(
                            settingsStore,
                            onBack = { settingsPage = null },
                            onOpenAbout = { settingsPage = SettingsPage.About },
                        )
                    }
                    SettingsPage.About -> {
                        BackHandler { settingsPage = SettingsPage.Settings }
                        AboutScreen(onBack = { settingsPage = SettingsPage.Settings })
                    }
                    null -> Unit
                }
            }
        }
    }

    // Pressing Home while the drawer or settings is open returns to home.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerOpen = false
        settingsPage = null
    }
}
