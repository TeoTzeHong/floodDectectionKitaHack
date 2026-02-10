package com.example.floodprediction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Dense wind field overlay that draws many small wind arrows across Malaysia,
 * interpolating wind speed and direction from city data points.
 * Similar to NOAA/GSL forecast wind maps.
 */
public class WindFieldOverlay extends Overlay {

    private final List<WindDataPoint> dataPoints = new ArrayList<>();
    private final Paint arrowPaint;
    private final Paint speedPaint;

    // Malaysia bounding box
    private static final double LAT_MIN = 1.0;
    private static final double LAT_MAX = 7.2;
    private static final double LON_MIN = 99.5;
    private static final double LON_MAX = 119.0;

    // Grid density — how many arrows across each axis
    private static final int GRID_COLS = 20;
    private static final int GRID_ROWS = 12;

    public static class WindDataPoint {
        public double lat, lon;
        public double speed; // m/s
        public double deg;   // degrees

        public WindDataPoint(double lat, double lon, double speed, double deg) {
            this.lat = lat;
            this.lon = lon;
            this.speed = speed;
            this.deg = deg;
        }
    }

    public WindFieldOverlay() {
        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(2.5f);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);

        speedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        speedPaint.setTextSize(18f);
        speedPaint.setTextAlign(Paint.Align.CENTER);
        speedPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK);
    }

    public void clearData() {
        dataPoints.clear();
    }

    public void addDataPoint(double lat, double lon, double speed, double deg) {
        dataPoints.add(new WindDataPoint(lat, lon, speed, deg));
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || dataPoints.isEmpty()) return;

        Projection proj = mapView.getProjection();

        double latStep = (LAT_MAX - LAT_MIN) / GRID_ROWS;
        double lonStep = (LON_MAX - LON_MIN) / GRID_COLS;

        for (int row = 0; row <= GRID_ROWS; row++) {
            for (int col = 0; col <= GRID_COLS; col++) {
                double lat = LAT_MAX - row * latStep;
                double lon = LON_MIN + col * lonStep;

                // Interpolate wind speed and direction at this grid point
                double[] wind = interpolateWind(lat, lon);
                double speed = wind[0];
                double deg = wind[1];

                // Convert to screen coordinates
                Point p = proj.toPixels(new GeoPoint(lat, lon), null);

                // Skip if off-screen
                if (p.x < -50 || p.x > canvas.getWidth() + 50 ||
                    p.y < -50 || p.y > canvas.getHeight() + 50) continue;

                // Draw wind arrow at this point
                drawWindArrow(canvas, p.x, p.y, speed, deg);
            }
        }
    }

    /**
     * IDW interpolation for wind — interpolates both speed and direction.
     * Direction interpolation uses vector decomposition to avoid 0°/360° wrap issues.
     */
    private double[] interpolateWind(double lat, double lon) {
        double power = 2.0;
        double wSum = 0;
        double speedSum = 0;
        double uSum = 0; // x-component of wind vector
        double vSum = 0; // y-component of wind vector

        for (WindDataPoint dp : dataPoints) {
            double dist = Math.sqrt(
                    Math.pow(dp.lat - lat, 2) + Math.pow(dp.lon - lon, 2)
            );

            if (dist < 0.01) return new double[]{dp.speed, dp.deg};

            double w = 1.0 / Math.pow(dist, power);
            wSum += w;
            speedSum += w * dp.speed;

            // Decompose direction into vector components for proper averaging
            double rad = Math.toRadians(dp.deg);
            uSum += w * Math.sin(rad);
            vSum += w * Math.cos(rad);
        }

        if (wSum == 0) return new double[]{0, 0};

        double avgSpeed = speedSum / wSum;
        double avgU = uSum / wSum;
        double avgV = vSum / wSum;
        double avgDeg = Math.toDegrees(Math.atan2(avgU, avgV));
        if (avgDeg < 0) avgDeg += 360;

        return new double[]{avgSpeed, avgDeg};
    }

    /**
     * Draw a single wind arrow — line + arrowhead, colored by speed.
     */
    private void drawWindArrow(Canvas canvas, float cx, float cy, double speed, double deg) {
        // Color by speed
        int color = speedToColor(speed);
        arrowPaint.setColor(color);
        arrowPaint.setStyle(Paint.Style.STROKE);

        // Arrow length scales with speed
        float baseLen = 12f;
        float len = baseLen + (float)(speed * 1.8f);
        len = Math.min(len, 55f); // cap length

        // Stroke width scales slightly with speed
        arrowPaint.setStrokeWidth(1.5f + (float)(speed / 15.0));

        // Direction: degrees → radians. Wind "from" direction, so arrow points downwind
        double rad = Math.toRadians(deg + 180); // flip to show where wind is going

        float ex = cx + (float)(Math.sin(rad) * len);
        float ey = cy - (float)(Math.cos(rad) * len);

        // Draw shaft
        canvas.drawLine(cx, cy, ex, ey, arrowPaint);

        // Draw arrowhead
        float headSize = 6f + (float)(speed / 5.0);
        headSize = Math.min(headSize, 14f);

        float ax1 = ex + (float)(Math.sin(rad + Math.toRadians(150)) * headSize);
        float ay1 = ey - (float)(Math.cos(rad + Math.toRadians(150)) * headSize);
        float ax2 = ex + (float)(Math.sin(rad - Math.toRadians(150)) * headSize);
        float ay2 = ey - (float)(Math.cos(rad - Math.toRadians(150)) * headSize);

        Paint fillPaint = new Paint(arrowPaint);
        fillPaint.setStyle(Paint.Style.FILL);
        Path arrow = new Path();
        arrow.moveTo(ex, ey);
        arrow.lineTo(ax1, ay1);
        arrow.lineTo(ax2, ay2);
        arrow.close();
        canvas.drawPath(arrow, fillPaint);
    }

    /**
     * Map wind speed to color: green → yellow → orange → red → purple
     */
    private int speedToColor(double speed) {
        // Clamp 0-30 m/s
        float t = (float) Math.min(speed / 30.0, 1.0);

        int r, g, b;
        if (t < 0.25f) {
            // Green → Yellow
            float s = t / 0.25f;
            r = (int)(100 * s);
            g = (int)(180 + 75 * s);
            b = (int)(50 * (1 - s));
        } else if (t < 0.5f) {
            // Yellow → Orange
            float s = (t - 0.25f) / 0.25f;
            r = (int)(100 + 155 * s);
            g = (int)(255 - 100 * s);
            b = 0;
        } else if (t < 0.75f) {
            // Orange → Red
            float s = (t - 0.5f) / 0.25f;
            r = 255;
            g = (int)(155 - 155 * s);
            b = 0;
        } else {
            // Red → Purple
            float s = (t - 0.75f) / 0.25f;
            r = (int)(255 - 55 * s);
            g = 0;
            b = (int)(180 * s);
        }

        return Color.argb(220, r, g, b);
    }
}
