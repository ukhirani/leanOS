package com.ukhirani.leanos.platform

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The Focus accessibility service. It only listens to window-state changes to learn which app
 * is in front, asks the Limit policy about it and covers a blocked app with the Block screen,
 * also mid-session at the instant the policy says it becomes blocked, and performs the global actions for home gestures. It reads no screen content.
 * The companion is the real [FocusService] the rest of LeanOS talks to.
 */
class FocusAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val limitPolicy by lazy {
        LimitPolicy(ScreenTime(AndroidUsageSource(this), SystemClockSource, packageName), SystemClockSource)
    }
    private val settingsStore by lazy { DataStoreSettingsStore(this) }
    private val handler = Handler(Looper.getMainLooper())
    // Null until the first store read lands; events before that check on demand instead of letting an app slip by.
    @Volatile private var dailyLimits: Map<String, Int>? = null
    // shortcut: one PackageManager lookup per (package, class) on the main thread, then cached; move off-thread if windows get slow to open.
    private val activityChecks = HashMap<Pair<String, String>, Boolean>()

    override fun onServiceConnected() {
        instance = this
        scope.launch { settingsStore.settings.collect { dailyLimits = it.dailyLimits } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Popups, dialogs, the notification shade and the keyboard (IME) also change window
        // state; only an activity coming to the front changes the foreground app.
        if (!isActivity(pkg, event.className?.toString())) return
        if (foreground.value == pkg) return
        foreground.value = pkg
        handler.removeCallbacksAndMessages(null) // the previous app's mid-session check
        if (pkg != packageName && dailyLimits?.containsKey(pkg) != false) scope.launch { enforceLimit(pkg) }
    }

    // Reads settings fresh, so an extension granted just now on the Block screen counts.
    private suspend fun enforceLimit(pkg: String) {
        val settings = settingsStore.settings.first()
        if (foreground.value != pkg) return
        when (val decision = limitPolicy.decide(pkg, settings.dailyLimits, settings.limitExtensions)) {
            is LimitDecision.Blocked -> startActivity(BlockActivity.intent(this, pkg, decision))
            // Check again when it runs out; at least a second later, in case usage stats lag.
            is LimitDecision.Allowed -> handler.postDelayed(
                { if (foreground.value == pkg) scope.launch { enforceLimit(pkg) } },
                decision.remaining.toMillis().coerceAtLeast(1_000),
            )
            LimitDecision.Unlimited -> Unit
        }
    }

    private fun isActivity(pkg: String, className: String?): Boolean = className != null &&
        activityChecks.getOrPut(pkg to className) {
            runCatching { packageManager.getActivityInfo(ComponentName(pkg, className), 0) }.isSuccess
        }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacksAndMessages(null)
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
