package com.ukhirani.leanos.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.R
import com.ukhirani.leanos.platform.City
import com.ukhirani.leanos.platform.WeatherSource
import com.ukhirani.leanos.settings.CitySearch

/**
 * One step of first-run setup: why it is needed, the action that opens the right system page, and Skip.
 * Moving on when the permission is granted is the caller's job: it recomputes [nextSetupStep] on resume.
 */
@Composable
fun SetupScreen(
    step: SetupStep,
    weatherSource: WeatherSource,
    onAction: () -> Unit,
    onPickCity: (City) -> Unit,
    onSkip: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
            val permission = when (step) {
                SetupStep.WeatherCity -> null
                SetupStep.DefaultLauncher -> R.string.setup_launcher_why to R.string.setup_launcher_action
                SetupStep.UsageAccess -> R.string.setup_usage_why to R.string.setup_usage_action
                SetupStep.NotificationAccess -> R.string.setup_notification_why to R.string.setup_notification_action
                SetupStep.Accessibility -> R.string.setup_accessibility_why to R.string.setup_accessibility_action
            }
            if (permission == null) {
                CitySearch(weatherSource, onPick = onPickCity)
            } else {
                val (why, action) = permission
                Text(stringResource(why), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onAction) { Text(stringResource(action)) }
            }
            TextButton(onClick = onSkip) { Text(stringResource(R.string.setup_skip)) }
        }
    }
}
