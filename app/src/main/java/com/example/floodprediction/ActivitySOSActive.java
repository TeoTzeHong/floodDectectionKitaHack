package com.example.floodprediction;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class ActivitySOSActive extends AppCompatActivity {

    private View pulseCircle1, centerRedDot, innerPulse;
    private TextView tvCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sos_active);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pulseContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pulseCircle1 = findViewById(R.id.pulseCircle1);
        centerRedDot = findViewById(R.id.centerRedDot);
        innerPulse = findViewById(R.id.innerPulse);
        tvCancel = findViewById(R.id.tvCancel);

        tvCancel.setOnClickListener(v -> {
            Toast.makeText(this, "Rescue Request Cancelled", Toast.LENGTH_SHORT).show();
            finish(); // Go back to SOS page
        });

        startPulseAnimation();
    }

    private void startPulseAnimation() {
        // 1. Inner Heartbeat (The red dot core pulses)
        ObjectAnimator scaleXInner = ObjectAnimator.ofFloat(innerPulse, "scaleX", 0f, 1.0f);
        ObjectAnimator scaleYInner = ObjectAnimator.ofFloat(innerPulse, "scaleY", 0f, 1.0f);
        ObjectAnimator alphaInner = ObjectAnimator.ofFloat(innerPulse, "alpha", 1f, 0f); // Fade out as it expands to edge of red dot
        
        scaleXInner.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYInner.setRepeatCount(ObjectAnimator.INFINITE);
        alphaInner.setRepeatCount(ObjectAnimator.INFINITE);
        
        AnimatorSet innerHeartbeat = new AnimatorSet();
        innerHeartbeat.playTogether(scaleXInner, scaleYInner, alphaInner);
        innerHeartbeat.setDuration(1200);
        innerHeartbeat.setInterpolator(new AccelerateDecelerateInterpolator());
        innerHeartbeat.start();

        // 2. Subtle Outer Ripple (Fades in slightly then out)
        ObjectAnimator scaleXOuter = ObjectAnimator.ofFloat(pulseCircle1, "scaleX", 0.8f, 1.4f);
        ObjectAnimator scaleYOuter = ObjectAnimator.ofFloat(pulseCircle1, "scaleY", 0.8f, 1.4f);
        ObjectAnimator alphaOuter = ObjectAnimator.ofFloat(pulseCircle1, "alpha", 0.5f, 0f);

        scaleXOuter.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYOuter.setRepeatCount(ObjectAnimator.INFINITE);
        alphaOuter.setRepeatCount(ObjectAnimator.INFINITE);

        AnimatorSet outerRipple = new AnimatorSet();
        outerRipple.playTogether(scaleXOuter, scaleYOuter, alphaOuter);
        outerRipple.setDuration(1200);
        outerRipple.setStartDelay(200); // Slight offset
        outerRipple.setInterpolator(new AccelerateDecelerateInterpolator());
        outerRipple.start();
        
        // 3. Main Dot "Breathing" (Very subtle scale)
        ObjectAnimator breatheX = ObjectAnimator.ofFloat(centerRedDot, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator breatheY = ObjectAnimator.ofFloat(centerRedDot, "scaleY", 1f, 1.05f, 1f);
        
        breatheX.setRepeatCount(ObjectAnimator.INFINITE);
        breatheY.setRepeatCount(ObjectAnimator.INFINITE);
        
        AnimatorSet breathing = new AnimatorSet();
        breathing.playTogether(breatheX, breatheY);
        breathing.setDuration(1200);
        breathing.setInterpolator(new AccelerateDecelerateInterpolator());
        breathing.start();
    }
}
