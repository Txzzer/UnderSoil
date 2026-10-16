package com.example.undersoil.iot;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.undersoil.R;
import com.example.undersoil.garden.GardenActivity;
import com.example.undersoil.home.HomeScreenActivity;
import com.example.undersoil.network.ApiClient;
import com.example.undersoil.network.ApiService;
import com.example.undersoil.network.SensorData;
import com.example.undersoil.network.ValveHistoryEntry;
import com.example.undersoil.widget.CircularGaugeView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IotScreenActivity extends AppCompatActivity implements IotAlertManager.Listener {

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
    private View viewValveGlow;
    private View viewValveCore;
    private AnimatorSet valvePulseAnimator;

    private MaterialCardView cardWarningBanner;
    private TextView tvWarningBannerText;
    private Button btnWarningAction;

    private static final int VALVE_HISTORY_LIMIT = 5;

    private MaterialCardView cardValveHistory;
    private android.widget.LinearLayout llValveHistory;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabHome;

    private boolean isValveOpen = false;

    private String sectorId;
    private String sectorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.iot_screen);

        extractIntentExtras();
        initViews();
        setupGauges();
        setupValveToggle();
        setupBottomNavigation();
        loadValveHistory();
    }

    private void extractIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            sectorId = intent.getStringExtra(IotWateringActivity.EXTRA_SECTOR_ID);
            sectorName = intent.getStringExtra(IotWateringActivity.EXTRA_SECTOR_NAME);
        }
    }

    private void initViews() {
        android.widget.ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

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
        viewValveGlow = findViewById(R.id.viewValveGlow);
        viewValveCore = findViewById(R.id.viewValveCore);

        cardWarningBanner = findViewById(R.id.cardWarningBanner);
        tvWarningBannerText = findViewById(R.id.tvWarningBannerText);
        btnWarningAction = findViewById(R.id.btnWarningAction);

        cardValveHistory = findViewById(R.id.cardValveHistory);
        llValveHistory = findViewById(R.id.llValveHistory);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabHome = findViewById(R.id.fabHome);

        if (tvDeviceTitle != null && sectorName != null && !sectorName.trim().isEmpty()) {
            tvDeviceTitle.setText(String.format("%s Telemetry", sectorName));
        }

        btnWarningAction.setOnClickListener(v -> performValveToggle(true));

        Button btnScheduleWatering = findViewById(R.id.btnScheduleWatering);
        btnScheduleWatering.setOnClickListener(v -> {
            if (sectorId == null || sectorId.trim().isEmpty()) {
                // e.g. opened from the IoT alert notification rather than the sector list
                Toast.makeText(this, "Open this screen from a sector to schedule watering", Toast.LENGTH_SHORT).show();
                return;
            }
            ScheduleWateringDialog.show(this, sectorId);
        });
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_iot);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_iot) {
                    startActivity(new Intent(IotScreenActivity.this, IotWateringActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                } else if (itemId == R.id.nav_garden) {
                    startActivity(new Intent(IotScreenActivity.this, GardenActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    return true;
                }
                return false;
            });
        }

        if (fabHome != null) {
            fabHome.setOnClickListener(v -> {
                startActivity(new Intent(IotScreenActivity.this, HomeScreenActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
        setValveButtonsEnabled(false);
        IotAlertManager.get().toggleValve(targetState);
    }

    private void setValveButtonsEnabled(boolean enabled) {
        btnToggleValve.setEnabled(enabled);
        btnWarningAction.setEnabled(enabled);
    }

    @Override
    public void onTelemetry(SensorData data) {
        updateTelemetryUi(data);
    }

    @Override
    public void onPollError(String message) {
        tvStreamStatus.setText("● " + message);
        tvStreamStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error));
        // Device unreachable: don't show possibly stale valve history
        cardValveHistory.setVisibility(View.GONE);
    }

    @Override
    public void onValveToggleResult(boolean success, boolean targetState, String message) {
        setValveButtonsEnabled(true);

        if (success) {
            isValveOpen = targetState;
            updateValveUi(isValveOpen);
        }
        Toast.makeText(IotScreenActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateTelemetryUi(SensorData data) {
        tvStreamStatus.setText("● Live (3s)");
        tvStreamStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected));

        gaugeTemperature.setValue((float) data.getTemperatureCelsius(), true);
        gaugeHumidity.setValue((float) data.getHumidityPercentage(), true);

        tvFlowRate.setText(String.format(Locale.US, "%.1f L/min", data.getFlowRateLitersPerMin()));

        updateFlowWarningUi(data);

        if (data.getDeviceId() != null) {
            tvDeviceId.setText(String.format("Device ID: %s", data.getDeviceId()));
        }
        tvLastUpdated.setText(String.format("Last update: %s", formatTimestamp(data.getTimestamp())));

        if (!IotAlertManager.get().isTogglingValve() && data.isValveIsOpen() != isValveOpen) {
            isValveOpen = data.isValveIsOpen();
            updateValveUi(isValveOpen);
        }
    }

    private void updateFlowWarningUi(SensorData data) {
        if (IotAlertManager.isUnintendedFlow(data)) {
            cardWarningBanner.setVisibility(View.VISIBLE);
            tvWarningBannerText.setText(String.format(Locale.US,
                    "Flow rate of %.1f L/min detected while valve is CLOSED!",
                    data.getFlowRateLitersPerMin()));

            tvFlowStatus.setText("⚠️ LEAK / UNINTENDED FLOW");
            tvFlowStatus.setTextColor(ContextCompat.getColor(this, R.color.us_danger));
        } else {
            cardWarningBanner.setVisibility(View.GONE);

            if (IotAlertManager.hasFlow(data)) {
                tvFlowStatus.setText("Active Flow");
                tvFlowStatus.setTextColor(ContextCompat.getColor(this, R.color.flow_rate_blue));
            } else {
                tvFlowStatus.setText("No Flow");
                tvFlowStatus.setTextColor(ContextCompat.getColor(this, R.color.us_text_secondary));
            }
        }
    }

    private void updateValveUi(boolean isOpen) {
        int stateColor = ContextCompat.getColor(this, isOpen ? R.color.valve_open : R.color.valve_closed);
        if (isOpen) {
            tvValveStatus.setText("Valve: OPEN");
            btnToggleValve.setText("Turn Valve OFF");
            startValvePulse();
        } else {
            tvValveStatus.setText("Valve: CLOSED");
            btnToggleValve.setText("Turn Valve ON");
            stopValvePulse();
        }
        tvValveStatus.setTextColor(stateColor);
        viewValveCore.setBackgroundTintList(ColorStateList.valueOf(stateColor));
    }

    private void startValvePulse() {
        if (valvePulseAnimator != null && valvePulseAnimator.isStarted()) return;

        viewValveGlow.setVisibility(View.VISIBLE);
        long duration = getResources().getInteger(R.integer.anim_duration_pulse);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(viewValveGlow, View.SCALE_X, 0.85f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(viewValveGlow, View.SCALE_Y, 0.85f, 1.0f);
        ObjectAnimator glowAlpha = ObjectAnimator.ofFloat(viewValveGlow, View.ALPHA, 1f, 0.6f);
        ObjectAnimator coreAlpha = ObjectAnimator.ofFloat(viewValveCore, View.ALPHA, 1f, 0.9f);
        for (ObjectAnimator animator : new ObjectAnimator[]{scaleX, scaleY, glowAlpha, coreAlpha}) {
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
        }

        valvePulseAnimator = new AnimatorSet();
        valvePulseAnimator.playTogether(scaleX, scaleY, glowAlpha, coreAlpha);
        valvePulseAnimator.setDuration(duration);
        valvePulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valvePulseAnimator.start();
    }

    private void stopValvePulse() {
        if (valvePulseAnimator != null) {
            valvePulseAnimator.cancel();
            valvePulseAnimator = null;
        }
        viewValveGlow.setVisibility(View.INVISIBLE);
        viewValveGlow.setScaleX(1f);
        viewValveGlow.setScaleY(1f);
        viewValveGlow.setAlpha(1f);
        viewValveCore.setAlpha(1f);
    }

    /** Fetches valve history once per screen open; the card stays hidden if offline or the call fails. */
    private void loadValveHistory() {
        cardValveHistory.setVisibility(View.GONE);
        if (!isNetworkAvailable()) return;

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getValveHistory().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ValveHistoryEntry>> call, Response<List<ValveHistoryEntry>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    showValveHistory(response.body());
                } else {
                    cardValveHistory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<ValveHistoryEntry>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                cardValveHistory.setVisibility(View.GONE);
            }
        });
    }

    private void showValveHistory(List<ValveHistoryEntry> entries) {
        // Newest first; ISO-8601 timestamps sort chronologically as text, missing ones go last
        List<ValveHistoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> {
            String ta = a.getTimestamp(), tb = b.getTimestamp();
            if (ta == null) return (tb == null) ? 0 : 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        llValveHistory.removeAllViews();
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        for (int i = 0; i < Math.min(VALVE_HISTORY_LIMIT, sorted.size()); i++) {
            ValveHistoryEntry entry = sorted.get(i);
            View row = inflater.inflate(R.layout.item_valve_action, llValveHistory, false);

            TextView tvAction = row.findViewById(R.id.tvValveAction);
            tvAction.setText(entry.isValveIsOpen() ? "Opened" : "Closed");
            tvAction.setTextColor(ContextCompat.getColor(this,
                    entry.isValveIsOpen() ? R.color.valve_open : R.color.valve_closed));
            ((TextView) row.findViewById(R.id.tvValveActionTime)).setText(formatHistoryTime(entry.getTimestamp()));

            llValveHistory.addView(row);
        }
        cardValveHistory.setVisibility(View.VISIBLE);
    }

    /** "2026-10-15T14:32:10Z" -> "15 Oct, 14:32" in the device time zone; raw string if unparseable. */
    private String formatHistoryTime(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isEmpty()) return "";
        DateTimeFormatter shown = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault());
        try {
            return shown.format(Instant.parse(rawTimestamp).atZone(ZoneId.systemDefault()));
        } catch (Exception ignored) {
        }
        try {
            // No offset in the string (e.g. .NET DateTime without Kind): show it as-is
            return shown.format(LocalDateTime.parse(rawTimestamp));
        } catch (Exception ignored) {
        }
        return rawTimestamp;
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
        if (cm == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
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
        IotAlertManager manager = IotAlertManager.get();
        manager.addListener(this);

        setValveButtonsEnabled(!manager.isTogglingValve());

        SensorData lastData = manager.getLastData();
        if (lastData != null) {
            updateTelemetryUi(lastData);
        }
        if (isValveOpen) {
            startValvePulse();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        IotAlertManager.get().removeListener(this);
        stopValvePulse();
    }

    @Override
    protected void onDestroy() {
        stopValvePulse();
        super.onDestroy();
    }
}
