@file:OptIn(ExperimentalMaterial3Api::class)

package com.ukhirani.leanos.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.R
import com.ukhirani.leanos.platform.Accent
import com.ukhirani.leanos.platform.BackgroundMode
import com.ukhirani.leanos.platform.ClockStyle
import com.ukhirani.leanos.platform.ColorSource
import com.ukhirani.leanos.platform.IconSize
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import kotlinx.coroutines.launch

/** LeanOS settings: the look. Every change is written to the store and applies live. */
@Composable
fun SettingsScreen(settingsStore: SettingsStore, onBack: () -> Unit, onOpenAbout: () -> Unit) {
    val settings by settingsStore.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    fun save(block: suspend SettingsStore.() -> Unit) { scope.launch { settingsStore.block() } }

    LeanScaffold(stringResource(R.string.settings_title), onBack) { modifier ->
        Column(modifier.verticalScroll(rememberScrollState())) {
            Choice(
                R.string.settings_clock_style, ClockStyle.entries, settings.clockStyle,
                label = { when (it) {
                    ClockStyle.CHUNKY -> R.string.clock_style_chunky
                    ClockStyle.CONDENSED -> R.string.clock_style_condensed
                    ClockStyle.LIGHT -> R.string.clock_style_light
                } },
            ) { save { setClockStyle(it) } }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_24_hour)) },
                trailingContent = {
                    Switch(checked = settings.use24Hour, onCheckedChange = { save { setUse24Hour(it) } })
                },
            )
            Choice(
                R.string.settings_colors, ColorSource.entries, settings.colorSource,
                label = { when (it) {
                    ColorSource.WALLPAPER -> R.string.color_source_wallpaper
                    ColorSource.FIXED_ACCENT -> R.string.color_source_accent
                } },
            ) { save { setColorSource(it) } }
            if (settings.colorSource == ColorSource.FIXED_ACCENT) {
                Choice(
                    R.string.settings_accent, Accent.entries, settings.accent,
                    label = { when (it) {
                        Accent.BLUE -> R.string.accent_blue
                        Accent.GREEN -> R.string.accent_green
                        Accent.ROSE -> R.string.accent_rose
                        Accent.AMBER -> R.string.accent_amber
                    } },
                ) { save { setAccent(it) } }
            }
            Choice(
                R.string.settings_background, BackgroundMode.entries, settings.background,
                label = { when (it) {
                    BackgroundMode.WALLPAPER -> R.string.background_wallpaper
                    BackgroundMode.SOLID -> R.string.background_solid
                } },
            ) { save { setBackground(it) } }
            Choice(
                R.string.settings_icon_size, IconSize.entries, settings.iconSize,
                label = { when (it) {
                    IconSize.SMALL -> R.string.icon_size_small
                    IconSize.MEDIUM -> R.string.icon_size_medium
                    IconSize.LARGE -> R.string.icon_size_large
                } },
            ) { save { setIconSize(it) } }
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_title)) },
                modifier = Modifier.clickable(onClick = onOpenAbout),
            )
        }
    }
}

/** A labelled single choice as a full-width Material 3 segmented button row with 48dp targets. */
@Composable
private fun <T> Choice(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(label(option)), maxLines = 1) }
            }
        }
    }
}

/** The settings destinations' shared frame: a top app bar whose back arrow returns one level. */
@Composable
internal fun LeanScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { padding -> content(Modifier.padding(padding)) }
}
