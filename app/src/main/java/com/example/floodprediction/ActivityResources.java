package com.example.floodprediction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ActivityResources extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resources);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBottomNavigation();
        setupHeader();
    }

    private void setupHeader() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        // Setup Search/Settings mock
        findViewById(R.id.btnViewMapShelters).setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
        });
        
        setupInteractiveElements();
    }

    private void setupInteractiveElements() {
        // Emergency Calls
        findViewById(R.id.btnCallFire).setOnClickListener(v -> dialEmergency("999"));
        findViewById(R.id.btnCallPolice).setOnClickListener(v -> dialEmergency("999"));

        // Categories (Mock Toast)
        setCategoryListener(R.id.btnCatShelters, "Shelters filtered");
        setCategoryListener(R.id.btnCatMedical, "Medical Aid filtered");
        setCategoryListener(R.id.btnCatWater, "Clean Water filtered");
        setCategoryListener(R.id.btnCatRelief, "Relief Goods filtered");
    }

    private void dialEmergency(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void setCategoryListener(int id, String message) {
        findViewById(id).setOnClickListener(v -> 
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        );
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        
        // Set the correct item as selected
        bottomNavigationView.setSelectedItemId(R.id.nav_resources);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0); // No animation for tab switch
                finish(); // Close this to prevent stacking
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_resources) {
                return true;
            }
            return false;
        });
    }
}
