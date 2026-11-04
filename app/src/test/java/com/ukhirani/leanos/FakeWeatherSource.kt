package com.ukhirani.leanos

import com.ukhirani.leanos.platform.City
import com.ukhirani.leanos.platform.CurrentWeather
import com.ukhirani.leanos.platform.WeatherSource
import java.time.Instant

class FakeWeatherSource : WeatherSource {
    var weather = CurrentWeather(celsius = 20.0, code = 0, at = Instant.EPOCH)
    var failure: Exception? = null
    val fetchedFor = mutableListOf<City>()
    val fetches get() = fetchedFor.size

    override fun searchCities(query: String): List<City> {
        failure?.let { throw it }
        return emptyList()
    }

    override fun current(city: City): CurrentWeather {
        fetchedFor += city
        failure?.let { throw it }
        return weather
    }
}
