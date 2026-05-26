package com.ferngames.travelguideapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PlacesRepository {

    suspend fun searchPlaces(query: String): List<PlacePrediction> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = URL(
                    "https://nominatim.openstreetmap.org/search" +
                            "?q=$encodedQuery" +
                            "&format=json" +
                            "&limit=10" +
                            "&addressdetails=1" +
                            "&accept-language=en" +
                            "&namedetails=1"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TravelGuideApp/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseNominatimResults(response)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("NOMINATIM", "Error: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun getWikipediaImage(placeName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val encodedName = URLEncoder.encode(placeName, "UTF-8")
                val url = URL(
                    "https://en.wikipedia.org/w/api.php" +
                            "?action=query" +
                            "&titles=$encodedName" +
                            "&prop=pageimages" +
                            "&format=json" +
                            "&pithumbsize=800"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "TravelGuideApp/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseWikipediaImage(response)
                } else {
                    getFallbackImage(placeName)
                }
            } catch (e: Exception) {
                getFallbackImage(placeName)
            }
        }
    }

    private fun parseWikipediaImage(response: String): String {
        return try {
            val json = JSONObject(response)
            val pages = json
                .getJSONObject("query")
                .getJSONObject("pages")
            val page = pages.getJSONObject(pages.keys().next())
            if (page.has("thumbnail")) {
                page.getJSONObject("thumbnail").getString("source")
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseNominatimResults(response: String): List<PlacePrediction> {
        val results = mutableListOf<PlacePrediction>()
        try {
            val jsonArray = JSONArray(response)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val displayName = item.getString("display_name")
                val parts = displayName.split(",")
                // Try to get English name from namedetails
                val nameDetails = item.optJSONObject("namedetails")
                val name = nameDetails?.optString("name:en")?.takeIf { it.isNotEmpty() }
                    ?: nameDetails?.optString("name")?.takeIf { it.isNotEmpty() }
                    ?: parts.firstOrNull()?.trim()
                    ?: displayName

                val address = parts.drop(1).joinToString(",").trim()
                val lat = item.getString("lat").toDoubleOrNull() ?: 0.0
                val lon = item.getString("lon").toDoubleOrNull() ?: 0.0
                val type = item.optString("type", "")
                val placeId = item.optString("place_id", "")

                results.add(
                    PlacePrediction(
                        placeId = placeId,
                        name = name,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        type = type,
                        imageUrl = getFallbackImage(name)
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NOMINATIM", "Parse error: ${e.message}")
        }
        return results
    }

    private fun getFallbackImage(name: String): String {
        val seed = name.hashCode().and(0x7FFFFFFF) % 1000
        return "https://picsum.photos/seed/$seed/800/600"
    }
}

data class PlacePrediction(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val imageUrl: String
)

data class PlaceDetails(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val photoMetadata: Any?,
    val description: String
)