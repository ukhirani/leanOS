package com.ukhirani.leanos

import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.FocusService
import com.ukhirani.leanos.platform.InstalledApp
import com.ukhirani.leanos.platform.InstalledAppsSource
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeClockSource(var current: ZonedDateTime) : ClockSource {
    constructor(iso: String) : this(ZonedDateTime.parse(iso))

    override fun now(): ZonedDateTime = current
}

class FakeSettingsStore(initial: Settings = Settings()) : SettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state

    override suspend fun setUse24Hour(enabled: Boolean) {
        state.update { it.copy(use24Hour = enabled) }
    }

    override suspend fun setFavorites(packages: List<String>) {
        state.update { it.copy(favorites = packages) }
    }
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
