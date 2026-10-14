package com.example.undersoil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.undersoil.data.Sector;
import com.example.undersoil.data.SectorDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class IotWateringActivity extends AppCompatActivity {

    public static final String EXTRA_SECTOR_ID = "EXTRA_SECTOR_ID";
    public static final String EXTRA_SECTOR_NAME = "EXTRA_SECTOR_NAME";

    private RecyclerView rvSectors;
    private View layoutEmptyState;
    private Button btnEmptyAddSector;
    private Button createSectorButton;
    private ImageButton backButton;

    private SectorAdapter adapter;
    private SectorDatabaseHelper dbHelper;
    private List<Sector> sectorsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iot_watering);

        initViews();
        setupRecyclerView();
        setupClickListeners();

        dbHelper = SectorDatabaseHelper.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSectorsFromDatabase();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        rvSectors = findViewById(R.id.rvSectors);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnEmptyAddSector = findViewById(R.id.btnEmptyAddSector);
        createSectorButton = findViewById(R.id.createSectorButton);
    }

    private void setupRecyclerView() {
        adapter = new SectorAdapter(sectorsList, sector -> {
            Intent intent = new Intent(IotWateringActivity.this, IotScreenActivity.class);
            intent.putExtra(EXTRA_SECTOR_ID, sector.getId());
            intent.putExtra(EXTRA_SECTOR_NAME, sector.getName());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        rvSectors.setLayoutManager(new LinearLayoutManager(this));
        rvSectors.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        View.OnClickListener addSectorListener = v -> {
            Intent intent = new Intent(IotWateringActivity.this, AddSectorActivity.class);
            startActivity(intent);
        };

        createSectorButton.setOnClickListener(addSectorListener);
        btnEmptyAddSector.setOnClickListener(addSectorListener);
    }

    private void loadSectorsFromDatabase() {
        if (dbHelper != null) {
            String userId = com.example.undersoil.data.SessionManager.getInstance(this).getUserIdString();
            sectorsList = dbHelper.getAllSectors(userId);
            adapter.updateData(sectorsList);

            if (sectorsList.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                rvSectors.setVisibility(View.GONE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                rvSectors.setVisibility(View.VISIBLE);
            }
        }
    }
}
