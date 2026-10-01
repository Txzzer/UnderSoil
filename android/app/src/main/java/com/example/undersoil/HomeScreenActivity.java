package com.example.undersoil;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        setupClickListeners();
        setupBottomNavigation();
    }

    private void setupClickListeners() {
        MaterialCardView cardDiagnose = findViewById(R.id.card_diagnose);
        MaterialCardView cardIotWatering = findViewById(R.id.card_iot_watering);

        if (cardDiagnose != null) {
            cardDiagnose.setOnClickListener(v -> startActivity(new Intent(this, Camera_detector.class)));
        }

        if (cardIotWatering != null) {
            cardIotWatering.setOnClickListener(v -> startActivity(new Intent(this, IotWateringActivity.class)));
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabHome = findViewById(R.id.fabHome);

        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_iot) {
                    startActivity(new Intent(this, IotWateringActivity.class));
                    return true;
                } else if (itemId == R.id.nav_garden) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                }
                return false;
            });
        }

        if (fabHome != null) {
            fabHome.setOnClickListener(v -> {
                // Already on Home screen
            });
        }
    }
}
