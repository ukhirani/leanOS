package com.ukhirani.leanos.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The next alarm clock: when it rings, and the intent that shows it in its alarm app (if it set one). */
data class NextAlarm(val at: Instant, val showIntent: PendingIntent? = null)

/** The next alarm, or null when none is set. Emits the current value, then every change. */
interface AlarmSource {
    val nextAlarm: Flow<NextAlarm?>
}

class AndroidAlarmSource(context: Context) : AlarmSource {
    private val context = context.applicationContext
    private val alarmManager = this.context.getSystemService(AlarmManager::class.java)

    override val nextAlarm: Flow<NextAlarm?> = callbackFlow {
        fun emit() {
            trySend(alarmManager.nextAlarmClock?.let { NextAlarm(Instant.ofEpochMilli(it.triggerTime), it.showIntent) })
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = emit()
        }
        context.registerReceiver(receiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        emit()
        awaitClose { context.unregisterReceiver(receiver) }
    }
}
