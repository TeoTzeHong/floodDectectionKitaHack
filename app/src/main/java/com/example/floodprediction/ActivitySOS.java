package com.example.floodprediction;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ActivitySOS extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView ivWarningIcon;
    private TextView tvPeopleCount, tvCurrentLocation;
    private Button btnMinus, btnPlus, btnActivateSOS;
    private CheckBox cbSpecialAssistance;
    
    private int peopleCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ivWarningIcon), (v, insets) -> {
             // Just a dummy listener to be safe with EdgeToEdge if root id isn't set
             return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ivWarningIcon = findViewById(R.id.ivWarningIcon);
        tvPeopleCount = findViewById(R.id.tvPeopleCount);
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        btnActivateSOS = findViewById(R.id.btnActivateSOS);
        cbSpecialAssistance = findViewById(R.id.cbSpecialAssistance);

        setupCounter();
        setupActivateButton();
        startJumpAnimation();
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        tvCurrentLocation.setText("📍 Detecting Location...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            String address = getAddress(location.getLatitude(), location.getLongitude());
                            tvCurrentLocation.setText("📍 " + address);
                        } else {
                            tvCurrentLocation.setText("📍 Location Unavailable (Try GPS)");
                        }
                    }
                });
    }

    private String getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String result = "";
                if (address.getThoroughfare() != null) result += address.getThoroughfare();
                if (address.getLocality() != null) {
                    if (!result.isEmpty()) result += ", ";
                    result += address.getLocality();
                }
                if (result.isEmpty()) result = "Lat: " + String.format("%.2f", lat) + ", Lng: " + String.format("%.2f", lng); 
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", lat, lng);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                tvCurrentLocation.setText("📍 Permission Denied");
            }
        }
    }

    private void setupCounter() {
        btnMinus.setOnClickListener(v -> {
            if (peopleCount > 1) {
                peopleCount--;
                tvPeopleCount.setText(String.valueOf(peopleCount));
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (peopleCount < 99) {
                peopleCount++;
                tvPeopleCount.setText(String.valueOf(peopleCount));
            }
        });
    }

    private void setupActivateButton() {
        btnActivateSOS = findViewById(R.id.btnActivateSOS);
        btnActivateSOS.setOnClickListener(v -> {
            vibratePhone();
            
            // Launch Active State
            Intent intent = new Intent(ActivitySOS.this, ActivitySOSActive.class);
            startActivity(intent);
            
            Toast.makeText(this, "SOS SIGNAL BROADCASTED!", Toast.LENGTH_LONG).show();
        });
    }
    
    private void startJumpAnimation() {
         ivWarningIcon.post(this::tryStartJumpAnimation);
    }

    private void tryStartJumpAnimation() {
        // Simple jump (translationY) with BounceInterpolator
        ObjectAnimator animator = ObjectAnimator.ofFloat(ivWarningIcon, "translationY", 0f, -50f, 0f);
        animator.setDuration(1000);
        animator.setInterpolator(new BounceInterpolator());
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.start();
    }

    private void vibratePhone() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            // Vibrate for 500 milliseconds
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // Deprecated in API 26 
                v.vibrate(500);
            }
        }
    }
}
