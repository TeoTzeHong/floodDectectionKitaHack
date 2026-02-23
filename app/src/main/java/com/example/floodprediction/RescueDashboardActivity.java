package com.example.floodprediction;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RescueDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RescueAdapter adapter;
    private List<DocumentSnapshot> missions = new ArrayList<>();
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_dashboard);

        recyclerView = findViewById(R.id.recyclerRescue);
        progressBar = findViewById(R.id.progressRescue);
        btnRefresh = findViewById(R.id.btnRefreshRescue);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RescueAdapter(this, missions);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadMissionsRealtime();

        btnRefresh.setOnClickListener(v -> loadMissionsRealtime());
    }

    private void loadMissionsRealtime() {
        progressBar.setVisibility(View.VISIBLE);

        // Listen for PENDING, ASSIGNED and ON_THE_WAY requests
        db.collection("sos_requests")
                .whereIn("status", java.util.Arrays.asList("PENDING", "ASSIGNED", "ON_THE_WAY"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        missions.clear();
                        missions.addAll(snapshots.getDocuments());
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
