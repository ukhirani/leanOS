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
import com.ukhirani.leanos.drawer.AppCatalog
import com.ukhirani.leanos.drawer.AppDrawer
import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.home.HomeClock
import com.ukhirani.leanos.home.HomeGestures
import com.ukhirani.leanos.home.HomeLayout
import com.ukhirani.leanos.home.HomeScreen
import com.ukhirani.leanos.platform.AndroidAlarmSource
import com.ukhirani.leanos.platform.AndroidBatterySource
import com.ukhirani.leanos.platform.AndroidUsageSource
import com.ukhirani.leanos.platform.FocusAccessibilityService
import com.ukhirani.leanos.platform.DataStoreSettingsStore
import com.ukhirani.leanos.platform.LauncherAppsSource
import com.ukhirani.leanos.platform.SystemClockSource
import com.ukhirani.leanos.ui.LeanTheme
import com.ukhirani.leanos.usage.ScreenTime
import com.ukhirani.leanos.usage.ScreenTimeToday
import kotlin.concurrent.thread

class HomeActivity : ComponentActivity() {
    // Wiring lives here: real platform sources in, logic and UI out.
    private val settingsStore by lazy { DataStoreSettingsStore(this) }
    private val homeClock = HomeClock(SystemClockSource)
    private val homeGestures = HomeGestures(FocusAccessibilityService)
    private val installedApps by lazy { LauncherAppsSource(this) }
    private val appCatalog by lazy { AppCatalog(installedApps) }
    private var drawerOpen by mutableStateOf(false)
    private val openDrawer = { drawerOpen = true }
    private val homeLayout = HomeLayout(GlanceFormat(SystemClockSource))
    private val alarmSource by lazy { AndroidAlarmSource(this) }
    private val batterySource by lazy { AndroidBatterySource(this) }
    private val usageSource by lazy { AndroidUsageSource(this) }
    private val screenTime by lazy { ScreenTime(usageSource, SystemClockSource, packageName) }
    private var usageAccess by mutableStateOf(false)
    private var screenTimeToday by mutableStateOf<ScreenTimeToday?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LeanTheme {
                // Back on home does nothing: a launcher has nowhere to go back to.
                BackHandler {}
                HomeScreen(
                    homeClock, settingsStore, homeGestures, homeLayout, alarmSource, batterySource,
                    onSwipeUp = openDrawer, usageAccess = usageAccess, screenTime = screenTimeToday,
                )
                val apps by appCatalog.drawer.collectAsState(initial = emptyList())
                AnimatedVisibility(
                    visible = drawerOpen,
                    enter = slideInVertically { it / 4 } + fadeIn(),
                    exit = slideOutVertically { it / 4 } + fadeOut(),
                ) {
                    BackHandler { drawerOpen = false }
                    AppDrawer(apps, appCatalog, installedApps::icon) { app ->
                        installedApps.launch(app)
                        drawerOpen = false
                    }
                }
            }
        }
    }

    // Screen time refreshes each time home becomes visible; reading a day of events stays off the main thread.
    override fun onResume() {
        super.onResume()
        usageAccess = usageSource.hasAccess()
        thread { screenTime.today().let { runOnUiThread { screenTimeToday = it } } }
    }

    // Pressing Home while the drawer is open returns to home.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerOpen = false
    }
}
