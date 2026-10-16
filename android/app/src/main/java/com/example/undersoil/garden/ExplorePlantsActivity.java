package com.example.undersoil.garden;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.undersoil.R;
import com.example.undersoil.data.model.Plant;
import com.example.undersoil.data.model.PlantVillageData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExplorePlantsActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView rvExplorePlants;
    private PlantAdapter adapter;
    private List<Plant> fullCatalog;
    private List<Plant> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_plants);

        ImageButton backButton = findViewById(R.id.backButton);
        searchInput = findViewById(R.id.searchInput);
        rvExplorePlants = findViewById(R.id.rvExplorePlants);

        backButton.setOnClickListener(v -> finish());

        fullCatalog = PlantVillageData.INSTANCE.getCatalog();
        filteredList = new ArrayList<>(fullCatalog);

        adapter = new PlantAdapter(filteredList, plant -> {
            Intent intent = new Intent(ExplorePlantsActivity.this, PlantDetailsActivity.class);
            intent.putExtra("plant_id", plant.getId());
            startActivity(intent);
        });

        rvExplorePlants.setLayoutManager(new LinearLayoutManager(this));
        rvExplorePlants.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPlants(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterPlants(String query) {
        filteredList.clear();
        if (query.trim().isEmpty()) {
            filteredList.addAll(fullCatalog);
        } else {
            String lowerQuery = query.toLowerCase(Locale.US);
            for (Plant plant : fullCatalog) {
                if (plant.getName().toLowerCase(Locale.US).contains(lowerQuery) ||
                    plant.getScientificName().toLowerCase(Locale.US).contains(lowerQuery)) {
                    filteredList.add(plant);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    public interface OnPlantClickListener {
        void onPlantClick(Plant plant);
    }

    private static class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.ViewHolder> {
        private final List<Plant> plants;
        private final OnPlantClickListener listener;

        PlantAdapter(List<Plant> plants, OnPlantClickListener listener) {
            this.plants = plants;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Plant plant = plants.get(position);
            holder.tvPlantName.setText(plant.getName());
            holder.tvScientificName.setText(plant.getScientificName());
            holder.ivPlantPhoto.setImageResource(plant.getImageResId());

            holder.itemView.setOnClickListener(v -> listener.onPlantClick(plant));
        }

        @Override
        public int getItemCount() {
            return plants.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPlantName;
            TextView tvScientificName;
            ImageView ivPlantPhoto;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPlantName = itemView.findViewById(R.id.tvPlantName);
                tvScientificName = itemView.findViewById(R.id.tvScientificName);
                ivPlantPhoto = itemView.findViewById(R.id.ivPlantPhoto);
            }
        }
    }
}
