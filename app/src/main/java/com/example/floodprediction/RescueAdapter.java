package com.example.floodprediction;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RescueAdapter extends RecyclerView.Adapter<RescueAdapter.ViewHolder> {

    private List<DocumentSnapshot> missions;
    private Context context;
    private FirebaseFirestore db;
    private FusedLocationProviderClient locationClient;
    private String currentRescuerId = "rescuer_demo_01"; // Hardcoded for hackathon

    // Average rescuer speed in km/h (city driving)
    private static final double AVG_SPEED_KMPH = 40.0;

    public RescueAdapter(Context context, List<DocumentSnapshot> missions) {
        this.context = context;
        this.missions = missions;
        this.db = FirebaseFirestore.getInstance();
        this.locationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_rescue_mission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = missions.get(position);
        Double latObj = doc.getDouble("lat");
        Double lonObj = doc.getDouble("lon");
        String status = doc.getString("status");
        Long timestamp = doc.getLong("timestamp");

        if (latObj == null || lonObj == null)
            return;
        double lat = latObj;
        double lon = lonObj;

        holder.tvLocation.setText(String.format("📍 Lat: %.4f, Lon: %.4f", lat, lon));

        // Time since request
        if (timestamp != null) {
            long diff = System.currentTimeMillis() - timestamp;
            long minutes = diff / (60 * 1000);
            holder.tvTime.setText(minutes + " min ago");
        }

        // ─── Status UI ───────────────────────────────────────────
        updateStatusUI(holder, status);

        // ─── ETA Calculation ─────────────────────────────────────
        calculateAndShowETA(holder, lat, lon, status);

        // ─── ACCEPT (PENDING → ASSIGNED) ─────────────────────────
        holder.btnAccept.setOnClickListener(v -> {
            holder.btnAccept.setEnabled(false);
            db.collection("sos_requests").document(doc.getId())
                    .update("status", "ASSIGNED", "rescuerId", currentRescuerId,
                            "assignedAt", System.currentTimeMillis())
                    .addOnFailureListener(e -> Toast
                            .makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // ─── ON THE WAY (ASSIGNED → ON_THE_WAY) ──────────────────
        holder.btnOnTheWay.setOnClickListener(v -> {
            holder.btnOnTheWay.setEnabled(false);
            // Get rescuer current location and store it for victim ETA display
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationClient.getLastLocation().addOnSuccessListener(location -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "ON_THE_WAY");
                    updates.put("departedAt", System.currentTimeMillis());
                    if (location != null) {
                        updates.put("rescuerLat", location.getLatitude());
                        updates.put("rescuerLon", location.getLongitude());
                    }
                    db.collection("sos_requests").document(doc.getId())
                            .update(updates)
                            .addOnFailureListener(e -> Toast
                                    .makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
            } else {
                db.collection("sos_requests").document(doc.getId())
                        .update("status", "ON_THE_WAY", "departedAt", System.currentTimeMillis())
                        .addOnFailureListener(e -> Toast
                                .makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        // ─── NAVIGATE ────────────────────────────────────────────
        holder.btnNavigate.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon));
                context.startActivity(browserIntent);
            }
        });

        // ─── RESCUED (ON_THE_WAY → RESCUED) ──────────────────────
        holder.btnComplete.setOnClickListener(v -> {
            holder.btnComplete.setEnabled(false);
            db.collection("sos_requests").document(doc.getId())
                    .update("status", "RESCUED", "rescuedAt", System.currentTimeMillis())
                    .addOnFailureListener(e -> Toast
                            .makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void updateStatusUI(ViewHolder holder, String status) {
        if (status == null)
            status = "PENDING";

        // Reset all buttons first
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnOnTheWay.setVisibility(View.GONE);
        holder.btnNavigate.setVisibility(View.GONE);
        holder.btnComplete.setVisibility(View.GONE);

        switch (status) {
            case "PENDING":
                holder.tvStatus.setText("🔴 PENDING");
                holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnAccept.setEnabled(true);
                break;

            case "ASSIGNED":
                holder.tvStatus.setText("🟡 ASSIGNED");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
                holder.btnOnTheWay.setVisibility(View.VISIBLE);
                holder.btnOnTheWay.setEnabled(true);
                holder.btnNavigate.setVisibility(View.VISIBLE);
                break;

            case "ON_THE_WAY":
                holder.tvStatus.setText("🔵 ON THE WAY");
                holder.tvStatus.setTextColor(Color.parseColor("#2196F3"));
                holder.btnNavigate.setVisibility(View.VISIBLE);
                holder.btnComplete.setVisibility(View.VISIBLE);
                holder.btnComplete.setEnabled(true);
                break;

            case "RESCUED":
                holder.tvStatus.setText("🟢 RESCUED");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                break;
        }
    }

    /**
     * Hardcoded ETA — always 15 minutes for demo purposes.
     */
    private void calculateAndShowETA(ViewHolder holder, double victimLat, double victimLon, String status) {
        if ("RESCUED".equals(status) || "CANCELLED".equals(status)) {
            holder.tvEta.setText("Mission complete.");
            holder.tvDistance.setText("");
            return;
        }
        holder.tvEta.setText("🕐 ETA: ~15 min");
        holder.tvDistance.setText("📍 Kuala Lumpur");
    }

    /**
     * Haversine formula - calculates great-circle distance between two GPS points
     * in km.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public int getItemCount() {
        return missions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvLocation, tvTime, tvEta, tvDistance;
        Button btnAccept, btnOnTheWay, btnNavigate, btnComplete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvMissionStatus);
            tvLocation = itemView.findViewById(R.id.tvMissionLocation);
            tvTime = itemView.findViewById(R.id.tvMissionTime);
            tvEta = itemView.findViewById(R.id.tvMissionEta);
            tvDistance = itemView.findViewById(R.id.tvMissionDistance);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnOnTheWay = itemView.findViewById(R.id.btnOnTheWay);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }
}
