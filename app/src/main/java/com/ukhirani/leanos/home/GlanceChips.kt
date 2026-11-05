package com.ukhirani.leanos.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.platform.NextAlarm

/** The row of tonal chips under the clock. */
@Composable
fun GlanceChipRow(chips: List<GlanceChip>, alarm: NextAlarm?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        chips.forEach { chip ->
            val onClick: (() -> Unit)? = when (chip.kind) {
                GlanceChip.Kind.Date -> ({ context.openCalendarApp() })
                GlanceChip.Kind.Alarm -> ({ context.openAlarmApp(alarm) })
                GlanceChip.Kind.Battery -> null
            }
            Chip(chip, onClick)
        }
    }
}

@Composable
private fun Chip(chip: GlanceChip, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(50)
    val color = MaterialTheme.colorScheme.secondaryContainer
    val modifier = Modifier
        .minimumInteractiveComponentSize()
        .semantics { contentDescription = chip.description }
    val content: @Composable () -> Unit = {
        Box(Modifier.heightIn(min = 32.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(chip.text, style = MaterialTheme.typography.labelLarge)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, content = content)
    }
}

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
