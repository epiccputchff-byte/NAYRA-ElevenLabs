package com.example.voice

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** User-owned ElevenLabs key. Network calls run off the main thread. */
class ElevenLabsVoice(private val context: Context) {
    private val prefs = context.getSharedPreferences("nayra_voice_settings", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
    private var player: MediaPlayer? = null

    var apiKey: String
        get() = prefs.getString("elevenlabs_api_key", "") ?: ""
        set(value) { prefs.edit().putString("elevenlabs_api_key", value.trim()).apply() }

    var voiceId: String
        get() = prefs.getString("elevenlabs_voice_id", "pNInz6obpgDQGcFmaJgB") ?: "pNInz6obpgDQGcFmaJgB"
        set(value) { prefs.edit().putString("elevenlabs_voice_id", value.trim()).apply() }

    suspend fun testConnection(key: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(key.isNotBlank()) { "Pehle ElevenLabs API key enter karo." }
            val request = Request.Builder().url("https://api.elevenlabs.io/v1/user")
                .header("xi-api-key", key).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("ElevenLabs error ${response.code}. API key/quota check karo.")
                "ElevenLabs connected successfully."
            }
        }
    }

    suspend fun speak(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey
            require(key.isNotBlank()) { "ElevenLabs key not connected" }
            val json = JSONObject().put("text", text.take(4500))
                .put("model_id", "eleven_multilingual_v2")
                .put("voice_settings", JSONObject().put("stability", 0.5).put("similarity_boost", 0.75))
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/${voiceId}/stream?output_format=mp3_44100_128")
                .header("xi-api-key", key).header("Accept", "audio/mpeg")
                .post(body).build()
            val audio = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("ElevenLabs TTS error ${response.code}; fallback voice used.")
                response.body?.bytes() ?: throw IllegalStateException("No audio returned")
            }
            val file = File(context.cacheDir, "nayra-elevenlabs.mp3")
            file.writeBytes(audio)
            withContext(Dispatchers.Main) {
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnCompletionListener { it.release(); player = null; file.delete() }
                    prepare()
                    start()
                }
            }
        }
    }

    fun stop() { player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }; player = null }
    fun disconnect() { stop(); apiKey = "" }
}
