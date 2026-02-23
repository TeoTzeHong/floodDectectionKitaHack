package com.example.floodprediction;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Image Detection views
    private ImageView imageView;
    private Button btnSelectImage, btnAnalyze;
    private ProgressBar progressBar;
    private TextView tvDetectionResult;

    // Weather views
    private TextView tvRainfall, tvHumidity, tvTemperature;
    private Button btnCheckWeather;
    private TextView tvWeatherRisk;

    // Forecast views
    private Spinner spinnerCity;
    private Button btnFetchForecast;
    private ProgressBar progressForecast;
    private LinearLayout forecastContainer;
    private TextView tvAiAnalysis;

    // Overall Assessment views
    private TextView tvOverallRisk, tvSafetyTips;

    private Bitmap selectedBitmap;

    // Results tracking
    private String detectionResult = null;
    private String detectionSeverity = null;
    private int weatherRiskScore = -1;
    private int forecastMaxRisk = -1;
    private String safetyTip = "";

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        imageView.setImageURI(uri);
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        selectedBitmap = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null)
                            inputStream.close();
                        btnAnalyze.setEnabled(true);
                        tvDetectionResult.setText("Image selected. Tap Analyze to detect flood.");
                        tvDetectionResult.setTextColor(getResources().getColor(R.color.text_secondary));
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind views
        imageView = findViewById(R.id.imageView);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        progressBar = findViewById(R.id.progressBar);
        tvDetectionResult = findViewById(R.id.tvDetectionResult);

        tvRainfall = findViewById(R.id.tvRainfall);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvTemperature = findViewById(R.id.tvTemperature);
        btnCheckWeather = findViewById(R.id.btnCheckWeather);
        tvWeatherRisk = findViewById(R.id.tvWeatherRisk);

        spinnerCity = findViewById(R.id.spinnerCity);
        btnFetchForecast = findViewById(R.id.btnFetchForecast);
        progressForecast = findViewById(R.id.progressForecast);
        forecastContainer = findViewById(R.id.forecastContainer);
        tvAiAnalysis = findViewById(R.id.tvAiAnalysis);

        tvOverallRisk = findViewById(R.id.tvOverallRisk);
        tvSafetyTips = findViewById(R.id.tvSafetyTips);

        // Setup city spinner
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ForecastHelper.MALAYSIA_CITIES);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);

        setupImageDetection();
        setupWeatherRisk();
        setupForecast();

        // Quick-action navigation
        findViewById(R.id.btnOpenMap).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.btnNavReport)
                .setOnClickListener(v -> startActivity(new Intent(this, FloodReportActivity.class)));
        findViewById(R.id.btnNavWeather).setOnClickListener(v -> btnCheckWeather.performClick());
        findViewById(R.id.btnNavSos).setOnClickListener(v -> startActivity(new Intent(this, SOSActivity.class)));
        findViewById(R.id.btnAdminPanel)
                .setOnClickListener(v -> startActivity(new Intent(this, AdminDashboardActivity.class)));
    }

    // ===========================================
    // IMAGE DETECTION
    // ===========================================
    private void setupImageDetection() {
        btnSelectImage.setOnClickListener(v -> pickImage.launch("image/*"));

        btnAnalyze.setOnClickListener(v -> {
            if (selectedBitmap != null) {
                progressBar.setVisibility(View.VISIBLE);
                btnAnalyze.setEnabled(false);
                tvDetectionResult.setText("🔍 Analyzing image with AI...");

                new GeminiHelper().analyzeFloodImage(
                        BuildConfig.API_KEY,
                        selectedBitmap,
                        response -> {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnAnalyze.setEnabled(true);
                                tvDetectionResult.setText(response);
                                parseDetectionResult(response);
                                updateOverallAssessment();
                            });
                            return null;
                        });
            }
        });
    }

    // ===========================================
    // WEATHER RISK
    // ===========================================
    private void setupWeatherRisk() {
        btnCheckWeather.setOnClickListener(v -> {
            btnCheckWeather.setEnabled(false);
            tvWeatherRisk.setText("🌍 Fetching weather data...");

            String weatherApiKey = BuildConfig.WEATHER_API_KEY;
            WeatherHelper weatherHelper = new WeatherHelper();

            WeatherHelper.WeatherCallback callback = new WeatherHelper.WeatherCallback() {
                @Override
                public void onResult(WeatherHelper.WeatherData data) {
                    displayWeatherResult(data);
                    btnCheckWeather.setEnabled(true);
                }

                @Override
                public void onError(String error) {
                    tvWeatherRisk.setText("❌ " + error);
                    btnCheckWeather.setEnabled(true);
                }
            };

            // Always use demo data (no API key needed)
            weatherHelper.fetchDemoWeather(callback);
        });
    }

    // ===========================================
    // FORECAST
    // ===========================================
    private void setupForecast() {
        btnFetchForecast.setOnClickListener(v -> {
            btnFetchForecast.setEnabled(false);
            progressForecast.setVisibility(View.VISIBLE);
            forecastContainer.removeAllViews();
            tvAiAnalysis.setVisibility(View.GONE);

            String city = spinnerCity.getSelectedItem().toString();
            String weatherApiKey = BuildConfig.WEATHER_API_KEY;
            ForecastHelper forecastHelper = new ForecastHelper();

            ForecastHelper.ForecastCallback callback = new ForecastHelper.ForecastCallback() {
                @Override
                public void onResult(List<ForecastHelper.ForecastItem> items) {
                    progressForecast.setVisibility(View.GONE);
                    btnFetchForecast.setEnabled(true);
                    displayForecast(items);

                    // Send to Gemini for AI analysis
                    String summary = ForecastHelper.buildForecastSummary(city, items);
                    analyzeWithGemini(city, summary);

                    // Track max risk for overall assessment
                    forecastMaxRisk = 0;
                    for (ForecastHelper.ForecastItem item : items) {
                        if (item.floodRiskScore > forecastMaxRisk) {
                            forecastMaxRisk = item.floodRiskScore;
                        }
                    }
                    updateOverallAssessment();
                }

                @Override
                public void onError(String error) {
                    progressForecast.setVisibility(View.GONE);
                    btnFetchForecast.setEnabled(true);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
            };

            // Always use demo data (no API key needed)
            forecastHelper.fetchDemoForecast(callback);
        });
    }

    private void displayForecast(List<ForecastHelper.ForecastItem> items) {
        forecastContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (ForecastHelper.ForecastItem item : items) {
            View card = inflater.inflate(R.layout.item_forecast, forecastContainer, false);

            TextView tvTime = card.findViewById(R.id.tvForecastTime);
            TextView tvDesc = card.findViewById(R.id.tvForecastDesc);
            TextView tvTemp = card.findViewById(R.id.tvForecastTemp);
            TextView tvRain = card.findViewById(R.id.tvForecastRain);
            TextView tvWind = card.findViewById(R.id.tvForecastWind);
            TextView tvHumid = card.findViewById(R.id.tvForecastHumidity);
            TextView tvWindLevel = card.findViewById(R.id.tvForecastWindLevel);
            TextView tvRisk = card.findViewById(R.id.tvForecastRisk);

            tvTime.setText(item.dateTime);
            tvDesc.setText(item.description);
            tvTemp.setText(String.format("🌡️ %.0f°C", item.temperature));
            tvRain.setText(String.format("🌧️ %.0fmm", item.rainfall));
            tvWind.setText(String.format("💨 %.0fm/s", item.windSpeed));
            tvHumid.setText("💧 " + item.humidity + "%");
            tvWindLevel.setText(item.windLevel);
            tvRisk.setText("Risk: " + item.floodRiskScore + "/100");

            // Color the risk based on level
            int riskColor;
            switch (item.floodRiskLevel) {
                case "HIGH":
                    riskColor = R.color.risk_high;
                    break;
                case "MEDIUM":
                    riskColor = R.color.risk_medium;
                    break;
                default:
                    riskColor = R.color.risk_low;
                    break;
            }
            tvRisk.setTextColor(getResources().getColor(riskColor));

            forecastContainer.addView(card);
        }
    }

    private void analyzeWithGemini(String city, String forecastSummary) {
        tvAiAnalysis.setVisibility(View.VISIBLE);
        tvAiAnalysis.setText("🤖 AI is analyzing the forecast...");

        String prompt = "You are a flood prediction expert for Malaysia.\n\n"
                + "Analyze this weather forecast and provide:\n"
                + "1. FLOOD_RISK: HIGH, MEDIUM, or LOW\n"
                + "2. PEAK_DANGER_TIME: when the flood risk is highest\n"
                + "3. WIND_ASSESSMENT: how serious the wind is\n"
                + "4. FLOOD_PREDICTION: will flooding likely occur? When?\n"
                + "5. SUGGESTIONS: 3-4 specific actions for people in " + city + "\n\n"
                + "Keep your response concise and practical.\n\n"
                + forecastSummary;

        new GeminiHelper().generateContent(
                BuildConfig.API_KEY,
                prompt,
                response -> {
                    runOnUiThread(() -> {
                        tvAiAnalysis.setText("🤖 AI Flood Analysis:\n\n" + response);
                    });
                    return null;
                });
    }

    // ===========================================
    // HELPERS
    // ===========================================
    private void displayWeatherResult(WeatherHelper.WeatherData data) {
        tvRainfall.setText(String.format("%.1f mm", data.rainfall));
        tvHumidity.setText(data.humidity + "%");
        tvTemperature.setText(String.format("%.1f°C", data.temperature));

        int riskColor;
        String riskEmoji;
        switch (data.riskLevel) {
            case "HIGH":
                riskColor = R.color.risk_high;
                riskEmoji = "🔴";
                break;
            case "MEDIUM":
                riskColor = R.color.risk_medium;
                riskEmoji = "🟡";
                break;
            default:
                riskColor = R.color.risk_low;
                riskEmoji = "🟢";
                break;
        }

        tvWeatherRisk.setText(riskEmoji + " Risk Score: " + data.riskScore + "/100 (" + data.riskLevel + ")\n"
                + "📋 " + data.description);
        tvWeatherRisk.setTextColor(getResources().getColor(riskColor));

        weatherRiskScore = data.riskScore;
        updateOverallAssessment();
    }

    private void parseDetectionResult(String response) {
        String upper = response.toUpperCase();
        if (upper.contains("FLOOD_DETECTED: YES") || upper.contains("FLOOD_DETECTED:YES")) {
            detectionResult = "YES";
        } else if (upper.contains("FLOOD_DETECTED: NO") || upper.contains("FLOOD_DETECTED:NO")) {
            detectionResult = "NO";
        }

        if (upper.contains("SEVERITY: HIGH") || upper.contains("SEVERITY:HIGH")) {
            detectionSeverity = "HIGH";
        } else if (upper.contains("SEVERITY: MEDIUM") || upper.contains("SEVERITY:MEDIUM")) {
            detectionSeverity = "MEDIUM";
        } else if (upper.contains("SEVERITY: LOW") || upper.contains("SEVERITY:LOW")) {
            detectionSeverity = "LOW";
        }

        int tipIndex = upper.indexOf("SAFETY_TIP:");
        if (tipIndex != -1) {
            safetyTip = response.substring(tipIndex + 11).trim();
            int newlineIndex = safetyTip.indexOf('\n');
            if (newlineIndex != -1)
                safetyTip = safetyTip.substring(0, newlineIndex);
        }
    }

    private void updateOverallAssessment() {
        boolean hasDetection = detectionResult != null;
        boolean hasWeather = weatherRiskScore >= 0;
        boolean hasForecast = forecastMaxRisk >= 0;

        if (!hasDetection && !hasWeather && !hasForecast) {
            tvOverallRisk.setText("Waiting for analysis...");
            tvOverallRisk.setTextColor(getResources().getColor(R.color.risk_unknown));
            return;
        }

        // Determine max risk from all sources
        int maxRisk = 0;
        if (hasDetection && "YES".equals(detectionResult)) {
            maxRisk = "HIGH".equals(detectionSeverity) ? 90 : 60;
        }
        if (hasWeather)
            maxRisk = Math.max(maxRisk, weatherRiskScore);
        if (hasForecast)
            maxRisk = Math.max(maxRisk, forecastMaxRisk);

        String overallLevel;
        int overallColor;
        if (maxRisk >= 60) {
            overallLevel = "🔴 HIGH FLOOD RISK";
            overallColor = R.color.risk_high;
        } else if (maxRisk >= 30) {
            overallLevel = "🟡 MODERATE FLOOD RISK";
            overallColor = R.color.risk_medium;
        } else {
            overallLevel = "🟢 LOW RISK - SAFE";
            overallColor = R.color.risk_low;
        }

        tvOverallRisk.setText(overallLevel + "\nCombined Score: " + maxRisk + "/100");
        tvOverallRisk.setTextColor(getResources().getColor(overallColor));

        // Safety tips
        StringBuilder tips = new StringBuilder();
        if (!safetyTip.isEmpty()) {
            tips.append("💡 ").append(safetyTip);
        }
        if (maxRisk >= 60) {
            if (tips.length() > 0)
                tips.append("\n");
            tips.append("🚨 FLOOD WARNING: Avoid low-lying areas and stay updated on local news.");
        } else if (maxRisk >= 30) {
            if (tips.length() > 0)
                tips.append("\n");
            tips.append("⚠️ Stay alert and monitor weather conditions closely.");
        }
        if (hasForecast && forecastMaxRisk >= 40) {
            if (tips.length() > 0)
                tips.append("\n");
            tips.append("🔮 Check the AI forecast analysis above for detailed predictions.");
        }
        tvSafetyTips.setText(tips.toString());
    }
}