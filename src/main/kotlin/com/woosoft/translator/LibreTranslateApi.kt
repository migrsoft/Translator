package com.woosoft.translator

import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson

object LibreTranslateApi {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://localhost:5000" // Default LibreTranslate URL
    private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val gson = Gson()

    fun translate(text: String, sourceLang: String, targetLang: String): String? {
        val formBody = FormBody.Builder()
            .add("q", text)
            .add("source", sourceLang)
            .add("target", targetLang)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/translate")
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    if (jsonResponse != null) {
                        val result = gson.fromJson(jsonResponse, Map::class.java)
                        result["translatedText"] as? String
                    } else {
                        null
                    }
                } else {
                    println("Translation request failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error during translation request: ${e.message}")
            null
        }
    }
}
