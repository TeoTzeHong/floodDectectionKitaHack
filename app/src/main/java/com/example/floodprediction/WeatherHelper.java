package com.example.floodprediction;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherHelper {

    public interface WeatherCallback {
        void onResult(WeatherData data);
        void onError(String error);
    }

    public static class WeatherData {
        public double rainfall;   // mm
        public int humidity;      // %
        public double temperature; // °C
        public String description;
        public int riskScore;     // 0-100
        public String riskLevel;  // LOW, MEDIUM, HIGH

        public WeatherData(double rainfall, int humidity, double temperature, String description) {
            this.rainfall = rainfall;
            this.humidity = humidity;
            this.temperature = temperature;
            this.description = description;
            this.riskScore = calculateRisk();
            this.riskLevel = getRiskLevel();
        }

        private int calculateRisk() {
            int score = 0;
            // Rainfall contribution (0-50 points)
            if (rainfall > 50) score += 50;
            else if (rainfall > 20) score += 35;
            else if (rainfall > 10) score += 20;
            else if (rainfall > 5) score += 10;

            // Humidity contribution (0-30 points)
            if (humidity > 90) score += 30;
            else if (humidity > 80) score += 20;
            else if (humidity > 70) score += 10;

            // Description keywords (0-20 points)
            String desc = description.toLowerCase();
            if (desc.contains("heavy rain") || desc.contains("thunderstorm")) score += 20;
            else if (desc.contains("rain") || desc.contains("drizzle")) score += 10;
            else if (desc.contains("cloud") || desc.contains("mist")) score += 5;

            return Math.min(score, 100);
        }

        private String getRiskLevel() {
            if (riskScore >= 60) return "HIGH";
            else if (riskScore >= 30) return "MEDIUM";
            else return "LOW";
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Fetch weather data from OpenWeatherMap API.
     *
     * @param apiKey   Your OpenWeatherMap API key
     * @param city     City name (e.g. "Kuala Lumpur")
     * @param callback Callback for result/error
     */
    public void fetchWeather(String apiKey, String city, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                String urlStr = "https://api.openweathermap.org/data/2.5/weather?q="
                        + java.net.URLEncoder.encode(city, "UTF-8")
                        + "&appid=" + apiKey
                        + "&units=metric";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONObject main = json.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    int humidity = main.getInt("humidity");

                    // Rainfall (last 1h) - may not always be present
                    double rainfall = 0;
                    if (json.has("rain")) {
                        JSONObject rain = json.getJSONObject("rain");
                        if (rain.has("1h")) rainfall = rain.getDouble("1h");
                        else if (rain.has("3h")) rainfall = rain.getDouble("3h");
                    }

                    String description = json.getJSONArray("weather")
                            .getJSONObject(0).getString("description");

                    WeatherData data = new WeatherData(rainfall, humidity, temp, description);
                    mainHandler.post(() -> callback.onResult(data));
                } else {
                    mainHandler.post(() -> callback.onError("API Error: HTTP " + responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    /**
     * Use demo weather data (no API key needed).
     * Good for testing on emulator without an API key.
     */
    public void fetchDemoWeather(WeatherCallback callback) {
        mainHandler.postDelayed(() -> {
            // Simulated rainy weather data for demo
            WeatherData data = new WeatherData(25.4, 88, 27.5, "heavy rain");
            callback.onResult(data);
        }, 1500); // Simulate network delay
    }
}
