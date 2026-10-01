package com.example.undersoil;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

import network.ApiClient;
import network.ApiService;
import network.SensorData;
import network.ValveRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IotScreenActivity extends AppCompatActivity {

    private static final long POLLING_INTERVAL_MS = 3000;

    private CircularGaugeView gaugeTemperature;
    private CircularGaugeView gaugeHumidity;
    private TextView tvDeviceTitle;
    private TextView tvFlowRate;
    private TextView tvFlowStatus;
    private TextView tvDeviceId;
    private TextView tvLastUpdated;
    private TextView tvStreamStatus;
    private TextView tvValveStatus;
    private Button btnToggleValve;

    private MaterialCardView cardWarningBanner;
    private TextView tvWarningBannerText;
    private Button btnWarningAction;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabHome;

    private boolean isValveOpen = false;
    private boolean isTogglingValve = false;

    private AlertDialog flowAlertDialog;
    private long alertSnoozedUntilMs = 0;

    private String sectorId;
    private String sectorName;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            fetchSensorData();
            pollHandler.postDelayed(this, POLLING_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.iot_screen);

        extractIntentExtras();
        initViews();
        setupGauges();
        setupValveToggle();
        setupBottomNavigation();
    }

    private void extractIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            sectorId = intent.getStringExtra(IotWateringActivity.EXTRA_SECTOR_ID);
            sectorName = intent.getStringExtra(IotWateringActivity.EXTRA_SECTOR_NAME);
        }
    }

    private void initViews() {
        tvDeviceTitle = findViewById(R.id.tvDeviceTitle);
        gaugeTemperature = findViewById(R.id.gaugeTemperature);
        gaugeHumidity = findViewById(R.id.gaugeHumidity);
        tvFlowRate = findViewById(R.id.tvFlowRate);
        tvFlowStatus = findViewById(R.id.tvFlowStatus);
        tvDeviceId = findViewById(R.id.tvDeviceId);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        tvStreamStatus = findViewById(R.id.tvStreamStatus);
        tvValveStatus = findViewById(R.id.tvValveStatus);
        btnToggleValve = findViewById(R.id.btnToggleValve);

        cardWarningBanner = findViewById(R.id.cardWarningBanner);
        tvWarningBannerText = findViewById(R.id.tvWarningBannerText);
        btnWarningAction = findViewById(R.id.btnWarningAction);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabHome = findViewById(R.id.fabHome);

        if (tvDeviceTitle != null && sectorName != null && !sectorName.trim().isEmpty()) {
            tvDeviceTitle.setText(String.format("%s Telemetry", sectorName));
        }

        btnWarningAction.setOnClickListener(v -> performValveToggle(true));
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_iot);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_iot) {
                    startActivity(new Intent(IotScreenActivity.this, IotWateringActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (itemId == R.id.nav_garden) {
                    startActivity(new Intent(IotScreenActivity.this, MainActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }

        if (fabHome != null) {
            fabHome.setOnClickListener(v -> {
                startActivity(new Intent(IotScreenActivity.this, HomeScreenActivity.class));
                overridePendingTransition(0, 0);
            });
        }
    }

    private void setupGauges() {
        gaugeTemperature.setRange(0f, 50f);
        gaugeTemperature.setTitle("Temperature");
        gaugeTemperature.setUnit("°C");
        gaugeTemperature.setThresholds(28f, 38f);

        gaugeHumidity.setRange(0f, 100f);
        gaugeHumidity.setTitle("Humidity");
        gaugeHumidity.setUnit("%");
        gaugeHumidity.setThresholds(60f, 80f);
    }

    private void setupValveToggle() {
        updateValveUi(isValveOpen);
        btnToggleValve.setOnClickListener(view -> performValveToggle(!isValveOpen));
    }

    private void performValveToggle(boolean targetState) {
        if (isTogglingValve) return;

        btnToggleValve.setEnabled(false);
        btnWarningAction.setEnabled(false);
        isTogglingValve = true;

        ValveRequest request = new ValveRequest(targetState);
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.toggleValve(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnToggleValve.setEnabled(true);
                btnWarningAction.setEnabled(true);
                isTogglingValve = false;

                if (response.isSuccessful()) {
                    isValveOpen = targetState;
                    updateValveUi(isValveOpen);
                    String state = isValveOpen ? "OPEN" : "CLOSED";
                    Toast.makeText(IotScreenActivity.this, "Valve toggled to " + state, Toast.LENGTH_SHORT).show();

                    if (flowAlertDialog != null && flowAlertDialog.isShowing()) {
                        flowAlertDialog.dismiss();
                    }
                } else {
                    Toast.makeText(IotScreenActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnToggleValve.setEnabled(true);
                btnWarningAction.setEnabled(true);
                isTogglingValve = false;
                Toast.makeText(IotScreenActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSensorData() {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getLatestSensorData().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<SensorData> call, Response<SensorData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SensorData data = response.body();
                    updateTelemetryUi(data);
                } else {
                    tvStreamStatus.setText(String.format(Locale.US, "● Error (%d)", response.code()));
                    tvStreamStatus.setTextColor(Color.parseColor("#F44336"));
                }
            }

            @Override
            public void onFailure(Call<SensorData> call, Throwable t) {
                tvStreamStatus.setText("● Offline");
                tvStreamStatus.setTextColor(Color.parseColor("#F44336"));
            }
        });
    }

    private void updateTelemetryUi(SensorData data) {
        tvStreamStatus.setText("● Live (3s)");
        tvStreamStatus.setTextColor(Color.parseColor("#2E7D32"));

        gaugeTemperature.setValue((float) data.getTemperatureCelsius(), true);
        gaugeHumidity.setValue((float) data.getHumidityPercentage(), true);

        tvFlowRate.setText(String.format(Locale.US, "%.1f L/min", data.getFlowRateLitersPerMin()));

        checkUnintendedFlowAlert(data);

        if (data.getDeviceId() != null) {
            tvDeviceId.setText(String.format("Device ID: %s", data.getDeviceId()));
        }
        tvLastUpdated.setText(String.format("Last update: %s", formatTimestamp(data.getTimestamp())));

        if (!isTogglingValve && data.isValveIsOpen() != isValveOpen) {
            isValveOpen = data.isValveIsOpen();
            updateValveUi(isValveOpen);
        }
    }

    private void checkUnintendedFlowAlert(SensorData data) {
        boolean isUnintendedFlow = (data.getFlowRateLitersPerMin() > 0.05) && (!data.isValveIsOpen());

        if (isUnintendedFlow) {
            cardWarningBanner.setVisibility(View.VISIBLE);
            tvWarningBannerText.setText(String.format(Locale.US,
                    "Flow rate of %.1f L/min detected while valve is CLOSED!",
                    data.getFlowRateLitersPerMin()));

            tvFlowStatus.setText("⚠️ LEAK / UNINTENDED FLOW");
            tvFlowStatus.setTextColor(Color.parseColor("#D32F2F"));

            long currentTime = System.currentTimeMillis();
            if ((flowAlertDialog == null || !flowAlertDialog.isShowing()) && currentTime > alertSnoozedUntilMs) {
                triggerFlowAlertDialog(data.getFlowRateLitersPerMin());
            }
        } else {
            cardWarningBanner.setVisibility(View.GONE);

            if (data.getFlowRateLitersPerMin() > 0.05) {
                tvFlowStatus.setText("Active Flow");
                tvFlowStatus.setTextColor(Color.parseColor("#0288D1"));
            } else {
                tvFlowStatus.setText("No Flow");
                tvFlowStatus.setTextColor(Color.parseColor("#757575"));
            }

            if (flowAlertDialog != null && flowAlertDialog.isShowing()) {
                flowAlertDialog.dismiss();
            }
            alertSnoozedUntilMs = 0;
        }
    }

    private void triggerFlowAlertDialog(double flowRate) {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("🚨 Warning: Unintended Flow Detected!");
        builder.setMessage(String.format(Locale.US,
                "The sensor is detecting active fluid flow of %.1f L/min, but the valve is reported CLOSED.\n\nThis may indicate a pipe leak, stuck valve actuator, or sensor mismatch.",
                flowRate));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(false);

        builder.setPositiveButton("Open Valve Now", (dialog, which) -> performValveToggle(true));

        builder.setNegativeButton("Acknowledge (Snooze 30s)", (dialog, which) -> {
            alertSnoozedUntilMs = System.currentTimeMillis() + 30_000;
            dialog.dismiss();
        });

        flowAlertDialog = builder.create();
        flowAlertDialog.show();
    }

    private void updateValveUi(boolean isOpen) {
        if (isOpen) {
            tvValveStatus.setText("Valve: OPEN");
            tvValveStatus.setTextColor(Color.parseColor("#2E7D32"));
            btnToggleValve.setText("Turn Valve OFF");
        } else {
            tvValveStatus.setText("Valve: CLOSED");
            tvValveStatus.setTextColor(Color.parseColor("#C62828"));
            btnToggleValve.setText("Turn Valve ON");
        }
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isEmpty()) {
            return "Just now";
        }
        try {
            if (rawTimestamp.contains("T")) {
                String[] parts = rawTimestamp.split("T");
                String timePart = parts[1];
                if (timePart.contains(".")) {
                    timePart = timePart.split("\\.")[0];
                }
                return timePart.replace("Z", "");
            }
        } catch (Exception ignored) {
        }
        return rawTimestamp;
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }
}
