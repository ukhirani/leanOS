package com.ukhirani.leanos.platform

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import org.json.JSONObject

/** A place weather is shown for: its display name ("Paris, Île-de-France, France") and coordinates. */
data class City(val name: String, val latitude: Double, val longitude: Double)

/** Current conditions as fetched: temperature in °C, WMO weather code, and when it was fetched. */
data class CurrentWeather(val celsius: Double, val code: Int, val at: Instant)

/** City geocoding plus current conditions. Calls block on the network: never call them on the main thread. Failures throw. */
interface WeatherSource {
    fun searchCities(query: String): List<City>
    fun current(city: City): CurrentWeather
}

/** Open-Meteo: free, no key. Platform HTTP and JSON only. */
class OpenMeteoWeatherSource(private val now: () -> Instant = Instant::now) : WeatherSource {

    override fun searchCities(query: String): List<City> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&format=json")
        // No "results" key means no match.
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            val name = listOf(r.getString("name"), r.optString("admin1"), r.optString("country"))
                .filter { it.isNotBlank() }.distinct().joinToString(", ")
            City(name, r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }

    override fun current(city: City): CurrentWeather {
        val json = get(
            "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
                "&current=temperature_2m,weather_code&temperature_unit=celsius",
        )
        val current = json.getJSONObject("current")
        return CurrentWeather(current.getDouble("temperature_2m"), current.getInt("weather_code"), now())
    }

    private fun get(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("HTTP ${connection.responseCode} from $url")
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}
