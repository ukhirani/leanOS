package com.ukhirani.leanos.drawer

import com.ukhirani.leanos.platform.InstalledApp
import com.ukhirani.leanos.platform.InstalledAppsSource
import com.ukhirani.leanos.platform.SettingsStore
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class FavoriteChange { Added, Removed, LimitReached }

/** Turns installed apps and settings into what home and the drawer show. */
class AppCatalog(private val source: InstalledAppsSource, private val settingsStore: SettingsStore) {
    /** Every launchable app that isn't hidden, alphabetical; updates live on install/uninstall. */
    val drawer: Flow<List<InstalledApp>> = combine(source.apps, settingsStore.settings) { apps, settings ->
        apps.filter { it.packageName !in settings.hidden }.sortedWith(alphabetical)
    }

    /** Installed apps the user has hidden, alphabetical: the Hidden apps page. */
    val hidden: Flow<List<InstalledApp>> = combine(source.apps, settingsStore.settings) { apps, settings ->
        apps.filter { it.packageName in settings.hidden }.sortedWith(alphabetical)
    }

    /** The favorites row, in the user's order; uninstalled favorites drop out. One entry per package. */
    val favorites: Flow<List<InstalledApp>> = combine(source.apps, settingsStore.settings) { apps, settings ->
        settings.favorites.mapNotNull { pkg -> apps.filter { it.packageName == pkg }.minWithOrNull(alphabetical) }
    }

    /** Adds [app]'s package to the end of the favorites, or removes it; refuses past [MAX_FAVORITES]. */
    suspend fun toggleFavorite(app: InstalledApp): FavoriteChange {
        val installed = source.apps.first().mapTo(HashSet()) { it.packageName }
        // Uninstalled favorites are pruned here so they don't hold a slot.
        val current = settingsStore.settings.first().favorites.filter { it in installed }
        val (next, change) = when {
            app.packageName in current -> current - app.packageName to FavoriteChange.Removed
            current.size >= MAX_FAVORITES -> return FavoriteChange.LimitReached
            else -> current + app.packageName to FavoriteChange.Added
        }
        settingsStore.setFavorites(next)
        return change
    }

    /** Hides [app]'s package from the drawer and search (and drops it from favorites), or brings it back. */
    suspend fun setHidden(app: InstalledApp, hidden: Boolean) {
        settingsStore.setAppHidden(app.packageName, hidden)
    }

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

    companion object {
        const val MAX_FAVORITES = 6

        /** 0 = starts with, 1 = a word starts with, 2 = contains, null = no match. */
        private fun rank(name: String, query: String): Int? = when {
            name.startsWith(query) -> 0
            name.split(WORD_BREAK).any { it.startsWith(query) } -> 1
            query in name -> 2
            else -> null
        }

        private val WORD_BREAK = Regex("[^\\p{L}\\p{N}]+")

        private val alphabetical: Comparator<InstalledApp> = compareBy({ fold(it.label) }, { it.label })

        /** Case- and accent-insensitive form of [text]. */
        private fun fold(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, "").lowercase(Locale.ROOT)

        private val MARKS = Regex("\\p{M}+")
    }
}
