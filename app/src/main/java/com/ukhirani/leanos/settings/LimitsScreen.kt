package com.ukhirani.leanos.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.home.openAccessibilitySettings
import com.ukhirani.leanos.home.openUsageAccessSettings
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.ukhirani.leanos.R
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import com.ukhirani.leanos.usage.ScreenTimeToday
import java.time.Duration
import kotlinx.coroutines.launch

/** Whether daily limits can work: they need usage access to count and the Focus service to block. */
data class LimitsAccess(val usage: Boolean, val focusService: Boolean)

/** A tap-to-enable warning for each missing piece, so a limit never silently does nothing. */
@Composable
private fun LimitsWarning(access: LimitsAccess, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier) {
        if (!access.usage) {
            WarningRow(R.string.limits_need_usage) { context.openUsageAccessSettings() }
        }
        if (!access.focusService) {
            WarningRow(R.string.limits_need_focus) { context.openAccessibilitySettings() }
        }
    }
}

@Composable
private fun WarningRow(text: Int, onClick: () -> Unit) {
    Text(
        stringResource(text),
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

/** Every daily limit with today's usage against it; tap one to edit or remove it. */
@Composable
fun LimitsScreen(
    settingsStore: SettingsStore,
    labelOf: (String) -> String,
    usage: ScreenTimeToday?,
    access: LimitsAccess,
    durationText: (Duration) -> String,
    onBack: () -> Unit,
) {
    val settings by settingsStore.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<String?>(null) }
    LeanScaffold(stringResource(R.string.limits_title), onBack) { modifier ->
        val limits = settings.dailyLimits.entries.sortedBy { labelOf(it.key).lowercase() }
        Column(modifier) {
            LimitsWarning(access)
            if (limits.isEmpty()) ListItem(headlineContent = { Text(stringResource(R.string.limits_empty)) })
            LazyColumn {
                items(limits, key = { it.key }) { (pkg, minutes) ->
                    val used = usage?.apps?.firstOrNull { it.packageName == pkg }?.duration ?: Duration.ZERO
                    ListItem(
                        headlineContent = { Text(labelOf(pkg)) },
                        supportingContent = {
                            Text(stringResource(R.string.limits_usage, durationText(used), durationText(Duration.ofMinutes(minutes.toLong()))))
                        },
                        modifier = Modifier.clickable { editing = pkg },
                    )
                }
            }
        }
    }
    editing?.let { pkg ->
        LimitDialog(labelOf(pkg), settings.dailyLimits[pkg], access, onDismiss = { editing = null }) { minutes ->
            scope.launch { settingsStore.setDailyLimit(pkg, minutes) }
        }
    }
}

/** Sets an app's minutes per day. [onSave] gets null when the limit is removed. */
@Composable
fun LimitDialog(appLabel: String, current: Int?, access: LimitsAccess, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var text by remember { mutableStateOf(current?.toString().orEmpty()) }
    val minutes = text.toIntOrNull()?.takeIf { it in 1..24 * 60 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.limit_dialog_title, appLabel)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.limit_dialog_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                LimitsWarning(access)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(minutes); onDismiss() }, enabled = minutes != null) {
                Text(stringResource(R.string.limit_save))
            }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSave(null); onDismiss() }) { Text(stringResource(R.string.limit_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.limit_cancel)) }
            }
        },
    )
}
