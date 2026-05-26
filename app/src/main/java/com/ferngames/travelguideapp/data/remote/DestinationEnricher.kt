package com.ferngames.travelguideapp.data.remote

import com.ferngames.travelguideapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DestinationEnricher {

    // Get beautiful photo from Unsplash
    suspend fun getUnsplashPhoto(query: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode("$query travel", "UTF-8")
                val url = URL(
                    "https://api.unsplash.com/search/photos" +
                            "?query=$encodedQuery" +
                            "&per_page=1" +
                            "&orientation=landscape" +
                            "&client_id=${BuildConfig.UNSPLASH_API_KEY}"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val results = json.getJSONArray("results")
                    if (results.length() > 0) {
                        results.getJSONObject(0)
                            .getJSONObject("urls")
                            .getString("regular")
                    } else ""
                } else ""
            } catch (e: Exception) {
                android.util.Log.e("UNSPLASH", "Error: ${e.message}")
                ""
            }
        }
    }

    // Get country info from RestCountries
    suspend fun getCountryInfo(countryName: String): CountryInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedName = URLEncoder.encode(countryName, "UTF-8")
                val url = URL(
                    "https://restcountries.com/v3.1/name/$encodedName" +
                            "?fields=name,currencies,languages,flags,population,capital"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() > 0) {
                        val country = jsonArray.getJSONObject(0)

                        // Get currency
                        val currencies = country.getJSONObject("currencies")
                        val currencyCode = currencies.keys().next()
                        val currencyName = currencies
                            .getJSONObject(currencyCode)
                            .getString("name")

                        // Get language
                        val languages = country.getJSONObject("languages")
                        val language = languages.getString(languages.keys().next())

                        // Get flag
                        val flag = country.getJSONObject("flags").getString("png")

                        // Get capital
                        val capital = country.optJSONArray("capital")
                            ?.getString(0) ?: ""

                        // Get population
                        val population = country.getLong("population")

                        CountryInfo(
                            currency = "$currencyCode - $currencyName",
                            language = language,
                            flag = flag,
                            capital = capital,
                            population = population
                        )
                    } else null
                } else null
            } catch (e: Exception) {
                android.util.Log.e("RESTCOUNTRIES", "Error: ${e.message}")
                null
            }
        }
    }

    // Get weather from Open-Meteo
    suspend fun getWeather(latitude: Double, longitude: Double): WeatherInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$latitude" +
                            "&longitude=$longitude" +
                            "&current=temperature_2m,weather_code,wind_speed_10m" +
                            "&timezone=auto"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val current = json.getJSONObject("current")
                    val temp = current.getDouble("temperature_2m")
                    val weatherCode = current.getInt("weather_code")
                    val windSpeed = current.getDouble("wind_speed_10m")

                    WeatherInfo(
                        temperature = temp,
                        description = getWeatherDescription(weatherCode),
                        windSpeed = windSpeed,
                        emoji = getWeatherEmoji(weatherCode)
                    )
                } else null
            } catch (e: Exception) {
                android.util.Log.e("WEATHER", "Error: ${e.message}")
                null
            }
        }
    }

    // Get AI description from Groq
    suspend fun getAIDescription(placeName: String, country: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.groq.com/openai/v1/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer ${BuildConfig.GROQ_API_KEY}"
                )
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("model", "llama-3.3-70b-versatile")
                    put("max_tokens", 150)
                    put("messages", org.json.JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content",
                                "Write a 2-sentence travel description for $placeName, $country. " +
                                        "Be inspiring and mention what makes it special. No emojis.")
                        }
                    ))
                }

                val writer = java.io.OutputStreamWriter(connection.outputStream)
                writer.write(body.toString())
                writer.flush()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    JSONObject(response)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else ""
            } catch (e: Exception) {
                android.util.Log.e("GROQ_DESC", "Error: ${e.message}")
                ""
            }
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Rain showers"
            95 -> "Thunderstorm"
            else -> "Variable"
        }
    }

    private fun getWeatherEmoji(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2, 3 -> "⛅"
            45, 48 -> "🌫️"
            51, 53, 55 -> "🌦️"
            61, 63, 65 -> "🌧️"
            71, 73, 75 -> "❄️"
            80, 81, 82 -> "🌨️"
            95 -> "⛈️"
            else -> "🌤️"
        }
    }
}

data class CountryInfo(
    val currency: String,
    val language: String,
    val flag: String,
    val capital: String,
    val population: Long
)

data class WeatherInfo(
    val temperature: Double,
    val description: String,
    val windSpeed: Double,
    val emoji: String
)