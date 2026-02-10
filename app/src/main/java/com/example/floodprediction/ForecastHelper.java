package com.example.floodprediction;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForecastHelper {

    public interface ForecastCallback {
        void onResult(List<ForecastItem> items);
        void onError(String error);
    }

    public static class ForecastItem {
        public String dateTime;
        public double temperature;  // °C
        public int humidity;        // %
        public double windSpeed;    // m/s
        public double windGust;     // m/s
        public int windDeg;         // degrees
        public double rainfall;     // mm (3h)
        public String description;
        public String icon;
        public int clouds;          // %

        // Computed
        public String windLevel;
        public String windWarning;
        public int floodRiskScore;
        public String floodRiskLevel;

        public void computeRisk() {
            // Wind Level (Beaufort Scale simplified)
            if (windSpeed >= 20) {
                windLevel = "🔴 STORM";
                windWarning = "Dangerous winds! Stay indoors.";
            } else if (windSpeed >= 14) {
                windLevel = "🟠 STRONG";
                windWarning = "Strong winds expected. Secure loose objects.";
            } else if (windSpeed >= 8) {
                windLevel = "🟡 MODERATE";
                windWarning = "Moderate winds. Be cautious outdoors.";
            } else {
                windLevel = "🟢 LIGHT";
                windWarning = "Light winds. Normal conditions.";
            }

            // Flood Risk Score
            int score = 0;

            // Rainfall (0-40 points)
            if (rainfall > 50) score += 40;
            else if (rainfall > 30) score += 30;
            else if (rainfall > 15) score += 20;
            else if (rainfall > 5) score += 10;

            // Humidity (0-25 points)
            if (humidity > 90) score += 25;
            else if (humidity > 80) score += 15;
            else if (humidity > 70) score += 8;

            // Wind (0-20 points) - strong wind + rain = worse
            if (windSpeed >= 14) score += 20;
            else if (windSpeed >= 8) score += 10;

            // Cloud cover (0-15 points)
            if (clouds > 90 && rainfall > 0) score += 15;
            else if (clouds > 70) score += 8;

            floodRiskScore = Math.min(score, 100);

            if (floodRiskScore >= 60) floodRiskLevel = "HIGH";
            else if (floodRiskScore >= 30) floodRiskLevel = "MEDIUM";
            else floodRiskLevel = "LOW";
        }

        /**
         * Format this item as a summary string for Gemini to analyze.
         */
        public String toSummary() {
            return dateTime + ": " + description
                    + ", Temp=" + temperature + "°C"
                    + ", Rain=" + rainfall + "mm"
                    + ", Humidity=" + humidity + "%"
                    + ", Wind=" + windSpeed + "m/s (gust " + windGust + "m/s)"
                    + ", Clouds=" + clouds + "%";
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Malaysian cities for quick selection
    public static final String[] MALAYSIA_CITIES = {
            "Kuala Lumpur", "George Town", "Johor Bahru", "Kuching",
            "Kota Kinabalu", "Shah Alam", "Ipoh", "Kuantan",
            "Melaka", "Kota Bharu", "Alor Setar", "Seremban"
    };

    /**
     * Fetch 5-day / 3-hour forecast from OpenWeatherMap.
     */
    public void fetchForecast(String apiKey, String city, ForecastCallback callback) {
        executor.execute(() -> {
            try {
                String urlStr = "https://api.openweathermap.org/data/2.5/forecast?q="
                        + java.net.URLEncoder.encode(city + ",MY", "UTF-8")
                        + "&appid=" + apiKey
                        + "&units=metric&cnt=16"; // 16 items = ~2 days of 3h intervals

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    List<ForecastItem> items = parseForecast(sb.toString());
                    mainHandler.post(() -> callback.onResult(items));
                } else {
                    mainHandler.post(() -> callback.onError("API Error: HTTP " + code));
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    private List<ForecastItem> parseForecast(String json) throws Exception {
        List<ForecastItem> items = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray list = root.getJSONArray("list");

        SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        SimpleDateFormat outputFmt = new SimpleDateFormat("EEE HH:mm", Locale.US);

        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            ForecastItem item = new ForecastItem();

            // Date
            String dtTxt = entry.getString("dt_txt");
            Date date = inputFmt.parse(dtTxt);
            item.dateTime = outputFmt.format(date);

            // Main
            JSONObject main = entry.getJSONObject("main");
            item.temperature = main.getDouble("temp");
            item.humidity = main.getInt("humidity");

            // Wind
            JSONObject wind = entry.getJSONObject("wind");
            item.windSpeed = wind.getDouble("speed");
            item.windGust = wind.optDouble("gust", item.windSpeed);
            item.windDeg = wind.optInt("deg", 0);

            // Rain
            if (entry.has("rain")) {
                JSONObject rain = entry.getJSONObject("rain");
                item.rainfall = rain.optDouble("3h", 0);
            }

            // Clouds
            item.clouds = entry.getJSONObject("clouds").getInt("all");

            // Weather description
            item.description = entry.getJSONArray("weather")
                    .getJSONObject(0).getString("description");
            item.icon = entry.getJSONArray("weather")
                    .getJSONObject(0).getString("icon");

            item.computeRisk();
            items.add(item);
        }
        return items;
    }

    /**
     * Demo forecast data for testing without API key.
     * Simulates a worsening weather scenario in Kuala Lumpur.
     */
    public void fetchDemoForecast(ForecastCallback callback) {
        mainHandler.postDelayed(() -> {
            List<ForecastItem> items = new ArrayList<>();

            // Simulate 8 forecast intervals (24 hours)
            String[] times = {"Today 12:00", "Today 15:00", "Today 18:00", "Today 21:00",
                    "Tomorrow 00:00", "Tomorrow 03:00", "Tomorrow 06:00", "Tomorrow 09:00"};
            double[] temps = {31, 29, 27, 26, 25, 25, 26, 28};
            int[] humids = {75, 82, 88, 92, 95, 93, 90, 85};
            double[] winds = {3, 5, 8, 12, 15, 18, 14, 8};
            double[] gusts = {5, 8, 12, 18, 22, 25, 20, 12};
            double[] rains = {0, 2, 8, 20, 35, 45, 30, 10};
            int[] clouds = {40, 60, 80, 95, 100, 100, 90, 70};
            String[] descs = {"scattered clouds", "light rain", "moderate rain",
                    "heavy rain", "heavy rain", "thunderstorm", "heavy rain", "moderate rain"};

            for (int i = 0; i < times.length; i++) {
                ForecastItem item = new ForecastItem();
                item.dateTime = times[i];
                item.temperature = temps[i];
                item.humidity = humids[i];
                item.windSpeed = winds[i];
                item.windGust = gusts[i];
                item.windDeg = 180 + (i * 20);
                item.rainfall = rains[i];
                item.clouds = clouds[i];
                item.description = descs[i];
                item.icon = "10d";
                item.computeRisk();
                items.add(item);
            }

            callback.onResult(items);
        }, 1200);
    }

    /**
     * Build a text summary of forecast for Gemini to analyze.
     */
    public static String buildForecastSummary(String city, List<ForecastItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("Weather forecast for ").append(city).append(", Malaysia:\n\n");
        for (ForecastItem item : items) {
            sb.append(item.toSummary()).append("\n");
        }
        return sb.toString();
    }
}
