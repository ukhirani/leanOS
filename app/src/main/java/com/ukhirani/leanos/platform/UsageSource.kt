package com.ukhirani.leanos.platform

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.Instant

/** An app's activity coming to the foreground (Resumed) or leaving it (Paused). */
data class UsageEvent(val packageName: String, val at: Instant, val type: Type) {
    enum class Type { Resumed, Paused }
}

/** Raw foreground events, oldest first. Needs the usage access permission. */
interface UsageSource {
    fun hasAccess(): Boolean

    /** Events from [since] until now; empty without access. */
    fun events(since: Instant): List<UsageEvent>
}

class AndroidUsageSource(context: Context) : UsageSource {
    private val context = context.applicationContext
    private val usageStats = this.context.getSystemService(UsageStatsManager::class.java)
    private val appOps = this.context.getSystemService(AppOpsManager::class.java)

    override fun hasAccess(): Boolean =
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
            AppOpsManager.MODE_ALLOWED

    override fun events(since: Instant): List<UsageEvent> {
        val events = usageStats.queryEvents(since.toEpochMilli(), System.currentTimeMillis())
        val event = UsageEvents.Event()
        return buildList {
            while (events.getNextEvent(event)) {
                val type = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Type.Resumed
                    UsageEvents.Event.ACTIVITY_PAUSED -> UsageEvent.Type.Paused
                    else -> continue
                }
                add(UsageEvent(event.packageName, Instant.ofEpochMilli(event.timeStamp), type))
            }
        }
    }
}
