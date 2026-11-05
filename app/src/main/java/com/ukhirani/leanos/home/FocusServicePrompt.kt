package com.ukhirani.leanos.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ukhirani.leanos.R

/** Shown when a gesture needs the Focus service and it is off. */
@Composable
fun FocusServicePrompt(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_prompt_title)) },
        text = { Text(stringResource(R.string.focus_prompt_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                context.openAccessibilitySettings()
            }) { Text(stringResource(R.string.focus_prompt_open)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.focus_prompt_not_now)) }
        },
    )
}
