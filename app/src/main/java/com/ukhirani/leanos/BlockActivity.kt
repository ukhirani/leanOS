package com.ukhirani.leanos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.home.appLabel
import com.ukhirani.leanos.platform.DataStoreSettingsStore
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.SystemClockSource
import com.ukhirani.leanos.ui.LeanTheme
import com.ukhirani.leanos.usage.LimitDecision
import java.time.Duration
import kotlinx.coroutines.launch

/**
 * The Block screen: covers an app whose daily limit is used up. Back goes home; "5 more minutes"
 * (once per app per day) returns to the app.
 */
class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val label = appLabel(pkg)
        val used = GlanceFormat(SystemClockSource).durationText(Duration.ofMillis(intent.getLongExtra(EXTRA_USED_MS, 0)))
        val canExtend = intent.getBooleanExtra(EXTRA_CAN_EXTEND, false)
        val settingsStore = DataStoreSettingsStore(this)
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = Settings())
            LeanTheme(settings) {
                BackHandler { goHome() }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.safeDrawingPadding().padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(label, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                        Text(
                            stringResource(R.string.block_used_today, used),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(R.string.block_unlocks),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = ::goHome, modifier = Modifier.padding(top = 16.dp).heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.block_go_home))
                        }
                        if (canExtend) {
                            TextButton(onClick = { extend(settingsStore, pkg) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.block_extend))
                            }
                        }
                    }
                }
            }
        }
    }

    // Saved before finishing, so the Focus service sees it when the app comes back to the front.
    private fun extend(store: DataStoreSettingsStore, pkg: String) {
        lifecycleScope.launch {
            store.setLimitExtension(pkg, SystemClockSource.now().toInstant())
            finish()
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_USED_MS = "used_ms"
        private const val EXTRA_CAN_EXTEND = "can_extend"

        fun intent(context: Context, packageName: String, blocked: LimitDecision.Blocked): Intent =
            Intent(context, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_USED_MS, blocked.used.toMillis())
                .putExtra(EXTRA_CAN_EXTEND, blocked.canExtend)
    }
}
