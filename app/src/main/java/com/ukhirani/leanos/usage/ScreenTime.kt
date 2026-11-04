package com.ukhirani.leanos.usage

import com.ukhirani.leanos.platform.ClockSource
import com.ukhirani.leanos.platform.UsageEvent
import com.ukhirani.leanos.platform.UsageSource
import java.time.Duration
import java.time.Instant

data class AppUsage(val packageName: String, val duration: Duration)

/** Today's foreground time: every app, longest first, and their total. */
data class ScreenTimeToday(val total: Duration, val apps: List<AppUsage>) {
    val top3: List<AppUsage> get() = apps.take(3)
}

/** Turns raw usage events since local midnight into per-app foreground time. */
class ScreenTime(private val usage: UsageSource, private val clock: ClockSource, private val ownPackage: String) {

    /** Null without usage access. */
    fun today(): ScreenTimeToday? {
        if (!usage.hasAccess()) return null
        val now = clock.now()
        val midnight = now.toLocalDate().atStartOfDay(now.zone).toInstant()
        val apps = foregroundTime(usage.events(midnight), midnight, now.toInstant())
            .filterKeys { it != ownPackage }
            .map { (pkg, time) -> AppUsage(pkg, time) }
            .sortedByDescending { it.duration }
        return ScreenTimeToday(apps.fold(Duration.ZERO) { sum, app -> sum + app.duration }, apps)
    }

    private fun foregroundTime(events: List<UsageEvent>, from: Instant, now: Instant): Map<String, Duration> {
        val opened = mutableMapOf<String, Instant>()
        val time = mutableMapOf<String, Duration>()
        for (event in events) {
            val pkg = event.packageName
            when (event.type) {
                UsageEvent.Type.Resumed -> opened.putIfAbsent(pkg, event.at)
                UsageEvent.Type.Paused -> {
                    // An app whose first event today is a pause was already in front at [from].
                    val start = opened.remove(pkg) ?: from.takeIf { pkg !in time } ?: continue
                    time.merge(pkg, Duration.between(start, event.at), Duration::plus)
                }
            }
        }
        // Still in front: count up to now.
        opened.forEach { (pkg, start) -> time.merge(pkg, Duration.between(start, now), Duration::plus) }
        return time
    }
}
