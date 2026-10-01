package com.example.undersoil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.undersoil.data.ScanLog;
import com.example.undersoil.data.ScanLogDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class LogListActivity extends AppCompatActivity {

    public static final String EXTRA_CROP_NAME = "EXTRA_CROP_NAME";
    public static final String EXTRA_PLANT_ID = "plant_id";

    private String cropName;
    private int plantId = 1;

    private TextView titleText;
    private RecyclerView rvScanLogs;
    private TextView tvEmptyLogs;
    private ImageButton diagnosisButton;
    private Button viewInfoButton;
    private ImageButton backButton;

    private ScanLogAdapter adapter;
    private ScanLogDatabaseHelper dbHelper;
    private List<ScanLog> scanLogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_list);

        extractIntentExtras();
        initViews();
        setupRecyclerView();
        setupClickListeners();

        dbHelper = ScanLogDatabaseHelper.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLogsFromDatabase();
    }

    private void extractIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            cropName = intent.getStringExtra(EXTRA_CROP_NAME);
            if (cropName == null) {
                cropName = intent.getStringExtra("crop_name");
            }
            if (cropName == null) {
                cropName = intent.getStringExtra("cropName");
            }
            plantId = intent.getIntExtra(EXTRA_PLANT_ID, 1);
        }

        if (cropName == null || cropName.trim().isEmpty()) {
            cropName = "Tomato";
        }
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        rvScanLogs = findViewById(R.id.rvScanLogs);
        tvEmptyLogs = findViewById(R.id.tvEmptyLogs);
        diagnosisButton = findViewById(R.id.diagnosisButton);
        viewInfoButton = findViewById(R.id.viewInfoButton);

        titleText.setText(String.format("%s Scan Logs", cropName));
    }

    private void setupRecyclerView() {
        adapter = new ScanLogAdapter(scanLogs);
        rvScanLogs.setLayoutManager(new LinearLayoutManager(this));
        rvScanLogs.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        // "Scan Plant" button opens Camera_detector passing EXTRA_CROP_NAME
        diagnosisButton.setOnClickListener(v -> {
            Intent scanIntent = new Intent(LogListActivity.this, Camera_detector.class);
            scanIntent.putExtra(Camera_detector.EXTRA_CROP_NAME, cropName);
            startActivity(scanIntent);
        });

        // "View Plant Information" button opens PlantDetailsActivity
        viewInfoButton.setOnClickListener(v -> {
            Intent infoIntent = new Intent(LogListActivity.this, PlantDetailsActivity.class);
            infoIntent.putExtra("plant_id", plantId);
            infoIntent.putExtra(Camera_detector.EXTRA_CROP_NAME, cropName);
            startActivity(infoIntent);
        });
    }

    private void loadLogsFromDatabase() {
        if (dbHelper != null) {
            long userId = com.example.undersoil.data.SessionManager.getInstance(this).getUserId();
            scanLogs = dbHelper.getLogsByCropName(cropName, userId);
            adapter.updateData(scanLogs);

            if (scanLogs.isEmpty()) {
                tvEmptyLogs.setVisibility(View.VISIBLE);
                rvScanLogs.setVisibility(View.GONE);
            } else {
                tvEmptyLogs.setVisibility(View.GONE);
                rvScanLogs.setVisibility(View.VISIBLE);
            }
        }
    }
}
