package com.ukhirani.leanos

import com.ukhirani.leanos.drawer.AppCatalog
import com.ukhirani.leanos.drawer.FavoriteChange
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.platform.InstalledApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {
    private val source = FakeInstalledAppsSource(
        app("Spotify"), app("Maps"), app("calculator"), app("Google Play Store"), app("Clock"),
    )
    private val settings = FakeSettingsStore()
    private val catalog = AppCatalog(source, settings)

    private fun labels(apps: List<InstalledApp>) = apps.map { it.label }
    private fun drawer() = runBlocking { catalog.drawer.first() }
    private fun search(query: String) = labels(catalog.search(drawer(), query))
    private fun favorites() = labels(runBlocking { catalog.favorites.first() })
    private fun toggle(label: String) = runBlocking { catalog.toggleFavorite(drawer().first { it.label == label }) }
    private fun stored() = runBlocking { settings.settings.first().favorites }

    @Test
    fun `drawer lists every launchable app alphabetically, ignoring case`() {
        assertEquals(
            listOf("calculator", "Clock", "Google Play Store", "Maps", "Spotify"),
            labels(drawer()),
        )
    }

    @Test
    fun `installing and uninstalling apps updates the drawer`() {
        source.install(app("Authenticator"))
        source.uninstall("pkg.maps")
        assertEquals(
            listOf("Authenticator", "calculator", "Clock", "Google Play Store", "Spotify"),
            labels(drawer()),
        )
    }

    @Test
    fun `empty or blank query returns the whole drawer`() {
        assertEquals(labels(drawer()), search(""))
        assertEquals(labels(drawer()), search("  "))
    }

    @Test
    fun `search ranks starts-with over word-starts-with over contains`() {
        val apps = listOf(app("Play Games"), app("Google Play Store"), app("Display Settings"), app("Spotify"))
        assertEquals(
            listOf("Play Games", "Google Play Store", "Display Settings"),
            labels(catalog.search(apps, "play")),
        )
    }

    @Test
    fun `ties within a rank are alphabetical`() {
        val apps = listOf(app("Calendar"), app("calculator"), app("Camera"), app("Phone"))
        assertEquals(listOf("calculator", "Calendar", "Camera"), labels(catalog.search(apps, "ca")))
    }

    @Test
    fun `search ignores case and accents in both name and query`() {
        val apps = listOf(app("Café Finder"), app("Maps"))
        assertEquals(listOf("Café Finder"), labels(catalog.search(apps, "CAFE")))
        assertEquals(listOf("Café Finder"), labels(catalog.search(apps, "café")))
    }

    @Test
    fun `query with no match returns nothing`() {
        assertEquals(emptyList<String>(), search("zzz"))
    }

    @Test
    fun `the top match is what Enter launches`() {
        assertEquals("Spotify", catalog.search(drawer(), "spo").first().label)
    }

    @Test
    fun `favorites row keeps the order favorites were added in`() {
        assertEquals(FavoriteChange.Added, toggle("Spotify"))
        toggle("Clock")
        toggle("Maps")
        assertEquals(listOf("Spotify", "Clock", "Maps"), favorites())
    }

    @Test
    fun `toggling a favorite again removes it`() {
        toggle("Spotify")
        toggle("Clock")
        assertEquals(FavoriteChange.Removed, toggle("Spotify"))
        assertEquals(listOf("Clock"), favorites())
    }

    @Test
    fun `uninstalled favorites drop out of the row`() {
        toggle("Spotify")
        toggle("Maps")
        source.uninstall("pkg.spotify")
        assertEquals(listOf("Maps"), favorites())
    }

    @Test
    fun `a seventh favorite is refused`() {
        listOf("Spotify", "Maps", "calculator", "Google Play Store", "Clock").forEach { toggle(it) }
        source.install(app("Camera"))
        source.install(app("Phone"))
        assertEquals(FavoriteChange.Added, toggle("Camera"))
        assertEquals(FavoriteChange.LimitReached, toggle("Phone"))
        assertEquals(listOf("Spotify", "Maps", "calculator", "Google Play Store", "Clock", "Camera"), favorites())
    }

    @Test
    fun `uninstalled favorites free their slot`() {
        val six = FakeSettingsStore(Settings(favorites = listOf("gone.1", "gone.2", "pkg.maps", "gone.3", "gone.4", "gone.5")))
        val catalog = AppCatalog(source, six)
        val spotify = drawer().first { it.label == "Spotify" }
        assertEquals(FavoriteChange.Added, runBlocking { catalog.toggleFavorite(spotify) })
        assertEquals(listOf("pkg.maps", "pkg.spotify"), runBlocking { six.settings.first().favorites })
    }

    @Test
    fun `an app with several launcher activities shows once in the row`() {
        source.install(InstalledApp("pkg.maps", "pkg.maps.Navigation", "Maps Navigation"))
        toggle("Maps")
        assertEquals(listOf("Maps"), favorites())
        assertEquals(listOf("pkg.maps"), stored())
    }

    private fun hide(label: String, hidden: Boolean = true) =
        runBlocking { catalog.setHidden(source.apps.first().first { it.label == label }, hidden) }

    @Test
    fun `a hidden app leaves the drawer`() {
        hide("Maps")
        assertEquals(listOf("calculator", "Clock", "Google Play Store", "Spotify"), labels(drawer()))
    }

    @Test
    fun `a hidden app never matches a search`() {
        hide("Spotify")
        assertEquals(emptyList<String>(), search("spo"))
    }

    @Test
    fun `hiding a favorite removes it from the row and frees its slot`() {
        toggle("Spotify")
        toggle("Maps")
        hide("Spotify")
        assertEquals(listOf("Maps"), favorites())
        assertEquals(listOf("pkg.maps"), stored())
    }

    @Test
    fun `an unhidden app returns to the drawer`() {
        hide("Maps")
        hide("Maps", hidden = false)
        assertEquals(listOf("calculator", "Clock", "Google Play Store", "Maps", "Spotify"), labels(drawer()))
    }

    @Test
    fun `the hidden list shows installed hidden apps alphabetically`() {
        hide("Spotify")
        hide("Clock")
        source.uninstall("pkg.clock")
        assertEquals(listOf("Spotify"), labels(runBlocking { catalog.hidden.first() }))
        hide("Maps")
        assertEquals(listOf("Maps", "Spotify"), labels(runBlocking { catalog.hidden.first() }))
    }
}
