package com.ferngames.travelguideapp.ui.assistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.Message
import com.ferngames.travelguideapp.MessageAdapter
import com.ferngames.travelguideapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.ferngames.travelguideapp.BuildConfig

class AssistantFragment : Fragment() {

    private lateinit var adapter: MessageAdapter
    private val API_KEY = BuildConfig.GROQ_API_KEY
    private val conversationHistory = mutableListOf<JSONObject>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assistant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvMessages = view.findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = view.findViewById<EditText>(R.id.etMessage)
        val btnSend = view.findViewById<ImageButton>(R.id.btnSend)

        // Setup RecyclerView
        adapter = MessageAdapter()
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        // Welcome message
        adapter.addMessage(
            Message(
                "Hello! 👋 I'm your AI Travel Assistant powered by Llama 3. Ask me anything about travel destinations, tips, best times to visit, or help planning your trip!",
                isUser = false
            )
        )

        // Send button
        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message, etMessage, rvMessages)
            }
        }

        // Suggested questions
        setupSuggestions(view, etMessage, rvMessages)
    }

    private fun setupSuggestions(
        view: View,
        etMessage: EditText,
        rvMessages: RecyclerView
    ) {
        view.findViewById<TextView>(R.id.suggestion1).setOnClickListener {
            sendMessage("What is the best time to visit Bali?", etMessage, rvMessages)
        }
        view.findViewById<TextView>(R.id.suggestion2).setOnClickListener {
            sendMessage("What are the top destinations in Asia?", etMessage, rvMessages)
        }
        view.findViewById<TextView>(R.id.suggestion3).setOnClickListener {
            sendMessage("Give me budget travel tips for backpackers.", etMessage, rvMessages)
        }
        view.findViewById<TextView>(R.id.suggestion4).setOnClickListener {
            sendMessage("What should I pack for a trip to Europe?", etMessage, rvMessages)
        }
    }

    private fun sendMessage(
        message: String,
        etMessage: EditText,
        rvMessages: RecyclerView
    ) {
        adapter.addMessage(Message(message, isUser = true))
        etMessage.setText("")
        rvMessages.scrollToPosition(adapter.itemCount - 1)

        // Add to conversation history
        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", message)
        }
        conversationHistory.add(userMessage)

        // Show typing indicator
        adapter.addMessage(Message("Thinking... 🤔", isUser = false))
        rvMessages.scrollToPosition(adapter.itemCount - 1)

        // Call Groq API
        viewLifecycleOwner.lifecycleScope.launch {
            val response = callGroqAPI()
            adapter.removeLastMessage()
            adapter.addMessage(Message(response, isUser = false))
            rvMessages.scrollToPosition(adapter.itemCount - 1)

            // Add to conversation history
            val assistantMessage = JSONObject().apply {
                put("role", "assistant")
                put("content", response)
            }
            conversationHistory.add(assistantMessage)
        }
    }

    private suspend fun callGroqAPI(): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.groq.com/openai/v1/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $API_KEY")
                connection.doOutput = true

                // Build messages array
                val messagesArray = JSONArray()

                // System message
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a helpful travel assistant. Provide friendly, concise, and accurate travel advice. Keep responses under 200 words. Use emojis to make responses engaging.")
                })

                // Add conversation history
                conversationHistory.forEach { msg ->
                    messagesArray.put(msg)
                }

                val body = JSONObject().apply {
                    put("model", "llama-3.3-70b-versatile")
                    put("messages", messagesArray)
                    put("max_tokens", 512)
                    put("temperature", 0.7)
                }

                android.util.Log.d("GROQ", "Request: $body")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(body.toString())
                writer.flush()

                val responseCode = connection.responseCode
                android.util.Log.d("GROQ", "Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    android.util.Log.d("GROQ", "Response: $response")
                    val jsonResponse = JSONObject(response)
                    jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText()
                    android.util.Log.e("GROQ", "Error: $responseCode - $error")
                    "Error $responseCode: $error"
                }
            } catch (e: Exception) {
                android.util.Log.e("GROQ", "Exception: ${e.message}", e)
                "Exception: ${e.message}"
            }
        }
    }
}