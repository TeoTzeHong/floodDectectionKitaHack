package com.example.floodprediction;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Status Card
    private TextView tvStatusMain, tvStatusDesc, tvLastUpdated;
    private ImageView ivStatusIcon;
    private LinearLayout cardStatus;

    // Active Risk Map
    private Button btnViewMap;
    private TextView tvMapBadge;
    private WindAnimationView windAnimView;

    // SOS
    private TextView btnSOS;

    // Today's Forecast
    private TextView tvCurrentTemp, tvMinMaxTemp, tvWeatherDesc;
    private TextView tvDetailRain, tvDetailHumidity, tvDetailWind;
    private LinearLayout layoutMetricRain, layoutMetricHumidity, layoutMetricWind;

    // Weekly Outlook & Monitoring
    private LinearLayout layoutWeeklyForecast;
    private LinearLayout layoutMonitorList;

    // Bottom Navigation
    private BottomNavigationView bottomNavigationView;

    // Auto-refresh
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private long lastUpdateTimestamp = 0;
    private static final long REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes

    // Timestamp updater
    private final Runnable timestampUpdater = new Runnable() {
        @Override
        public void run() {
            updateTimestamp();
            refreshHandler.postDelayed(this, 30_000); // update every 30s
        }
    };

    // Auto-refresh runnable
    private final Runnable autoRefresh = new Runnable() {
        @Override
        public void run() {
            loadAllWeatherData();
            refreshHandler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

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

        // Bind Views
        bindViews();

        // Setup navigation & buttons
        setupRiskMap();
        setupBottomNavigation();
        setupSOS();

        // Show loading state
        tvStatusMain.setText("Loading...");
        tvStatusDesc.setText("Fetching live weather data...");
        tvLastUpdated.setText("UPDATING...");

        // Load all real data
        loadAllWeatherData();

        // Start auto-refresh
        refreshHandler.postDelayed(autoRefresh, REFRESH_INTERVAL);
        refreshHandler.postDelayed(timestampUpdater, 30_000);
    }

    private void bindViews() {
        tvStatusMain = findViewById(R.id.tvStatusMain);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        cardStatus = findViewById(R.id.cardStatus);

        btnViewMap = findViewById(R.id.btnViewMap);
        tvMapBadge = findViewById(R.id.tvMapBadge);
        windAnimView = findViewById(R.id.windAnimView);
        btnSOS = findViewById(R.id.btnSOS);

        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvMinMaxTemp = findViewById(R.id.tvMinMaxTemp);
        tvWeatherDesc = findViewById(R.id.tvWeatherDesc);
        tvDetailRain = findViewById(R.id.tvDetailRain);
        tvDetailHumidity = findViewById(R.id.tvDetailHumidity);
        tvDetailWind = findViewById(R.id.tvDetailWind);

        layoutMetricRain = findViewById(R.id.layoutMetricRain);
        layoutMetricHumidity = findViewById(R.id.layoutMetricHumidity);
        layoutMetricWind = findViewById(R.id.layoutMetricWind);

        layoutWeeklyForecast = findViewById(R.id.layoutWeeklyForecast);
        layoutMonitorList = findViewById(R.id.layoutMonitorList);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    // ══════════                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       ══════════════════════════════════════
    //  LOAD ALL WEATHER DATA (Current + Forecast)
    // ════════════════════════════════════════════════
    private void loadAllWeatherData() {
        String weatherApiKey = BuildConfig.WEATHER_API_KEY;
        WeatherHelper weatherHelper = new WeatherHelper();
        ForecastHelper forecastHelper = new ForecastHelper();

        WeatherHelper.WeatherCallback currentCallback = new WeatherHelper.WeatherCallback() {
            @Override
            public void onResult(WeatherHelper.WeatherData data) {
                runOnUiThread(() -> {
                    lastUpdateTimestamp = System.currentTimeMillis();
                    applyCurrentWeather(data);
                    updateTimestamp();
                });
            }

            @Override
            public void onError(String error) {
                // API failed — fall back to demo data so UI isn't stuck
                android.util.Log.w("MainActivity", "Weather API failed: " + error + " — using demo data");
                runOnUiThread(() -> weatherHelper.fetchDemoWeather(new WeatherHelper.WeatherCallback() {
                    @Override
                    public void onResult(WeatherHelper.WeatherData data) {
                        lastUpdateTimestamp = System.currentTimeMillis();
                        applyCurrentWeather(data);
                        updateTimestamp();
                    }
                    @Override
                    public void onError(String e) {}
                }));
            }
        };

        ForecastHelper.ForecastCallback forecastCallback = new ForecastHelper.ForecastCallback() {
            @Override
            public void onResult(List<ForecastHelper.ForecastItem> items) {
                runOnUiThread(() -> applyWeeklyForecast(items));
            }

            @Override
            public void onError(String error) {
                // API failed — fall back to demo forecast
                android.util.Log.w("MainActivity", "Forecast API failed: " + error + " — using demo data");
                runOnUiThread(() -> forecastHelper.fetchDemoForecast(new ForecastHelper.ForecastCallback() {
                    @Override
                    public void onResult(List<ForecastHelper.ForecastItem> items) {
                        applyWeeklyForecast(items);
                    }
                    @Override
                    public void onError(String e) {}
                }));
            }
        };

        if (weatherApiKey == null || weatherApiKey.isEmpty()) {
            weatherHelper.fetchDemoWeather(currentCallback);
            forecastHelper.fetchDemoForecast(forecastCallback);
        } else {
            weatherHelper.fetchWeather(weatherApiKey, "Kuala Lumpur", currentCallback);
            forecastHelper.fetchForecast(weatherApiKey, "Kuala Lumpur", forecastCallback);
        }

        // Always setup local monitoring (simulated river data based on weather)
        setupLocalMonitoring();
    }

    // ════════════════════════════════════════════════
    //  APPLY CURRENT WEATHER — Color-coded + Animated
    // ════════════════════════════════════════════════
    private void applyCurrentWeather(WeatherHelper.WeatherData data) {
        // Temperature
        tvCurrentTemp.setText(String.format(Locale.US, "%.0f°C", data.temperature));
        tvMinMaxTemp.setText(String.format(Locale.US, "/ %.0f°C", data.tempMin));

        // Weather description
        tvWeatherDesc.setText(capitalizeFirst(data.description));

        // ── Rain metric (color-coded) ──
        String rainText = String.format(Locale.US, "%.1fmm", data.rainfall);
        tvDetailRain.setText(rainText);
        if (data.rainfall > 20) {
            applyMetricColor(layoutMetricRain, tvDetailRain, "danger");
        } else if (data.rainfall > 5) {
            applyMetricColor(layoutMetricRain, tvDetailRain, "warn");
        } else {
            applyMetricColor(layoutMetricRain, tvDetailRain, "safe");
        }

        // ── Humidity metric (color-coded) ──
        String humText = data.humidity + "%";
        tvDetailHumidity.setText(humText);
        if (data.humidity > 85) {
            applyMetricColor(layoutMetricHumidity, tvDetailHumidity, "danger");
        } else if (data.humidity > 70) {
            applyMetricColor(layoutMetricHumidity, tvDetailHumidity, "warn");
        } else {
            applyMetricColor(layoutMetricHumidity, tvDetailHumidity, "safe");
        }

        // ── Wind metric (color-coded) ──
        double windKmh = data.windSpeed * 3.6;
        String windText = String.format(Locale.US, "%.0f km/h", windKmh);
        tvDetailWind.setText(windText);
        if (windKmh >= 50) { // 50km/h ~ 14m/s
            applyMetricColor(layoutMetricWind, tvDetailWind, "danger");
        } else if (windKmh >= 30) { // 30km/h ~ 8m/s
            applyMetricColor(layoutMetricWind, tvDetailWind, "warn");
        } else {
            applyMetricColor(layoutMetricWind, tvDetailWind, "safe");
        }

        // ── Status Card (dynamic) ──
        applyStatusCard(data);

        // ── Map Badge ──
        applyMapBadge(data);

        // ── Wind Animation View (live!) ──
        if (windAnimView != null) {
            windAnimView.setWeatherData(data.windSpeed, 180,
                    data.rainfall, data.humidity, data.riskLevel);
        }

        // Fade-in animation for the whole forecast section
        View forecastSection = tvCurrentTemp.getRootView().findViewById(R.id.tvCurrentTemp);
        if (forecastSection != null) {
            forecastSection.setAlpha(0f);
            forecastSection.animate().alpha(1f).setDuration(600).start();
        }
    }

    // ════════════════════════════════════════════════
    //  COLOR-CODE METRIC BLOCKS
    // ════════════════════════════════════════════════
    private void applyMetricColor(LinearLayout block, TextView valueText, String level) {
        int bgColor, textColor;
        switch (level) {
            case "danger":
                bgColor = getResources().getColor(R.color.metric_danger_bg);
                textColor = getResources().getColor(R.color.metric_danger_text);
                break;
            case "warn":
                bgColor = getResources().getColor(R.color.metric_warn_bg);
                textColor = getResources().getColor(R.color.metric_warn_text);
                break;
            default:
                bgColor = getResources().getColor(R.color.metric_safe_bg);
                textColor = getResources().getColor(R.color.metric_safe_text);
                break;
        }

        // Animate background color change
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(bgColor);
        block.setBackground(bg);

        // Animate text color
        int oldColor = valueText.getCurrentTextColor();
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, textColor);
        colorAnim.setDuration(800);
        colorAnim.addUpdateListener(a -> valueText.setTextColor((int) a.getAnimatedValue()));
        colorAnim.start();

        valueText.setTextColor(textColor);
    }

    // ════════════════════════════════════════════════
    //  DYNAMIC STATUS CARD (5-Level Standard)
    // ════════════════════════════════════════════════
    private void applyStatusCard(WeatherHelper.WeatherData data) {
        String riskLevel = data.riskLevel; // SAFE, ADVISORY, ALERT, WARNING, DANGER
        int statusBgColor, statusTextColor;
        String statusText, statusDesc;
        int iconBgRes;

        switch (riskLevel) {
            case "DANGER": // Level 5
                statusBgColor = getResources().getColor(R.color.status_danger_bg);
                statusTextColor = getResources().getColor(R.color.danger_red);
                statusText = "🚨 DANGER (Level 5)";
                statusDesc = "Extreme risk! Chest-deep water possible. Evacuate if ordered.";
                iconBgRes = R.drawable.bg_status_danger;
                startPulseAnimation(ivStatusIcon);
                break;
            case "WARNING": // Level 4
                statusBgColor = getResources().getColor(R.color.status_danger_bg);
                statusTextColor = getResources().getColor(R.color.danger_red);
                statusText = "⛔ WARNING (Level 4)";
                statusDesc = "High risk. Waist-deep water. Structural damage possible.";
                iconBgRes = R.drawable.bg_status_danger;
                startPulseAnimation(ivStatusIcon);
                break;
            case "ALERT": // Level 3
                statusBgColor = getResources().getColor(R.color.status_warn_bg);
                statusTextColor = getResources().getColor(R.color.warning_amber);
                statusText = "⚠️ ALERT (Level 3)";
                statusDesc = "Minor flooding expected. Knee-deep water. Drains overflow.";
                iconBgRes = R.drawable.bg_status_warning;
                stopPulseAnimation(ivStatusIcon);
                break;
            case "ADVISORY": // Level 2
                statusBgColor = getResources().getColor(R.color.status_warn_bg);
                statusTextColor = getResources().getColor(R.color.info_blue);
                statusText = "ℹ️ ADVISORY (Level 2)";
                statusDesc = "Nuisance flooding. Puddles on roads. Be cautious.";
                iconBgRes = R.drawable.bg_status_safe; // Blueish tint overlay handled below
                stopPulseAnimation(ivStatusIcon);
                break;
            default: // SAFE (Level 1)
                statusBgColor = getResources().getColor(R.color.status_green_bg);
                statusTextColor = getResources().getColor(R.color.success_green);
                statusText = "✅ STATUS: SAFE";
                statusDesc = "Normal conditions. Drains functioning well.";
                iconBgRes = R.drawable.bg_status_safe;
                stopPulseAnimation(ivStatusIcon);
                break;
        }

        // Animate card background
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dpToPx(16));
        cardBg.setColor(statusBgColor);
        cardStatus.setBackground(cardBg);

        tvStatusMain.setText(statusText);
        tvStatusMain.setTextColor(statusTextColor);
        tvStatusDesc.setText(statusDesc);

        ivStatusIcon.setBackgroundResource(iconBgRes);
        ivStatusIcon.setColorFilter(statusTextColor);
    }

    // ════════════════════════════════════════════════
    //  MAP BADGE — Dynamic (5-Level)
    // ════════════════════════════════════════════════
    private void applyMapBadge(WeatherHelper.WeatherData data) {
        if (tvMapBadge == null) return;
        switch (data.riskLevel) {
            case "DANGER":
                tvMapBadge.setText("⚡ DANGER");
                tvMapBadge.setTextColor(getResources().getColor(R.color.danger_red));
                tvMapBadge.setBackgroundResource(R.drawable.bg_status_danger);
                break;
            case "WARNING":
                tvMapBadge.setText("⛔ WARNING");
                tvMapBadge.setTextColor(getResources().getColor(R.color.danger_red));
                tvMapBadge.setBackgroundResource(R.drawable.bg_status_danger);
                break;
            case "ALERT":
                tvMapBadge.setText("⚠️ ALERT");
                tvMapBadge.setTextColor(getResources().getColor(R.color.warning_amber));
                tvMapBadge.setBackgroundResource(R.drawable.bg_status_warning);
                break;
            case "ADVISORY":
                tvMapBadge.setText("ℹ️ ADVISORY");
                tvMapBadge.setTextColor(getResources().getColor(R.color.info_blue));
                tvMapBadge.setBackgroundResource(R.drawable.bg_status_safe); // Using safe bg but blue text
                break;
            default:
                tvMapBadge.setText("ALL CLEAR");
                tvMapBadge.setTextColor(getResources().getColor(R.color.success_green));
                tvMapBadge.setBackgroundResource(R.drawable.bg_status_safe);
                break;
        }
    }

    // ════════════════════════════════════════════════
    //  WEEKLY FORECAST — Real API Data
    // ════════════════════════════════════════════════
    private void applyWeeklyForecast(List<ForecastHelper.ForecastItem> items) {
        layoutWeeklyForecast.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // Group by day: collect daily high temp, total rain, max risk
        Map<String, DaySummary> dailyMap = new java.util.LinkedHashMap<>();

        for (ForecastHelper.ForecastItem item : items) {
            // Extract day name (e.g. "Mon", "Tue") from dateTime like "Mon 12:00"
            String dayName = item.dateTime.split(" ")[0];

            DaySummary summary = dailyMap.get(dayName);
            if (summary == null) {
                summary = new DaySummary();
                summary.dayName = dayName;
                summary.highTemp = item.temperature;
                summary.totalRain = item.rainfall;
                summary.maxRisk = item.floodRiskScore;
                summary.description = item.description;
                dailyMap.put(dayName, summary);
            } else {
                summary.highTemp = Math.max(summary.highTemp, item.temperature);
                summary.totalRain += item.rainfall;
                summary.maxRisk = Math.max(summary.maxRisk, item.floodRiskScore);
                // Keep the worst description
                if (item.floodRiskScore > summary.maxRisk) {
                    summary.description = item.description;
                }
            }
        }

        for (DaySummary day : dailyMap.values()) {
            View itemView = inflater.inflate(R.layout.item_weekly_forecast, layoutWeeklyForecast, false);
            TextView tvDay = itemView.findViewById(R.id.tvDayName);
            TextView tvTemp = itemView.findViewById(R.id.tvTemp);
            TextView tvProb = itemView.findViewById(R.id.tvProb);
            View riskDot = itemView.findViewById(R.id.riskDot);

            tvDay.setText(day.dayName);
            tvTemp.setText(String.format(Locale.US, "%.0f°", day.highTemp));

            // Rain probability text
            String rainInfo = String.format(Locale.US, "%.0fmm", day.totalRain);
            tvProb.setText(rainInfo);

            // Color code by risk
            int riskColor;
            if (day.maxRisk >= 60) {
                riskColor = getResources().getColor(R.color.risk_high);
                tvProb.setTextColor(getResources().getColor(R.color.risk_high));
            } else if (day.maxRisk >= 30) {
                riskColor = getResources().getColor(R.color.risk_medium);
                tvProb.setTextColor(getResources().getColor(R.color.risk_medium));
            } else {
                riskColor = getResources().getColor(R.color.success_green);
                tvProb.setTextColor(getResources().getColor(R.color.success_green));
            }

            // Risk dot color
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(riskColor);
            riskDot.setBackground(dotBg);

            // Weather icon tint based on conditions
            ImageView ivIcon = itemView.findViewById(R.id.ivWeatherIcon);
            if (day.totalRain > 15) {
                ivIcon.setColorFilter(getResources().getColor(R.color.info_blue));
            } else if (day.totalRain > 5) {
                ivIcon.setColorFilter(getResources().getColor(R.color.text_secondary_light));
            } else {
                ivIcon.setColorFilter(getResources().getColor(R.color.warning_amber));
            }

            // Slide-in animation for each card
            itemView.setAlpha(0f);
            itemView.setTranslationX(50f);
            int delay = layoutWeeklyForecast.getChildCount() * 100;
            itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setStartDelay(delay)
                    .setDuration(400)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            layoutWeeklyForecast.addView(itemView);
        }
    }

    // Helper class for daily aggregation
    private static class DaySummary {
        String dayName;
        double highTemp;
        double totalRain;
        int maxRisk;
        String description;
    }

    // ════════════════════════════════════════════════
    //  LOCAL MONITORING — Color-coded river levels
    // ════════════════════════════════════════════════
    private void setupLocalMonitoring() {
        layoutMonitorList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // Item 1: Klang River
        addMonitorItem(inflater, "Klang River", "STATION: MASJID JAMEK", "NORMAL", "2.1m / 4.5m", true);

        // Item 2: Gombak River
        addMonitorItem(inflater, "Gombak River", "STATION: JALAN TUN RAZAK", "SOS", "Warning Level", false);
    }

    private void addMonitorItem(LayoutInflater inflater, String name, String station, String status, String level, boolean isSafe) {
        View itemView = inflater.inflate(R.layout.item_river_level, layoutMonitorList, false);
        TextView tvName = itemView.findViewById(R.id.tvRiverName);
        TextView tvStation = itemView.findViewById(R.id.tvStationName);
        TextView tvBadge = itemView.findViewById(R.id.tvStatusBadge);
        TextView tvLevel = itemView.findViewById(R.id.tvWaterLevel);

        tvName.setText(name);
        tvStation.setText(station);
        tvBadge.setText(status);
        tvLevel.setText(level);

        if (isSafe) {
            tvBadge.setBackgroundResource(R.drawable.bg_status_safe);
            tvBadge.setTextColor(getResources().getColor(R.color.success_green));
        } else {
            // For danger/SOS — pulse the badge
            tvBadge.setBackgroundResource(R.drawable.bg_status_danger);
            tvBadge.setTextColor(getResources().getColor(R.color.risk_high));
            startPulseAnimation(tvBadge);
        }

        // Slide-in from left animation
        itemView.setAlpha(0f);
        itemView.setTranslationY(30f);
        int delay = layoutMonitorList.getChildCount() * 150;
        itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        layoutMonitorList.addView(itemView);
    }

    // ════════════════════════════════════════════════
    //  ANIMATIONS
    // ════════════════════════════════════════════════
    private void startPulseAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f);
        scaleX.setDuration(1200);
        scaleY.setDuration(1200);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
        view.setTag(R.id.ivStatusIcon, new ObjectAnimator[]{scaleX, scaleY});
    }

    private void stopPulseAnimation(View view) {
        Object tag = view.getTag(R.id.ivStatusIcon);
        if (tag instanceof ObjectAnimator[]) {
            ObjectAnimator[] anims = (ObjectAnimator[]) tag;
            for (ObjectAnimator a : anims) a.cancel();
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
    }

    // ════════════════════════════════════════════════
    //  TIMESTAMP
    // ════════════════════════════════════════════════
    private void updateTimestamp() {
        if (lastUpdateTimestamp == 0) {
            tvLastUpdated.setText("UPDATING...");
            return;
        }
        long diff = System.currentTimeMillis() - lastUpdateTimestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;

        String timeText;
        if (seconds < 60) {
            timeText = "LAST UPDATED: JUST NOW";
        } else if (minutes < 60) {
            timeText = "LAST UPDATED: " + minutes + " MIN" + (minutes > 1 ? "S" : "") + " AGO";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            timeText = "LAST UPDATED: " + sdf.format(new Date(lastUpdateTimestamp));
        }
        tvLastUpdated.setText(timeText);
    }

    // ════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════
    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════
    private void setupRiskMap() {
        btnViewMap.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
    }

    private void setupSOS() {
        btnSOS.setOnClickListener(v -> {
            startActivity(new Intent(this, ActivitySOS.class));
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            } else if (id == R.id.nav_resources) {
                startActivity(new Intent(this, ActivityResources.class));
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(autoRefresh);
        refreshHandler.removeCallbacks(timestampUpdater);
    }
}