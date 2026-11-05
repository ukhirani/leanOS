package com.ukhirani.leanos.home

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.ukhirani.leanos.platform.MediaListenerService

/** Starts the first of [intents] some app handles; nothing happens when none is. */
internal fun Context.startFirstAvailable(vararg intents: Intent) {
    for (intent in intents) {
        try {
            return startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}

/** Our own usage-access page where the system offers one, else the full list. */
internal fun Context.openUsageAccessSettings() = startFirstAvailable(
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")),
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
)

/** Our own notification-access page where the system offers one, else the full list. */
internal fun Context.openNotificationAccessSettings() {
    val listener = ComponentName(this, MediaListenerService::class.java).flattenToString()
    startFirstAvailable(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener),
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
    )
}

internal fun Context.openAccessibilitySettings() = startFirstAvailable(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

/** [packageName]'s display name, or the package name itself when it isn't installed. */
fun Context.appLabel(packageName: String): String = try {
    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
} catch (_: PackageManager.NameNotFoundException) {
    packageName
}

/** Sends another app's intent. Android 14+ only lets it start an activity if we, the visible app, opt in. */
fun PendingIntent.sendFromForeground(context: Context) {
    val options = if (Build.VERSION.SDK_INT >= 34) {
        @Suppress("DEPRECATION")
        ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            .toBundle()
    } else {
        null
    }
    send(context, 0, null, null, null, null, options)
}
