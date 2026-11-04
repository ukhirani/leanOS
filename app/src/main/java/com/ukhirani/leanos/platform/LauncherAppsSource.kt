package com.ukhirani.leanos.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Installed apps from [LauncherApps], for the current user. LeanOS itself is left out. */
class LauncherAppsSource(context: Context) : InstalledAppsSource {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val self = context.packageName
    private val user: UserHandle = Process.myUserHandle()
    private val iconDpi = context.resources.displayMetrics.densityDpi

    override val apps: Flow<List<InstalledApp>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackagesAvailable(packages: Array<String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(packages: Array<String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
        }
        trySend(Unit)
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate().map { load() }.flowOn(Dispatchers.IO)

    private fun load(): List<InstalledApp> = launcherApps.getActivityList(null, user)
        .filter { it.componentName.packageName != self }
        .map { InstalledApp(it.componentName.packageName, it.componentName.className, it.label.toString()) }

    /** The app's launcher icon, or null if it is gone. Call off the main thread. */
    fun icon(app: InstalledApp): ImageBitmap? {
        val info = launcherApps.resolveActivity(Intent().setComponent(component(app)), user) ?: return null
        val drawable = info.getIcon(iconDpi) ?: return null
        val size = drawable.intrinsicWidth.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }

    fun launch(app: InstalledApp) {
        // An app uninstalled since the list was drawn just doesn't launch.
        runCatching { launcherApps.startMainActivity(component(app), user, null, null) }
    }

    private fun component(app: InstalledApp) = ComponentName(app.packageName, app.activityName)
}
