package com.example.floodprediction;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

/**
 * Activity where users can:
 * 1. Take a photo or pick from gallery
 * 2. Have Gemini AI verify if the image shows flooding
 * 3. Submit confirmed flood reports to appear on the map
 *
 * Works on emulator: uses gallery picker (no physical camera needed).
 */
public class FloodReportActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private Button btnTakePhoto, btnPickGallery, btnVerify, btnSubmit;
    private ProgressBar progressVerify;
    private LinearLayout layoutResult;
    private TextView tvVerifyResult, tvFloodStatus;
    private Spinner spinnerLocation;

    private Bitmap selectedBitmap;
    private boolean floodConfirmed = false;
    private String detectedSeverity = "MEDIUM";
    private String detectedDescription = "";

    // City coordinates for location selection
    private static final String[] LOCATIONS = {
            "Kuala Lumpur", "George Town", "Johor Bahru", "Kuching",
            "Kota Kinabalu", "Shah Alam", "Ipoh", "Kuantan",
            "Melaka", "Kota Bharu", "Alor Setar", "Seremban"
    };

    private static final double[][] COORDS = {
            {3.1390, 101.6869}, {5.4164, 100.3327}, {1.4927, 103.7414},
            {1.5535, 110.3593}, {5.9804, 116.0735}, {3.0738, 101.5183},
            {4.5975, 101.0901}, {3.8077, 103.3260}, {2.1896, 102.2501},
            {6.1254, 102.2381}, {6.1184, 100.3685}, {2.7258, 101.9424}
    };

    // Gallery picker
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        ivPhoto.setImageURI(uri);
                        InputStream is = getContentResolver().openInputStream(uri);
                        selectedBitmap = BitmapFactory.decodeStream(is);
                        if (is != null) is.close();
                        onImageSelected();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Camera (returns thumbnail bitmap — works on emulator)
    private final ActivityResultLauncher<Void> takePhoto = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap);
                    selectedBitmap = bitmap;
                    onImageSelected();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_report);

        ivPhoto = findViewById(R.id.ivReportPhoto);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        btnVerify = findViewById(R.id.btnVerifyAI);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        progressVerify = findViewById(R.id.progressVerify);
        layoutResult = findViewById(R.id.layoutResult);
        tvVerifyResult = findViewById(R.id.tvVerifyResult);
        tvFloodStatus = findViewById(R.id.tvFloodStatus);
        spinnerLocation = findViewById(R.id.spinnerReportLocation);

        // Back button
        findViewById(R.id.btnReportBack).setOnClickListener(v -> finish());

        // Location spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, LOCATIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(adapter);

        // Photo buttons
        btnTakePhoto.setOnClickListener(v -> takePhoto.launch(null));
        btnPickGallery.setOnClickListener(v -> pickImage.launch("image/*"));

        // Verify button
        btnVerify.setOnClickListener(v -> verifyWithAI());

        // Submit button
        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void onImageSelected() {
        btnVerify.setEnabled(true);
        layoutResult.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.GONE);
        floodConfirmed = false;
    }

    private void verifyWithAI() {
        if (selectedBitmap == null) return;

        progressVerify.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);
        layoutResult.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.GONE);

        new GeminiHelper().analyzeFloodImage(
                BuildConfig.API_KEY,
                selectedBitmap,
                response -> {
                    runOnUiThread(() -> {
                        progressVerify.setVisibility(View.GONE);
                        btnVerify.setEnabled(true);
                        layoutResult.setVisibility(View.VISIBLE);
                        tvVerifyResult.setText(response);

                        // Parse the AI response
                        parseAIResponse(response);
                    });
                    return null;
                }
        );
    }

    private void parseAIResponse(String response) {
        String upper = response.toUpperCase();

        if (upper.contains("FLOOD_DETECTED: YES") || upper.contains("FLOOD_DETECTED:YES")) {
            floodConfirmed = true;

            // Parse severity
            if (upper.contains("SEVERITY: HIGH") || upper.contains("SEVERITY:HIGH")) {
                detectedSeverity = "HIGH";
            } else if (upper.contains("SEVERITY: MEDIUM") || upper.contains("SEVERITY:MEDIUM")) {
                detectedSeverity = "MEDIUM";
            } else {
                detectedSeverity = "LOW";
            }

            // Parse description
            int descIdx = upper.indexOf("DESCRIPTION:");
            if (descIdx != -1) {
                String desc = response.substring(descIdx + 12).trim();
                int nl = desc.indexOf('\n');
                detectedDescription = (nl != -1) ? desc.substring(0, nl).trim() : desc.trim();
            } else {
                detectedDescription = "Flood detected by AI";
            }

            tvFloodStatus.setText("✅ FLOOD CONFIRMED — " + detectedSeverity + " severity");
            tvFloodStatus.setTextColor(getResources().getColor(
                    detectedSeverity.equals("HIGH") ? R.color.risk_high :
                            detectedSeverity.equals("MEDIUM") ? R.color.risk_medium : R.color.risk_low
            ));

            btnSubmit.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(true);

        } else {
            floodConfirmed = false;
            tvFloodStatus.setText("❌ No flooding detected in this image");
            tvFloodStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            btnSubmit.setVisibility(View.GONE);
        }
    }

    private void submitReport() {
        if (!floodConfirmed) return;

        int idx = spinnerLocation.getSelectedItemPosition();
        double lat = COORDS[idx][0];
        double lon = COORDS[idx][1];
        String locationName = LOCATIONS[idx];

        FloodReportManager.getInstance(this)
                .saveReport(lat, lon, detectedSeverity, detectedDescription, locationName);

        Toast.makeText(this,
                "🚨 Flood report submitted for " + locationName + "!\nIt will appear on the map.",
                Toast.LENGTH_LONG).show();

        // Return to map
        finish();
    }
}
