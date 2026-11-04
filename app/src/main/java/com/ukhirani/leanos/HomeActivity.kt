package com.ukhirani.leanos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.home.HomeClock
import com.ukhirani.leanos.home.HomeLayout
import com.ukhirani.leanos.home.HomeScreen
import com.ukhirani.leanos.platform.AndroidAlarmSource
import com.ukhirani.leanos.platform.AndroidBatterySource
import com.ukhirani.leanos.platform.DataStoreSettingsStore
import com.ukhirani.leanos.platform.SystemClockSource
import com.ukhirani.leanos.ui.LeanTheme

class HomeActivity : ComponentActivity() {
    // Wiring lives here: real platform sources in, logic and UI out.
    private val settingsStore by lazy { DataStoreSettingsStore(this) }
    private val homeClock = HomeClock(SystemClockSource)
    private val homeLayout = HomeLayout(GlanceFormat(SystemClockSource))
    private val alarmSource by lazy { AndroidAlarmSource(this) }
    private val batterySource by lazy { AndroidBatterySource(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LeanTheme {
                // Back on home does nothing: a launcher has nowhere to go back to.
                BackHandler {}
                HomeScreen(homeClock, settingsStore, homeLayout, alarmSource, batterySource)
            }
        }
    }
}
