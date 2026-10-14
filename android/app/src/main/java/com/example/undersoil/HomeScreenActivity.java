package com.example.undersoil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeScreenActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        checkAndRequestNotificationPermission();
        setupClickListeners();
        setupBottomNavigation();

        // Schedule background sync when network connection is active
        com.example.undersoil.data.SyncManager.scheduleSync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.example.undersoil.data.SessionManager sessionManager = com.example.undersoil.data.SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, Log_in.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    private void setupClickListeners() {
        android.widget.ImageView ivProfile = findViewById(R.id.iv_profile);
        MaterialCardView cardDiagnose = findViewById(R.id.card_diagnose);
        MaterialCardView cardIotWatering = findViewById(R.id.card_iot_watering);

        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, Settings.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        if (cardDiagnose != null) {
            cardDiagnose.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        if (cardIotWatering != null) {
            cardIotWatering.setOnClickListener(v -> {
                startActivity(new Intent(this, IotWateringActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabHome = findViewById(R.id.fabHome);

        if (bottomNavigationView != null) {
            // Uncheck all bottom navigation items on Home screen
            bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                bottomNavigationView.getMenu().getItem(i).setChecked(false);
            }

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_iot) {
                    startActivity(new Intent(this, IotWateringActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_garden) {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
