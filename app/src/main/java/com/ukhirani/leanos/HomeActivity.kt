package com.ukhirani.leanos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.app.role.RoleManager
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.ukhirani.leanos.home.openNotificationAccessSettings
import com.ukhirani.leanos.home.openUsageAccessSettings
import com.ukhirani.leanos.onboarding.SetupScreen
import com.ukhirani.leanos.onboarding.SetupStep
import com.ukhirani.leanos.onboarding.nextSetupStep
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ukhirani.leanos.drawer.AppAction
import com.ukhirani.leanos.drawer.AppActionsSheet
import com.ukhirani.leanos.drawer.FavoriteChange
import com.ukhirani.leanos.drawer.FavoritesRow
import com.ukhirani.leanos.drawer.LocalGrayscaleIcons
import com.ukhirani.leanos.platform.InstalledApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import com.ukhirani.leanos.platform.AndroidMediaSource
import com.ukhirani.leanos.platform.AndroidUsageSource
import com.ukhirani.leanos.platform.FocusAccessibilityService
import com.ukhirani.leanos.platform.DataStoreSettingsStore
import com.ukhirani.leanos.platform.LauncherAppsSource
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SystemClockSource
import com.ukhirani.leanos.settings.AboutScreen
import com.ukhirani.leanos.settings.HiddenAppsScreen
import com.ukhirani.leanos.settings.LimitDialog
import com.ukhirani.leanos.settings.LimitsScreen
import com.ukhirani.leanos.settings.SettingsScreen
import com.ukhirani.leanos.ui.LeanTheme
import com.ukhirani.leanos.usage.ScreenTime
import com.ukhirani.leanos.usage.ScreenTimeToday
import kotlin.concurrent.thread
import androidx.lifecycle.lifecycleScope

/** Destinations layered over home; null means home itself. */
private enum class SettingsPage { Settings, Hidden, About, Limits, WeatherCity, GlanceCards }

class HomeActivity : ComponentActivity() {
    // Wiring lives here: real platform sources in, logic and UI out.
    private val settingsStore by lazy { DataStoreSettingsStore(this) }
    private val homeClock = HomeClock(SystemClockSource)
    private val homeGestures = HomeGestures(FocusAccessibilityService)
    private val installedApps by lazy { LauncherAppsSource(this) }
    private val appCatalog by lazy { AppCatalog(installedApps, settingsStore) }
    private var drawerOpen by mutableStateOf(false)
    private val openDrawer = { drawerOpen = true }
    private var settingsPage by mutableStateOf<SettingsPage?>(null)
    private val openSettings = { settingsPage = SettingsPage.Settings }
    private val homeLayout = HomeLayout(GlanceFormat(SystemClockSource))
    private val alarmSource by lazy { AndroidAlarmSource(this) }
    private val batterySource by lazy { AndroidBatterySource(this) }
    private val usageSource by lazy { AndroidUsageSource(this) }
    private val screenTime by lazy { ScreenTime(usageSource, SystemClockSource, packageName) }
    private var usageAccess by mutableStateOf(false)
    private var screenTimeToday by mutableStateOf<ScreenTimeToday?>(null)
    private val weatherSource = com.ukhirani.leanos.platform.OpenMeteoWeatherSource()
    private val weatherRefresh by lazy { com.ukhirani.leanos.weather.WeatherRefresh(weatherSource, settingsStore, SystemClockSource) }

    /** Off the main thread; any failure (network, storage) leaves home showing what it had. */
    private fun refreshWeather(before: suspend () -> Unit = {}) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                before()
                weatherRefresh.refreshIfStale()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    private val mediaSource by lazy { AndroidMediaSource(this) }
    private var mediaAccess by mutableStateOf(false)
    private var limitFor by mutableStateOf<InstalledApp?>(null)

    // First-run setup: what is granted is re-read on every resume, so coming back from a system page advances.
    private var setupGranted by mutableStateOf(emptySet<SetupStep>())
    private var setupSkipped by mutableStateOf(emptySet<SetupStep>())
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { readSetupAccess() }

