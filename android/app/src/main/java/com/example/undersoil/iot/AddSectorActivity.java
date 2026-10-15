package com.example.undersoil.iot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.GardenDatabase;
import com.example.undersoil.data.db.GardenEntity;
import com.example.undersoil.data.db.SectorDatabaseHelper;
import com.example.undersoil.data.model.Plant;
import com.example.undersoil.data.model.PlantVillageData;
import com.example.undersoil.data.model.Sector;

import java.util.ArrayList;
import java.util.List;

public class AddSectorActivity extends AppCompatActivity {

    private EditText etSectorName;
    private RecyclerView rvSelectCrops;
    private Button btnAddSector;
    private ImageButton backButton;

    private CropSelectionAdapter cropAdapter;
    private List<String> availableCrops = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sector);

        initViews();
        loadAvailableCrops();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        etSectorName = findViewById(R.id.sectorNameInput);
        rvSelectCrops = findViewById(R.id.rvSelectCrops);
        btnAddSector = findViewById(R.id.addSectorButton);
    }

    private void loadAvailableCrops() {
        String userId = SessionManager.getInstance(this).getUserIdString();
        List<GardenEntity> gardenEntities = GardenDatabase.Companion.getInstance(this).gardenDao().getAllSync(userId);

        availableCrops.clear();
        if (gardenEntities != null && !gardenEntities.isEmpty()) {
            for (GardenEntity entity : gardenEntities) {
                Plant plant = getPlantById(entity.getPlantId());
                if (plant != null && !availableCrops.contains(plant.getName())) {
                    availableCrops.add(plant.getName());
                }
            }
        }

        // Fallback to default catalog crops if user has no garden entries yet
        if (availableCrops.isEmpty()) {
            for (Plant plant : PlantVillageData.INSTANCE.getCatalog()) {
                if (!availableCrops.contains(plant.getName())) {
                    availableCrops.add(plant.getName());
                }
            }
        }
    }

    private Plant getPlantById(int id) {
        for (Plant item : PlantVillageData.INSTANCE.getCatalog()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    private void setupRecyclerView() {
        cropAdapter = new CropSelectionAdapter(availableCrops);
        rvSelectCrops.setLayoutManager(new LinearLayoutManager(this));
        rvSelectCrops.setAdapter(cropAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        btnAddSector.setOnClickListener(v -> saveNewSector());
    }

    private void saveNewSector() {
        String name = etSectorName.getText().toString().trim();
        if (name.isEmpty()) {
            etSectorName.setError("Sector name is required");
            return;
        }

        List<String> selectedCrops = cropAdapter.getSelectedCrops();
        String cropNamesStr;
        if (selectedCrops.isEmpty()) {
            cropNamesStr = "None";
        } else {
            cropNamesStr = String.join(", ", selectedCrops);
        }

        String userId = SessionManager.getInstance(this).getUserIdString();
        Sector newSector = new Sector(name, cropNamesStr, userId);
        String result = SectorDatabaseHelper.getInstance(this).insertSector(newSector, userId);

        if (result != null) {
            Toast.makeText(this, "Sector '" + name + "' created!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save sector.", Toast.LENGTH_SHORT).show();
        }
    }
}
