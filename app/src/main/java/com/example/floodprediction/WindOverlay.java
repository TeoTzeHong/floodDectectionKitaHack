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

public class WindOverlay extends Overlay {

    private final GeoPoint location;
    private final double windSpeed;
    private final double windDeg;
    private final Paint linePaint;
    private final Paint arrowPaint;
    private final Paint textPaint;

    public WindOverlay(GeoPoint location, double windSpeed, double windDeg) {
        this.location = location;
        this.windSpeed = windSpeed;
        this.windDeg = windDeg;

        int color;
        if (windSpeed >= 20) color = Color.rgb(244, 67, 54);
        else if (windSpeed >= 14) color = Color.rgb(255, 152, 0);
        else if (windSpeed >= 8) color = Color.rgb(255, 235, 59);
        else color = Color.rgb(76, 175, 80);

        linePaint = new Paint();
        linePaint.setColor(color);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setAntiAlias(true);

        arrowPaint = new Paint();
        arrowPaint.setColor(color);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(4f, 1f, 1f, Color.BLACK);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) return;

        Projection proj = mapView.getProjection();
        Point p = proj.toPixels(location, null);

        float len = 50f + (float) windSpeed * 2.5f;
        double rad = Math.toRadians(windDeg + 90);

        float ex = p.x + (float) (Math.cos(rad) * len);
        float ey = p.y + (float) (Math.sin(rad) * len);

        canvas.drawLine(p.x, p.y, ex, ey, linePaint);

        float hs = 18f;
        float ax1 = ex + (float) (Math.cos(rad + Math.toRadians(150)) * hs);
        float ay1 = ey + (float) (Math.sin(rad + Math.toRadians(150)) * hs);
        float ax2 = ex + (float) (Math.cos(rad - Math.toRadians(150)) * hs);
        float ay2 = ey + (float) (Math.sin(rad - Math.toRadians(150)) * hs);

        Path arrow = new Path();
        arrow.moveTo(ex, ey);
        arrow.lineTo(ax1, ay1);
        arrow.lineTo(ax2, ay2);
        arrow.close();
        canvas.drawPath(arrow, arrowPaint);

        canvas.drawText(String.format("%.0f m/s", windSpeed), p.x, p.y - 15, textPaint);
    }
}
