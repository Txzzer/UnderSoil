package com.example.undersoil;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import network.ApiClient;
import network.ApiResponse;
import network.ApiService;
import network.TelemetryReading;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button testApiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testApiButton = findViewById(R.id.testApiButton);

        testApiButton.setOnClickListener(view -> {

            TelemetryReading reading = new TelemetryReading(
                    "Android_Test",
                    42.5,
                    true,
                    "2026-08-03T15:00:00"
            );

            ApiService api = ApiClient.getClient()
                    .create(ApiService.class);

            api.sendTelemetry(reading).enqueue(new Callback<ApiResponse>() {

//                @Override
//                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
//
//                    Toast.makeText(
//                            MainActivity.this,
//                            "Success",
//                            Toast.LENGTH_SHORT
//                    ).show();
//
//                }

                @Override
                public void onResponse(Call<ApiResponse> call,
                                       Response<ApiResponse> response) {

                    if (response.isSuccessful() && response.body() != null) {

                        Toast.makeText(
                                MainActivity.this,
                                response.body().getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                MainActivity.this,
                                "Server Error: " + response.code(),
                                Toast.LENGTH_LONG
                        ).show();

                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {

                    Toast.makeText(
                            MainActivity.this,
                            "Failed: " + t.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                }

            });

        });
    }
}