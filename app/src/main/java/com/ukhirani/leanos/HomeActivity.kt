package com.ukhirani.leanos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ukhirani.leanos.home.HomeClock
import com.ukhirani.leanos.home.HomeGestures
import com.ukhirani.leanos.home.HomeScreen
import com.ukhirani.leanos.platform.FocusAccessibilityService
import com.ukhirani.leanos.platform.DataStoreSettingsStore
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SystemClockSource
import com.ukhirani.leanos.settings.AboutScreen
import com.ukhirani.leanos.settings.SettingsScreen
import com.ukhirani.leanos.ui.LeanTheme

private enum class Destination { Home, Settings, About }

class HomeActivity : ComponentActivity() {
    // Wiring lives here: real platform sources in, logic and UI out.
    private val settingsStore by lazy { DataStoreSettingsStore(this) }
    private val homeClock = HomeClock(SystemClockSource)
    private val homeGestures = HomeGestures(FocusAccessibilityService)
    private val destination = mutableStateOf(Destination.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = Settings())
            val openSettings = remember { { destination.value = Destination.Settings } }
            LeanTheme(settings) {
                when (destination.value) {
                    Destination.Home -> {
                        // Back on home does nothing: a launcher has nowhere to go back to.
                        BackHandler {}
                        HomeScreen(homeClock, settingsStore, homeGestures, onOpenSettings = openSettings)
                    }
                    Destination.Settings -> {
                        val goHome = { destination.value = Destination.Home }
                        BackHandler(onBack = goHome)
                        SettingsScreen(settingsStore, onBack = goHome, onOpenAbout = { destination.value = Destination.About })
                    }
                    Destination.About -> {
                        val goSettings = { destination.value = Destination.Settings }
                        BackHandler(onBack = goSettings)
                        AboutScreen(onBack = goSettings)
                    }
                }
            }
        }
    }

    // Pressing Home while in settings returns home, like any launcher.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) destination.value = Destination.Home
    }
}
