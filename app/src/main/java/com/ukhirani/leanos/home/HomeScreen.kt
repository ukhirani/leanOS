package com.ukhirani.leanos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.R
import com.ukhirani.leanos.platform.AlarmSource
import com.ukhirani.leanos.platform.BackgroundMode
import com.ukhirani.leanos.platform.BatterySource
import com.ukhirani.leanos.platform.ClockStyle
import com.ukhirani.leanos.platform.MediaSource
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import com.ukhirani.leanos.ui.rememberClockFontFamily
import com.ukhirani.leanos.usage.ScreenTimeToday
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeScreen(
    homeClock: HomeClock,
    settingsStore: SettingsStore,
    gestures: HomeGestures,
    homeLayout: HomeLayout,
    alarmSource: AlarmSource,
    batterySource: BatterySource,
    onSwipeUp: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    usageAccess: Boolean = false,
    screenTime: ScreenTimeToday? = null,
    onSetCity: () -> Unit = {},
    onRetryWeather: () -> Unit = {},
    mediaSource: MediaSource? = null,
    mediaAccess: Boolean = false,
    favorites: @Composable () -> Unit = {},
) {
    val settings by settingsStore.settings.collectAsState(initial = Settings())
    val alarm by alarmSource.nextAlarm.collectAsState(initial = null)
    val battery by batterySource.battery.collectAsState(initial = null)
    val context = LocalContext.current
    var showFocusPrompt by remember { mutableStateOf(false) }
    val onSwipeDown = remember(gestures) {
        { showFocusPrompt = gestures.onSwipeDown() == GestureOutcome.EnableServicePrompt }
    }
    val onDoubleTap = remember(gestures) {
        { showFocusPrompt = gestures.onDoubleTap() == GestureOutcome.EnableServicePrompt }
    }
    if (showFocusPrompt) FocusServicePrompt(onDismiss = { showFocusPrompt = false })
    var time by remember(settings) { mutableStateOf(homeClock.text(settings)) }
    LaunchedEffect(settings) {
        while (true) {
            time = homeClock.text(settings)
            delay(homeClock.millisUntilNextMinute())
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Light scrim over the wallpaper so text keeps its contrast, or a solid Material You fill.
            .background(
                when (settings.background) {
                    BackgroundMode.WALLPAPER -> MaterialTheme.colorScheme.surface.copy(alpha = if (isSystemInDarkTheme()) 0.65f else 0.35f)
                    BackgroundMode.SOLID -> MaterialTheme.colorScheme.surface
                },
            )
            // Swipe up (drawer) and long-press (settings) plug into this same modifier.
            .homeGestures(onSwipeDown = onSwipeDown, onDoubleTap = onDoubleTap, onSwipeUp = onSwipeUp, onLongPress = onOpenSettings)
            .safeDrawingPadding(),
    ) {
        ClockGlow(Modifier.align(Alignment.TopStart).padding(top = 24.dp).fillMaxWidth(0.7f).height(200.dp))
        Column(Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 28.dp, end = 28.dp)) {
            Clock(
                time,
                settings.clockStyle,
                Modifier.clickable(onClickLabel = stringResource(R.string.open_alarms)) { context.openAlarmApp(alarm) },
            )
            // Recomputed each minute with the clock, so the alarm countdown and date stay current.
            val chips = remember(time, settings, alarm, battery) { homeLayout.chips(settings, alarm, battery) }
            GlanceChipRow(chips, alarm)
            // Re-subscribed when access changes, so granting it on the settings page shows the card on return.
            val nowPlaying by remember(mediaSource, mediaAccess) { mediaSource?.nowPlaying ?: flowOf(null) }.collectAsState(initial = null)
            // Keyed on time too, so the weather's "Updated 3h ago" ages with the clock.
            val cards = remember(usageAccess, screenTime, mediaAccess, nowPlaying, settings, time) {
                homeLayout.cards(
                    usageAccess, screenTime,
                    mediaAccess = mediaSource == null || mediaAccess, nowPlaying = nowPlaying, settings = settings,
                )
            }
            GlanceCards(cards, Modifier.padding(top = 8.dp), media = mediaSource, onSetCity = onSetCity, onRetryWeather = onRetryWeather)
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start = 28.dp, end = 28.dp, bottom = 24.dp)) { favorites() }
    }
}

@Composable
private fun Clock(time: String, style: ClockStyle, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // shortcut: sized from the screen width in dp, so it ignores the system font scale; scale it by a clamped fontScale if users want that.
        val base = with(LocalDensity.current) { (maxWidth * 0.6f / 3.4f).toSp() }
        // The wide styles overflow at that size: shrink step by step until the whole time fits on one line.
        var shrink by remember(time, style, maxWidth) { mutableStateOf(1f) }
        val size = base * shrink
        Text(
            text = time,
            style = TextStyle(
                fontFamily = rememberClockFontFamily(style),
                fontSize = size,
                lineHeight = size,
                color = MaterialTheme.colorScheme.primary,
            ),
            maxLines = 1,
            softWrap = false,
            onTextLayout = { if (it.didOverflowWidth && shrink > 0.3f) shrink *= 0.92f },
        )
    }
}
