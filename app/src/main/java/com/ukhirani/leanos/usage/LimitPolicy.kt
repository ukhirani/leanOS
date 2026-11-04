package com.ukhirani.leanos.usage

import com.ukhirani.leanos.platform.ClockSource
import java.time.Duration
import java.time.ZonedDateTime

/** What the Limit policy decided for one app right now. */
sealed interface LimitDecision {
    /** No limit set, or no usage access to enforce it. */
    data object Unlimited : LimitDecision

    /** Under its limit; becomes blocked once [remaining] more foreground time is used. */
    data class Allowed(val remaining: Duration) : LimitDecision

    /** Limit used up: blocked until [unlocksAt], the next local midnight. */
    data class Blocked(val used: Duration, val unlocksAt: ZonedDateTime) : LimitDecision
}

/**
 * Decides whether an app with a daily limit may be used now, from today's screen time.
 * The day is the current zone's local day, so limits reset at local midnight, also across
 * time-zone changes.
 */
class LimitPolicy(private val screenTime: ScreenTime, private val clock: ClockSource) {

    /** [limits] maps package name to minutes per day. */
    fun decide(packageName: String, limits: Map<String, Int>): LimitDecision {
        val limit = limits[packageName]?.let { Duration.ofMinutes(it.toLong()) } ?: return LimitDecision.Unlimited
        val today = screenTime.today() ?: return LimitDecision.Unlimited
        val used = today.apps.firstOrNull { it.packageName == packageName }?.duration ?: Duration.ZERO
        if (used < limit) return LimitDecision.Allowed(limit - used)
        val now = clock.now()
        return LimitDecision.Blocked(used, now.toLocalDate().plusDays(1).atStartOfDay(now.zone))
    }
}
