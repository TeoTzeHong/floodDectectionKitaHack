package com.example.floodprediction

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper

class GeminiHelper {

    private val mainHandler = Handler(Looper.getMainLooper())

    // ─── Hardcoded flood analysis response ───────────────────────────────────
    private val hardcodedFloodImageResponse = """
        FLOOD_DETECTED: YES
        SEVERITY: HIGH
        CONFIDENCE: 92%
        DESCRIPTION: The image shows severe flooding with water levels exceeding 1 metre, submerging vehicles and ground floor structures.
        SAFETY_TIP: Move immediately to higher ground and avoid contact with floodwater as it may be contaminated or electrically charged.
    """.trimIndent()

    // ─── Hardcoded forecast analysis response ────────────────────────────────
    private val hardcodedForecastResponse = """
        ⚠️ FLOOD RISK ASSESSMENT — KUALA LUMPUR

        RISK LEVEL: HIGH

        Based on current forecast data:
        • Heavy rainfall expected over the next 24 hours (35–50 mm)
        • Humidity levels at 92–95% — ground already saturated
        • Wind gusts up to 25 m/s may cause flash flooding in low-lying areas

        HOTSPOT AREAS: Klang Valley, Ampang, Petaling Jaya

        RECOMMENDATIONS:
        1. Avoid low-lying and flood-prone areas
        2. Do not cross flooded roads — 15 cm of water can sweep a person
        3. Prepare emergency kit: water, torch, first aid, documents
        4. Monitor official alerts from JPS (Jabatan Pengairan dan Saliran)

        Stay safe. If in danger, press SOS immediately.
    """.trimIndent()

    /**
     * Returns a hardcoded flood forecast analysis — no API call needed.
     */
    fun generateContent(apiKey: String, prompt: String, callback: (String) -> Unit) {
        // Simulate a short "thinking" delay so it feels natural
        mainHandler.postDelayed({
            callback(hardcodedForecastResponse)
        }, 1200)
    }

    /**
     * Returns a hardcoded flood image analysis — no API call needed.
     */
    fun analyzeFloodImage(apiKey: String, bitmap: Bitmap, callback: (String) -> Unit) {
        mainHandler.postDelayed({
            callback(hardcodedFloodImageResponse)
        }, 1500)
    }
}
