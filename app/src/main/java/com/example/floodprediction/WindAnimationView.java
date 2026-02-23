package com.example.floodprediction;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rich animated risk-map view showing:
 *  - Humidity heatmap zones (NOAA-style color gradient)
 *  - Flowing wind particles with direction arrows
 *  - Rain streaks when raining
 *  - 5-level risk category badge
 *  - Legend bar
 *
 * Call setWeatherData() after receiving API data.
 */
public class WindAnimationView extends View {

    // ── Particle pool ──
    private static final int PARTICLE_COUNT = 40;
    private final List<WindParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    // ── Paints ──
    private final Paint bgPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zonePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeText   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint= new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Weather state ──
    private float windSpeedMs = 5f;   // m/s
    private int windDeg = 180;         // degrees
    private float rainfall = 0f;       // mm
    private int humidity = 60;         // %
    private int riskCategory = 1;      // 1-5
    private String riskLabel = "SAFE";
    private int riskColor = 0xFF4CAF50;

    // ── Animation ──
    private ValueAnimator animator;
    private float animPhase = 0f;

    // ── 5-Level Risk Colors ──
    private static final int COLOR_SAFE     = 0xFF4CAF50; // Green
    private static final int COLOR_ADVISORY = 0xFF2196F3; // Blue
    private static final int COLOR_ALERT    = 0xFFFF9800; // Orange
    private static final int COLOR_WARNING  = 0xFFFF5722; // Deep Orange
    private static final int COLOR_DANGER   = 0xFFF44336; // Red

    // ── Humidity heatmap colors (like NOAA) ──
    private static final int[] HUMIDITY_COLORS = {
        0xFF66BB6A, // 30-45% green
        0xFFFDD835, // 46-60% yellow
        0xFFFFA726, // 61-75% orange
        0xFFEF5350, // 76-85% red-orange
        0xFFD32F2F, // 86-95% deep red
        0xFF880E4F  // >95% dark red/purple
    };

    public WindAnimationView(Context context) { super(context); init(); }
    public WindAnimationView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public WindAnimationView(Context context, AttributeSet attrs, int ds) { super(context, attrs, ds); init(); }

