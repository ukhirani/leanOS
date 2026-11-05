package com.ukhirani.leanos

import com.ukhirani.leanos.home.GlanceCard
import com.ukhirani.leanos.home.GlanceFormat
import com.ukhirani.leanos.home.HomeLayout
import com.ukhirani.leanos.platform.City
import com.ukhirani.leanos.platform.CurrentWeather
import com.ukhirani.leanos.platform.Settings
import com.ukhirani.leanos.usage.ScreenTimeToday
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCardTest {
    private val clock = FakeClockSource("2026-11-04T12:00:00+01:00[Europe/Paris]")
    private val layout = HomeLayout(GlanceFormat(clock, Locale.US))
    private val paris = City("Paris, Île-de-France, France", 48.85, 2.35)
    private fun at(iso: String) = ZonedDateTime.parse(iso).toInstant()

    private fun cards(settings: Settings) = layout.cards(usageAccess = true, screenTime = null, settings = settings)

    @Test
    fun `without a city the weather card is a set your city chip`() {
        assertEquals(listOf(GlanceCard.Enable(GlanceCard.Feature.Weather)), cards(Settings()))
    }

    @Test
    fun `weather card shows the city, temperature and condition`() {
        val settings = Settings(weatherCity = paris, weather = CurrentWeather(11.4, 3, at("2026-11-04T11:50:00+01:00")))
        assertEquals(listOf(GlanceCard.Weather("Paris", "11°C", "Overcast", age = null)), cards(settings))
    }

    @Test
    fun `weather card follows the Fahrenheit switch`() {
        val settings = Settings(useFahrenheit = true, weatherCity = paris, weather = CurrentWeather(22.0, 0, at("2026-11-04T11:50:00+01:00")))
        assertEquals("72°F", (cards(settings).single() as GlanceCard.Weather).temperature)
    }

    @Test
    fun `offline the card shows the last known weather with its age`() {
        val settings = Settings(weatherCity = paris, weather = CurrentWeather(9.0, 61, at("2026-11-04T08:40:00+01:00")))
        assertEquals(listOf(GlanceCard.Weather("Paris", "9°C", "Rain", age = "Updated 3h ago")), cards(settings))
    }

    @Test
    fun `with a city but no weather yet the card says it is unavailable and can be retried`() {
        assertEquals(listOf(GlanceCard.WeatherUnavailable("Paris")), cards(Settings(weatherCity = paris)))
    }

    @Test
    fun `the age label appears once the weather is past the refresh age`() {
        fun age(iso: String) = (cards(Settings(weatherCity = paris, weather = CurrentWeather(9.0, 61, at(iso)))).single() as GlanceCard.Weather).age
        assertEquals(null, age("2026-11-04T11:40:00+01:00"))
        assertEquals("Updated 45m ago", age("2026-11-04T11:15:00+01:00"))
    }

    @Test
    fun `weather sits between screen time and media`() {
        val cards = layout.cards(
            usageAccess = true, screenTime = ScreenTimeToday(Duration.ZERO, emptyList()),
            mediaAccess = false, settings = Settings(),
        )
        assertEquals(
            listOf(
                GlanceCard.ScreenTime::class to null,
                GlanceCard.Enable::class to GlanceCard.Feature.Weather,
                GlanceCard.Enable::class to GlanceCard.Feature.Media,
            ),
            cards.map { it::class to (it as? GlanceCard.Enable)?.feature },
        )
    }
}