    private fun readSetupAccess() {
        setupGranted = buildSet {
            if (getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)) add(SetupStep.DefaultLauncher)
            if (usageSource.hasAccess()) add(SetupStep.UsageAccess)
            if (mediaSource.hasAccess()) add(SetupStep.NotificationAccess)
            if (FocusAccessibilityService.isRunning()) add(SetupStep.Accessibility)
        }
    }

    private fun startSetupAction(step: SetupStep) {
        when (step) {
            SetupStep.DefaultLauncher ->
                roleRequest.launch(getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME))
            SetupStep.UsageAccess -> openUsageAccessSettings()
            SetupStep.NotificationAccess -> openNotificationAccessSettings()
            SetupStep.Accessibility ->
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            SetupStep.WeatherCity -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = Settings())
            LeanTheme(settings) { CompositionLocalProvider(LocalGrayscaleIcons provides settings.grayscaleIcons) {
                // Back on home does nothing: a launcher has nowhere to go back to.
                BackHandler {}
                val scope = rememberCoroutineScope()
                val snackbar = remember { SnackbarHostState() }
                var menuFor by remember { mutableStateOf<InstalledApp?>(null) }
                val showMenu = remember { { app: InstalledApp -> menuFor = app } }
                val favorites by appCatalog.favorites.collectAsState(initial = emptyList())
                val iconSize = settings.iconSize.dp.dp
                HomeScreen(
                    homeClock, settingsStore, homeGestures, homeLayout, alarmSource, batterySource,
                    onSwipeUp = openDrawer, onOpenSettings = openSettings,
                    usageAccess = usageAccess, screenTime = screenTimeToday,
                    onSetCity = { settingsPage = SettingsPage.WeatherCity },
                    mediaSource = mediaSource, mediaAccess = mediaAccess,
                ) {
                    FavoritesRow(favorites, installedApps::icon, installedApps::launch, showMenu, iconSize)
                }
                val apps by appCatalog.drawer.collectAsState(initial = emptyList())
                AnimatedVisibility(
                    visible = drawerOpen,
                    enter = slideInVertically { it / 4 } + fadeIn(),
                    exit = slideOutVertically { it / 4 } + fadeOut(),
                ) {
                    BackHandler { drawerOpen = false }
                    AppDrawer(apps, appCatalog, installedApps::icon, iconSize, showMenu) { app ->
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
                            onOpenHidden = { settingsPage = SettingsPage.Hidden },
                            onOpenAbout = { settingsPage = SettingsPage.About },
                            onOpenWeatherCity = { settingsPage = SettingsPage.WeatherCity },
                            onOpenLimits = { settingsPage = SettingsPage.Limits },
                            onOpenGlanceCards = { settingsPage = SettingsPage.GlanceCards },
                            onRerunSetup = {
                                setupSkipped = emptySet()
                                settingsPage = null
                                scope.launch { settingsStore.setOnboardingCompleted(false) }
                            },
                        )
                    }
                    SettingsPage.WeatherCity -> {
                        BackHandler { settingsPage = SettingsPage.Settings }
                        com.ukhirani.leanos.settings.WeatherCityScreen(
                            settingsStore, weatherSource,
                            onPick = { city ->
                                refreshWeather { settingsStore.setWeatherCity(city) }
                                settingsPage = SettingsPage.Settings
                            },
                            onBack = { settingsPage = SettingsPage.Settings },
                        )
                    }
                    SettingsPage.GlanceCards -> {
                        BackHandler { settingsPage = SettingsPage.Settings }
                        com.ukhirani.leanos.settings.GlanceCardsScreen(settingsStore, onBack = { settingsPage = SettingsPage.Settings })
                    }
                    SettingsPage.Limits -> {
                        BackHandler { settingsPage = SettingsPage.Settings }
                        val installed by installedApps.apps.collectAsState(initial = emptyList())
                        LimitsScreen(
                            settingsStore,
                            labelOf = { pkg -> installed.firstOrNull { it.packageName == pkg }?.label ?: pkg },
                            usage = screenTimeToday,
                            durationText = GlanceFormat(SystemClockSource)::durationText,
                            onBack = { settingsPage = SettingsPage.Settings },
                        )
                    }
                    SettingsPage.Hidden -> {
                        BackHandler { settingsPage = SettingsPage.Settings }
                        val hidden by appCatalog.hidden.collectAsState(initial = emptyList())
                        HiddenAppsScreen(
                            hidden, installedApps::icon, iconSize,
                            onOpen = installedApps::launch,
                            onUnhide = { app -> scope.launch { appCatalog.setHidden(app, false) } },
                            onBack = { settingsPage = SettingsPage.Settings },
                        )
                    }
                    SettingsPage.About -> {
                        BackHandler { settingsPage = SettingsPage.Settings }
                        AboutScreen(onBack = { settingsPage = SettingsPage.Settings })
                    }
                    null -> Unit
                }
                menuFor?.let { app ->
                    val isFavorite = favorites.any { it.packageName == app.packageName }
                    AppActionsSheet(app, appActions(app, isFavorite, scope, snackbar)) { menuFor = null }
                }
                limitFor?.let { app ->
                    LimitDialog(app.label, settings.dailyLimits[app.packageName], onDismiss = { limitFor = null }) { minutes ->
                        scope.launch { settingsStore.setDailyLimit(app.packageName, minutes) }
                    }
                }
                // Null until the store has loaded, so a returning user never sees a flash of setup.
                val stored by settingsStore.settings.collectAsState(initial = null)
                if (stored?.onboardingCompleted == false) {
                    BackHandler {}
                    val granted = if (stored?.weatherCity != null) setupGranted + SetupStep.WeatherCity else setupGranted
                    val step = nextSetupStep(granted, setupSkipped)
                    if (step == null) {
                        LaunchedEffect(Unit) { settingsStore.setOnboardingCompleted(true) }
                    } else {
                        SetupScreen(
                            step, weatherSource,
                            onAction = { startSetupAction(step) },
                            onPickCity = { city -> refreshWeather { settingsStore.setWeatherCity(city) } },
                            onSkip = { setupSkipped = setupSkipped + step },
                        )
                    }
                }
                SnackbarHost(snackbar,Modifier.fillMaxSize().safeDrawingPadding().wrapContentHeight(Alignment.Bottom))
            } }
        }
    }

    /** The long-press menu for [app]. New per-app actions go in this list. */
    private fun appActions(app: InstalledApp, isFavorite: Boolean, scope: CoroutineScope, snackbar: SnackbarHostState) = listOf(
        AppAction(if (isFavorite) "Remove from favorites" else "Add to favorites") {
            scope.launch {
                if (appCatalog.toggleFavorite(app) == FavoriteChange.LimitReached) {
                    snackbar.showSnackbar("You can have up to ${AppCatalog.MAX_FAVORITES} favorites")
                }
            }
        },
        AppAction("Hide") { scope.launch { appCatalog.setHidden(app, true) } },
        AppAction(getString(R.string.limit_set)) { limitFor = app },
        AppAction("App info") { installedApps.openAppInfo(app) },
        AppAction("Uninstall") { installedApps.uninstall(app) },
    )

    // Screen time refreshes each time home becomes visible; reading a day of events stays off the main thread.
    override fun onResume() {
        super.onResume()
        usageAccess = usageSource.hasAccess()
        mediaAccess = mediaSource.hasAccess()
        readSetupAccess()
        thread { screenTime.today().let { runOnUiThread { screenTimeToday = it } } }
        refreshWeather()
    }

    // Pressing Home while the drawer or settings is open returns to home.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerOpen = false
        settingsPage = null
    }
}
