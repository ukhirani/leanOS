package com.ukhirani.leanos.home

import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.Settings
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HomeClock(private val clock: ClockSource) {
    fun text(settings: Settings): String =
        clock.now().format(if (settings.use24Hour) H24 else H12)

    fun millisUntilNextMinute(): Long {
        val now = clock.now()
        val next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        return Duration.between(now, next).toMillis()
    }

    private companion object {
        val H24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val H12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm")
    }
}
