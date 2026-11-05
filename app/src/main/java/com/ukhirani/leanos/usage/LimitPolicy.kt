package com.ukhirani.leanos.usage

import com.ukhirani.leanos.platform.ClockSource
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/** What the Limit policy decided for one app right now. */
sealed interface LimitDecision {
    /** No limit set, or no usage access to enforce it. */
    data object Unlimited : LimitDecision

    /**
     * Under its limit, or inside today's extension. While it stays in the foreground it becomes
     * blocked [remaining] from now.
     */
    data class Allowed(val remaining: Duration) : LimitDecision

    /**
     * Limit used up: blocked until [unlocksAt], the next local midnight. [canExtend] is true
     * while today's one extension is still unused.
     */
    data class Blocked(val used: Duration, val unlocksAt: ZonedDateTime, val canExtend: Boolean = true) : LimitDecision
}

/**
 * Decides whether an app with a daily limit may be used now, from today's screen time.
 * The day is the current zone's local day, so limits reset at local midnight, also across
 * time-zone changes. Once a day each blocked app may be extended by [EXTENSION]: the
 * extension starts when it is granted and only counts on the local day it was granted.
 */
class LimitPolicy(private val screenTime: ScreenTime, private val clock: ClockSource) {

    /** [limits] maps package name to minutes per day; [extensions] to when its last extension was granted. */
    fun decide(packageName: String, limits: Map<String, Int>, extensions: Map<String, Instant> = emptyMap()): LimitDecision {
        val limit = limits[packageName]?.let { Duration.ofMinutes(it.toLong()) } ?: return LimitDecision.Unlimited
        // shortcut: rescans all of today's usage events on every check; cache or read incrementally if limit checks get slow.
        val today = screenTime.today() ?: return LimitDecision.Unlimited
        val used = today.apps.firstOrNull { it.packageName == packageName }?.duration ?: Duration.ZERO
        if (used < limit) return LimitDecision.Allowed(limit - used)
        val now = clock.now()
        val extended = extensions[packageName]?.takeIf { it.atZone(now.zone).toLocalDate() == now.toLocalDate() }
        val extensionLeft = extended?.let { Duration.between(now.toInstant(), it + EXTENSION) }
        if (extensionLeft != null && extensionLeft > Duration.ZERO) return LimitDecision.Allowed(extensionLeft)
        return LimitDecision.Blocked(used, now.toLocalDate().plusDays(1).atStartOfDay(now.zone), canExtend = extended == null)
    }

    companion object {
        val EXTENSION: Duration = Duration.ofMinutes(5)
    }
}
