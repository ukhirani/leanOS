package com.ukhirani.leanos.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.R

/** The app version and the Google Sans Flex license (OFL 1.1), which must ship with the app. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    // Referenced from code, so release resource shrinking keeps the license file.
    val licenses = remember {
        listOf(R.raw.google_sans_ofl, R.raw.google_sans_flex_ofl).map { id ->
            context.resources.openRawResource(id).bufferedReader().use { it.readText() }
        }
    }
    LeanScaffold(stringResource(R.string.about_title), onBack) { modifier ->
        Column(modifier.verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.about_version, version)) },
            )
            listOf(R.string.about_font_license_sans, R.string.about_font_license).zip(licenses).forEach { (title, license) ->
                ListItem(headlineContent = { Text(stringResource(title)) })
                Text(
                    license,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
