package com.example.floodprediction;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

/**
 * Draws a title banner and gradient legend bar on the map canvas,
 * similar to the reference US wind speed map.
 *
 * - Title: "MALAYSIA — Flood Risk" in top-right
 * - Legend: Color gradient bar with scale labels at bottom
 */
public class MapLegendOverlay extends Overlay {

    private final Paint titlePaint;
    private final Paint subtitlePaint;
    private final Paint labelPaint;
    private final Paint barPaint;
    private final Paint bgPaint;
    private final Paint borderPaint;

    public MapLegendOverlay() {
        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(42f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.RIGHT);
        titlePaint.setShadowLayer(6f, 2f, 2f, Color.BLACK);

        subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtitlePaint.setColor(Color.rgb(0, 229, 255)); // accent color
        subtitlePaint.setTextSize(28f);
        subtitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        subtitlePaint.setTextAlign(Paint.Align.RIGHT);
        subtitlePaint.setShadowLayer(4f, 1f, 1f, Color.BLACK);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(22f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK);

        barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);

        bgPaint = new Paint();
        bgPaint.setColor(Color.argb(160, 10, 25, 41)); // dark semi-transparent bg
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.argb(100, 255, 255, 255));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) return;

        int w = canvas.getWidth();
        int h = canvas.getHeight();

        // ── Title (top-right) ──
        float titleX = w - 30f;
        float titleY = 60f;

        // Background for title area
        RectF titleBg = new RectF(titleX - 380, titleY - 50, titleX + 10, titleY + 30);
        canvas.drawRoundRect(titleBg, 12, 12, bgPaint);

        canvas.drawText("MALAYSIA", titleX, titleY, titlePaint);
        canvas.drawText("Flood Risk", titleX, titleY + 28, subtitlePaint);

        // ── Legend Bar (bottom-center) ──
        float barWidth = Math.min(w * 0.7f, 500f);
        float barHeight = 20f;
        float barX = (w - barWidth) / 2f;
        float barY = h - 80f;

        // Background
        RectF legendBg = new RectF(barX - 20, barY - 35, barX + barWidth + 20, barY + barHeight + 35);
        canvas.drawRoundRect(legendBg, 12, 12, bgPaint);
        canvas.drawRoundRect(legendBg, 12, 12, borderPaint);

        // Title
        Paint legendTitle = new Paint(labelPaint);
        legendTitle.setTextSize(20f);
        canvas.drawText("Flood Risk Score — [0–100]",
                barX + barWidth / 2f, barY - 12, legendTitle);

        // Gradient bar segments
        int segments = 5;
        float segWidth = barWidth / segments;
        int[][] colors = {
                {0, 0, 255},     // Blue (0-20)
                {0, 200, 200},   // Cyan (20-40)
                {0, 255, 0},     // Green (40-60)
                {255, 255, 0},   // Yellow (60-80)
                {255, 0, 0},     // Red (80-100)
        };

        for (int i = 0; i < segments; i++) {
            float sx = barX + i * segWidth;
            int[] c1 = colors[i];
            int[] c2 = (i < segments - 1) ? colors[i + 1] : colors[i];

            barPaint.setShader(new LinearGradient(
                    sx, barY, sx + segWidth, barY,
                    Color.rgb(c1[0], c1[1], c1[2]),
                    Color.rgb(c2[0], c2[1], c2[2]),
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRect(sx, barY, sx + segWidth, barY + barHeight, barPaint);
        }
        barPaint.setShader(null);

        // Scale labels
        String[] labels = {"0", "20", "40", "60", "80", "100"};
        for (int i = 0; i <= segments; i++) {
            float lx = barX + i * segWidth;
            canvas.drawText(labels[i], lx, barY + barHeight + 22, labelPaint);
        }
    }
}
