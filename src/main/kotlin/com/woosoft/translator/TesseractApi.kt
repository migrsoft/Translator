package com.woosoft.translator

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import com.google.gson.Gson

object TesseractApi {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://localhost:8884" // Assuming tesseract-server runs on localhost:8884
    private val gson = Gson()

    fun ocrImage(image: BufferedImage, options: Map<String, Any>? = null): String? {
        val stream = ByteArrayOutputStream()
        ImageIO.write(image, "png", stream)
        val imageBytes = stream.toByteArray()

        val requestBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "image.png",
                imageBytes.toRequestBody("image/png".toMediaTypeOrNull()))

        options?.let {
            requestBodyBuilder.addFormDataPart("options", null, gson.toJson(it).toRequestBody("application/json".toMediaTypeOrNull()))
        }

        val request = Request.Builder()
            .url("$BASE_URL/tesseract")
            .post(requestBodyBuilder.build())
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    if (jsonResponse != null) {
                        val ocrResult = gson.fromJson(jsonResponse, Map::class.java)
                        val data = ocrResult["data"] as? Map<*, *>
                        data?.get("stdout") as? String
                    } else {
                        null
                    }
                } else {
                    println("OCR request failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error during OCR request: ${e.message}")
            null
        }
    }
}
