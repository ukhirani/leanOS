package com.ukhirani.leanos.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.R
import com.ukhirani.leanos.ui.springPress
import com.ukhirani.leanos.platform.City
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SettingsStore
import com.ukhirani.leanos.platform.WeatherSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Settings page for the weather city: the current one, a way to remove it, and [CitySearch]. */
@Composable
fun WeatherCityScreen(
    settingsStore: SettingsStore,
    weatherSource: WeatherSource,
    onPick: (City?) -> Unit,
    onBack: () -> Unit,
) {
    val settings by settingsStore.settings.collectAsState(initial = Settings())
    LeanScaffold(stringResource(R.string.weather_city_title), onBack) { modifier ->
        Column(modifier) {
            settings.weatherCity?.let { city ->
                ListItem(
                    headlineContent = { Text(city.name) },
                    trailingContent = { TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.weather_city_clear)) } },
                )
            }
            CitySearch(weatherSource, onPick = onPick)
        }
    }
}

private sealed interface SearchState {
    data object Idle : SearchState
    data class Results(val cities: List<City>) : SearchState
    data object Failed : SearchState
}

/**
 * Type a city name, pick one of Open-Meteo's matches. Self-contained so first-run setup can reuse it.
 * Searches off the main thread, a moment after typing stops; network errors show a message, never crash.
 */
@Composable
fun CitySearch(weatherSource: WeatherSource, onPick: (City) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            state = SearchState.Idle
            return@LaunchedEffect
        }
        delay(400)
        state = try {
            SearchState.Results(withContext(Dispatchers.IO) { weatherSource.searchCities(query) })
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            SearchState.Failed
        }
    }
    Column(modifier) {
        Text(
            stringResource(R.string.weather_city_why),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.weather_city_search)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        when (val s = state) {
            SearchState.Idle -> Unit
            SearchState.Failed -> Message(stringResource(R.string.weather_city_error))
            is SearchState.Results -> if (s.cities.isEmpty()) {
                Message(stringResource(R.string.weather_city_no_results))
            } else {
                LazyColumn {
                    items(s.cities) { city ->
                        ListItem(headlineContent = { Text(city.name) }, modifier = Modifier.springPress({ onPick(city) }))
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}
