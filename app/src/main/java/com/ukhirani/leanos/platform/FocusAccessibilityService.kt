package com.ukhirani.leanos.platform

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The Focus accessibility service. It only listens to window-state changes to learn which app
 * is in front, and performs the global actions for home gestures. It reads no screen content.
 * The companion is the real [FocusService] the rest of LeanOS talks to.
 */
class FocusAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { foreground.value = it }
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
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
