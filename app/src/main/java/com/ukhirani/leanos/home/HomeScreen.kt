package com.ukhirani.leanos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.platform.AlarmSource
import com.ukhirani.leanos.platform.BatterySource
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import com.ukhirani.leanos.ui.ClockFontFamily
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    homeClock: HomeClock,
    settingsStore: SettingsStore,
    gestures: HomeGestures,
    homeLayout: HomeLayout,
    alarmSource: AlarmSource,
    batterySource: BatterySource,
    onSwipeUp: () -> Unit = {},
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
            // Light scrim over the wallpaper so text keeps its contrast.
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            // Swipe up (drawer) and long-press (settings) plug into this same modifier.
            .homeGestures(onSwipeDown = onSwipeDown, onDoubleTap = onDoubleTap, onSwipeUp = onSwipeUp)
            .safeDrawingPadding(),
    ) {
        Column(Modifier.align(Alignment.TopCenter).padding(top = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Clock(
                time,
                Modifier.clickable(onClickLabel = "Open alarms") { context.openAlarmApp(alarm) },
            )
            // Recomputed each minute with the clock, so the alarm countdown and date stay current.
            val chips = remember(time, settings, alarm, battery) { homeLayout.chips(settings, alarm, battery) }
            GlanceChipRow(chips, alarm, Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp))
        }
    }
}

@Composable
private fun Clock(time: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Sized from the screen width in dp, not sp, so large system text can't make it clip.
        val size = with(LocalDensity.current) { (maxWidth / 3.4f).toSp() }
        Text(
            text = time,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontFamily = ClockFontFamily,
                fontSize = size,
                lineHeight = size,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}
