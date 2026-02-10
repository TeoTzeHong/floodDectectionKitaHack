package com.example.floodprediction

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeminiHelper {

    fun generateContent(apiKey: String, prompt: String, callback: (String) -> Unit) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )

        MainScope().launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(prompt)
                }
                callback(response.text ?: "No response text")
            } catch (e: Exception) {
                callback("Error: ${e.message}")
            }
        }
    }

    fun analyzeFloodImage(apiKey: String, bitmap: Bitmap, callback: (String) -> Unit) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )

        val prompt = """
            You are a flood detection AI expert. Analyze this image carefully.

            Respond in EXACTLY this format:
            FLOOD_DETECTED: YES or NO
            SEVERITY: LOW, MEDIUM, or HIGH
            CONFIDENCE: percentage (e.g. 85%)
            DESCRIPTION: One sentence describing what you see
            SAFETY_TIP: One actionable safety tip

            If the image does not show flooding, set SEVERITY to NONE and explain what the image shows instead.
        """.trimIndent()

        MainScope().launch {
            try {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(inputContent)
                }
                callback(response.text ?: "No response text")
            } catch (e: Exception) {
                callback("Error: ${e.message}")
            }
        }
    }
}
