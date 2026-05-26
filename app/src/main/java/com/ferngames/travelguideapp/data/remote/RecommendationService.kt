package com.ferngames.travelguideapp.data.remote

import com.ferngames.travelguideapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RecommendationService {

    suspend fun getRecommendedDestinations(): List<Pair<String, String>> {
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
                    put("max_tokens", 200)
                    put("messages", JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content",
                                "List 8 trending travel destinations for 2025. " +
                                        "Return ONLY a JSON array of objects with 'city' and 'country' fields. " +
                                        "Example: [{\"city\":\"Kyoto\",\"country\":\"Japan\"}]. " +
                                        "No explanation, just the JSON array."
                            )
                        }
                    ))
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(body.toString())
                writer.flush()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val content = JSONObject(response)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()

                    android.util.Log.d("RECOMMENDATIONS", "AI response: $content")

                    // Parse JSON array
                    val cleanContent = content
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val jsonArray = JSONArray(cleanContent)
                    val destinations = mutableListOf<Pair<String, String>>()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val city = item.getString("city")
                        val country = item.getString("country")
                        destinations.add(city to country)
                    }
                    destinations
                } else {
                    android.util.Log.e("RECOMMENDATIONS", "Error: ${connection.responseCode}")
                    getDefaultDestinations()
                }
            } catch (e: Exception) {
                android.util.Log.e("RECOMMENDATIONS", "Exception: ${e.message}")
                getDefaultDestinations()
            }
        }
    }

    private fun getDefaultDestinations(): List<Pair<String, String>> {
        return listOf(
            "Kyoto" to "Japan",
            "Lisbon" to "Portugal",
            "Cape Town" to "South Africa",
            "Queenstown" to "New Zealand",
            "Dubrovnik" to "Croatia",
            "Cartagena" to "Colombia",
            "Chiang Mai" to "Thailand",
            "Reykjavik" to "Iceland"
        )
    }
}