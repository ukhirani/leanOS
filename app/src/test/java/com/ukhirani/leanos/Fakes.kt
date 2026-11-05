package com.ukhirani.leanos

import com.ukhirani.leanos.platform.Accent
import com.ukhirani.leanos.platform.BackgroundMode
import com.ukhirani.leanos.platform.AlarmSource
import com.ukhirani.leanos.platform.BatterySource
import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.ClockStyle
import com.ukhirani.leanos.platform.ColorSource
import com.ukhirani.leanos.platform.IconSize
import com.ukhirani.leanos.platform.FocusService
import com.ukhirani.leanos.platform.InstalledApp
import com.ukhirani.leanos.platform.InstalledAppsSource
import com.ukhirani.leanos.platform.MediaSource
import com.ukhirani.leanos.platform.NextAlarm
import com.ukhirani.leanos.platform.NowPlaying
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import com.ukhirani.leanos.platform.UsageEvent
import com.ukhirani.leanos.platform.UsageSource
import java.time.Instant
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeClockSource(var current: ZonedDateTime) : ClockSource {
    constructor(iso: String) : this(ZonedDateTime.parse(iso))

    override fun now(): ZonedDateTime = current
}

class FakeAlarmSource(initial: NextAlarm? = null) : AlarmSource {
    private val state = MutableStateFlow(initial)
    override val nextAlarm: Flow<NextAlarm?> = state

    fun set(alarm: NextAlarm?) {
        state.value = alarm
    }
}

class FakeBatterySource(initial: BatteryState) : BatterySource {
    private val state = MutableStateFlow(initial)
    override val battery: Flow<BatteryState> = state

    fun set(battery: BatteryState) {
        state.value = battery
    }
}

class FakeSettingsStore(initial: Settings = Settings()) : SettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state

    override suspend fun setUse24Hour(enabled: Boolean) {
        state.update { it.copy(use24Hour = enabled) }
    }

    override suspend fun setClockStyle(style: ClockStyle) = state.update { it.copy(clockStyle = style) }
    override suspend fun setColorSource(source: ColorSource) = state.update { it.copy(colorSource = source) }
    override suspend fun setAccent(accent: Accent) = state.update { it.copy(accent = accent) }
    override suspend fun setBackground(mode: BackgroundMode) = state.update { it.copy(background = mode) }
    override suspend fun setIconSize(size: IconSize) = state.update { it.copy(iconSize = size) }

    override suspend fun setFavorites(packages: List<String>) {
        state.update { it.copy(favorites = packages) }
    }

    override suspend fun setUseFahrenheit(enabled: Boolean) = state.update { it.copy(useFahrenheit = enabled) }
    override suspend fun setWeatherCity(city: com.ukhirani.leanos.platform.City?) =
        state.update { it.copy(weatherCity = city, weather = null) }
    override suspend fun setWeather(weather: com.ukhirani.leanos.platform.CurrentWeather) =
        state.update { it.copy(weather = weather) }
    override suspend fun setDailyLimit(packageName: String, minutes: Int?) = state.update {
        it.copy(dailyLimits = if (minutes == null) it.dailyLimits - packageName else it.dailyLimits + (packageName to minutes))
    }
    override suspend fun setGrayscaleIcons(enabled: Boolean) = state.update { it.copy(grayscaleIcons = enabled) }
    override suspend fun setAppHidden(packageName: String, hidden: Boolean) = state.update {
        if (hidden) it.copy(hidden = it.hidden + packageName, favorites = it.favorites - packageName)
        else it.copy(hidden = it.hidden - packageName)
    }
    override suspend fun setGlanceCards(order: List<com.ukhirani.leanos.home.GlanceCard.Feature>, hidden: Set<com.ukhirani.leanos.home.GlanceCard.Feature>) =
        state.update { it.copy(cardOrder = order, hiddenCards = hidden) }
    override suspend fun setLimitExtension(packageName: String, at: java.time.Instant) =
        state.update { it.copy(limitExtensions = it.limitExtensions + (packageName to at)) }
    override suspend fun setOnboardingCompleted(completed: Boolean) = state.update { it.copy(onboardingCompleted = completed) }
}

class FakeFocusService(var running: Boolean = true) : FocusService {
    var refuseActions = false
    val performed = mutableListOf<String>()
    override val foregroundPackage = MutableStateFlow<String?>(null)
    override fun isRunning() = running
    override fun openNotifications() = perform("notifications")
    override fun lockScreen() = perform("lock")

    private fun perform(action: String): Boolean {
        if (!running || refuseActions) return false
        performed += action
        return true
    }
}

fun app(label: String, packageName: String = "pkg." + label.lowercase().replace(" ", "")) =
    InstalledApp(packageName = packageName, activityName = "$packageName.Main", label = label)

class FakeInstalledAppsSource(vararg initial: InstalledApp) : InstalledAppsSource {
    private val state = MutableStateFlow(initial.toList())
    override val apps: Flow<List<InstalledApp>> = state

    fun install(app: InstalledApp) = state.update { it + app }
    fun uninstall(packageName: String) = state.update { apps -> apps.filter { it.packageName != packageName } }
}

class FakeUsageSource(var access: Boolean = true) : UsageSource {
    val recorded = mutableListOf<UsageEvent>()

    fun resumed(packageName: String, iso: String) = add(packageName, iso, UsageEvent.Type.Resumed)
    fun paused(packageName: String, iso: String) = add(packageName, iso, UsageEvent.Type.Paused)

    private fun add(packageName: String, iso: String, type: UsageEvent.Type) {
        recorded += UsageEvent(packageName, ZonedDateTime.parse(iso).toInstant(), type)
    }

    override fun hasAccess() = access
    override fun events(since: Instant) = if (access) recorded.filter { !it.at.isBefore(since) } else emptyList()
}

class FakeMediaSource(var access: Boolean = true) : MediaSource {
    private val state = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: Flow<NowPlaying?> = state
    val sent = mutableListOf<String>()

    fun play(media: NowPlaying?) {
        state.value = media
    }

    override fun hasAccess() = access
    override fun playPause() { sent += "playPause" }
    override fun next() { sent += "next" }
    override fun previous() { sent += "previous" }
    override fun open() { sent += "open" }
}
