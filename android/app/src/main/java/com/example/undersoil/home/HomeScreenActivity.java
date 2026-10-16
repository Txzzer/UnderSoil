package com.example.undersoil.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.undersoil.R;
import com.example.undersoil.auth.LoginActivity;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.IotAlertDatabaseHelper;
import com.example.undersoil.data.model.IotAlert;
import com.example.undersoil.data.sync.SyncManager;
import com.example.undersoil.garden.GardenActivity;
import com.example.undersoil.iot.IotAlertManager;
import com.example.undersoil.iot.IotWateringActivity;
import com.example.undersoil.settings.SettingsActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeScreenActivity extends AppCompatActivity implements IotAlertManager.AlertSavedListener {

    private static final int NOTIFICATION_PERMISSION_CODE = 1002;
    private static final int ALERTS_LOG_LIMIT = 5;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private LinearLayout llAlertsLog;
    private TextView tvNoAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        checkAndRequestNotificationPermission();
        setupClickListeners();
        setupBottomNavigation();
        setupAlertsLog();

        SyncManager.scheduleSync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        IotAlertManager.get().addAlertSavedListener(this);
        loadAlertsLog();
    }

    @Override
    protected void onPause() {
        IotAlertManager.get().removeAlertSavedListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        IotAlertManager.get().removeAlertSavedListener(this);
        dbExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onAlertSaved() {
        loadAlertsLog();
    }

    private void setupAlertsLog() {
        llAlertsLog = findViewById(R.id.ll_alerts_log);
        tvNoAlerts = findViewById(R.id.tv_no_alerts);

        // DEBUG-TRIGGER: long-press the "Alerts Log" header to flip fake flow HIGH/NORMAL (debug builds only)
        findViewById(R.id.tv_alerts_log_header).setOnLongClickListener(v -> {
            boolean high = IotAlertManager.get().toggleDebugFakeFlow();
            Toast.makeText(this, "Debug fake flow: " + (high ? "HIGH" : "NORMAL"), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void loadAlertsLog() {
        String userId = SessionManager.getInstance(this).getUserIdString();
        dbExecutor.execute(() -> {
            List<IotAlert> alerts = IotAlertDatabaseHelper.getInstance(getApplicationContext())
                    .getRecentAlerts(userId, ALERTS_LOG_LIMIT);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) showAlertsLog(alerts);
            });
        });
    }

    private void showAlertsLog(List<IotAlert> alerts) {
        llAlertsLog.removeAllViews();
        tvNoAlerts.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (IotAlert alert : alerts) {
            View row = inflater.inflate(R.layout.item_iot_alert, llAlertsLog, false);
            ((TextView) row.findViewById(R.id.tv_alert_time)).setText(formatAlertTime(alert.getDateTime()));
            llAlertsLog.addView(row);
        }
    }

    /** "2026-10-14 17:24" (as saved by IotAlertManager) -> "14 Oct 2026, 17:24"; raw string if unparseable. */
    private String formatAlertTime(String dateTime) {
        if (dateTime == null) return "";
        try {
            SimpleDateFormat stored = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat shown = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault());
            return shown.format(stored.parse(dateTime));
        } catch (ParseException e) {
            return dateTime;
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
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        if (cardDiagnose != null) {
            cardDiagnose.setOnClickListener(v -> {
                startActivity(new Intent(this, GardenActivity.class));
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
                    startActivity(new Intent(this, GardenActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    return true;
                }
                return false;
            });
        }

        if (fabHome != null) {
            fabHome.setOnClickListener(v -> {
                // Already on Home, so the button does nothing here
            });
        }
    }
}
