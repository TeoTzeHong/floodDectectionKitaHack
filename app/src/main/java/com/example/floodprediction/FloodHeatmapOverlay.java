package com.example.floodprediction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a smooth color-gradient heatmap overlay across Malaysia,
 * interpolating flood risk values between city data points using
 * inverse-distance weighting (IDW).
 *
 * Inspired by the reference US wind speed map style.
 */
public class FloodHeatmapOverlay extends Overlay {

    private final List<DataPoint> dataPoints = new ArrayList<>();
    private final Paint cellPaint;

    // Malaysia bounding box (approximate)
    private static final double LAT_MIN = 0.8;   // southern tip
    private static final double LAT_MAX = 7.5;   // northern tip
    private static final double LON_MIN = 99.5;   // western coast
    private static final double LON_MAX = 119.5;  // eastern Sabah

    // Grid resolution — lower = faster, higher = smoother
    private static final int GRID_COLS = 60;
    private static final int GRID_ROWS = 40;

    public static class DataPoint {
        public double lat, lon;
        public int riskScore; // 0-100

        public DataPoint(double lat, double lon, int riskScore) {
            this.lat = lat;
            this.lon = lon;
            this.riskScore = riskScore;
        }
    }

    public FloodHeatmapOverlay() {
        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.FILL);
        cellPaint.setAntiAlias(false);
    }

    public void clearData() {
        dataPoints.clear();
    }

    public void addDataPoint(double lat, double lon, int riskScore) {
        dataPoints.add(new DataPoint(lat, lon, riskScore));
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || dataPoints.isEmpty()) return;

        Projection proj = mapView.getProjection();

        // Convert bounding-box corners to screen coordinates
        Point topLeft = proj.toPixels(new GeoPoint(LAT_MAX, LON_MIN), null);
        Point bottomRight = proj.toPixels(new GeoPoint(LAT_MIN, LON_MAX), null);

        float screenWidth = bottomRight.x - topLeft.x;
        float screenHeight = bottomRight.y - topLeft.y;

        if (screenWidth <= 0 || screenHeight <= 0) return;

        float cellW = screenWidth / GRID_COLS;
        float cellH = screenHeight / GRID_ROWS;

        double latStep = (LAT_MAX - LAT_MIN) / GRID_ROWS;
        double lonStep = (LON_MAX - LON_MIN) / GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                double lat = LAT_MAX - row * latStep - latStep / 2;
                double lon = LON_MIN + col * lonStep + lonStep / 2;

                // Inverse-distance-weighted interpolation
                double value = interpolateIDW(lat, lon);

                // Map value to color
                int color = riskToColor(value);

                float x = topLeft.x + col * cellW;
                float y = topLeft.y + row * cellH;

                cellPaint.setColor(color);
                canvas.drawRect(x, y, x + cellW + 1, y + cellH + 1, cellPaint);
            }
        }
    }

    /**
     * Inverse-distance-weighted interpolation.
     * Gives closer data points more influence.
     */
    private double interpolateIDW(double lat, double lon) {
        double weightSum = 0;
        double valueSum = 0;
        double power = 2.5; // higher = more local influence

        for (DataPoint dp : dataPoints) {
            double dist = Math.sqrt(
                    Math.pow(dp.lat - lat, 2) + Math.pow(dp.lon - lon, 2)
            );

            if (dist < 0.01) return dp.riskScore; // exact match

            double w = 1.0 / Math.pow(dist, power);
            weightSum += w;
            valueSum += w * dp.riskScore;
        }

        if (weightSum == 0) return 0;
        return valueSum / weightSum;
    }

    /**
     * Maps a risk score (0-100) to a heatmap color.
     * Gradient: Blue → Cyan → Green → Yellow → Orange → Red
     */
    private int riskToColor(double value) {
        value = Math.max(0, Math.min(100, value));
        float t = (float) (value / 100.0);

        int r, g, b;

        if (t < 0.2f) {
            // Blue → Cyan (0-20)
            float s = t / 0.2f;
            r = 0;
            g = (int) (100 * s);
            b = (int) (200 + 55 * (1 - s));
        } else if (t < 0.4f) {
            // Cyan → Green (20-40)
            float s = (t - 0.2f) / 0.2f;
            r = 0;
            g = (int) (100 + 155 * s);
            b = (int) (200 * (1 - s));
        } else if (t < 0.6f) {
            // Green → Yellow (40-60)
            float s = (t - 0.4f) / 0.2f;
            r = (int) (255 * s);
            g = 255;
            b = 0;
        } else if (t < 0.8f) {
            // Yellow → Orange (60-80)
            float s = (t - 0.6f) / 0.2f;
            r = 255;
            g = (int) (255 - 130 * s);
            b = 0;
        } else {
            // Orange → Red (80-100)
            float s = (t - 0.8f) / 0.2f;
            r = 255;
            g = (int) (125 - 125 * s);
            b = 0;
        }

        return Color.argb(130, r, g, b); // semi-transparent
    }
}
