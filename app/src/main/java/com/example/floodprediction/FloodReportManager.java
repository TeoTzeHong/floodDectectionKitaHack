package com.example.floodprediction;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages user-submitted flood reports using SharedPreferences.
 * Reports are stored as a JSON array so they persist across sessions.
 *
 * For a real app, this would use Firebase/Cloud storage so all users
 * can see each other's reports. This local version works for demo/emulator.
 */
public class FloodReportManager {

    private static final String PREFS_NAME = "flood_reports";
    private static final String KEY_REPORTS = "reports_json";

    private static FloodReportManager instance;
    private final SharedPreferences prefs;

    public static class FloodReport {
        public double lat;
        public double lon;
        public String severity;    // HIGH, MEDIUM, LOW
        public String description;
        public String locationName;
        public long timestamp;

        public FloodReport(double lat, double lon, String severity,
                           String description, String locationName, long timestamp) {
            this.lat = lat;
            this.lon = lon;
            this.severity = severity;
            this.description = description;
            this.locationName = locationName;
            this.timestamp = timestamp;
        }
    }

    private FloodReportManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized FloodReportManager getInstance(Context context) {
        if (instance == null) {
            instance = new FloodReportManager(context);
        }
        return instance;
    }

    /**
     * Save a new flood report.
     */
    public void saveReport(double lat, double lon, String severity,
                           String description, String locationName) {
        try {
            JSONArray arr = loadArray();
            JSONObject obj = new JSONObject();
            obj.put("lat", lat);
            obj.put("lon", lon);
            obj.put("severity", severity);
            obj.put("description", description);
            obj.put("locationName", locationName);
            obj.put("timestamp", System.currentTimeMillis());
            arr.put(obj);

            prefs.edit().putString(KEY_REPORTS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    /**
     * Get all saved flood reports.
     */
    public List<FloodReport> getReports() {
        List<FloodReport> list = new ArrayList<>();
        try {
            JSONArray arr = loadArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new FloodReport(
                        obj.getDouble("lat"),
                        obj.getDouble("lon"),
                        obj.getString("severity"),
                        obj.getString("description"),
                        obj.optString("locationName", "Unknown"),
                        obj.getLong("timestamp")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    /**
     * Clear all reports.
     */
    public void clearReports() {
        prefs.edit().remove(KEY_REPORTS).apply();
    }

    private JSONArray loadArray() {
        try {
            String json = prefs.getString(KEY_REPORTS, "[]");
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
