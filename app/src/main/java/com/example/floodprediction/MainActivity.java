package com.example.floodprediction;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Status Card
    private TextView tvStatusMain, tvStatusDesc, tvLastUpdated;
    private ImageView ivStatusIcon;
    private LinearLayout cardStatus;

    // Active Risk Map
    private Button btnViewMap;
    
    // SOS
    private TextView btnSOS;

    // Today's Forecast
    private TextView tvCurrentTemp, tvMinMaxTemp, tvWeatherDesc;
    private TextView tvDetailRain, tvDetailHumidity, tvDetailWind;

    // Weekly Outlook & Monitoring
    private LinearLayout layoutWeeklyForecast;
    private LinearLayout layoutMonitorList;

    // Bottom Navigation
    private BottomNavigationView bottomNavigationView;

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

        // Setup UI
        setupStatusCard();
        setupRiskMap();
        setupWeatherForecast();
        setupWeeklyOutlook();
        setupLocalMonitoring();
        setupBottomNavigation();
        setupSOS();
    }

    private void bindViews() {
        tvStatusMain = findViewById(R.id.tvStatusMain);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        cardStatus = findViewById(R.id.cardStatus);

        btnViewMap = findViewById(R.id.btnViewMap);
        btnSOS = findViewById(R.id.btnSOS);

        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvMinMaxTemp = findViewById(R.id.tvMinMaxTemp);
        tvWeatherDesc = findViewById(R.id.tvWeatherDesc);
        tvDetailRain = findViewById(R.id.tvDetailRain);
        tvDetailHumidity = findViewById(R.id.tvDetailHumidity);
        tvDetailWind = findViewById(R.id.tvDetailWind);

        layoutWeeklyForecast = findViewById(R.id.layoutWeeklyForecast);
        layoutMonitorList = findViewById(R.id.layoutMonitorList);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void setupStatusCard() {
        // Initial Mock Data - In real app, this comes from an aggregator logic
        tvStatusMain.setText("Status: Safe");
        tvStatusDesc.setText("No acti       ve risks detected in your area.");
        tvLastUpdated.setText("LAST UPDATED: 2 MINS AGO");
        ivStatusIcon.setBackgroundResource(R.drawable.bg_status_safe);
        ivStatusIcon.setImageResource(R.drawable.ic_launcher_foreground); // Replace with checkmark if avail
        ivStatusIcon.setColorFilter(getResources().getColor(R.color.success_green));
    }

    private void setupRiskMap() {
        btnViewMap.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
    }

    private void setupSOS() {
        btnSOS.setOnClickListener(v -> {
            // Navigate to SOS Activity
            startActivity(new Intent(this, ActivitySOS.class));
        });
    }

    private void setupWeatherForecast() {
        // Fetch weather for "Kuala Lumpur" (hardcoded to match UI for now)
        String weatherApiKey = BuildConfig.WEATHER_API_KEY;
        WeatherHelper weatherHelper = new WeatherHelper();

        WeatherHelper.WeatherCallback callback = new WeatherHelper.WeatherCallback() {
            @Override
            public void onResult(WeatherHelper.WeatherData data) {
                runOnUiThread(() -> {
                    tvCurrentTemp.setText(String.format("%.0f°C", data.temperature));
                    tvMinMaxTemp.setText(String.format("/ %.0f°C", data.temperature - 5)); // Mock min
                    tvDetailRain.setText(String.format("%.0fmm", data.rainfall));
                    tvDetailHumidity.setText(data.humidity + "%");
                    // Wind not in data object? Check WeatherHelper. Assuming it might be there or I mock it.
                    tvDetailWind.setText("8km/h"); 
                    tvWeatherDesc.setText(data.description);
                });
            }

            @Override
            public void onError(String error) {
                // Fail silently or show toast
            }
        };

        if (weatherApiKey.isEmpty()) {
            weatherHelper.fetchDemoWeather(callback);
        } else {
            // Hardcode city for now as spinner is removed
            weatherHelper.fetchWeather(weatherApiKey, "Kuala Lumpur", callback);
        }
    }

    private void setupWeeklyOutlook() {
        // Mock Data for Weekly Outlook
        layoutWeeklyForecast.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // Days: Mon, Tue, Wed, Thu, Fri
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        String[] temps = {"31°", "29°", "28°", "30°", "32°"};
        String[] probs = {"5%", "15%", "20%", "10%", "5%"};

        for (int i = 0; i < days.length; i++) {
            View itemView = inflater.inflate(R.layout.item_weekly_forecast, layoutWeeklyForecast, false);
            TextView tvDay = itemView.findViewById(R.id.tvDayName);
            TextView tvTemp = itemView.findViewById(R.id.tvTemp);
            TextView tvProb = itemView.findViewById(R.id.tvProb);

            tvDay.setText(days[i]);
            tvTemp.setText(temps[i]);
            tvProb.setText(probs[i]);

            layoutWeeklyForecast.addView(itemView);
        }
    }

    private void setupLocalMonitoring() {
        layoutMonitorList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // Item 1: Klang River
        addMonitorItem(inflater, "Klang River", "STATION: MASJID JAMEK", "NORMAL", "2.1m / 4.5m", true);
        
        // Item 2: Gombak River (Using SOS red style for demo?) Design said SOS in Red Circle but status is SOS.
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
            // For danger/SOS
            tvBadge.setBackgroundResource(R.drawable.bg_status_warning); 
            tvBadge.setTextColor(getResources().getColor(R.color.risk_high)); 
        }

        layoutMonitorList.addView(itemView);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already here
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            } else if (id == R.id.nav_resources) {
                Toast.makeText(this, "Opening Resources...", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ActivityResources.class));
                return true;
            }
            // Add other activities if they exist
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }
}