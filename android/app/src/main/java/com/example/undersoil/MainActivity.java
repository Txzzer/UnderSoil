package com.example.undersoil;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import network.ApiClient;
import network.ApiService;
import network.ValveRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    Button testApiButton;
    boolean isValveOpen = false; // Tracks the current state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testApiButton = findViewById(R.id.testApiButton);
        testApiButton.setText("Turn Valve ON");

        testApiButton.setOnClickListener(view -> {
            testApiButton.setEnabled(false);

            // Flip the state
            isValveOpen = !isValveOpen;
            ValveRequest request = new ValveRequest(isValveOpen);

            ApiService api = ApiClient.getClient().create(ApiService.class);

            api.toggleValve(request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    testApiButton.setEnabled(true);

                    if (response.isSuccessful()) {
                        // Update button text for the next click
                        testApiButton.setText(isValveOpen ? "Turn Valve OFF" : "Turn Valve ON");
                        String state = isValveOpen ? "OPEN" : "CLOSED";
                        Toast.makeText(MainActivity.this, "Success! Valve is " + state, Toast.LENGTH_SHORT).show();
                    } else {
                        // Revert state on server error
                        isValveOpen = !isValveOpen;
                        Toast.makeText(MainActivity.this, "Server Error: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    testApiButton.setEnabled(true);
                    // Revert state on network failure
                    isValveOpen = !isValveOpen;
                    Toast.makeText(MainActivity.this, "Failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}