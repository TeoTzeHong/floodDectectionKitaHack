package com.example.floodprediction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private MapView map;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView tvActiveCases;
    private Button btnRefresh, btnOpenRescueDashboard;
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_admin_dashboard);

        map = findViewById(R.id.mapAdmin);
        progressBar = findViewById(R.id.progressAdmin);
        tvActiveCases = findViewById(R.id.tvActiveCases);
        btnRefresh = findViewById(R.id.btnRefreshAdmin);
        btnOpenRescueDashboard = findViewById(R.id.btnOpenRescueDashboard);

        db = FirebaseFirestore.getInstance();

        setupMap();
        loadSosRequestsRealtime();

        btnRefresh.setOnClickListener(v -> loadSosRequestsRealtime());
        btnOpenRescueDashboard.setOnClickListener(v -> startActivity(new Intent(this, RescueDashboardActivity.class)));
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(13.0);
        map.getController().setCenter(new GeoPoint(3.1390, 101.6869)); // Kuala Lumpur
    }

    private void loadSosRequestsRealtime() {
        progressBar.setVisibility(View.VISIBLE);

        // Cancel previous listener if any
        if (listenerReg != null)
            listenerReg.remove();

        listenerReg = db.collection("sos_requests")
                .whereIn("status", Arrays.asList("PENDING", "ASSIGNED", "ON_THE_WAY"))
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (queryDocumentSnapshots == null)
                        return;

                    // Clear map
                    map.getOverlays().clear();

                    int pendingCount = 0, assignedCount = 0, onTheWayCount = 0;
                    List<GeoPoint> points = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double lat = document.getDouble("lat");
                        Double lon = document.getDouble("lon");
                        String status = document.getString("status");
                        String severity = document.getString("severity");
                        Long timestamp = document.getLong("timestamp");

                        if (lat != null && lon != null) {
                            GeoPoint pt = new GeoPoint(lat, lon);
                            points.add(pt);

                            switch (status != null ? status : "") {
                                case "PENDING":
                                    pendingCount++;
                                    break;
                                case "ASSIGNED":
                                    assignedCount++;
                                    break;
                                case "ON_THE_WAY":
                                    onTheWayCount++;
                                    break;
                            }

                            // Calculate time ago
                            String timeAgo = "";
                            if (timestamp != null) {
                                long mins = (System.currentTimeMillis() - timestamp) / 60000;
                                timeAgo = mins + " min ago";
                            }

                            addMarker(pt, status, severity, timeAgo);
                        }
                    }

                    int total = pendingCount + assignedCount + onTheWayCount;
                    tvActiveCases.setText(
                            "🚨 Active Cases: " + total + "\n" +
                                    "🔴 Pending: " + pendingCount +
                                    "  🟡 Assigned: " + assignedCount +
                                    "  🔵 On The Way: " + onTheWayCount);

                    if (!points.isEmpty()) {
                        map.getController().animateTo(points.get(0));
                        map.getController().setZoom(10.0);
                    }
                    map.invalidate();
                });
    }

    /**
     * Color coding:
     * PENDING → Red (#F44336)
     * ASSIGNED → Orange (#FF9800)
     * ON_THE_WAY → Blue (#2196F3)
     */
    private void addMarker(GeoPoint pt, String status, String severity, String timeAgo) {
        int fillColor;
        int strokeColor;
        String emoji;

        switch (status != null ? status : "PENDING") {
            case "ASSIGNED":
                fillColor = Color.argb(80, 255, 152, 0); // Orange
                strokeColor = Color.parseColor("#FF9800");
                emoji = "🟡";
                break;
            case "ON_THE_WAY":
                fillColor = Color.argb(80, 33, 150, 243); // Blue
                strokeColor = Color.parseColor("#2196F3");
                emoji = "🔵";
                break;
            default: // PENDING
                fillColor = Color.argb(80, 244, 67, 54); // Red
                strokeColor = Color.parseColor("#F44336");
                emoji = "🔴";
                break;
        }

        // Circle radius
        Polygon circle = new Polygon(map);
        circle.setPoints(Polygon.pointsAsCircle(pt, 2000)); // 2km radius
        circle.getFillPaint().setColor(fillColor);
        circle.getOutlinePaint().setColor(strokeColor);
        circle.getOutlinePaint().setStrokeWidth(3f);
        map.getOverlays().add(circle);

        // Marker
        Marker marker = new Marker(map);
        marker.setPosition(pt);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(emoji + " SOS — " + status);
        marker.setSnippet(
                "Severity: " + (severity != null ? severity : "HIGH") +
                        "\n" + timeAgo);
        map.getOverlays().add(marker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        // Always re-center on Kuala Lumpur (overrides osmdroid's saved position)
        map.getController().setZoom(13.0);
        map.getController().setCenter(new GeoPoint(3.1390, 101.6869));
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null)
            listenerReg.remove();
    }
}
