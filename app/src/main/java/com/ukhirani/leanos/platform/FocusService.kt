package com.ukhirani.leanos.platform

import kotlinx.coroutines.flow.StateFlow

/** The package currently in front, or null when not known yet. */
interface ForegroundAppSource {
    val foregroundPackage: StateFlow<String?>
}

/** The Focus accessibility service: the foreground app plus the global actions home gestures use. */
interface FocusService : ForegroundAppSource {
    fun isRunning(): Boolean

    /** Each returns false when the service is off or the system refused the action. */
    fun openNotifications(): Boolean
    fun lockScreen(): Boolean
}
