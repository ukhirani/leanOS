package com.ukhirani.leanos.platform

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.ukhirani.leanos.BlockActivity
import com.ukhirani.leanos.usage.LimitDecision
import com.ukhirani.leanos.usage.LimitPolicy
import com.ukhirani.leanos.usage.ScreenTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The Focus accessibility service. It only listens to window-state changes to learn which app
 * is in front, asks the Limit policy about it and covers a blocked app with the Block screen,
 * and performs the global actions for home gestures. It reads no screen content.
 * The companion is the real [FocusService] the rest of LeanOS talks to.
 */
class FocusAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val limitPolicy by lazy {
        LimitPolicy(ScreenTime(AndroidUsageSource(this), SystemClockSource, packageName), SystemClockSource)
    }
    @Volatile private var dailyLimits = emptyMap<String, Int>()

    override fun onServiceConnected() {
        instance = this
        scope.launch {
            DataStoreSettingsStore(this@FocusAccessibilityService).settings.collect { dailyLimits = it.dailyLimits }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Popups, dialogs, the notification shade and the keyboard (IME) also change window
        // state; only an activity coming to the front changes the foreground app.
        if (!isActivity(pkg, event.className?.toString())) return
        if (foreground.value == pkg) return
        foreground.value = pkg
        if (pkg != packageName && pkg in dailyLimits) scope.launch { enforceLimit(pkg) }
    }

    private fun enforceLimit(pkg: String) {
        val blocked = limitPolicy.decide(pkg, dailyLimits) as? LimitDecision.Blocked ?: return
        if (foreground.value == pkg) startActivity(BlockActivity.intent(this, pkg, blocked))
    }

    private fun isActivity(pkg: String, className: String?): Boolean = className != null &&
        runCatching { packageManager.getActivityInfo(ComponentName(pkg, className), 0) }.isSuccess

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object : FocusService {
        @Volatile private var instance: FocusAccessibilityService? = null
        private val foreground = MutableStateFlow<String?>(null)

        override val foregroundPackage: StateFlow<String?> = foreground
        override fun isRunning() = instance != null
        override fun openNotifications() = perform(GLOBAL_ACTION_NOTIFICATIONS)
        override fun lockScreen() = perform(GLOBAL_ACTION_LOCK_SCREEN)

        private fun perform(action: Int) = instance?.performGlobalAction(action) ?: false
    }
}
