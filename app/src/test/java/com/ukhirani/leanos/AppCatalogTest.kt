package com.ukhirani.leanos

import com.ukhirani.leanos.drawer.AppCatalog
import com.ukhirani.leanos.platform.InstalledApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {
    private val source = FakeInstalledAppsSource(
        app("Spotify"), app("Maps"), app("calculator"), app("Google Play Store"), app("Clock"),
    )
    private val catalog = AppCatalog(source)

    private fun labels(apps: List<InstalledApp>) = apps.map { it.label }
    private fun drawer() = runBlocking { catalog.drawer.first() }
    private fun search(query: String) = labels(catalog.search(drawer(), query))

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
}
