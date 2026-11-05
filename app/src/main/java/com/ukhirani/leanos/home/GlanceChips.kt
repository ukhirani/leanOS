package com.ukhirani.leanos.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.platform.NextAlarm
import com.ukhirani.leanos.ui.springPress

/** The info line under the clock: "Wed, 4 Nov · 82% · ⏰ 7:00 · in 21h 19m". Each segment keeps its own tap. */
@Composable
fun GlanceChipRow(chips: List<GlanceChip>, alarm: NextAlarm?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FlowRow(modifier) {
        chips.forEachIndexed { index, chip ->
            val onClick: (() -> Unit)? = when (chip.kind) {
                GlanceChip.Kind.Date -> ({ context.openCalendarApp() })
                GlanceChip.Kind.Alarm -> ({ context.openAlarmApp(alarm) })
                GlanceChip.Kind.Battery -> null
            }
            Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    (if (onClick != null) Modifier.springPress(onClick) else Modifier)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = chip.description },
                    contentAlignment = Alignment.Center,
                ) { InfoText(chip.text) }
                // The separator rides with its segment, so a wrapped line never starts with one.
                if (index < chips.lastIndex) InfoText("·", Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
private fun InfoText(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

/** Opens the alarm app: the next alarm's own intent if it set one, else the system alarm list. */
fun Context.openAlarmApp(alarm: NextAlarm?) {
    try {
        val pending = alarm?.showIntent ?: return startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        pending.sendFromForeground(this)
    } catch (_: PendingIntent.CanceledException) {
        startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }
}

fun Context.openCalendarApp() =
    startSafely(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))

private fun Context.startSafely(intent: Intent) = startFirstAvailable(intent)
