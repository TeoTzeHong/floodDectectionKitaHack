package com.example.floodprediction;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SOSActivity extends AppCompatActivity {

    private ImageButton btnSos;
    private Button btnCancelSos;
    private TextView tvSosStatus, tvSosLocation;
    private ProgressBar progressBarSos;

    // ETA card views
    private LinearLayout layoutEtaCard;
    private TextView tvRescueStatus, tvUserEta, tvUserDistance, tvEtaUpdated;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private String userId;
    private String currentDocId = null;
    private SharedPreferences prefs;

    // Polling instead of snapshot listener — works even if Firestore rules deny
    // listeners
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private static final int POLL_INTERVAL_MS = 3000; // check every 3 seconds

    private boolean isSending = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private static final long PRESS_DURATION = 3000;

    // Hardcoded location: Kuala Lumpur
    private static final double HARDCODED_LAT = 3.1390;
    private static final double HARDCODED_LON = 101.6869;

    // Average rescuer speed assumption: 40 km/h
    private static final double AVG_SPEED_KMPH = 40.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        btnSos = findViewById(R.id.btnSos);
        btnCancelSos = findViewById(R.id.btnCancelSos);
        tvSosStatus = findViewById(R.id.tvSosStatus);
        tvSosLocation = findViewById(R.id.tvSosLocation);
        progressBarSos = findViewById(R.id.progressBarSos);

        // ETA Card
        layoutEtaCard = findViewById(R.id.layoutEtaCard);
        tvRescueStatus = findViewById(R.id.tvRescueStatus);
        tvUserEta = findViewById(R.id.tvUserEta);
        tvUserDistance = findViewById(R.id.tvUserDistance);
        tvEtaUpdated = findViewById(R.id.tvEtaUpdated);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        userId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        prefs = getSharedPreferences("sos_prefs", MODE_PRIVATE);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
        } else {
            fetchLocation();
        }

        setupSosButton();
        btnCancelSos.setOnClickListener(v -> cancelRequest());

        // ── Restore active SOS: first check prefs, then fallback to Firestore query ──
        String savedDocId = prefs.getString("active_sos_doc_id", null);
        if (savedDocId != null) {
            currentDocId = savedDocId;
            restoreActiveSosUI();
        } else {
            // Fallback: query Firestore for any active SOS by this userId (no orderBy = no
            // index needed)
            db.collection("sos_requests")
                    .whereEqualTo("userId", userId)
                    .whereIn("status", java.util.Arrays.asList("PENDING", "ASSIGNED", "ON_THE_WAY"))
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            currentDocId = querySnapshot.getDocuments().get(0).getId();
                            prefs.edit().putString("active_sos_doc_id", currentDocId).apply();
                            restoreActiveSosUI();
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        // Hardcoded to Kuala Lumpur
        tvSosLocation.setText(String.format("📍 Kuala Lumpur (%.4f, %.4f)",
                HARDCODED_LAT, HARDCODED_LON));
    }

    private void setupSosButton() {
        longPressRunnable = this::sendSosSignal;

        btnSos.setOnTouchListener((v, event) -> {
            if (isSending)
                return true;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    tvSosStatus.setText("Keep holding...");
                    v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(300).start();
                    handler.postDelayed(longPressRunnable, PRESS_DURATION);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
                    handler.removeCallbacks(longPressRunnable);
                    if (!isSending && currentDocId == null) {
                        tvSosStatus.setText("Press and hold 3s for HELP");
                        Toast.makeText(this, "⚠️ Hold for 3 seconds to send SOS!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
            return false;
        });
    }

    private void sendSosSignal() {
        isSending = true;
        progressBarSos.setVisibility(View.VISIBLE);
        tvSosStatus.setText("SENDING SOS SIGNAL...");

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show();
            isSending = false;
            progressBarSos.setVisibility(View.GONE);
            return;
        }

        // Use hardcoded Kuala Lumpur coordinates
        Map<String, Object> sosData = new HashMap<>();
        sosData.put("userId", userId);
        sosData.put("lat", HARDCODED_LAT);
        sosData.put("lon", HARDCODED_LON);
        sosData.put("timestamp", System.currentTimeMillis());
        sosData.put("status", "PENDING");
        sosData.put("severity", "HIGH");

        db.collection("sos_requests")
                .add(sosData)
                .addOnSuccessListener(documentReference -> {
                    currentDocId = documentReference.getId();
                    onSosSentSuccess();
                })
                .addOnFailureListener(e -> {
                    isSending = false;
                    progressBarSos.setVisibility(View.GONE);
                    tvSosStatus.setText("FAILED TO SEND. TRY AGAIN.");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onSosSentSuccess() {
        isSending = false;
        progressBarSos.setVisibility(View.GONE);
        tvSosStatus.setText("🚨 SOS SENT!\nWaiting for rescue team to accept...");
        tvSosStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        btnSos.setVisibility(View.GONE);
        btnCancelSos.setVisibility(View.VISIBLE);
        // Persist doc ID so we can restore if user navigates away
        prefs.edit().putString("active_sos_doc_id", currentDocId).apply();
        // Start polling for status changes
        startPolling();
    }

    /**
     * Called when user returns to SOSActivity with an existing active SOS.
     * Restores the UI immediately (hides button, shows waiting status) then starts
     * polling.
     */
    private void restoreActiveSosUI() {
        btnSos.setVisibility(View.GONE);
        btnCancelSos.setVisibility(View.VISIBLE);
        tvSosStatus.setText("🚨 SOS ACTIVE — Checking status...");
        tvSosStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        startPolling();
    }

    /**
     * Polls Firestore every 3 seconds to check SOS status.
     * More reliable than snapshot listener for demo/hackathon use.
     */
    private void startPolling() {
        stopPolling(); // cancel any existing poll
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentDocId == null)
                    return;
                db.collection("sos_requests").document(currentDocId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot == null || !snapshot.exists()) {
                                // Document gone — reset to fresh state
                                resetToFreshState();
                                return;
                            }
                            String status = snapshot.getString("status");
                            updateStatusDisplay(status);

                            if ("ASSIGNED".equals(status) || "ON_THE_WAY".equals(status)) {
                                showHardcodedEta();
                            }

                            if ("RESCUED".equals(status)) {
                                prefs.edit().remove("active_sos_doc_id").apply();
                                tvSosStatus.setText("🎉 YOU ARE RESCUED!");
                                tvSosStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                                layoutEtaCard.setVisibility(View.GONE);
                                Toast.makeText(SOSActivity.this, "Rescue confirmed. Stay safe!", Toast.LENGTH_LONG)
                                        .show();
                                pollHandler.postDelayed(() -> finish(), 3000);
                                return; // stop polling
                            }

                            if ("CANCELLED".equals(status)) {
                                // Old request was cancelled externally — reset UI
                                resetToFreshState();
                                return; // stop polling
                            }

                            // Reschedule next poll
                            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                        })
                        .addOnFailureListener(e -> {
                            // On error, keep polling
                            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                        });
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    /** Clears saved SOS state and shows the SOS button again. */
    private void resetToFreshState() {
        stopPolling();
        currentDocId = null;
        prefs.edit().remove("active_sos_doc_id").apply();
        btnSos.setVisibility(View.VISIBLE);
        btnCancelSos.setVisibility(View.GONE);
        layoutEtaCard.setVisibility(View.GONE);
        tvSosStatus.setText("Press and hold 3s for HELP");
        tvSosStatus.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void updateStatusDisplay(String status) {
        if (status == null)
            return;
        switch (status) {
            case "PENDING":
                // Still waiting — do NOT show ETA card yet
                tvSosStatus.setText("🚨 SOS SENT!\nWaiting for rescue team to accept...");
                tvSosStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                layoutEtaCard.setVisibility(View.GONE);
                break;
            case "ASSIGNED":
                tvSosStatus.setText("✅ RESCUE TEAM ACCEPTED!");
                tvSosStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                tvRescueStatus.setText("🟡 ASSIGNED — Preparing to depart");
                tvRescueStatus.setTextColor(0xFFFF9800);
                layoutEtaCard.setVisibility(View.VISIBLE);
                break;
            case "ON_THE_WAY":
                tvSosStatus.setText("🚑 RESCUE IS ON THE WAY!");
                tvSosStatus.setTextColor(0xFF4FC3F7);
                tvRescueStatus.setText("🔵 ON THE WAY");
                tvRescueStatus.setTextColor(0xFF2196F3);
                layoutEtaCard.setVisibility(View.VISIBLE);
                break;
        }
    }

    /** Always shows hardcoded 15-minute ETA for demo. */
    private void showHardcodedEta() {
        String now = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        tvUserEta.setText("~15 min");
        tvUserDistance.setText("📍 Kuala Lumpur");
        tvEtaUpdated.setText("Updated at " + now);
    }

    private void cancelRequest() {
        // Reset UI immediately — don't wait for Firestore
        String docToCancel = currentDocId;
        resetToFreshState(); // this clears currentDocId
        if (docToCancel != null) {
            db.collection("sos_requests").document(docToCancel)
                    .update("status", "CANCELLED");
            // No need to handle success/failure — UI already reset
        }
    }

    // Haversine formula
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        handler.removeCallbacksAndMessages(null);
    }
}
