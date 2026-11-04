package com.ukhirani.leanos

import com.ukhirani.leanos.platform.ClockSource
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
}
