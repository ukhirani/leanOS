package com.ukhirani.leanos.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ukhirani.leanos.R
import com.ukhirani.leanos.home.GlanceCard.Feature
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import kotlinx.coroutines.launch

/** Show, hide and reorder the glance cards. Move up/down buttons keep reordering accessible without dragging. */
@Composable
fun GlanceCardsScreen(settingsStore: SettingsStore, onBack: () -> Unit) {
    val settings by settingsStore.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    fun save(order: List<Feature>, hidden: Set<Feature>) { scope.launch { settingsStore.setGlanceCards(order, hidden) } }
    fun move(from: Int, to: Int) = save(settings.cardOrder.toMutableList().apply { add(to, removeAt(from)) }, settings.hiddenCards)

    LeanScaffold(stringResource(R.string.glance_cards_title), onBack) { modifier ->
        Column(modifier) {
            val order = settings.cardOrder
            order.forEachIndexed { i, feature ->
                val name = stringResource(when (feature) {
                    Feature.ScreenTime -> R.string.glance_card_screen_time
                    Feature.Weather -> R.string.glance_card_weather
                    Feature.Media -> R.string.glance_card_media
                })
                val up = stringResource(R.string.glance_card_move_up, name)
                val down = stringResource(R.string.glance_card_move_down, name)
                ListItem(
                    headlineContent = { Text(name) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { move(i, i - 1) }, enabled = i > 0, modifier = Modifier.semantics { contentDescription = up }) { Text("▲") }
                            IconButton(onClick = { move(i, i + 1) }, enabled = i < order.lastIndex, modifier = Modifier.semantics { contentDescription = down }) { Text("▼") }
                            Switch(
                                checked = feature !in settings.hiddenCards,
                                onCheckedChange = { shown -> save(order, if (shown) settings.hiddenCards - feature else settings.hiddenCards + feature) },
                                modifier = Modifier.semantics { contentDescription = name },
                            )
                        }
                    },
                )
            }
        }
    }
}
