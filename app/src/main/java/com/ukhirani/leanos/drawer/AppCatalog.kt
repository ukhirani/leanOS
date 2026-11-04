package com.ukhirani.leanos.drawer

import com.ukhirani.leanos.platform.InstalledApp
import com.ukhirani.leanos.platform.InstalledAppsSource
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Turns installed apps into what the drawer shows: the full list and search results. */
class AppCatalog(source: InstalledAppsSource) {
    /** Every launchable app, alphabetical; updates live on install/uninstall. */
    val drawer: Flow<List<InstalledApp>> = source.apps.map { apps -> apps.sortedWith(alphabetical) }

    /**
     * Apps matching [query], best first: name starts with it, then a word in the name starts
     * with it, then the name contains it; alphabetical within each rank. The first result is
     * the "top match" Enter launches. A blank query returns [apps] alphabetically.
     */
    fun search(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        val q = fold(query.trim())
        if (q.isEmpty()) return apps.sortedWith(alphabetical)
        return apps
            .mapNotNull { app -> rank(fold(app.label), q)?.let { app to it } }
            .sortedWith(compareBy<Pair<InstalledApp, Int>> { it.second }.thenBy(alphabetical) { it.first })
            .map { it.first }
    }

    private companion object {
        /** 0 = starts with, 1 = a word starts with, 2 = contains, null = no match. */
        fun rank(name: String, query: String): Int? = when {
            name.startsWith(query) -> 0
            name.split(WORD_BREAK).any { it.startsWith(query) } -> 1
            query in name -> 2
            else -> null
        }

        val WORD_BREAK = Regex("[^\\p{L}\\p{N}]+")

        val alphabetical: Comparator<InstalledApp> = compareBy({ fold(it.label) }, { it.label })

        /** Case- and accent-insensitive form of [text]. */
        fun fold(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, "").lowercase(Locale.ROOT)

        val MARKS = Regex("\\p{M}+")
    }
}