    private void init() {
        gridPaint.setColor(0x15000000);
        gridPaint.setStrokeWidth(1f);

        outlinePaint.setColor(0x30000000);
        outlinePaint.setStrokeWidth(2f);
        outlinePaint.setStyle(Paint.Style.STROKE);

        particlePaint.setStrokeCap(Paint.Cap.ROUND);
        particlePaint.setStyle(Paint.Style.STROKE);

        badgeText.setTypeface(Typeface.DEFAULT_BOLD);
        badgeText.setTextAlign(Paint.Align.CENTER);

        textPaint.setTextAlign(Paint.Align.CENTER);

        legendPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new WindParticle());
        }
        startAnimation();
    }

    /**
     * Feed real weather data. Call from MainActivity after API response.
     */
    public void setWeatherData(double windSpeedMs, int windDeg,
                                double rainfall, int humidity, String riskLevel) {
        this.windSpeedMs = (float) windSpeedMs;
        this.windDeg = windDeg;
        this.rainfall = (float) rainfall;
        this.humidity = humidity;

        // Determine 5-level category from the standard
        float windKmh = this.windSpeedMs * 3.6f;  // convert m/s → km/h

        if (windKmh > 90 || humidity > 95) {
            riskCategory = 5; riskLabel = "DANGER"; riskColor = COLOR_DANGER;
        } else if (windKmh > 70 || humidity > 90) {
            riskCategory = 4; riskLabel = "WARNING"; riskColor = COLOR_WARNING;
        } else if (windKmh > 50 || humidity > 80) {
            riskCategory = 3; riskLabel = "ALERT"; riskColor = COLOR_ALERT;
        } else if (windKmh > 30 || humidity > 60) {
            riskCategory = 2; riskLabel = "ADVISORY"; riskColor = COLOR_ADVISORY;
        } else {
            riskCategory = 1; riskLabel = "SAFE"; riskColor = COLOR_SAFE;
        }

        // Allow override from WeatherHelper risk level (prioritize API comprehensive analysis)
        if ("DANGER".equals(riskLevel)) {
            riskCategory = 5; riskLabel = "DANGER"; riskColor = COLOR_DANGER;
        } else if ("WARNING".equals(riskLevel) && riskCategory < 4) {
            riskCategory = 4; riskLabel = "WARNING"; riskColor = COLOR_WARNING;
        } else if ("ALERT".equals(riskLevel) && riskCategory < 3) {
            riskCategory = 3; riskLabel = "ALERT"; riskColor = COLOR_ALERT;
        } else if ("ADVISORY".equals(riskLevel) && riskCategory < 2) {
            riskCategory = 2; riskLabel = "ADVISORY"; riskColor = COLOR_ADVISORY;
        }

        for (WindParticle p : particles) {
            p.speedMultiplier = 0.4f + this.windSpeedMs / 15f;
        }
        invalidate();
    }

    private void startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            animPhase = (float) a.getAnimatedValue();
            updateParticles();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        drawBackground(canvas, w, h);
        drawHumidityZones(canvas, w, h);
        drawGrid(canvas, w, h);
        drawMapOutline(canvas, w, h);
        drawWindParticles(canvas, w, h);
        drawRainDrops(canvas, w, h);
        drawRiskBadge(canvas, w, h);
        drawLegend(canvas, w, h);
        drawWindInfo(canvas, w, h);
    }

    // ═══════════════════════════════════════
    //  1. GRADIENT BACKGROUND
    // ═══════════════════════════════════════
    private void drawBackground(Canvas canvas, int w, int h) {
        // Subtle gradient base matching risk
        int baseLight = blendColor(0xFFF5F5F5, riskColor, 0.08f);
        int baseDark  = blendColor(0xFFEEEEEE, riskColor, 0.15f);
        LinearGradient bg = new LinearGradient(0, 0, w, h, baseLight, baseDark, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
    }

    // ═══════════════════════════════════════
    //  2. HUMIDITY HEATMAP ZONES
    // ═══════════════════════════════════════
    private void drawHumidityZones(Canvas canvas, int w, int h) {
        // Draw humidity color zones (like NOAA heatmap) as organic rounded shapes
        int humColor = getHumidityColor(humidity);

        // Central large zone
        zonePaint.setColor(blendColor(humColor, 0xFFFFFFFF, 0.4f));
        RectF centerZone = new RectF(w * 0.15f, h * 0.1f, w * 0.85f, h * 0.75f);
        canvas.drawRoundRect(centerZone, 30, 30, zonePaint);

        // Inner hotter zone (more saturated)
        zonePaint.setColor(blendColor(humColor, 0xFFFFFFFF, 0.2f));
        RectF innerZone = new RectF(w * 0.25f, h * 0.18f, w * 0.7f, h * 0.6f);
        canvas.drawRoundRect(innerZone, 25, 25, zonePaint);

        // Core (most intense)
        zonePaint.setColor(blendColor(humColor, 0xFFFFFFFF, 0.05f));
        RectF coreZone = new RectF(w * 0.35f, h * 0.25f, w * 0.6f, h * 0.5f);
        canvas.drawRoundRect(coreZone, 20, 20, zonePaint);

        // Pulse animation on core zone for WARNING+
        if (riskCategory >= 4) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(animPhase * Math.PI * 2);
            zonePaint.setColor(blendColor(humColor, 0x00FFFFFF, 1f - pulse * 0.3f));
            canvas.drawRoundRect(coreZone, 20, 20, zonePaint);
        }
    }

    // ═══════════════════════════════════════
    //  3. GRID
    // ═══════════════════════════════════════
    private void drawGrid(Canvas canvas, int w, int h) {
        float spacing = 35f;
        for (float x = 0; x < w; x += spacing)
            canvas.drawLine(x, 0, x, h, gridPaint);
        for (float y = 0; y < h; y += spacing)
            canvas.drawLine(0, y, w, y, gridPaint);
    }

    // ═══════════════════════════════════════
    //  4. MALAYSIA MAP OUTLINE (simplified)
    // ═══════════════════════════════════════


    // ═══════════════════════════════════════
    //  5. WIND PARTICLES
    // ═══════════════════════════════════════
    private void drawWindParticles(Canvas canvas, int w, int h) {
        double rad = Math.toRadians(windDeg);
        float dirX = (float) Math.sin(rad);
        float dirY = -(float) Math.cos(rad);

        for (WindParticle p : particles) {
            float len = 12f + windSpeedMs * 2.2f;
            float x2 = p.x - dirX * len;
            float y2 = p.y - dirY * len;

            int alpha = (int) (p.alpha * 200);
            int pColor;
            if (riskCategory >= 4) {
                pColor = blendColor(riskColor, 0xFFFFFFFF, 0.3f);
            } else {
                pColor = 0xFF546E7A; // blue-grey
            }
            particlePaint.setColor((pColor & 0x00FFFFFF) | (alpha << 24));
            particlePaint.setStrokeWidth(1.2f + p.thickness);

            canvas.drawLine(p.x, p.y, x2, y2, particlePaint);

            // Arrowhead
            if (windSpeedMs > 2) {
                float aLen = 5f;
                float ax1 = p.x - (float) Math.sin(rad + 0.5) * aLen;
                float ay1 = p.y + (float) Math.cos(rad + 0.5) * aLen;
                float ax2 = p.x - (float) Math.sin(rad - 0.5) * aLen;
                float ay2 = p.y + (float) Math.cos(rad - 0.5) * aLen;
                particlePaint.setStrokeWidth(1.2f);
                canvas.drawLine(p.x, p.y, ax1, ay1, particlePaint);
                canvas.drawLine(p.x, p.y, ax2, ay2, particlePaint);
            }
        }
    }

    // ═══════════════════════════════════════
    //  6. RAIN DROPS
    // ═══════════════════════════════════════
    private void drawRainDrops(Canvas canvas, int w, int h) {
        if (rainfall < 1) return;
        Paint rainP = new Paint(Paint.ANTI_ALIAS_FLAG);
        rainP.setStrokeWidth(1.5f);
        rainP.setStrokeCap(Paint.Cap.ROUND);

        int dropCount = Math.min((int) (rainfall * 3), 50);
        Random dr = new Random((long) (animPhase * 1000));
        for (int i = 0; i < dropCount; i++) {
            float dx = dr.nextFloat() * w;
            float dy = (dr.nextFloat() * h + animPhase * h * 4) % h;
            float dropLen = 6f + rainfall * 0.5f;

            int rainAlpha = 40 + (int) (rainfall * 3);
            rainP.setColor(Color.argb(Math.min(rainAlpha, 120), 33, 150, 243));
            canvas.drawLine(dx, dy, dx + 1, dy + dropLen, rainP);
        }
    }

    // ═══════════════════════════════════════
    //  7. RISK CATEGORY BADGE (top right)
    // ═══════════════════════════════════════
    private void drawRiskBadge(Canvas canvas, int w, int h) {
        float bx = w - 14f, by = 14f;
        float bw = 90f, bh = 40f;

        // Badge background
        RectF badgeRect = new RectF(bx - bw, by, bx, by + bh);
        badgePaint.setColor(riskColor);
        badgePaint.setAlpha(220);
        canvas.drawRoundRect(badgeRect, 10, 10, badgePaint);

        // Badge outline glow for WARNING+
        if (riskCategory >= 4) {
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(2f);
            float pulse = 0.5f + 0.5f * (float) Math.sin(animPhase * Math.PI * 2);
            glowPaint.setColor(riskColor);
            glowPaint.setAlpha((int) (pulse * 180));
            RectF glowRect = new RectF(badgeRect.left - 2, badgeRect.top - 2,
                                        badgeRect.right + 2, badgeRect.bottom + 2);
            canvas.drawRoundRect(glowRect, 12, 12, glowPaint);
        }

        // Category number
        badgeText.setColor(Color.WHITE);
        badgeText.setTextSize(22f);
        canvas.drawText(String.valueOf(riskCategory), bx - bw + 18f, by + bh * 0.72f, badgeText);

        // Category label
        badgeText.setTextSize(11f);
        badgeText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(riskLabel, bx - bw + 32f, by + bh * 0.68f, badgeText);
        badgeText.setTextAlign(Paint.Align.CENTER);
    }

    // ═══════════════════════════════════════
    //  8. HUMIDITY LEGEND BAR (bottom left)
    // ═══════════════════════════════════════
    private void drawLegend(Canvas canvas, int w, int h) {
        float lx = 10f, ly = h - 36f;
        float lw = w * 0.45f, lh = 10f;

        // Gradient bar
        LinearGradient legendGrad = new LinearGradient(
            lx, ly, lx + lw, ly,
            new int[]{HUMIDITY_COLORS[0], HUMIDITY_COLORS[1], HUMIDITY_COLORS[2],
                      HUMIDITY_COLORS[3], HUMIDITY_COLORS[4], HUMIDITY_COLORS[5]},
            null, Shader.TileMode.CLAMP);
        legendPaint.setShader(legendGrad);
        RectF legendBar = new RectF(lx, ly, lx + lw, ly + lh);
        canvas.drawRoundRect(legendBar, 5, 5, legendPaint);
        legendPaint.setShader(null);

        // Labels
        Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
        lbl.setColor(0x99000000);
        lbl.setTextSize(8f);
        canvas.drawText("30%", lx, ly - 3f, lbl);
        lbl.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(">95%", lx + lw, ly - 3f, lbl);
        lbl.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("HUMIDITY", lx + lw / 2f, ly - 3f, lbl);

        // Current humidity marker
        float markerX = lx + lw * Math.max(0, Math.min(1, (humidity - 30f) / 70f));
        Paint markerP = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerP.setColor(0xDD000000);
        markerP.setStyle(Paint.Style.FILL);
        // Triangle marker
        Path tri = new Path();
        tri.moveTo(markerX, ly);
        tri.lineTo(markerX - 4, ly - 6);
        tri.lineTo(markerX + 4, ly - 6);
        tri.close();
        canvas.drawPath(tri, markerP);
    }

    // ════════════════════════════════════════════════
    //  4. MALAYSIA MAP OUTLINE (Refined)
    // ════════════════════════════════════════════════
    private void drawMapOutline(Canvas canvas, int w, int h) {
        // Peninsular Malaysia
        Path peninsula = new Path();
        float px = w * 0.28f, py = h * 0.15f;
        peninsula.moveTo(px, py); // Thai border
        peninsula.lineTo(px + w * 0.10f, py + h * 0.08f); // Kelantan/Terengganu coast
        peninsula.lineTo(px + w * 0.14f, py + h * 0.25f); // East coast
        peninsula.lineTo(px + w * 0.12f, py + h * 0.45f); // Johor east
        peninsula.lineTo(px + w * 0.06f, py + h * 0.55f); // Johor south (singapore tip)
        peninsula.lineTo(px + w * 0.00f, py + h * 0.45f); // Malacca strait
        peninsula.lineTo(px - w * 0.05f, py + h * 0.25f); // Perak coast
        peninsula.lineTo(px - w * 0.02f, py + h * 0.05f); // Perlis/Kedah
        peninsula.close();

        // East Malaysia (Borneo)
        Path east = new Path();
        float ex = w * 0.55f, ey = h * 0.25f; // Starts near Kuching
        east.moveTo(ex, ey);
        east.lineTo(ex + w * 0.10f, ey - h * 0.10f); // Sarawak inland
        east.lineTo(ex + w * 0.25f, ey - h * 0.15f); // Brunei border area
        east.lineTo(ex + w * 0.32f, ey - h * 0.05f); // Sabah north tip
        east.lineTo(ex + w * 0.35f, ey + h * 0.10f); // Sabah east coast
        east.lineTo(ex + w * 0.25f, ey + h * 0.20f); // Sabah/Indo border
        east.lineTo(ex + w * 0.10f, ey + h * 0.15f); // Sarawak/Indo border
        east.close();

        outlinePaint.setColor(0x50000000);
        outlinePaint.setStrokeWidth(2f);
        outlinePaint.setStyle(Paint.Style.STROKE);
        
        // Draw filled shape first for contrast against heatmap
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(0x20FFFFFF);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(peninsula, fillPaint);
        canvas.drawPath(east, fillPaint);
        
        // Then outline
        canvas.drawPath(peninsula, outlinePaint);
        canvas.drawPath(east, outlinePaint);

        // City dots
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(0x80000000);
        float dotR = 4f;

        // Kuala Lumpur
        canvas.drawCircle(px + w * 0.02f, py + h * 0.35f, dotR, dotPaint);
        // Johor Bahru
        canvas.drawCircle(px + w * 0.08f, py + h * 0.52f, dotR, dotPaint);
        // Kuching
        canvas.drawCircle(ex + w * 0.05f, ey + h * 0.05f, dotR, dotPaint);
        // Kota Kinabalu
        canvas.drawCircle(ex + w * 0.28f, ey - h * 0.05f, dotR, dotPaint);
    }
    
    // ...

    // ════════════════════════════════════════════════
    //  9. WIND INFO (km/h)
    // ════════════════════════════════════════════════
    private void drawWindInfo(Canvas canvas, int w, int h) {
        float windKmh = windSpeedMs * 3.6f;
        String info = String.format("💨 %.0f km/h %s", windKmh, getWindArrow());

        textPaint.setColor(0xAA000000);
        textPaint.setTextSize(32f); // Slightly larger
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(info, w - 20f, h - 16f, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT);
    }

    // ═══════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════
    private String getWindArrow() {
        int d = ((windDeg + 22) % 360) / 45;
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        return arrows[d % 8];
    }

    private int getHumidityColor(int hum) {
        if (hum > 95) return HUMIDITY_COLORS[5];
        if (hum > 85) return HUMIDITY_COLORS[4];
        if (hum > 75) return HUMIDITY_COLORS[3];
        if (hum > 60) return HUMIDITY_COLORS[2];
        if (hum > 45) return HUMIDITY_COLORS[1];
        return HUMIDITY_COLORS[0];
    }

    private int blendColor(int c1, int c2, float ratio) {
        float inv = 1f - ratio;
        int r = (int) (Color.red(c1) * inv + Color.red(c2) * ratio);
        int g = (int) (Color.green(c1) * inv + Color.green(c2) * ratio);
        int b = (int) (Color.blue(c1) * inv + Color.blue(c2) * ratio);
        int a = (int) (Color.alpha(c1) * inv + Color.alpha(c2) * ratio);
        return Color.argb(a, r, g, b);
    }

    // ═══════════════════════════════════════
    //  PARTICLE UPDATE
    // ═══════════════════════════════════════
    private void updateParticles() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        double rad = Math.toRadians(windDeg);
        float dirX = (float) Math.sin(rad);
        float dirY = -(float) Math.cos(rad);

        for (WindParticle p : particles) {
            float speed = 1.5f + windSpeedMs * 0.7f * p.speedMultiplier;
            p.x += dirX * speed;
            p.y += dirY * speed;
            p.life -= 0.012f * p.speedMultiplier;

            if (p.life > 0.85f) p.alpha = (1f - p.life) * 6.67f;
            else if (p.life < 0.15f) p.alpha = p.life * 6.67f;
            else p.alpha = 1f;

            if (p.life <= 0 || p.x < -60 || p.x > w + 60 || p.y < -60 || p.y > h + 60) {
                resetParticle(p, w, h);
            }
        }
    }

    private void resetParticle(WindParticle p, int w, int h) {
        double rad = Math.toRadians(windDeg);
        float dirX = (float) Math.sin(rad);
        float dirY = -(float) Math.cos(rad);

        if (Math.abs(dirX) > Math.abs(dirY)) {
            p.x = dirX > 0 ? random.nextFloat() * -40 : w + random.nextFloat() * 40;
            p.y = random.nextFloat() * h;
        } else {
            p.x = random.nextFloat() * w;
            p.y = dirY > 0 ? h + random.nextFloat() * 40 : -random.nextFloat() * 40;
        }
        p.life = 0.8f + random.nextFloat() * 0.2f;
        p.alpha = 0f;
        p.thickness = random.nextFloat() * 1.8f;
        p.speedMultiplier = 0.4f + random.nextFloat() * 1.3f;
    }

    private static class WindParticle {
        float x, y;
        float life = 1f;
        float alpha = 1f;
        float thickness = 1f;
        float speedMultiplier = 1f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (WindParticle p : particles) {
            p.x = random.nextFloat() * w;
            p.y = random.nextFloat() * h;
            p.life = random.nextFloat();
            p.thickness = random.nextFloat() * 1.8f;
            p.speedMultiplier = 0.4f + random.nextFloat() * 1.3f;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isRunning()) animator.start();
    }
}
