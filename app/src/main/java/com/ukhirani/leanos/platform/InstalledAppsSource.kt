package com.ukhirani.leanos.platform

import kotlinx.coroutines.flow.Flow

/** One launchable activity: what the drawer lists and launches. */
data class InstalledApp(val packageName: String, val activityName: String, val label: String)

/** Launchable apps. Emits the current list, then again on every install, uninstall or update. */
interface InstalledAppsSource {
    val apps: Flow<List<InstalledApp>>
}
