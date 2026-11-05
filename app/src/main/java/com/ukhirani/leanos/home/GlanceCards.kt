package com.ukhirani.leanos.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.R
import com.ukhirani.leanos.platform.MediaSource
import com.ukhirani.leanos.ui.springPress

/** The glance rows under the info line, in the order HomeLayout gives them: quiet text, no surfaces. */
@Composable
fun GlanceCards(
    cards: List<GlanceCard>,
    modifier: Modifier = Modifier,
    media: MediaSource? = null,
    onSetCity: () -> Unit = {},
    onRetryWeather: () -> Unit = {},
) {
    Column(modifier) {
        cards.forEach { card ->
            when (card) {
                is GlanceCard.ScreenTime -> ScreenTimeRow(card)
                is GlanceCard.Weather -> WeatherRow(card, onSetCity)
                is GlanceCard.Media -> MediaRow(card, media)
                is GlanceCard.WeatherUnavailable -> QuietRow(stringResource(R.string.weather_unavailable), onRetryWeather)
                is GlanceCard.Enable -> EnableRow(card.feature, onSetCity)
            }
        }
    }
}

/** Primary text in onSurface, then the secondary parts in onSurfaceVariant, on one ellipsized line. */
@Composable
private fun primaryAndSecondary(primary: String, vararg secondary: String?): AnnotatedString {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    return buildAnnotatedString {
        append(primary)
        withStyle(SpanStyle(color = muted)) { secondary.filterNotNull().forEach { append(" · $it") } }
    }
}

@Composable
private fun GlanceLine(text: AnnotatedString, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
private fun WeatherRow(card: GlanceCard.Weather, onClick: () -> Unit) {
    val description = stringResource(R.string.weather_in, card.city) + ", ${card.temperature}, ${card.condition}"
    Box(
        Modifier.fillMaxWidth().springPress(onClick).heightIn(min = 48.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.CenterStart,
    ) { GlanceLine(primaryAndSecondary("${card.temperature} ${card.condition}", card.city, card.age)) }
}

@Composable
private fun ScreenTimeRow(card: GlanceCard.ScreenTime) {
    // 0 = just the line, 1 = top 3 below it, 2 = every app (only when there are more than 3).
    var level by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val rows = if (level == 2) card.all else if (level == 1) card.top else emptyList()
    val labels = remember(card) { card.all.associate { it.packageName to context.appLabel(it.packageName) } }
    val topApp = card.top.firstOrNull()?.let { "${labels[it.packageName]} ${it.time}" }
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Box(
            Modifier.fillMaxWidth()
                .springPress({ level = if (level == 0 || (level == 1 && card.all.size > card.top.size)) level + 1 else 0 })
                .heightIn(min = 48.dp),
            contentAlignment = Alignment.CenterStart,
        ) { GlanceLine(primaryAndSecondary(stringResource(R.string.glance_screen_time_today, card.total), topApp)) }
        rows.forEach { app ->
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                val style = MaterialTheme.typography.bodyMedium
                val muted = MaterialTheme.colorScheme.onSurfaceVariant
                Text(labels[app.packageName] ?: app.packageName, Modifier.weight(1f).padding(end = 12.dp), style = style, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.time, style = style, color = muted)
            }
        }
    }
}

@Composable
private fun MediaRow(card: GlanceCard.Media, media: MediaSource?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).springPress({ media?.open() }).heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
            GlanceLine(primaryAndSecondary(card.title, card.artist))
        }
        if (card.playing) {
            MediaButton(android.R.drawable.ic_media_pause, stringResource(R.string.media_pause)) { media?.playPause() }
        } else {
            MediaButton(android.R.drawable.ic_media_play, stringResource(R.string.media_play)) { media?.playPause() }
        }
        MediaButton(android.R.drawable.ic_media_next, stringResource(R.string.media_next)) { media?.next() }
    }
}

@Composable
private fun MediaButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).springPress(onClick, role = Role.Button), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = description, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Tap to enable": stands in for a feature whose permission is missing and opens its settings page. */
@Composable
private fun EnableRow(feature: GlanceCard.Feature, onSetCity: () -> Unit) {
    val context = LocalContext.current
    val text = stringResource(when (feature) {
        GlanceCard.Feature.ScreenTime -> R.string.enable_screen_time
        GlanceCard.Feature.Weather -> R.string.enable_weather
        GlanceCard.Feature.Media -> R.string.enable_media
    })
    QuietRow(text) {
        when (feature) {
            GlanceCard.Feature.ScreenTime -> context.openUsageAccessSettings()
            GlanceCard.Feature.Weather -> onSetCity()
            GlanceCard.Feature.Media -> context.openNotificationAccessSettings()
        }
    }
}

@Composable
private fun QuietRow(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().springPress(onClick).heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
