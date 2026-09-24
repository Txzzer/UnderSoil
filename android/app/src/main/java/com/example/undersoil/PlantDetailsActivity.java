package com.example.undersoil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.data.GardenDatabase;
import com.example.undersoil.data.GardenEntity;
import com.example.undersoil.data.Plant;
import com.example.undersoil.data.PlantVillageData;

import java.util.Locale;

public class PlantDetailsActivity extends AppCompatActivity {

    private Plant plant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_details);

        ImageButton backButton = findViewById(R.id.backButton);
        ImageView heroImage = findViewById(R.id.heroImage);
        TextView tvPlantNameScientific = findViewById(R.id.tvPlantNameScientific);
        TextView tvPlantDescription = findViewById(R.id.tvPlantDescription);
        TextView tvWaterValue = findViewById(R.id.tvWaterValue);
        TextView tvSunlightValue = findViewById(R.id.tvSunlightValue);
        TextView tvFertilizerValue = findViewById(R.id.tvFertilizerValue);
        TextView tvHumidityValue = findViewById(R.id.tvHumidityValue);
        Button btnScanCrop = findViewById(R.id.btnScanCrop);
        Button addToPlantsButton = findViewById(R.id.addToPlantsButton);

        backButton.setOnClickListener(v -> finish());

        int plantId = getIntent().getIntExtra("plant_id", 1);
        plant = getPlantById(plantId);

        if (plant != null) {
            tvPlantNameScientific.setText(String.format(Locale.US, "%s (%s)", plant.getScientificName(), plant.getName()));
            tvPlantDescription.setText(plant.getDescription());
            tvWaterValue.setText(plant.getWaterAmount());
            tvSunlightValue.setText(plant.getSunlight());
            tvFertilizerValue.setText(plant.getFertilizer());
            tvHumidityValue.setText(plant.getHumidity());
            heroImage.setImageResource(plant.getImageResId());
        }

        if (btnScanCrop != null) {
            btnScanCrop.setOnClickListener(v -> {
                if (plant != null) {
                    Intent scanIntent = new Intent(this, Camera_detector.class);
                    scanIntent.putExtra(Camera_detector.EXTRA_CROP_NAME, plant.getName());
                    startActivity(scanIntent);
                }
            });
        }

        addToPlantsButton.setOnClickListener(v -> {
            if (plant != null) {
                GardenDatabase.Companion.getInstance(this).gardenDao().insert(new GardenEntity(plant.getId()));
                Toast.makeText(this, "Added " + plant.getName() + " to My Garden!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private Plant getPlantById(int id) {
        for (Plant item : PlantVillageData.INSTANCE.getCatalog()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return PlantVillageData.INSTANCE.getCatalog().get(0);
    }
}
