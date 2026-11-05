package com.ukhirani.leanos.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ukhirani.leanos.R
import com.ukhirani.leanos.platform.InstalledApp
import com.ukhirani.leanos.ui.springPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One entry in an app's long-press menu (favorite, hide, daily limit, app info, uninstall). */
data class AppAction(val label: String, val run: () -> Unit)

/**
 * Tap and long-press for an app. Handling the long-press here consumes it, so the home screen's
 * empty-space long-press never fires too.
 */
fun Modifier.appPress(onClick: () -> Unit, onLongPress: () -> Unit): Modifier = composed {
    springPress(onClick, onLongPress, stringResource(R.string.app_open), stringResource(R.string.app_options))
}

/** Whether [AppIcon]s draw in grayscale; provided once from the settings at the root. */
val LocalGrayscaleIcons = staticCompositionLocalOf { true }

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** [app]'s icon, loaded off the main thread. Grayscale is a draw-time filter, not a new bitmap. */
@Composable
fun AppIcon(app: InstalledApp, icon: (InstalledApp) -> ImageBitmap?, size: Dp) {
    val bitmap by produceState<ImageBitmap?>(null, app) { value = withContext(Dispatchers.IO) { icon(app) } }
    val filter = if (LocalGrayscaleIcons.current) grayscale else null
    Box(Modifier.size(size)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = filter) }
    }
}

/** The favorites row: icon over name, in the thumb zone. */
@Composable
fun FavoritesRow(
    apps: List<InstalledApp>,
    icon: (InstalledApp) -> ImageBitmap?,
    onLaunch: (InstalledApp) -> Unit,
    onMenu: (InstalledApp) -> Unit,
    iconSize: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        apps.forEach { app ->
            Column(
                Modifier
                    .width(64.dp)
                    .appPress(onClick = { onLaunch(app) }, onLongPress = { onMenu(app) })
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(app, icon, iconSize)
                Text(
                    app.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** The per-app long-press menu, as a bottom sheet listing [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(app: InstalledApp, actions: List<AppAction>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            app.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(Modifier.padding(bottom = 24.dp)) {
            actions.forEach { action ->
                Text(
                    action.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { onDismiss(); action.run() }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
    }
}
