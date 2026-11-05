package com.ukhirani.leanos.home

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.platform.MediaListenerService
import com.ukhirani.leanos.platform.MediaSource

/** The glance cards under the chips, in the order HomeLayout gives them. */
@Composable
fun GlanceCards(
    cards: List<GlanceCard>,
    modifier: Modifier = Modifier,
    media: MediaSource? = null,
    onSetCity: () -> Unit = {},
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        cards.forEach { card ->
            when (card) {
                is GlanceCard.ScreenTime -> ScreenTimeCard(card)
                is GlanceCard.Weather -> WeatherCard(card, onSetCity)
                is GlanceCard.Media -> MediaCard(card, media)
                is GlanceCard.Enable -> EnableChip(card.feature, onSetCity)
            }
        }
    }
}

@Composable
private fun WeatherCard(card: GlanceCard.Weather, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        // TalkBack reads the texts inside: "Weather in Paris, 11°C, Overcast".
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Weather in ${card.city}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(card.temperature, style = MaterialTheme.typography.headlineMedium)
                Text(card.condition, Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.bodyLarge)
            }
            card.age?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(card: GlanceCard.ScreenTime) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val rows = if (expanded) card.all else card.top
    val labels = remember(rows) { rows.associate { it.packageName to context.appLabel(it.packageName) } }
    Surface(
        onClick = { expanded = !expanded },
        // TalkBack reads the texts inside: "Screen time, 1h 15m, Chat, 50m, ...".
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Screen time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(card.total, style = MaterialTheme.typography.headlineMedium)
            rows.forEach { app ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        labels[app.packageName] ?: app.packageName,
                        Modifier.weight(1f).padding(end = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(app.time, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MediaCard(card: GlanceCard.Media, media: MediaSource?) {
    Surface(
        onClick = { media?.open() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // No artwork: the text simply takes the room.
            card.artwork?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(card.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                card.artist?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            MediaButton(android.R.drawable.ic_media_previous, "Previous") { media?.previous() }
            if (card.playing) {
                MediaButton(android.R.drawable.ic_media_pause, "Pause") { media?.playPause() }
            } else {
                MediaButton(android.R.drawable.ic_media_play, "Play") { media?.playPause() }
            }
            MediaButton(android.R.drawable.ic_media_next, "Next") { media?.next() }
        }
    }
}

@Composable
private fun MediaButton(icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = description, Modifier.size(24.dp))
    }
}

/** "Tap to enable": stands in for a feature whose permission is missing and opens its settings page. */
@Composable
private fun EnableChip(feature: GlanceCard.Feature, onSetCity: () -> Unit) {
    val context = LocalContext.current
    val text = when (feature) {
        GlanceCard.Feature.ScreenTime -> "Screen time · tap to enable"
        GlanceCard.Feature.Weather -> "Weather · set your city"
        GlanceCard.Feature.Media -> "Media · tap to enable"
    }
    Surface(
        onClick = {
            when (feature) {
                GlanceCard.Feature.ScreenTime -> context.openUsageAccessSettings()
                GlanceCard.Feature.Weather -> onSetCity()
                GlanceCard.Feature.Media -> context.openNotificationAccessSettings()
            }
        },
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 14.dp), style = MaterialTheme.typography.labelLarge)
    }
}

private fun Context.appLabel(packageName: String): String = try {
    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
} catch (_: PackageManager.NameNotFoundException) {
    packageName
}

/** Our own usage-access page where the system offers one, else the full list. */
internal fun Context.openUsageAccessSettings() {
    val direct = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))
    for (intent in listOf(direct, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))) {
        try {
            return startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}

/** Our own notification-access page where the system offers one, else the full list. */
internal fun Context.openNotificationAccessSettings() {
    val listener = ComponentName(this, MediaListenerService::class.java).flattenToString()
    val direct = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener)
    for (intent in listOf(direct, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))) {
        try {
            return startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}
