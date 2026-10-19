package com.example.undersoil.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

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
import com.example.undersoil.weather.LocationHelper;
import com.example.undersoil.weather.WeatherCache;
import com.example.undersoil.weather.WeatherCodes;
import com.example.undersoil.weather.WeatherResponse;
import com.example.undersoil.weather.WeatherService;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeScreenActivity extends AppCompatActivity implements IotAlertManager.AlertSavedListener {

    private static final int STARTUP_PERMISSIONS_CODE = 1002;
    private static final int LOCATION_PERMISSION_CODE = 1003;
    private static final int ALERTS_LOG_LIMIT = 5;
    private static final long WEATHER_MAX_AGE_MS = 15 * 60 * 1000;
    private static final String KEY_LOCATION_ASKED = "location_permission_asked";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private LinearLayout llAlertsLog;
    private TextView tvNoAlerts;

    private TextView tvLocation;
    private TextView tvTemperature;
    private TextView tvWeatherSummary;
    private ImageView ivWeatherIcon;
    private WeatherCache weatherCache;
    private boolean isRefreshingWeather = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        setupWeatherViews();
        requestStartupPermissions();
        setupClickListeners();
        setupBottomNavigation();
        setupAlertsLog();

        SyncManager.scheduleSync(this);

        // A sync can restore alerts from the server (e.g. right after login), so refresh the log when it finishes
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(SyncManager.SYNC_WORK_NAME)
                .observe(this, infos -> {
                    for (WorkInfo info : infos) {
                        if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            loadAlertsLog();
                            return;
                        }
                    }
                });
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

        renderCachedWeather();
        refreshWeather(false);
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

    // Ask for notifications and location together so the user sees one prompt sequence, not two competing requests
    private void requestStartupPermissions() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!hasLocationPermission()) {
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            markLocationAsked();
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), STARTUP_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != STARTUP_PERMISSIONS_CODE && requestCode != LOCATION_PERMISSION_CODE) return;

        if (hasLocationPermission()) {
            refreshWeather(true);
        } else {
            showLocationOff();
        }
    }

    // ---- Location + weather ----

    private void setupWeatherViews() {
        weatherCache = new WeatherCache(this);
        tvLocation = findViewById(R.id.tv_location);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvWeatherSummary = findViewById(R.id.tv_weather_summary);
        ivWeatherIcon = findViewById(R.id.iv_weather_icon);

        // Tap the location to enable it, or to refresh the weather once enabled
        tvLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                refreshWeather(true);
            } else {
                requestLocationPermission();
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void markLocationAsked() {
        getPreferences(MODE_PRIVATE).edit().putBoolean(KEY_LOCATION_ASKED, true).apply();
    }

    private void requestLocationPermission() {
        boolean askedBefore = getPreferences(MODE_PRIVATE).getBoolean(KEY_LOCATION_ASKED, false);
        boolean canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (askedBefore && !canAskAgain) {
            // "Don't ask again": the system won't show the prompt, so send the user to app settings
            Toast.makeText(this, "Allow location in Permissions to see local weather", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
            return;
        }

        markLocationAsked();
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_CODE);
    }

    private void refreshWeather(boolean force) {
        if (!hasLocationPermission()) {
            showLocationOff();
            return;
        }
        if (isRefreshingWeather) return;
        if (!force && !weatherCache.isOlderThan(WEATHER_MAX_AGE_MS)) return;

        isRefreshingWeather = true;
        if (!weatherCache.hasData()) {
            tvLocation.setText("Locating…");
        }

        LocationHelper.getLocation(this, location -> {
            if (isFinishing() || isDestroyed()) {
                isRefreshingWeather = false;
                return;
            }
            if (location == null) {
                isRefreshingWeather = false;
                if (!weatherCache.hasData()) {
                    tvLocation.setText("Location unavailable · tap to retry");
                    showWeatherUnavailable();
                }
                return;
            }

            double lat = location.getLatitude();
            double lon = location.getLongitude();
            try {
                dbExecutor.execute(() -> {
                    String place = LocationHelper.placeName(this, lat, lon);
                    runOnUiThread(() -> fetchWeather(place, lat, lon));
                });
            } catch (RejectedExecutionException e) {
                isRefreshingWeather = false;
            }
        });
    }

    private void fetchWeather(String place, double lat, double lon) {
        WeatherService.get()
                .getCurrentWeather(lat, lon, WeatherService.CURRENT_FIELDS, "auto")
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                        isRefreshingWeather = false;
                        if (isFinishing() || isDestroyed()) return;

                        WeatherResponse body = response.body();
                        if (response.isSuccessful() && body != null && body.getCurrent() != null) {
                            weatherCache.save(place, body.getCurrent());
                            renderCachedWeather();
                        } else {
                            onWeatherFailed(place);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                        isRefreshingWeather = false;
                        if (isFinishing() || isDestroyed()) return;
                        onWeatherFailed(place);
                    }
                });
    }

    // Offline or service error: keep showing the last known conditions if there are any
    private void onWeatherFailed(String place) {
        if (!weatherCache.hasData()) {
            tvLocation.setText(place);
            showWeatherUnavailable();
        }
    }

    private void renderCachedWeather() {
        if (!weatherCache.hasData()) return;

        int code = weatherCache.getWeatherCode();
        tvLocation.setText(weatherCache.getPlace());
        tvTemperature.setText(String.format(Locale.getDefault(), "%d°C", Math.round(weatherCache.getTemperature())));
        tvWeatherSummary.setText(String.format(Locale.getDefault(), "%s · %d%% humidity",
                WeatherCodes.label(code, weatherCache.isDay()), weatherCache.getHumidity()));
        setWeatherIcon(WeatherCodes.iconRes(code), WeatherCodes.iconTintRes(code));
    }

    private void showLocationOff() {
        tvLocation.setText("Location off · tap to enable");
        if (!weatherCache.hasData()) {
            tvTemperature.setText("--°C");
            tvWeatherSummary.setText("Enable location for local weather");
            setWeatherIcon(R.drawable.cloud, 0);
        }
    }

    private void showWeatherUnavailable() {
        tvTemperature.setText("--°C");
        tvWeatherSummary.setText("Weather unavailable");
        setWeatherIcon(R.drawable.cloud, 0);
    }

    private void setWeatherIcon(int iconRes, int tintRes) {
        ivWeatherIcon.setImageResource(iconRes);
        ivWeatherIcon.setImageTintList(tintRes == 0
                ? null
                : ColorStateList.valueOf(ContextCompat.getColor(this, tintRes)));
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
