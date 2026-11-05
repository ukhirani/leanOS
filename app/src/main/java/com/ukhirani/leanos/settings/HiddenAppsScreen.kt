package com.ukhirani.leanos.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.R
import com.ukhirani.leanos.ui.springPress
import com.ukhirani.leanos.drawer.AppIcon
import com.ukhirani.leanos.platform.InstalledApp

/** Hidden apps, each one openable (tap or Open) or unhidable, so hiding is never permanent. */
@Composable
fun HiddenAppsScreen(
    apps: List<InstalledApp>,
    icon: (InstalledApp) -> ImageBitmap?,
    iconSize: Dp,
    onOpen: (InstalledApp) -> Unit,
    onUnhide: (InstalledApp) -> Unit,
    onBack: () -> Unit,
) {
    LeanScaffold(stringResource(R.string.hidden_apps_title), onBack) { modifier ->
        if (apps.isEmpty()) {
            Text(stringResource(R.string.hidden_apps_empty), modifier.padding(16.dp))
        } else {
            LazyColumn(modifier) {
                items(apps, key = { it.packageName + "/" + it.activityName }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        leadingContent = { AppIcon(app, icon, iconSize) },
                        trailingContent = {
                            TextButton(onClick = { onUnhide(app) }) { Text(stringResource(R.string.hidden_apps_unhide)) }
                        },
                        modifier = Modifier.springPress({ onOpen(app) }, onClickLabel = stringResource(R.string.hidden_apps_open)),
                    )
                }
            }
        }
    }
}
