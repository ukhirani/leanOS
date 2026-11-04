package com.ukhirani.leanos.platform

import java.time.ZonedDateTime

/** The current time in the device's current time zone. */
fun interface ClockSource {
    fun now(): ZonedDateTime
}

object SystemClockSource : ClockSource {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
}
