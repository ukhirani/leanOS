package com.ukhirani.leanos

import com.ukhirani.leanos.platform.AlarmSource
import com.ukhirani.leanos.platform.BatterySource
import com.ukhirani.leanos.platform.BatteryState
import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.FocusService
import com.ukhirani.leanos.platform.NextAlarm
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
