package com.example.floodprediction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private ProgressBar mapProgress;
    private LinearLayout detailPanel;
    private TextView tvDetailTitle, tvDetailWind, tvDetailRain, tvDetailFlood, tvDetailDesc;
    private TilesOverlay weatherOverlay;
    private String currentLayer = ""; // "", "wind_new", "precipitation_new"
    private FloodHeatmapOverlay heatmapOverlay;
    private MapLegendOverlay legendOverlay;
    private WindFieldOverlay windFieldOverlay;
    private boolean heatmapEnabled = false;
    private boolean windFieldEnabled = false;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final double[][] CITIES = {
            {3.1390, 101.6869},   // Kuala Lumpur
            {5.4164, 100.3327},   // George Town
            {1.4927, 103.7414},   // Johor Bahru
            {1.5535, 110.3593},   // Kuching
            {5.9804, 116.0735},   // Kota Kinabalu
            {3.0738, 101.5183},   // Shah Alam
            {4.5975, 101.0901},   // Ipoh
            {3.8077, 103.3260},   // Kuantan
            {2.1896, 102.2501},   // Melaka
            {6.1254, 102.2381},   // Kota Bharu
            {6.1184, 100.3685},   // Alor Setar
            {2.7258, 101.9424},   // Seremban
    };

    private static final String[] CITY_NAMES = {
            "Kuala Lumpur", "George Town", "Johor Bahru", "Kuching",
            "Kota Kinabalu", "Shah Alam", "Ipoh", "Kuantan",
            "Melaka", "Kota Bharu", "Alor Setar", "Seremban"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mapMain), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Initialize Views
        mapView = findViewById(R.id.mapView);
        mapProgress = findViewById(R.id.mapProgress);
        detailPanel = findViewById(R.id.detailPanel);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailWind = findViewById(R.id.tvDetailWind);
        tvDetailRain = findViewById(R.id.tvDetailRain);
        tvDetailFlood = findViewById(R.id.tvDetailFlood);
        tvDetailDesc = findViewById(R.id.tvDetailDesc);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnRefresh = findViewById(R.id.btnRefreshMap);
        ImageButton btnClose = findViewById(R.id.btnCloseDetail);
        Button btnWind = findViewById(R.id.btnLayerWind);
        Button btnRain = findViewById(R.id.btnLayerRain);
        Button btnHeatmap = findViewById(R.id.btnLayerHeatmap);
        Button btnReport = findViewById(R.id.btnReportFlood);

        // Map Setup
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController ctrl = mapView.getController();
        ctrl.setZoom(7.0);
        ctrl.setCenter(new GeoPoint(4.0, 109.0)); // center of Malaysia (between Peninsular & Borneo)

        // Heatmap + Legend + WindField overlays
        heatmapOverlay = new FloodHeatmapOverlay();
        legendOverlay = new MapLegendOverlay();
        windFieldOverlay = new WindFieldOverlay();

        // Listeners
        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> loadAllCities());
        btnClose.setOnClickListener(v -> detailPanel.setVisibility(View.GONE));
        
        btnWind.setOnClickListener(v -> toggleWindField());
        btnRain.setOnClickListener(v -> toggleLayer("precipitation_new"));
        btnHeatmap.setOnClickListener(v -> toggleHeatmap());
        btnReport.setOnClickListener(v ->
                startActivity(new Intent(this, FloodReportActivity.class)));

        // Load Initial Data
        // Load Initial Data
        loadAllCities();
        
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_map);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                return true;
            } else if (id == R.id.nav_resources) {
                startActivity(new Intent(this, ActivityResources.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    // ───────────────────────────────────────
    // TILE LAYERS (Wind/Rain Heatmap)
    // ───────────────────────────────────────
    private void toggleLayer(String layer) {
        // Toggle off if same layer clicked
        if (currentLayer.equals(layer)) {
            if (weatherOverlay != null) {
                mapView.getOverlays().remove(weatherOverlay);
                weatherOverlay = null;
            }
            currentLayer = "";
            mapView.invalidate();
            return;
        }

        // Remove old overlay
        if (weatherOverlay != null) {
            mapView.getOverlays().remove(weatherOverlay);
        }

        currentLayer = layer;
        String apiKey = BuildConfig.WEATHER_API_KEY;
        
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(this, "API Key required for weather map", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new overlay
        // URL format: https://tile.openweathermap.org/map/{layer}/{z}/{x}/{y}.png?appid={API_KEY}
        OnlineTileSourceBase tileSource = new XYTileSource(
                "OWM_" + layer,
                0, 18, 256, ".png?appid=" + apiKey,
                new String[] {"https://tile.openweathermap.org/map/" + layer + "/"}
        );

        MapTileProviderBasic provider = new MapTileProviderBasic(getApplicationContext(), tileSource);
        weatherOverlay = new TilesOverlay(provider, getApplicationContext());
        weatherOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        
        // Add below markers (index 0 usually tiles, 1 overlays. Add to overlays list index 0?)
        // Or just add to overlays. It draws in order. We want it above base map (tiles) but below markers.
        // Base map is handled by MapView. TileOverlay is separate.
        // Adding it at index 0 of overlays list puts it at bottom of overlays stack (above base map).
        if (mapView.getOverlays().size() > 0) {
            mapView.getOverlays().add(0, weatherOverlay);
        } else {
            mapView.getOverlays().add(weatherOverlay);
        }

        mapView.invalidate();
        Toast.makeText(this, (layer.equals("wind_new") ? "Wind" : "Rain") + " layer enabled", Toast.LENGTH_SHORT).show();
    }

    // ───────────────────────────────────────
    // DATA LOADING (City Markers)
    // ───────────────────────────────────────
    private void loadAllCities() {
        mapProgress.setVisibility(View.VISIBLE);
        detailPanel.setVisibility(View.GONE);
        
        // Clear old overlays but preserve weather tile overlay if exists
        mapView.getOverlays().clear();
        if (weatherOverlay != null) {
            mapView.getOverlays().add(weatherOverlay);
        }
        if (heatmapEnabled) {
            heatmapOverlay.clearData();
            mapView.getOverlays().add(0, heatmapOverlay);
        }
        if (windFieldEnabled) {
            windFieldOverlay.clearData();
        }

        String apiKey = BuildConfig.WEATHER_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            loadDemoData();
            return;
        }

        final int total = CITY_NAMES.length;
        final int[] done = {0};

        for (int i = 0; i < total; i++) {
            final String name = CITY_NAMES[i];
            final double lat = CITIES[i][0];
            final double lon = CITIES[i][1];

            executor.execute(() -> {
                try {
                    String u = "https://api.openweathermap.org/data/2.5/weather?lat="
                            + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=metric";
                    HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(10000);

                    if (c.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        r.close();

                        JSONObject j = new JSONObject(sb.toString());
                        JSONObject main = j.getJSONObject("main");
                        JSONObject wind = j.getJSONObject("wind");

                        double ws = wind.getDouble("speed");
                        double wg = wind.optDouble("gust", ws);
                        double wd = wind.optDouble("deg", 0);
                        int hum = main.getInt("humidity");
                        double tmp = main.getDouble("temp");
                        String desc = j.getJSONArray("weather").getJSONObject(0).getString("description");

                        double rain = 0;
                        if (j.has("rain")) {
                            JSONObject rObj = j.getJSONObject("rain");
                            rain = rObj.optDouble("1h", rObj.optDouble("3h", 0));
                        }
                        final double fRain = rain;

                        mainHandler.post(() -> addCity(name, lat, lon, ws, wg, wd, hum, tmp, fRain, desc));
                    }
                    c.disconnect();
                } catch (Exception ignored) {}

                mainHandler.post(() -> {
                    done[0]++;
                    if (done[0] >= total) {
                        mapProgress.setVisibility(View.GONE);
                        mapView.invalidate();
                    }
                });
            });
        }
    }

    private void loadDemoData() {
        double[] ws =  {5,  12, 3,  8,  18, 6,  4,  22, 7,  15, 9,  3};
        double[] wg =  {8,  18, 5,  12, 25, 9,  7,  30, 10, 22, 14, 5};
        double[] wd =  {45, 180,270,90, 135,225,315,10, 50, 200,160,0};
        int[]    hm =  {78, 88, 65, 82, 92, 75, 70, 95, 80, 90, 85, 68};
        double[] tp =  {30, 28, 32, 29, 26, 31, 30, 25, 29, 27, 28, 31};
        double[] rn =  {0,  15, 0,  5,  35, 2,  0,  50, 3,  25, 8,  0};
        String[] dc = {"scattered clouds","moderate rain","clear sky",
                "light rain","thunderstorm","few clouds","clear sky",
                "heavy rain","light rain","heavy rain","moderate rain","clear sky"};

        for (int i = 0; i < CITY_NAMES.length; i++) {
            addCity(CITY_NAMES[i], CITIES[i][0], CITIES[i][1],
                    ws[i], wg[i], wd[i], hm[i], tp[i], rn[i], dc[i]);
        }
        mapProgress.setVisibility(View.GONE);
        mapView.invalidate();
    }

    // ───────────────────────────────────────
    // MARKERS & OVERLAYS logic
    // ───────────────────────────────────────
    private void addCity(String city, double lat, double lon,
                         double windSpd, double windGust, double windDeg,
                         int humidity, double temp, double rain, String desc) {
        GeoPoint pt = new GeoPoint(lat, lon);

        // Wind level for marker color
        int circleColor;
        String windLvl;
        if (windSpd >= 20) {
            circleColor = Color.argb(80, 244, 67, 54);
            windLvl = "STORM";
        } else if (windSpd >= 14) {
            circleColor = Color.argb(80, 255, 152, 0);
            windLvl = "STRONG";
        } else if (windSpd >= 8) {
            circleColor = Color.argb(80, 255, 235, 59);
            windLvl = "MODERATE";
        } else {
            circleColor = Color.argb(60, 76, 175, 80);
            windLvl = "LIGHT";
        }

        // Flood risk score
        int risk = 0;
        if (rain > 50) risk += 40;
        else if (rain > 20) risk += 25;
        else if (rain > 5) risk += 15;
        if (humidity > 85) risk += 20;
        if (windSpd >= 14) risk += 20;
        if (desc.contains("heavy") || desc.contains("thunder")) risk += 20;
        risk = Math.min(risk, 100);

        // Feed data into heatmap overlay
        heatmapOverlay.addDataPoint(lat, lon, risk);

        // Feed data into wind field overlay
        windFieldOverlay.addDataPoint(lat, lon, windSpd, windDeg);

        String floodLvl;
        int riskClr;
        if (risk >= 60) {
            floodLvl = "HIGH";
            riskClr = getResources().getColor(R.color.risk_high);
        } else if (risk >= 30) {
            floodLvl = "MEDIUM";
            riskClr = getResources().getColor(R.color.risk_medium);
        } else {
            floodLvl = "LOW";
            riskClr = getResources().getColor(R.color.risk_low);
        }

        // 1) Colored circle (Risk Zone)
        Polygon circle = new Polygon(mapView);
        circle.setPoints(Polygon.pointsAsCircle(pt, 15000));
        circle.getFillPaint().setColor(circleColor);
        circle.getOutlinePaint().setColor(Color.TRANSPARENT);
        mapView.getOverlays().add(circle);

        // 2) Wind arrow (Custom Overlay)
        WindOverlay arrow = new WindOverlay(pt, windSpd, windDeg);
        mapView.getOverlays().add(arrow);

        // 3) Tap-able marker
        Marker mk = new Marker(mapView);
        mk.setPosition(pt);
        mk.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mk.setTitle(city);

        final int fRisk = risk;
        final String fFlood = floodLvl;
        final int fClr = riskClr;
        mk.setOnMarkerClickListener((m, mv) -> {
            showDetail(city, windSpd, windGust, windDeg, humidity, temp, rain, desc,
                    windLvl, fFlood, fRisk, fClr);
            return true;
        });

        mapView.getOverlays().add(mk);
    }

    private void showDetail(String city, double ws, double wg, double wd,
                            int hum, double tmp, double rain, String desc,
                            String windLvl, String floodLvl, int risk, int clr) {
        detailPanel.setVisibility(View.VISIBLE);
        tvDetailTitle.setText(city);

        String[] dirs = {"N","NE","E","SE","S","SW","W","NW"};
        String dirName = dirs[((int) Math.round(wd / 45.0)) % 8];

        tvDetailWind.setText(String.format("%.1f m/s\nFrom %s\n%s", ws, dirName, windLvl));
        tvDetailRain.setText(String.format("%.1f mm\n%d%% humid\n%s", rain, hum,
                rain > 20 ? "Heavy" : (rain > 5 ? "Moderate" : "Light")));
        tvDetailFlood.setText(floodLvl + "\n" + risk + "/100");
        tvDetailFlood.setTextColor(clr);

        StringBuilder exp = new StringBuilder();
        exp.append(desc.substring(0, 1).toUpperCase()).append(desc.substring(1));
        exp.append("\n");
        if (ws >= 14) exp.append("Strong wind pushing moisture inland. ");
        if (rain > 20) exp.append("Heavy rainfall may overwhelm drainage. ");
        if (hum > 85) exp.append("Saturated air increases rain probability. ");
        if (risk >= 60) exp.append("\n⚠️ HIGH flood risk — avoid low areas!");
        else if (risk >= 30) exp.append("\n🟡 Moderate risk — stay alert.");
        else exp.append("\n✅ Low risk — conditions are safe.");
        tvDetailDesc.setText(exp.toString());
    }

    // ───────────────────────────────────────
    // WIND FIELD TOGGLE
    // ───────────────────────────────────────
    private void toggleWindField() {
        windFieldEnabled = !windFieldEnabled;
        if (windFieldEnabled) {
            if (!mapView.getOverlays().contains(windFieldOverlay)) {
                // Add wind field above heatmap but below markers
                int idx = heatmapEnabled ? 1 : 0;
                mapView.getOverlays().add(idx, windFieldOverlay);
            }
            Toast.makeText(this, "Wind field enabled", Toast.LENGTH_SHORT).show();
        } else {
            mapView.getOverlays().remove(windFieldOverlay);
            Toast.makeText(this, "Wind field disabled", Toast.LENGTH_SHORT).show();
        }
        mapView.invalidate();
    }

    // ───────────────────────────────────────
    // HEATMAP TOGGLE
    // ───────────────────────────────────────
    private void toggleHeatmap() {
        heatmapEnabled = !heatmapEnabled;
        if (heatmapEnabled) {
            // Re-add heatmap & legend at bottom of overlays stack
            if (!mapView.getOverlays().contains(heatmapOverlay)) {
                mapView.getOverlays().add(0, heatmapOverlay);
            }
            if (!mapView.getOverlays().contains(legendOverlay)) {
                mapView.getOverlays().add(legendOverlay);
            }
            Toast.makeText(this, "Heatmap enabled", Toast.LENGTH_SHORT).show();
        } else {
            mapView.getOverlays().remove(heatmapOverlay);
            mapView.getOverlays().remove(legendOverlay);
            Toast.makeText(this, "Heatmap disabled", Toast.LENGTH_SHORT).show();
        }
        mapView.invalidate();
    }

    // ───────────────────────────────────────
    // USER FLOOD REPORTS
    // ───────────────────────────────────────
    private void loadUserReports() {
        List<FloodReportManager.FloodReport> reports =
                FloodReportManager.getInstance(this).getReports();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());

        for (FloodReportManager.FloodReport report : reports) {
            GeoPoint pt = new GeoPoint(report.lat, report.lon);

            // Red circle for reports
            Polygon circle = new Polygon(mapView);
            circle.setPoints(Polygon.pointsAsCircle(pt, 10000));
            int circleColor;
            if ("HIGH".equals(report.severity)) {
                circleColor = Color.argb(100, 244, 67, 54);
            } else if ("MEDIUM".equals(report.severity)) {
                circleColor = Color.argb(80, 255, 152, 0);
            } else {
                circleColor = Color.argb(60, 255, 235, 59);
            }
            circle.getFillPaint().setColor(circleColor);
            circle.getOutlinePaint().setColor(Color.argb(150, 255, 0, 0));
            circle.getOutlinePaint().setStrokeWidth(2f);
            mapView.getOverlays().add(circle);

            // Marker
            Marker mk = new Marker(mapView);
            mk.setPosition(pt);
            mk.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mk.setTitle("📸 " + report.locationName);
            mk.setSnippet(report.severity + " — " + report.description
                    + "\n" + sdf.format(new Date(report.timestamp)));
            mapView.getOverlays().add(mk);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        // Reload user reports every time we come back (e.g. after submitting a report)
        loadUserReports();
        mapView.invalidate();
    }

    @Override
    public void onPause() { super.onPause(); mapView.onPause(); }
}
