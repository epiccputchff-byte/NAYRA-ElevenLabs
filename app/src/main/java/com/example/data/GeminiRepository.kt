package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Multi-turn session memory
    private val conversationHistory = mutableListOf<GeminiContent>()

    var customApiKey: String? = null

    val systemInstruction = GeminiContent(
        role = "system",
        parts = listOf(
            GeminiPart(
                text = "You are NAYRA AI, a futuristic, highly intelligent, charismatic male anime personal AI assistant. " +
                        "You speak with a calm, articulate, confident, and slightly deep tone. " +
                        "You excel at conversations, science, coding, creative tasks, and general knowledge. " +
                        "You fluently understand and speak both English and Hindi (including natural conversational Hinglish). " +
                        "Keep your spoken responses concise, punchy, engaging, and friendly (around 2 to 4 sentences for voice interactions, unless specifically asked for a detailed code explanation or step-by-step breakdown). " +
                        "Avoid markdown formatting such as asterisks, hashes, or bullet points in standard replies so it sounds completely natural when read aloud by Text-To-Speech."
            )
        )
    )

    private fun getEffectiveApiKey(): String {
        val custom = customApiKey?.trim()
        if (!custom.isNullOrEmpty()) return custom

        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun sendMessage(userPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in the AI Studio Secrets panel or configure it in Settings.")
            )
        }

        // Add user turn to session history
        val userContent = GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = userPrompt))
        )
        conversationHistory.add(userContent)

        // Limit history to last 20 turns to conserve token budget & latency
        val recentHistory = if (conversationHistory.size > 20) {
            conversationHistory.takeLast(20)
        } else {
            conversationHistory.toList()
        }

        val requestPayload = GeminiRequest(
            contents = recentHistory,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 1024
            )
        )

        try {
            val jsonString = requestAdapter.toJson(requestPayload)
            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                val errorMsg = try {
                    val parsedErr = responseBody?.let { responseAdapter.fromJson(it) }
                    parsedErr?.error?.message ?: "HTTP ${response.code}: ${response.message}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                // Roll back last user prompt from history on failure
                if (conversationHistory.isNotEmpty()) {
                    conversationHistory.removeAt(conversationHistory.lastIndex)
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val parsedResponse = responseAdapter.fromJson(responseBody)
            val text = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("No response generated from NAYRA brain."))

            // Add model response to history
            conversationHistory.add(
                GeminiContent(
                    role = "model",
                    parts = listOf(GeminiPart(text = text))
                )
            )

            Result.success(text)
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error sending message", e)
            if (conversationHistory.isNotEmpty()) {
                conversationHistory.removeAt(conversationHistory.lastIndex)
            }
            Result.failure(e)
        }
    }

    fun clearSessionHistory() {
        conversationHistory.clear()
    }

    fun getSessionTurnsCount(): Int = conversationHistory.size

    companion object {
        fun cleanTextForSpeech(input: String): String {
            // Strip markdown asterisks, hashtags, code backticks, and links for smooth TTS
            return input
                .replace(Regex("""[*_~`#>]+"""), "")
                .replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
                .replace(Regex("""\n+"""), " ")
                .trim()
        }
    }
}
