package com.example.undersoil;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import network.ApiClient;
import network.ApiService;
import network.TelemetryReading;
import network.ValveRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    Button testApiButton;
    TextView flowrateTextView;
    TextView alertBanner;

    boolean isValveOpen = false;

    // Handler for the 3-second auto-refresh loop
    private Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Link UI elements
        testApiButton = findViewById(R.id.testApiButton);
        flowrateTextView = findViewById(R.id.flowrate);
        alertBanner = findViewById(R.id.alertBanner);

        testApiButton.setText("Turn Valve ON");

        // 2. Button Click Listener (Sends Command)
        testApiButton.setOnClickListener(view -> {
            testApiButton.setEnabled(false);
            isValveOpen = !isValveOpen;

            // Sending the command with a 30-second hardware auto-shutoff
            ValveRequest request = new ValveRequest(isValveOpen, 30);

            ApiService api = ApiClient.getClient().create(ApiService.class);
            api.toggleValve(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    testApiButton.setEnabled(true);
                    if (response.isSuccessful()) {
                        testApiButton.setText(isValveOpen ? "Turn Valve OFF" : "Turn Valve ON");
                    } else {
                        isValveOpen = !isValveOpen; // Revert if failed
                        Toast.makeText(MainActivity.this, "Server rejected command", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    testApiButton.setEnabled(true);
                    isValveOpen = !isValveOpen; // Revert if failed
                    Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 3. Setup the background data fetch loop
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLatestSensorData();
                autoRefreshHandler.postDelayed(this, 3000); // 3000ms = 3 seconds
            }
        };

        // Start the loop
        autoRefreshHandler.post(refreshRunnable);
    }

    // 4. GET the data and evaluate the state
    private void fetchLatestSensorData() {
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.getLatestTelemetry().enqueue(new Callback<TelemetryReading>() {
            @Override
            public void onResponse(Call<TelemetryReading> call, Response<TelemetryReading> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TelemetryReading data = response.body();

                    // Sync the button state with the physical hardware
                    isValveOpen = data.valveIsOpen;
                    testApiButton.setText(isValveOpen ? "Turn Valve OFF" : "Turn Valve ON");

                    // Format the normal display text
                    String valveStatus = data.valveIsOpen ? "OPEN" : "CLOSED";
                    String displayData = "Valve Status: " + valveStatus + "\n\n" +
                            "Flow Rate: " + data.flowRateLitersPerMin + " L/min\n" +
                            "Temp: " + data.temperatureCelsius + "°C\n" +
                            "Humidity: " + data.humidityPercentage + "%";

                    // THE ANOMALY DETECTOR
                    if (!data.valveIsOpen && data.flowRateLitersPerMin > 0.5) {
                        // Valve is closed but water is flowing: SHOW ALARM
                        alertBanner.setVisibility(View.VISIBLE);
                        flowrateTextView.setTextColor(Color.RED);
                    } else {
                        // Everything is normal: HIDE ALARM
                        alertBanner.setVisibility(View.GONE);
                        flowrateTextView.setTextColor(Color.DKGRAY);
                    }

                    // Update the screen
                    flowrateTextView.setText(displayData);
                }
            }

            @Override
            public void onFailure(Call<TelemetryReading> call, Throwable t) {
                // Silently fail so the user isn't spammed with error toasts every 3 seconds
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks by stopping the timer when the app closes
        autoRefreshHandler.removeCallbacks(refreshRunnable);
    }
}